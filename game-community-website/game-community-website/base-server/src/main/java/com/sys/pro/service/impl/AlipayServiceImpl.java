package com.sys.pro.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.sys.pro.config.AliPaymentConfig;
import com.sys.pro.service.AlipayService;
import com.sys.pro.utils.CacheKeys;
import com.sys.pro.utils.RedisDistributedLock;
import com.sys.pro.web.ServiceException;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 支付宝支付服务实现，处理预下单、扫码状态查询、回调验签和退款请求。
 */
@Slf4j

@Service
public class AlipayServiceImpl implements AlipayService {

    private static final long QR_READY_CACHE_SECONDS = 30 * 60L;
    private static final long QR_PENDING_CACHE_SECONDS = 90L;
    private static final String QR_STATUS_PENDING = "PENDING";
    private static final String QR_STATUS_READY = "READY";
    private static final String QR_STATUS_FAILED = "FAILED";
    private static final int PRECREATE_MAX_ATTEMPTS = 2;
    private static final long PRECREATE_RETRY_INTERVAL_MS = 500L;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AliPaymentConfig aliPaymentConfig;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    private final ExecutorService paymentQrExecutor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "alipay-qr-precreate");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 完成支付宝支付中的 alipayNotifyHandel 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param params params 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付处理后的文本结果。
     */
    @Override
    public String alipayNotifyHandel(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "failure";
        }
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                    params,
                    aliPaymentConfig.getAlipayPublicKey(),
                    AlipayConstants.CHARSET_UTF8,
                    AlipayConstants.SIGN_TYPE_RSA2);
            if (!verified) {
                log.warn("支付宝异步通知验签失败 orderNo:{}", params.get("out_trade_no"));
                return "failure";
            }
            String appId = params.get("app_id");
            if (StringUtils.hasText(appId) && !appId.equals(aliPaymentConfig.getAppid())) {
                log.warn("支付宝异步通知 appId 不匹配 orderNo:{}, appId:{}", params.get("out_trade_no"), appId);
                return "failure";
            }
            String sellerId = params.get("seller_id");
            if (StringUtils.hasText(sellerId) && !sellerId.equals(aliPaymentConfig.getSellerId())) {
                log.warn("支付宝异步通知 sellerId 不匹配 orderNo:{}, sellerId:{}", params.get("out_trade_no"), sellerId);
                return "failure";
            }
            return "success";
        } catch (AlipayApiException e) {
            log.warn("支付宝异步通知验签异常 orderNo:{}", params.get("out_trade_no"), e);
            return "failure";
        }
    }

    /**
     * 完成支付宝支付中的 tradePagePay 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param totalAmount totalAmount 字段，来源于当前接口入参或内部调用上下文。
     * @param subject subject 字段，来源于当前接口入参或内部调用上下文。
     * @param returnUrl returnUrl 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, String> tradePagePay(String orderNo, String totalAmount, String subject, String returnUrl) {
        log.info("支付宝统一下单收单调用 orderNo:{},totalAmount:{},subject:{},returnUrl:{}", orderNo, totalAmount, subject, returnUrl);
        try {
            String formattedAmount = formatAmount(totalAmount);
            String resolvedReturnUrl = resolveReturnUrl(returnUrl);

            //拉起支付宝支付
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            //设置请求参数
            model.setOutTradeNo(orderNo);
            model.setTotalAmount(formattedAmount);
            model.setSubject(subject);
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            request.setBizModel(model);
            if (isHttpUrl(aliPaymentConfig.getNotifyUrl())) {
                request.setNotifyUrl(aliPaymentConfig.getNotifyUrl().trim());
            }
            if (StringUtils.hasText(resolvedReturnUrl)) {
                request.setReturnUrl(resolvedReturnUrl);
            }
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                Map<String, String> respParams = new HashMap<>(4);
                respParams.put("resp_code", response.getCode());
                respParams.put("msg", response.getMsg());
                respParams.put("sub_msg", response.getSubMsg());
                respParams.put("body", response.getBody());
                log.info("支付宝统一下单收单调用 SUCCESS orderNo:{}", orderNo);
                return respParams;
            } else {
                log.info("支付宝统一下单收单调用 FAIL orderNo:{}", orderNo);
                /**
                 * 完成支付宝支付中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
                 * @return 支付宝支付在该步骤产出的业务结果。
                 */
                throw new ServiceException(600, "确认订单失败");
            }
        } catch (AlipayApiException e) {
            log.error("tradePagePay FAIL...", e);
            /**
             * 完成支付宝支付中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 支付宝支付在该步骤产出的业务结果。
             */
            throw new ServiceException(600, "确认订单失败");
        }
    }

    /**
     * 完成支付宝支付中的 tradePrecreate 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param totalAmount totalAmount 字段，来源于当前接口入参或内部调用上下文。
     * @param subject subject 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, String> tradePrecreate(String orderNo, String totalAmount, String subject) {
        Map<String, String> cached = readPaymentQrCache(orderNo);
        if (QR_STATUS_READY.equals(cached == null ? null : cached.get("qrStatus"))) {
            return cached;
        }
        Map<String, String> result = executeTradePrecreate(orderNo, totalAmount, subject);
        writePaymentQrCache(orderNo, result, QR_READY_CACHE_SECONDS);
        return result;
    }

    /**
     * 完成支付宝支付中的 prepareTradePrecreate 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param totalAmount totalAmount 字段，来源于当前接口入参或内部调用上下文。
     * @param subject subject 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, String> prepareTradePrecreate(String orderNo, String totalAmount, String subject) {
        String formattedAmount = formatAmount(totalAmount);
        Map<String, String> cached = readPaymentQrCache(orderNo);
        if (cached != null && !QR_STATUS_FAILED.equals(cached.get("qrStatus"))) {
            return cached;
        }

        String lockKey = "payment:qr:prepare:" + orderNo;
        String token = redisDistributedLock.tryLock(lockKey, Duration.ofSeconds(10));
        if (token == null) {
            Map<String, String> latest = readPaymentQrCache(orderNo);
            return latest == null
                    ? basePaymentResult(orderNo, formattedAmount, subject, QR_STATUS_PENDING, "二维码生成中")
                    : latest;
        }
        try {
            cached = readPaymentQrCache(orderNo);
            if (cached != null && !QR_STATUS_FAILED.equals(cached.get("qrStatus"))) {
                return cached;
            }

            Map<String, String> pending = basePaymentResult(orderNo, formattedAmount, subject, QR_STATUS_PENDING, "二维码生成中");
            writePaymentQrCache(orderNo, pending, QR_PENDING_CACHE_SECONDS);
            Future<Map<String, String>> future = paymentQrExecutor.submit(() -> {
                try {
                    Map<String, String> ready = executeTradePrecreateWithRetry(orderNo, formattedAmount, subject);
                    writePaymentQrCache(orderNo, ready, QR_READY_CACHE_SECONDS);
                    return ready;
                } catch (RuntimeException e) {
                    Map<String, String> failed = basePaymentResult(orderNo, formattedAmount, subject, QR_STATUS_FAILED, e.getMessage());
                    writePaymentQrCache(orderNo, failed, 60L);
                    return failed;
                }
            });
            try {
                return future.get(1800, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                return pending;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return pending;
            } catch (ExecutionException e) {
                Map<String, String> latest = readPaymentQrCache(orderNo);
                return latest == null ? pending : latest;
            }
        } finally {
            redisDistributedLock.unlock(lockKey, token);
        }
    }

    /**
     * 完成支付宝支付中的 executeTradePrecreateWithRetry 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param totalAmount totalAmount 字段，来源于当前接口入参或内部调用上下文。
     * @param subject subject 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, String> executeTradePrecreateWithRetry(String orderNo, String totalAmount, String subject) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= PRECREATE_MAX_ATTEMPTS; attempt++) {
            try {
                return executeTradePrecreate(orderNo, totalAmount, subject);
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt < PRECREATE_MAX_ATTEMPTS && isTimeoutMessage(e.getMessage())) {
                    log.warn("支付宝扫码预下单超时，准备重试 orderNo:{}, attempt:{}", orderNo, attempt + 1);
                    sleepBeforeRetry();
                    continue;
                }
                throw e;
            }
        }
        throw lastException == null ? new ServiceException(600, "支付宝沙箱预下单失败") : lastException;
    }

    /**
     * 完成支付宝支付中的 executeTradePrecreate 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param totalAmount totalAmount 字段，来源于当前接口入参或内部调用上下文。
     * @param subject subject 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, String> executeTradePrecreate(String orderNo, String totalAmount, String subject) {
        log.info("支付宝扫码预下单调用 orderNo:{},totalAmount:{},subject:{}", orderNo, totalAmount, subject);
        try {
            String formattedAmount = formatAmount(totalAmount);
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            model.setOutTradeNo(orderNo);
            model.setTotalAmount(formattedAmount);
            model.setSubject(subject);
            model.setTimeoutExpress("30m");
            request.setBizModel(model);
            if (isHttpUrl(aliPaymentConfig.getNotifyUrl())) {
                request.setNotifyUrl(aliPaymentConfig.getNotifyUrl().trim());
            }

            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (response.isSuccess() && StringUtils.hasText(response.getQrCode())) {
                Map<String, String> respParams = basePaymentResult(orderNo, formattedAmount, subject, QR_STATUS_READY, "二维码已生成");
                respParams.put("resp_code", response.getCode());
                respParams.put("msg", response.getMsg());
                respParams.put("sub_msg", response.getSubMsg());
                respParams.put("qrCode", response.getQrCode());
                respParams.put("qrImage", generateQrImage(response.getQrCode()));
                log.info("支付宝扫码预下单调用 SUCCESS orderNo:{}", orderNo);
                return respParams;
            }
            log.warn("支付宝扫码预下单调用 FAIL orderNo:{}, code:{}, msg:{}, subCode:{}, subMsg:{}",
                    orderNo, response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
            /**
             * 完成支付宝支付中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 支付宝支付在该步骤产出的业务结果。
             */
            throw new ServiceException(600, "支付宝沙箱预下单失败：" + errorMessage(response.getMsg(), response.getSubMsg()));
        } catch (AlipayApiException e) {
            if (isTimeoutException(e)) {
                log.warn("tradePrecreate TIMEOUT orderNo:{}, reason:{}", orderNo, e.getMessage());
                /**
                 * 完成支付宝支付中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
                 * @return 支付宝支付在该步骤产出的业务结果。
                 */
                throw new ServiceException(600, "支付宝沙箱响应超时，正在重试");
            }
            log.error("tradePrecreate FAIL orderNo:{}", orderNo, e);
            /**
             * 完成支付宝支付中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 支付宝支付在该步骤产出的业务结果。
             */
            throw new ServiceException(600, "支付宝沙箱预下单失败，请检查APPID、应用私钥和支付宝公钥配置");
        }
    }

    /**
     * 判断支付宝支付当前状态是否满足业务条件。
     * @param throwable throwable 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示支付宝支付当前状态满足业务判断。
     */
    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return isTimeoutMessage(throwable == null ? null : throwable.getMessage());
    }

    /**
     * 判断支付宝支付当前状态是否满足业务条件。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @return true 表示支付宝支付当前状态满足业务判断。
     */
    private boolean isTimeoutMessage(String message) {
        return StringUtils.hasText(message) && (message.contains("超时") || message.contains("timed out") || message.contains("Read timed out"));
    }

    /**
     * 完成支付宝支付中的 sleepBeforeRetry 步骤，保证该环节的数据和状态可以继续向下流转。
     */
    private void sleepBeforeRetry() {
        try {
            TimeUnit.MILLISECONDS.sleep(PRECREATE_RETRY_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 完成支付宝支付中的 readPaymentQrCache 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return 支付宝支付聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, String> readPaymentQrCache(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        try {
            Object cached = redisTemplate.opsForValue().get(CacheKeys.paymentQr(orderNo));
            if (cached instanceof Map) {
                Map<String, String> result = new HashMap<>();
                ((Map<?, ?>) cached).forEach((key, value) -> result.put(String.valueOf(key), value == null ? null : String.valueOf(value)));
                return result;
            }
        } catch (Exception e) {
            log.warn("读取支付二维码缓存失败 orderNo:{}", orderNo, e);
        }
        return null;
    }

    /**
     * 完成支付宝支付中的 writePaymentQrCache 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @param ttlSeconds ttlSeconds 字段，来源于当前接口入参或内部调用上下文。
     */
    private void writePaymentQrCache(String orderNo, Map<String, String> value, long ttlSeconds) {
        if (!StringUtils.hasText(orderNo) || value == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(CacheKeys.paymentQr(orderNo), value, Math.max(1L, ttlSeconds), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入支付二维码缓存失败 orderNo:{}", orderNo, e);
        }
    }

    /**
     * 完成支付宝支付中的 basePaymentResult 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param amount amount 字段，来源于当前接口入参或内部调用上下文。
     * @param subject subject 字段，来源于当前接口入参或内部调用上下文。
     * @param qrStatus qrStatus 字段，来源于当前接口入参或内部调用上下文。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @return 支付宝支付聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, String> basePaymentResult(String orderNo, String amount, String subject, String qrStatus, String message) {
        Map<String, String> result = new HashMap<>(10);
        result.put("paymentType", "ALIPAY_QR");
        result.put("orderNo", orderNo);
        result.put("amount", amount);
        result.put("subject", subject);
        result.put("qrStatus", qrStatus);
        result.put("message", message);
        result.put("expireSeconds", String.valueOf(30 * 60));
        return result;
    }

    /**
     * 完成支付宝支付中的 generateQrImage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param qrCode qrCode 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付处理后的文本结果。
     */
    private String generateQrImage(String qrCode) {
        if (!StringUtils.hasText(qrCode)) {
            return "";
        }
        try {
            QrConfig config = new QrConfig(240, 240);
            String base64 = QrCodeUtil.generateAsBase64(qrCode, config, "png");
            return base64.startsWith("data:") ? base64 : "data:image/png;base64," + base64;
        } catch (Exception e) {
            log.warn("生成本地支付二维码图片失败，降级为前端二维码内容渲染");
            return "";
        }
    }

    /**
     * 关闭操作日志线程池，保证应用停止时不遗留后台线程。
     */
    @PreDestroy
    public void shutdown() {
        paymentQrExecutor.shutdown();
    }

    /**
     * 完成支付宝支付中的 tradeQuery 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return 支付宝支付聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> tradeQuery(String orderNo) {
        Map<String, Object> result = new HashMap<>(8);
        result.put("orderNo", orderNo);
        result.put("paid", false);
        if (!StringUtils.hasText(orderNo)) {
            result.put("message", "订单号不能为空");
            return result;
        }

        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);
            AlipayTradeQueryResponse response = alipayClient.execute(request);

            result.put("code", response.getCode());
            result.put("msg", response.getMsg());
            result.put("subCode", response.getSubCode());
            result.put("subMsg", response.getSubMsg());
            result.put("tradeStatus", response.getTradeStatus());
            result.put("totalAmount", response.getTotalAmount());
            boolean paid = "TRADE_SUCCESS".equals(response.getTradeStatus())
                    || "TRADE_FINISHED".equals(response.getTradeStatus());
            result.put("paid", paid);
            if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                result.put("tradeStatus", "WAIT_BUYER_PAY");
                result.put("message", "等待扫码支付");
            } else {
                result.put("message", paid ? "支付成功" : errorMessage(response.getMsg(), response.getSubMsg()));
            }
        } catch (AlipayApiException e) {
            log.warn("tradeQuery FAIL orderNo:{}", orderNo, e);
            result.put("message", "支付宝沙箱订单查询失败");
        }
        return result;
    }

    /**
     * 完成支付宝支付中的 tradeRefund 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param totalAmount totalAmount 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示支付宝支付当前状态满足业务判断。
     */
    @Override
    public boolean tradeRefund(String orderNo, String totalAmount) {
        log.info("鏀粯瀹濋€€娆捐皟鐢? orderNo:{},totalAmount:{}", orderNo, totalAmount);
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setOutTradeNo(orderNo);
            model.setRefundAmount(formatAmount(totalAmount));
            model.setRefundReason("游戏退款");
            model.setOutRequestNo(orderNo + "-" + System.currentTimeMillis());
            request.setBizModel(model);
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("鏀粯瀹濋€€娆炬垚鍔? orderNo:{}", orderNo);
                return true;
            }
            log.warn("鏀粯瀹濋€€娆惧け璐? orderNo:{}, code:{}, msg:{}, subMsg:{}",
                    orderNo, response.getCode(), response.getMsg(), response.getSubMsg());
            return false;
        } catch (AlipayApiException e) {
            log.error("tradeRefund FAIL orderNo:{}", orderNo, e);
            return false;
        }
    }

    /**
     * 完成支付宝支付中的 formatAmount 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param amount amount 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付处理后的文本结果。
     */
    private String formatAmount(String amount) {
        try {
            BigDecimal value = new BigDecimal(amount);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                /**
                 * 完成支付宝支付中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
                 * @return 支付宝支付在该步骤产出的业务结果。
                 */
                throw new ServiceException(600, "支付金额必须大于0");
            }
            return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            /**
             * 完成支付宝支付中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 支付宝支付在该步骤产出的业务结果。
             */
            throw new ServiceException(600, "支付金额格式不正确");
        }
    }

    /**
     * 完成支付宝支付中的 resolveReturnUrl 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param returnUrl returnUrl 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付处理后的文本结果。
     */
    private String resolveReturnUrl(String returnUrl) {
        if (isHttpUrl(returnUrl)) {
            return returnUrl.trim();
        }
        if (isHttpUrl(aliPaymentConfig.getReturnUrl())) {
            return aliPaymentConfig.getReturnUrl().trim();
        }
        return null;
    }

    /**
     * 判断支付宝支付当前状态是否满足业务条件。
     * @param url url 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示支付宝支付当前状态满足业务判断。
     */
    private boolean isHttpUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String value = url.trim().toLowerCase();
        return value.startsWith("http://") || value.startsWith("https://");
    }

    /**
     * 完成支付宝支付中的 errorMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param msg msg 字段，来源于当前接口入参或内部调用上下文。
     * @param subMsg subMsg 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付处理后的文本结果。
     */
    private String errorMessage(String msg, String subMsg) {
        if (StringUtils.hasText(subMsg)) {
            return subMsg;
        }
        if (StringUtils.hasText(msg)) {
            return msg;
        }
        return "支付宝沙箱返回失败";
    }
}
