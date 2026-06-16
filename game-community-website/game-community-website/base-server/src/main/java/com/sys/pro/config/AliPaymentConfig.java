package com.sys.pro.config;

import com.alipay.api.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝沙箱支付配置，创建扫码支付、查询和退款共用的 AlipayClient。
 */
@Data

@Configuration

@ConfigurationProperties(prefix = "alipay")
public class AliPaymentConfig {



    private String url;
    private String appid;
    private String sellerId;
    private String privateKey;
    private String alipayPublicKey;
    private String returnUrl;
    private String notifyUrl;
    private Integer connectTimeout = 3000;
    private Integer readTimeout = 8000;
    private Integer maxIdleConnections = 8;
    private Long keepAliveDuration = 60L;

    /**
     * 创建支付宝 SDK 客户端，集中读取沙箱网关、密钥和超时配置。
     * @return AliPayment在该步骤产出的业务结果。
     */
    @Bean
    public AlipayClient alipayClient() throws AlipayApiException {
        AlipayConfig alipayConfig = new AlipayConfig();
        //设置网关地址
        alipayConfig.setServerUrl(url);
        //设置应用APPID
        alipayConfig.setAppId(appid);
        //设置应用私钥
        alipayConfig.setPrivateKey(privateKey);
        //设置请求格式，固定值json
        alipayConfig.setFormat(AlipayConstants.FORMAT_JSON);
        //设置字符集
        alipayConfig.setCharset(AlipayConstants.CHARSET_UTF8);
        //设置支付宝公钥
        alipayConfig.setAlipayPublicKey(alipayPublicKey);
        //设置签名类型
        alipayConfig.setSignType(AlipayConstants.SIGN_TYPE_RSA2);
        // 沙箱偶发响应较慢，显式设置超时，避免单次预下单长时间卡住用户支付弹窗。
        alipayConfig.setConnectTimeout(connectTimeout);
        alipayConfig.setReadTimeout(readTimeout);
        alipayConfig.setMaxIdleConnections(maxIdleConnections);
        alipayConfig.setKeepAliveDuration(normalizeKeepAliveDuration());
        //构造client
        return new DefaultAlipayClient(alipayConfig);
    }

    /**
     * 限制支付宝 HTTP 连接存活时间，满足 SDK 最大 60 秒约束。
     * @return AliPayment统计值或主键结果。
     */
    private long normalizeKeepAliveDuration() {
        if (keepAliveDuration == null || keepAliveDuration <= 0) {
            return 60L;
        }
        return Math.min(keepAliveDuration, 60L);
    }
}
