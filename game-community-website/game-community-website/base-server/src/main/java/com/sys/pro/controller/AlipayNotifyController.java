package com.sys.pro.controller;

import com.sys.pro.service.OrderApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝回调接口。
 * 支付宝网关要求直接返回 success 或 failure，因此这里不包装 CommonResult。
 */
@RestController
@RequiredArgsConstructor
public class AlipayNotifyController {

    private final OrderApplicationService orderApplicationService;

    /**
     * 接收支付宝异步回调，校验后更新订单支付状态。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @return 支付宝支付处理后的文本结果。
     */
    @PostMapping("/alipay/notify")
    public String notify(HttpServletRequest request) {
        return orderApplicationService.handleAlipayNotify(readParams(request));
    }

    /**
     * 读取支付宝回调中的所有参数，交给支付服务做签名和状态校验。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @return 支付宝支付聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, String> readParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values == null || values.length == 0 ? "" : values[0]));
        return params;
    }
}
