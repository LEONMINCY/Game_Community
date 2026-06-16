package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sys.pro.dto.PasswordResetDTO;
import com.sys.pro.dto.PasswordResetSendCodeDTO;
import com.sys.pro.mapper.UserInfoMapper;
import com.sys.pro.pojo.User;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.repository.UserAccountRepository;
import com.sys.pro.service.PasswordResetService;
import com.sys.pro.utils.HashUtil;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱找回密码服务实现。
 * <p>
 * 通过账号绑定邮箱确认身份，验证码写入 Redis 并设置有效期，避免验证码长期留存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String CODE_KEY_PREFIX = "gc:password-reset:code:";
    private static final String RESEND_KEY_PREFIX = "gc:password-reset:resend:";
    private static final String DAILY_KEY_PREFIX = "gc:password-reset:daily:";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final UserAccountRepository accountRepository;
    private final UserInfoMapper userInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.host:smtp.qq.com}")
    private String mailHost;

    @Value("${spring.mail.port:465}")
    private int mailPort;

    @Value("${app.password-reset.code-ttl-minutes:10}")
    private long codeTtlMinutes;

    @Value("${app.password-reset.resend-seconds:60}")
    private long resendSeconds;

    @Value("${app.password-reset.daily-limit:10}")
    private long dailyLimit;

    /**
     * 校验账号和绑定邮箱，匹配后发送验证码。
     * <p>
     * 账号或邮箱不匹配时直接返回成功，避免恶意请求探测账号是否存在。
     *
     * @param request 前端提交的账号标识和绑定邮箱。
     */
    @Override
    public void sendCode(PasswordResetSendCodeDTO request) {
        String identifier = normalize(request == null ? null : request.getIdentifier());
        String email = normalizeEmail(request == null ? null : request.getEmail());
        validateIdentifierAndEmail(identifier, email);

        User user = accountRepository.findByIdentifier(identifier);
        UserInfo userInfo = user == null ? null : findUserInfo(user.getId());
        if (user == null || !emailMatches(userInfo, email)) {
            log.info("找回密码验证码未发送，账号或邮箱不匹配，identifier={}", mask(identifier));
            return;
        }

        checkMailConfiguration();
        checkSendLimit(email);

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String codeValue = email + "|" + code;
        String codeKey = codeKey(user.getId());
        stringRedisTemplate.opsForValue().set(codeKey, codeValue, codeTtlMinutes, TimeUnit.MINUTES);

        try {
            sendResetMail(email, code, codeTtlMinutes);
        } catch (RuntimeException ex) {
            stringRedisTemplate.delete(codeKey);
            throw ex;
        }
    }

    /**
     * 校验验证码、邮箱和新密码，校验通过后写入新的加密密码。
     *
     * @param request 前端提交的账号、邮箱、验证码和新密码。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(PasswordResetDTO request) {
        String identifier = normalize(request == null ? null : request.getIdentifier());
        String email = normalizeEmail(request == null ? null : request.getEmail());
        String code = normalize(request == null ? null : request.getCode());
        String newPassword = request == null ? null : request.getNewPassword();
        String confirmPassword = request == null ? null : request.getConfirmPassword();

        validateIdentifierAndEmail(identifier, email);
        validatePassword(newPassword, confirmPassword);
        if (StringUtils.isBlank(code)) {
            throw new ServiceException(500, "请输入邮箱验证码");
        }

        User user = accountRepository.findByIdentifier(identifier);
        UserInfo userInfo = user == null ? null : findUserInfo(user.getId());
        if (user == null || !emailMatches(userInfo, email)) {
            throw new ServiceException(500, "账号、邮箱或验证码不正确");
        }

        String codeKey = codeKey(user.getId());
        String cached = stringRedisTemplate.opsForValue().get(codeKey);
        if (!verificationCodeMatches(cached, email, code)) {
            throw new ServiceException(500, "验证码错误或已过期");
        }

        accountRepository.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        stringRedisTemplate.delete(codeKey);
    }

    /**
     * 读取用户资料表里的绑定邮箱，用于确认找回密码申请属于账号本人。
     *
     * @param userId 账号主键。
     * @return 用户资料；不存在时返回 null。
     */
    private UserInfo findUserInfo(Integer userId) {
        return userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getUserId, userId)
                .last("LIMIT 1"));
    }

    /**
     * 校验账号标识和邮箱格式，阻止明显无效的请求进入邮件发送流程。
     *
     * @param identifier 用户名、手机号或邮箱。
     * @param email 收件邮箱。
     */
    private void validateIdentifierAndEmail(String identifier, String email) {
        if (StringUtils.isBlank(identifier)) {
            throw new ServiceException(500, "请输入用户名、手机号或邮箱");
        }
        if (StringUtils.isBlank(email)) {
            throw new ServiceException(500, "请输入绑定邮箱");
        }
        if (!email.matches(EMAIL_PATTERN)) {
            throw new ServiceException(500, "邮箱格式不正确");
        }
    }

    /**
     * 校验新密码长度和二次确认，避免写入无法登录的新密码。
     *
     * @param newPassword 新密码。
     * @param confirmPassword 确认密码。
     */
    private void validatePassword(String newPassword, String confirmPassword) {
        if (StringUtils.isBlank(newPassword) || newPassword.length() < 6 || newPassword.length() > 32) {
            throw new ServiceException(500, "密码长度为6-32位");
        }
        if (!Objects.equals(newPassword, confirmPassword)) {
            throw new ServiceException(500, "两次输入的新密码不一致");
        }
    }

    /**
     * 判断用户资料中的绑定邮箱是否与本次请求邮箱一致。
     *
     * @param userInfo 用户资料。
     * @param email 本次请求邮箱。
     * @return true 表示邮箱匹配。
     */
    private boolean emailMatches(UserInfo userInfo, String email) {
        return userInfo != null && StringUtils.equalsIgnoreCase(normalizeEmail(userInfo.getEmail()), email);
    }

    /**
     * 校验邮件服务是否配置了发件账号，避免运行时静默发送失败。
     */
    private void checkMailConfiguration() {
        MailAccount mailAccount = resolveMailAccount();
        if (!mailAccount.isConfigured()) {
            throw new ServiceException(500, "邮箱服务未配置，请设置环境变量 MAIL_USERNAME、MAIL_PASSWORD，或填写 base-server/config/mail-local.properties");
        }
    }

    /**
     * 对验证码发送做频率限制和每日次数限制，降低邮箱轰炸风险。
     *
     * @param email 收件邮箱。
     */
    private void checkSendLimit(String email) {
        String emailHash = HashUtil.createMd5Hash(email.toLowerCase());
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(RESEND_KEY_PREFIX + emailHash, "1", resendSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new ServiceException(500, "验证码发送太频繁，请稍后再试");
        }

        String dailyKey = DAILY_KEY_PREFIX + LocalDate.now() + ":" + emailHash;
        Long current = stringRedisTemplate.opsForValue().increment(dailyKey);
        if (current != null && current == 1L) {
            stringRedisTemplate.expire(dailyKey, 1, TimeUnit.DAYS);
        }
        if (current != null && current > dailyLimit) {
            throw new ServiceException(500, "今日验证码发送次数已达上限");
        }
    }

    /**
     * 发送包含验证码和有效期的找回密码邮件。
     *
     * @param email 收件邮箱。
     * @param code 验证码。
     * @param ttlMinutes 有效分钟数。
     */
    private void sendResetMail(String email, String code, long ttlMinutes) {
        MailAccount mailAccount = resolveMailAccount();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailAccount.username);
        message.setTo(email);
        message.setSubject("游戏社区密码重置验证码");
        message.setText("您好，您正在重置游戏社区账号密码。\n\n验证码：" + code
                + "\n有效期：" + ttlMinutes + "分钟\n\n如果不是您本人操作，请忽略这封邮件。");
        try {
            buildMailSender(mailAccount).send(message);
        } catch (Exception ex) {
            log.warn("发送找回密码验证码失败，email={}", mask(email), ex);
            throw new ServiceException(500, "验证码邮件发送失败，请稍后再试");
        }
    }

    /**
     * 解析当前可用的邮箱账号配置；环境变量优先，本地开发配置文件兜底。
     *
     * @return 发送邮件所需的账号、授权码和 SMTP 地址。
     */
    private MailAccount resolveMailAccount() {
        String username = normalize(mailUsername);
        String password = normalize(mailPassword);
        String host = StringUtils.defaultIfBlank(normalize(mailHost), "smtp.qq.com");
        int port = mailPort <= 0 ? 465 : mailPort;

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            Properties localProperties = loadLocalMailProperties();
            username = StringUtils.defaultIfBlank(normalize(localProperties.getProperty("MAIL_USERNAME")), username);
            password = StringUtils.defaultIfBlank(normalize(localProperties.getProperty("MAIL_PASSWORD")), password);
            host = StringUtils.defaultIfBlank(normalize(localProperties.getProperty("MAIL_HOST")), host);
            port = parsePort(localProperties.getProperty("MAIL_PORT"), port);
        }
        return new MailAccount(host, port, username, password);
    }

    /**
     * 读取本地邮箱配置文件，兼容从项目根目录、后端目录或工作区根目录启动后端的场景。
     *
     * @return 本地配置文件中的邮件参数；文件不存在时返回空配置。
     */
    private Properties loadLocalMailProperties() {
        Properties properties = new Properties();
        Path[] candidates = new Path[]{
                Paths.get("config", "mail-local.properties"),
                Paths.get("base-server", "config", "mail-local.properties"),
                Paths.get("game-community-website", "base-server", "config", "mail-local.properties"),
                Paths.get("game-community-website", "game-community-website", "base-server", "config", "mail-local.properties")
        };
        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try (InputStream inputStream = Files.newInputStream(candidate)) {
                properties.load(inputStream);
                return properties;
            } catch (Exception ex) {
                log.warn("读取本地邮箱配置失败，path={}", candidate.toAbsolutePath(), ex);
                return properties;
            }
        }
        return properties;
    }

    /**
     * 根据当前邮箱账号创建 SMTP 发信组件。
     *
     * @param mailAccount 邮箱账号配置。
     * @return 可直接发送简单邮件的 JavaMailSender。
     */
    private JavaMailSenderImpl buildMailSender(MailAccount mailAccount) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailAccount.host);
        sender.setPort(mailAccount.port);
        sender.setUsername(mailAccount.username);
        sender.setPassword(mailAccount.password);
        sender.setDefaultEncoding("UTF-8");

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.port", String.valueOf(mailAccount.port));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    /**
     * 解析 SMTP 端口，配置错误时保留默认端口。
     *
     * @param value 配置中的端口文本。
     * @param defaultPort 默认端口。
     * @return 可用的端口号。
     */
    private int parsePort(String value, int defaultPort) {
        if (StringUtils.isBlank(value)) {
            return defaultPort;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultPort;
        }
    }

    /**
     * 比对 Redis 中保存的邮箱和验证码。
     *
     * @param cached Redis 中的验证码记录。
     * @param email 本次请求邮箱。
     * @param code 用户输入的验证码。
     * @return true 表示验证码有效且属于同一邮箱。
     */
    private boolean verificationCodeMatches(String cached, String email, String code) {
        if (StringUtils.isBlank(cached)) {
            return false;
        }
        String[] parts = cached.split("\\|", 2);
        return parts.length == 2
                && StringUtils.equalsIgnoreCase(parts[0], email)
                && StringUtils.equals(parts[1], code);
    }

    /**
     * 生成单个账号的验证码缓存键。
     *
     * @param userId 账号主键。
     * @return Redis 缓存键。
     */
    private String codeKey(Integer userId) {
        return CODE_KEY_PREFIX + userId;
    }

    /**
     * 去除输入两侧空白，统一空值处理。
     *
     * @param value 原始输入。
     * @return 清理后的文本。
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 规范化邮箱大小写，便于匹配和缓存。
     *
     * @param email 原始邮箱。
     * @return 小写邮箱。
     */
    private String normalizeEmail(String email) {
        return normalize(email).toLowerCase();
    }

    /**
     * 对日志中的账号或邮箱做脱敏，避免敏感信息进入日志。
     *
     * @param value 原始文本。
     * @return 脱敏后的文本。
     */
    private String mask(String value) {
        if (StringUtils.isBlank(value) || value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    /**
     * 邮箱账号配置值对象，只在发送验证码时临时使用，不会写入数据库。
     */
    private static class MailAccount {
        private final String host;
        private final int port;
        private final String username;
        private final String password;

        private MailAccount(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        /**
         * 判断 SMTP 账号是否已经具备发信所需的最小配置。
         *
         * @return true 表示可以尝试发送邮件。
         */
        private boolean isConfigured() {
            return StringUtils.isNotBlank(host)
                    && port > 0
                    && StringUtils.isNotBlank(username)
                    && StringUtils.isNotBlank(password);
        }
    }
}
