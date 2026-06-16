package com.sys.pro.service;

import com.sys.pro.pojo.Order;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * OrderService 定义订单支付业务能力，供控制器和其他服务组合调用。
 */
public interface OrderService extends IService<Order> {



    /**
     * 完成订单中的 createOrder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, String> createOrder(Order order);



    /**
     * 继续支付待支付订单，复用原订单号重新获取支付入口。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, String> continuePay(Order order);



    /**
     * 完成订单中的 cancelOrder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */

    void cancelOrder(Integer id);

}

