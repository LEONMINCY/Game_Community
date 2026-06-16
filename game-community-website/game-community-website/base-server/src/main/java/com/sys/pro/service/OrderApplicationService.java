package com.sys.pro.service;

import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.Order;
import com.sys.pro.vo.OrderVo;

import java.util.Map;

/**
 * 订单应用服务，负责订单购买、支付状态、退款审核等用例编排。
 */
public interface OrderApplicationService {
    /**
     * 根据主键读取订单详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 订单在该步骤产出的业务结果。
     */

    Order getById(Long id);
    /**
     * 接收新增订单数据，完成入口校验后交由业务层保存。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, String> create(Order order, Integer userId);
    /**
     * 继续支付待支付订单，复用原订单号重新获取支付入口。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, String> continuePay(Order order, Integer userId);
    /**
     * 获取订单扫码支付二维码和订单摘要信息。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, String> paymentQr(String orderNo, Integer userId);
    /**
     * 取消待支付订单，释放用户未完成的购买流程。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */

    void cancelPendingOrder(Integer id, Integer userId);
    /**
     * 发起或处理订单退款流程，保留审核所需的退款信息。
     * @param id 记录主键，用来定位本次要处理的数据。
     */

    void refund(Integer id);
    /**
     * 完成订单中的 handleAlipayReturn 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     */

    void handleAlipayReturn(String orderNo);
    /**
     * 完成订单中的 handleAlipayNotify 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param params params 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单处理后的文本结果。
     */

    String handleAlipayNotify(Map<String, String> params);
    /**
     * 完成订单中的 queryPayStatus 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> queryPayStatus(String orderNo);
    /**
     * 按主键删除订单记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     */

    void delete(Long id);
    /**
     * 完成订单中的 deleteMyOrder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */

    void deleteMyOrder(Long id, Integer userId);
    /**
     * 保存订单编辑后的内容，让前台展示和后台管理保持一致。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     */

    void update(Order order);
    /**
     * 分页查询订单数据，返回列表内容和总数信息。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @param forcedUserId forcedUser 主键，用来定位关联业务数据。
     * @return 订单分页结果，包含当前页数据和总数。
     */

    PageResult<Order> page(OrderVo dto, Integer forcedUserId);
    /**
     * 提交用户退款申请，等待后台管理员或审核员处理。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */

    void applyRefund(Integer id, OrderVo dto, Integer userId);
    /**
     * 审核退款申请并写入处理意见。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     */

    void reviewRefund(OrderVo dto);
}
