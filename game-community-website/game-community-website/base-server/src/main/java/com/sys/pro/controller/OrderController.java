package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.Order;
import com.sys.pro.service.OrderApplicationService;
import com.sys.pro.vo.OrderVo;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 订单接口层。
 * 只负责接收请求、传递当前用户上下文和包装返回结果，业务流转交给 OrderApplicationService。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/order")
public class OrderController extends BaseController {

    private final OrderApplicationService orderApplicationService;

    /**
     * 接收新增订单数据，完成入口校验后交由业务层保存。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "创建购买游戏订单")
    @PostMapping("/add")
    public CommonResult<Map<String, String>> create(@RequestBody Order order) {
        return CommonResult.success(orderApplicationService.create(order, getUserId()));
    }

    /**
     * 继续支付待支付订单，复用原订单号重新获取支付入口。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "继续付款")
    @PostMapping("/continuePay")
    public CommonResult<Map<String, String>> continuePay(@RequestBody Order order) {
        return CommonResult.success(orderApplicationService.continuePay(order, getUserId()));
    }

    /**
     * 获取订单扫码支付二维码和订单摘要信息。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取支付宝扫码支付二维码")
    @GetMapping("/pay/qr")
    public CommonResult<Map<String, String>> paymentQr(String orderNo) {
        return CommonResult.success(orderApplicationService.paymentQr(orderNo, getUserId()));
    }

    /**
     * 取消待支付订单，释放用户未完成的购买流程。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "取消待支付订单")
    @PutMapping("/cancel/{id}")
    public CommonResult<Void> cancelPendingOrder(@PathVariable Integer id) {
        orderApplicationService.cancelPendingOrder(id, getUserId());
        return CommonResult.success();
    }

    /**
     * 发起或处理订单退款流程，保留审核所需的退款信息。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "兼容旧退款接口")
    @PostMapping("/refund/{id}")
    public CommonResult<Void> refund(@PathVariable Integer id) {
        orderApplicationService.refund(id);
        return CommonResult.success();
    }

    /**
     * 处理支付完成后的跳转确认，更新订单和拥有游戏状态。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation("支付宝返回页面回调处理")
    @GetMapping("/pay")
    public CommonResult<Void> payEnd(String orderNo) {
        orderApplicationService.handleAlipayReturn(orderNo);
        return CommonResult.success();
    }

    /**
     * 查询订单支付状态，供支付弹窗轮询刷新。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation("查询支付宝沙箱支付状态")
    @GetMapping("/pay/status")
    public CommonResult<Map<String, Object>> payStatus(String orderNo) {
        return CommonResult.success(orderApplicationService.queryPayStatus(orderNo));
    }

    /**
     * 按主键删除订单记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除数据")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        orderApplicationService.delete(id);
        return CommonResult.success();
    }

    /**
     * 完成订单中的 deleteMyOrder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "用户删除自己的订单")
    @DeleteMapping("/my/delete/{id}")
    public CommonResult<Void> deleteMyOrder(@PathVariable("id") Long id) {
        orderApplicationService.deleteMyOrder(id, getUserId());
        return CommonResult.success();
    }

    /**
     * 保存订单编辑后的内容，让前台展示和后台管理保持一致。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "更新数据")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody Order order) {
        orderApplicationService.update(order);
        return CommonResult.success();
    }

    /**
     * 分页查询订单数据，返回列表内容和总数信息。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "分页查询订单")
    @PostMapping("/page")
    public CommonResult<PageResult<Order>> page(@RequestBody OrderVo dto) {
        return CommonResult.success(orderApplicationService.page(dto, null));
    }

    /**
     * 分页查询当前用户自己的订单数据，用于个人中心列表。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "分页查询当前用户订单")
    @PostMapping("/my/page")
    public CommonResult<PageResult<Order>> myPage(@RequestBody OrderVo dto) {
        return CommonResult.success(orderApplicationService.page(dto, getUserId()));
    }

    /**
     * 提交用户退款申请，等待后台管理员或审核员处理。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "申请退款")
    @PutMapping("/refund/apply/{id}")
    public CommonResult<Void> applyRefund(@PathVariable Integer id, @RequestBody OrderVo dto) {
        orderApplicationService.applyRefund(id, dto, getUserId());
        return CommonResult.success();
    }

    /**
     * 审核退款申请并写入处理意见。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "审核退款")
    @PutMapping("/refund/review")
    public CommonResult<Void> reviewRefund(@RequestBody OrderVo dto) {
        orderApplicationService.reviewRefund(dto);
        return CommonResult.success();
    }
}
