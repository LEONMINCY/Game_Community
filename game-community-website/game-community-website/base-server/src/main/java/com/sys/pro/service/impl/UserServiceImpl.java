package com.sys.pro.service.impl;

import com.sys.pro.common.PageResult;
import com.sys.pro.dto.UserPageDTO;
import com.sys.pro.pojo.User;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.repository.UserAccountRepository;
import com.sys.pro.security.JwtTokenProvider;
import com.sys.pro.service.UserInfoService;
import com.sys.pro.service.UserService;
import com.sys.pro.utils.HashUtil;
import com.sys.pro.vo.LoginParam;
import com.sys.pro.vo.LoginSuccessVo;
import com.sys.pro.vo.UserPageVo;
import com.sys.pro.vo.UserParam;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;

import static com.sys.pro.common.GlobalErrorCodeConstants.*;

/**
 * Account service. Accounts are split by role into three physical tables,
 * while the rest of the application keeps using the unified User DTO.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_AVATAR = "https://c-ssl.dtstatic.com/uploads/item/202106/29/20210629000609_JCBXJ.thumb.1000_0.jpeg";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final UserAccountRepository accountRepository;
    private final UserInfoService userInfoService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 校验用户名或手机号登录信息，成功后签发访问令牌。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return User在该步骤产出的业务结果。
     */
    @Override
    public LoginSuccessVo login(LoginParam param) {
        User user = accountRepository.findByIdentifier(param.getUsername());
        if (user == null) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param USER_NOT_EXIST USER_NOT_EXIST 字段，来源于当前接口入参或内部调用上下文。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(USER_NOT_EXIST);
        }
        if (!matchesPassword(param.getPassword(), user)) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param LOGIN_FAIL LOGIN_FAIL 字段，来源于当前接口入参或内部调用上下文。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(LOGIN_FAIL);
        }
        restoreIfBanExpired(user);
        if (!Boolean.TRUE.equals(user.getEnableFlag())) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param FORBIDDEN_LOGIN FORBIDDEN_LOGIN 字段，来源于当前接口入参或内部调用上下文。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(FORBIDDEN_LOGIN);
        }

        accountRepository.touchLastLogin(user.getId());
        UserInfo userInfo = userInfoService.getUserInfo(user.getId());
        String token = "Bearer " + jwtTokenProvider.createToken(userInfo);

        LoginSuccessVo successVo = new LoginSuccessVo();
        successVo.setToken(token);
        successVo.setUserId(Long.valueOf(user.getId()));
        successVo.setUsername(user.getUsername());
        successVo.setRoleId(user.getRoleId());
        return successVo;
    }

    /**
     * 注册普通用户账号，并写入手机号、性别和年龄等基础资料。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(User user) {
        validateUsernameAndPassword(user.getUsername(), user.getPassword());
        validateOptionalEmail(user.getEmail());
        if (accountRepository.existsUsername(user.getUsername())) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param USERNAME_EXIST USERNAME_EXIST 字段，来源于当前接口入参或内部调用上下文。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(USERNAME_EXIST);
        }
        user.setRoleId(UserAccountRepository.ROLE_PLAYER);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnableFlag(true);
        user.setDeleted(false);
        accountRepository.insert(user);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUserId(user.getId());
        userInfo.setAvatar(DEFAULT_AVATAR);
        userInfo.setNickname("普通用户");
        userInfo.setPhone(StringUtils.trimToNull(user.getPhone()));
        userInfo.setEmail(StringUtils.trimToNull(user.getEmail()));
        userInfo.setGender(StringUtils.trimToNull(user.getGender()));
        userInfo.setAge(user.getAge());
        userInfo.setEx1(0);
        userInfo.setEx2(0);
        userInfo.setDeleted(false);
        userInfoService.save(userInfo);
    }

    /**
     * 按用户名查询账号，支持登录和注册重复校验。
     * @param username username 字段，来源于当前接口入参或内部调用上下文。
     * @return User在该步骤产出的业务结果。
     */
    @Override
    public User findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    /**
     * 完成User中的 createAccount 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userParam userParam 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAccount(UserParam userParam) {
        String username = StringUtils.trim(userParam.getUsername());
        String password = StringUtils.defaultIfBlank(userParam.getNewPassword(), "123456");
        validateUsernameAndPassword(username, password);
        if (accountRepository.existsUsername(username)) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param USERNAME_EXIST USERNAME_EXIST 字段，来源于当前接口入参或内部调用上下文。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(USERNAME_EXIST);
        }
        Integer roleId = userParam.getRoleId() == null ? 2 : userParam.getRoleId();
        if (!Integer.valueOf(2).equals(roleId) && !Integer.valueOf(3).equals(roleId)) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "只能新增普通用户或社区审核员");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoleId(roleId.longValue());
        user.setEnableFlag(true);
        user.setDeleted(false);
        accountRepository.insert(user);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUserId(user.getId());
        userInfo.setNickname(StringUtils.defaultIfBlank(userParam.getNickname(), username));
        userInfo.setPhone(userParam.getPhone());
        userInfo.setAvatar(DEFAULT_AVATAR);
        userInfo.setEx1(0);
        userInfo.setEx2(0);
        userInfo.setDeleted(false);
        userInfoService.save(userInfo);
    }

    /**
     * 修改当前用户密码，校验旧密码后写入新哈希。
     * @param userParam userParam 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UserParam userParam) {
        User user = accountRepository.findById(userParam.getUserId());
        if (user == null) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param USER_NOT_EXIST USER_NOT_EXIST 字段，来源于当前接口入参或内部调用上下文。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(USER_NOT_EXIST);
        }
        if (!matchesRawPassword(userParam.getOldPassword(), user.getPassword())) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param PASSWORD_ERROR PASSWORD_ERROR 字段，来源于当前接口入参或内部调用上下文。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(PASSWORD_ERROR);
        }
        accountRepository.updatePassword(user.getId(), passwordEncoder.encode(userParam.getNewPassword()));
    }

    /**
     * 完成User中的 pageUser 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return User分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<UserPageVo> pageUser(UserPageDTO dto) {
        PageResult<UserPageVo> page = accountRepository.pageUsers(dto);
        page.getList().forEach(user -> user.setUserLevel(HashUtil.calculateLevel(user.getEx1())));
        return page;
    }

    /**
     * 后台重置指定用户密码，用于用户无法自行找回时处理。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    @Override
    public void resetPassword(Integer userId) {
        User user = accountRepository.findById(userId);
        if (user == null) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param USER_NOT_EXIST USER_NOT_EXIST 字段，来源于当前接口入参或内部调用上下文。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(USER_NOT_EXIST);
        }
        accountRepository.updatePassword(userId, passwordEncoder.encode("123456"));
    }

    /**
     * 切换账号启用状态，用于后台冻结或恢复用户登录。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void changeEnable(User user) {
        updateById(user);
    }

    /**
     * 调整账号角色，并在对应角色账号表之间迁移数据。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void changeRole(User user) {
        if (user.getId() == null || user.getRoleId() == null) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "用户和角色不能为空");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setRoleId(user.getRoleId());
        updateById(updateUser);
    }

    /**
     * 完成User中的 deleteAccount 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void deleteAccount(Long id) {
        removeById(id);
    }

    /**
     * 根据主键读取User详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return User在该步骤产出的业务结果。
     */
    @Override
    public User getById(Serializable id) {
        return accountRepository.findById(id);
    }

    /**
     * 保存User业务结果，并维护后续读取需要的一致状态。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示User当前状态满足业务判断。
     */
    @Override
    public boolean save(User user) {
        return accountRepository.insert(user);
    }

    /**
     * 按账号主键更新对应角色表中的账号信息。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示User当前状态满足业务判断。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(User user) {
        return accountRepository.updateById(user);
    }

    /**
     * 移除User中的指定数据，并同步清理关联展示状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return true 表示User当前状态满足业务判断。
     */
    @Override
    public boolean removeById(Serializable id) {
        return accountRepository.softDeleteById(id);
    }

    /**
     * 完成User中的 count 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return User统计值或主键结果。
     */
    @Override
    public int count() {
        long total = accountRepository.countAll();
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    /**
     * 统计指定时间之后新增的账号数量。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @return User统计值或主键结果。
     */
    @Override
    public long countCreatedAfter(LocalDateTime start) {
        return accountRepository.countCreatedAfter(start);
    }

    /**
     * 统计指定时间区间内新增的账号数量。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @param end end 字段，来源于当前接口入参或内部调用上下文。
     * @return User统计值或主键结果。
     */
    @Override
    public long countCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return accountRepository.countCreatedBetween(start, end);
    }

    /**
     * 统计最近登录过的账号数量，用于活跃度分析。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @return User统计值或主键结果。
     */
    @Override
    public long countLastLoginAfter(LocalDateTime start) {
        return accountRepository.countLastLoginAfter(start);
    }

    /**
     * 完成User中的 matchesPassword 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param rawPassword rawPassword 字段，来源于当前接口入参或内部调用上下文。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示User当前状态满足业务判断。
     */
    private boolean matchesPassword(String rawPassword, User user) {
        boolean matched = matchesRawPassword(rawPassword, user.getPassword());
        if (matched && isLegacyMd5(user.getPassword())) {
            accountRepository.updatePassword(user.getId(), passwordEncoder.encode(rawPassword));
        }
        return matched;
    }

    /**
     * 完成User中的 matchesRawPassword 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param rawPassword rawPassword 字段，来源于当前接口入参或内部调用上下文。
     * @param encodedPassword encodedPassword 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示User当前状态满足业务判断。
     */
    private boolean matchesRawPassword(String rawPassword, String encodedPassword) {
        if (StringUtils.isBlank(rawPassword) || StringUtils.isBlank(encodedPassword)) {
            return false;
        }
        try {
            if (passwordEncoder.matches(rawPassword, encodedPassword)) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
            // Non-Argon2 hashes continue to the legacy MD5 compatibility branch.
        }
        return isLegacyMd5(encodedPassword) && HashUtil.createMd5Hash(rawPassword).equalsIgnoreCase(encodedPassword);
    }

    /**
     * 判断User当前状态是否满足业务条件。
     * @param password password 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示User当前状态满足业务判断。
     */
    private boolean isLegacyMd5(String password) {
        return password != null && password.matches("(?i)^[a-f0-9]{32}$");
    }

    /**
     * 完成User中的 restoreIfBanExpired 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     */
    private void restoreIfBanExpired(User user) {
        if (Boolean.FALSE.equals(user.getEnableFlag())
                && user.getBanEndTime() != null
                && user.getBanEndTime().isBefore(LocalDateTime.now())) {
            User patch = new User();
            patch.setId(user.getId());
            patch.setEnableFlag(true);
            patch.setBanEndTime(null);
            accountRepository.updateById(patch);
            user.setEnableFlag(true);
            user.setBanEndTime(null);
        }
    }

    /**
     * 校验User操作前置条件，提前阻断不合法请求。
     * @param username username 字段，来源于当前接口入参或内部调用上下文。
     * @param password password 字段，来源于当前接口入参或内部调用上下文。
     */
    private void validateUsernameAndPassword(String username, String password) {
        if (StringUtils.isBlank(username) || username.length() < 4 || username.length() > 16) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "用户名长度为4-16位");
        }
        if (StringUtils.isBlank(password) || password.length() < 6 || password.length() > 32) {
            /**
             * 完成User中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return User在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "密码长度为6-32位");
        }
    }

    /**
     * 校验注册时可选邮箱的格式，避免无效邮箱写入资料后影响找回密码。
     * @param email 用户提交的邮箱地址，可以为空。
     */
    private void validateOptionalEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return;
        }
        if (!email.trim().matches(EMAIL_PATTERN)) {
            throw new ServiceException(500, "邮箱格式不正确");
        }
    }
}
