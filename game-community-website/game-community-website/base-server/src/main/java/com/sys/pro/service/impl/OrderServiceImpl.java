package com.sys.pro.service.impl;

import cn.hutool.Hutool;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.sys.pro.common.CommonResult;
import com.sys.pro.pojo.Order;
import com.sys.pro.service.AlipayService;
import com.sys.pro.service.OrderService;
import com.sys.pro.mapper.OrderMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单基础服务实现，封装订单表的创建、查询、状态变更和退款更新。
 */
@Slf4j

@Service

@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {



    private final OrderMapper orderMapper;



    private final AlipayService alipayService;



    /**
     * 完成订单中的 createOrder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, String> createOrder(Order order) {
        //生成订单号
        String orderNo = DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + System.currentTimeMillis();
        order.setOrderNo(orderNo);
        order.setRefundStatus("NONE");
        BigDecimal totalPrice = order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice();
        if (totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            // 免费游戏不需要生成支付宝二维码，直接落库为购买成功。
            order.setTotalPrice(BigDecimal.ZERO);
            order.setStatus("购买成功");
            orderMapper.insert(order);
            Map<String, String> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("status", "购买成功");
            result.put("paid", "true");
            result.put("free", "true");
            result.put("totalAmount", "0.00");
            return result;
        }
        //设为待付款
        order.setStatus("待支付");
        orderMapper.insert(order);
        // 异步生成支付宝沙箱扫码支付二维码，避免下单请求被沙箱网络耗时阻塞。
        String subject = order.getGameName() == null || order.getGameName().trim().isEmpty()
                ? "下单支付"
                : order.getGameName();
        return alipayService.prepareTradePrecreate(orderNo, order.getTotalPrice().toPlainString(), subject);
    }

    /**
     * 继续支付待支付订单，复用原订单号重新获取支付入口。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, String> continuePay(Order order) {
        String subject = order.getGameName() == null || order.getGameName().trim().isEmpty()
                ? "下单支付"
                : order.getGameName();
        return alipayService.prepareTradePrecreate(order.getOrderNo(), order.getTotalPrice().toPlainString(), subject);
    }

    /**
     * 完成订单中的 cancelOrder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void cancelOrder(Integer id) {
        Order order = orderMapper.selectById(id);
        order.setStatus("已取消");
        orderMapper.updateById(order);

        // 支付宝退款
        alipayService.tradeRefund(order.getOrderNo(), order.getTotalPrice().toPlainString());
    }
}
