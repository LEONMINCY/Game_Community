package com.sys.pro.service;

import com.sys.pro.common.CommonResult;

import java.util.Map;

/**
 * AlipayService 定义支付宝沙箱支付业务能力，供控制器和其他服务组合调用。
 */
public interface AlipayService {



    /**
     * 完成支付宝支付中的 alipayNotifyHandel 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param params params 字段，来源于当前接口入参或内部调用上下文。
     * @return 支付宝支付处理后的文本结果。
     */

    String alipayNotifyHandel(Map<String, String> params);



    /**

     * 描述：统一收单下单并支付页面接口

     *

     * @param orderNo     订单号

     * @param subject     主题

     * @param totalAmount 订单总金额

     * @return RespBean

     */

    default Map<String, String> tradePagePay(String orderNo, String totalAmount, String subject) {
        return tradePagePay(orderNo, totalAmount, subject, null);
    }

    default Map<String, String> tradePagePay(String orderNo, String totalAmount, String subject, String returnUrl) {
        return null;
    }

    default Map<String, String> tradePrecreate(String orderNo, String totalAmount, String subject) {
        return null;
    }

    default Map<String, String> prepareTradePrecreate(String orderNo, String totalAmount, String subject) {
        return tradePrecreate(orderNo, totalAmount, subject);
    }

    default Map<String, Object> tradeQuery(String orderNo) {
        return null;
    }

    /**
     * 描述：统一收单交易退款接口

     *

     * @param orderNo     订单号

     * @param totalAmount 订单总金额

     * @return 退款成功返回true

     */

    default boolean tradeRefund(String orderNo,String totalAmount) {

        return false;

    }

}

