package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.Game;
import com.sys.pro.pojo.Order;
import com.sys.pro.service.AlipayService;
import com.sys.pro.service.GameService;
import com.sys.pro.service.GameUserCommerceService;
import com.sys.pro.service.OrderApplicationService;
import com.sys.pro.service.OrderService;
import com.sys.pro.service.UserExperienceService;
import com.sys.pro.utils.CacheKeys;
import com.sys.pro.utils.RedisCacheService;
import com.sys.pro.utils.RedisDistributedLock;
import com.sys.pro.vo.OrderVo;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单业务编排实现。
 * 这里集中处理订单状态流转、支付分布式锁和缓存失效，Controller 不直接参与业务决策。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationServiceImpl implements OrderApplicationService {

    private static final Duration ORDER_LOCK_TTL = Duration.ofSeconds(30);

    private final OrderService orderService;
    private final GameService gameService;
    private final AlipayService alipayService;
    private final UserExperienceService userExperienceService;
    private final RedisDistributedLock redisDistributedLock;
    private final RedisCacheService redisCacheService;
    private final GameUserCommerceService gameUserCommerceService;


    /**
     * 根据主键读取订单详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 订单在该步骤产出的业务结果。
     */
    @Override
    public Order getById(Long id) {
        Order order = orderService.getById(id);
        fillGameName(order);
        return order;
    }

    /**
     * 接收新增订单数据，完成入口校验后交由业务层保存。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, String> create(Order order, Integer userId) {
        if (order == null || order.getGameId() == null) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "请选择要购买的游戏");
        }
        String lockKey = "order:create:" + userId + ":" + order.getGameId();
        String token = redisDistributedLock.tryLock(lockKey, ORDER_LOCK_TTL);
        if (token == null) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单正在提交，请稍后再试");
        }
        try {
            return createLocked(order, userId);
        } finally {
            redisDistributedLock.unlock(lockKey, token);
        }
    }

    /**
     * 继续支付待支付订单，复用原订单号重新获取支付入口。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, String> continuePay(Order order, Integer userId) {
        Order dbOrder = order == null || order.getId() == null ? null : orderService.getById(order.getId());
        if (dbOrder == null || !userId.equals(dbOrder.getUserId())) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单不存在");
        }
        if (!"待支付".equals(dbOrder.getStatus())) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "只有待支付订单可以继续支付");
        }
        if (dbOrder.getCreateTime() != null && dbOrder.getCreateTime().plusMinutes(30).isBefore(LocalDateTime.now())) {
            expirePendingOrderIfNeeded(dbOrder.getOrderNo());
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单已超时，请重新购买");
        }
        String lockKey = "order:continuePay:" + dbOrder.getOrderNo();
        String token = redisDistributedLock.tryLock(lockKey, ORDER_LOCK_TTL);
        if (token == null) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单正在拉起支付，请稍后再试");
        }
        try {
            return paymentQr(dbOrder.getOrderNo(), userId);
        } finally {
            redisDistributedLock.unlock(lockKey, token);
        }
    }

    /**
     * 获取订单扫码支付二维码和订单摘要信息。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, String> paymentQr(String orderNo, Integer userId) {
        if (!StringUtils.hasText(orderNo)) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单号不能为空");
        }
        List<Order> orders = orderService.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .and(wrapper -> wrapper.eq(Order::getDeleted, false).or().isNull(Order::getDeleted)));
        if (orders.isEmpty() || orders.stream().anyMatch(order -> !userId.equals(order.getUserId()))) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单不存在");
        }
        if (orders.stream().noneMatch(order -> "待支付".equals(order.getStatus()))) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "该订单当前不是待支付状态");
        }
        boolean expired = orders.stream()
                .filter(order -> "待支付".equals(order.getStatus()))
                .filter(order -> order.getCreateTime() != null)
                .anyMatch(order -> order.getCreateTime().plusMinutes(30).isBefore(LocalDateTime.now()));
        if (expired) {
            expirePendingOrderIfNeeded(orderNo);
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单已超时，请重新购买");
        }

        BigDecimal total = orders.stream()
                .filter(order -> "待支付".equals(order.getStatus()))
                .map(Order::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String subject = buildOrderSubject(orders);
        return alipayService.prepareTradePrecreate(orderNo, total.setScale(2, RoundingMode.HALF_UP).toPlainString(), subject);
    }

    /**
     * 取消待支付订单，释放用户未完成的购买流程。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    @Override
    public void cancelPendingOrder(Integer id, Integer userId) {
        Order order = orderService.getById(id);
        if (order == null || !userId.equals(order.getUserId())) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单不存在");
        }
        String lockKey = "order:cancel:" + order.getOrderNo();
        String token = redisDistributedLock.tryLock(lockKey, ORDER_LOCK_TTL);
        if (token == null) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单正在处理，请稍后再试");
        }
        try {
            if (!"待支付".equals(order.getStatus())) {
                /**
                 * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
                 * @return 订单在该步骤产出的业务结果。
                 */
                throw new ServiceException(500, "只有待支付订单可以取消");
            }
            order.setStatus("已取消");
            order.setRefundStatus("NONE");
            updateOrderAndInvalidate(order);
            redisCacheService.delayedDoubleDelete(CacheKeys.paymentQr(order.getOrderNo()), CacheKeys.paymentStatus(order.getOrderNo()));
        } finally {
            redisDistributedLock.unlock(lockKey, token);
        }
    }

    /**
     * 发起或处理订单退款流程，保留审核所需的退款信息。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void refund(Integer id) {
        Order order = orderService.getById(id);
        orderService.cancelOrder(id);
        invalidateOrderRelatedGameCaches(order);
    }

    /**
     * 完成订单中的 handleAlipayReturn 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     */
    @Override
    public void handleAlipayReturn(String orderNo) {
        log.info("支付宝返回页面回调处理 订单号：{}", orderNo);
        markOrderPaid(orderNo);
    }

    /**
     * 完成订单中的 handleAlipayNotify 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param params params 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单处理后的文本结果。
     */
    @Override
    public String handleAlipayNotify(Map<String, String> params) {
        String verified = alipayService.alipayNotifyHandel(params);
        if (!"success".equals(verified)) {
            return "failure";
        }
        String orderNo = params == null ? null : params.get("out_trade_no");
        String tradeStatus = params == null ? null : params.get("trade_status");
        if (!StringUtils.hasText(orderNo)) {
            return "failure";
        }
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            String lockKey = "order:notifyPaid:" + orderNo;
            String token = redisDistributedLock.tryLock(lockKey, ORDER_LOCK_TTL);
            if (token == null) {
                return "success";
            }
            try {
                markOrderPaid(orderNo);
            } finally {
                redisDistributedLock.unlock(lockKey, token);
            }
        }
        return "success";
    }

    /**
     * 完成订单中的 queryPayStatus 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> queryPayStatus(String orderNo) {
        Map<String, Object> localStatus = queryLocalPayStatus(orderNo);
        if (localStatus != null && Boolean.TRUE.equals(localStatus.get("terminal"))) {
            return localStatus;
        }
        Object qrCache = redisCacheService.get(CacheKeys.paymentQr(orderNo));
        if (!isPaymentQrReady(qrCache)) {
            return waitingPaymentStatus(orderNo, "QR_PENDING", "二维码生成中");
        }
        Map<String, Object> status = redisCacheService.getOrLoadWithLock(
                CacheKeys.paymentStatus(orderNo),
                "payment:status:" + orderNo,
                Duration.ofSeconds(12),
                300,
                4,
                () -> alipayService.tradeQuery(orderNo));
        if (Boolean.TRUE.equals(status.get("paid"))) {
            String lockKey = "order:markPaid:" + orderNo;
            String token = redisDistributedLock.tryLock(lockKey, ORDER_LOCK_TTL);
            if (token != null) {
                try {
                    markOrderPaid(orderNo);
                } finally {
                    redisDistributedLock.unlock(lockKey, token);
                }
            }
            redisCacheService.delayedDoubleDelete(CacheKeys.paymentStatus(orderNo));
        } else {
            expirePendingOrderIfNeeded(orderNo);
        }
        return status;
    }

    /**
     * 完成订单中的 queryLocalPayStatus 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> queryLocalPayStatus(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return waitingPaymentStatus(orderNo, "INVALID_ORDER_NO", "订单号不能为空");
        }
        List<Order> orders = orderService.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .and(wrapper -> wrapper.eq(Order::getDeleted, false).or().isNull(Order::getDeleted)));
        if (orders.isEmpty()) {
            return waitingPaymentStatus(orderNo, "ORDER_NOT_FOUND", "订单不存在");
        }
        if (orders.stream().allMatch(order -> "购买成功".equals(order.getStatus()))) {
            Map<String, Object> result = waitingPaymentStatus(orderNo, "TRADE_SUCCESS", "支付成功");
            result.put("paid", true);
            result.put("terminal", true);
            return result;
        }
        if (orders.stream().allMatch(order -> "已取消".equals(order.getStatus()))) {
            Map<String, Object> result = waitingPaymentStatus(orderNo, "ORDER_CLOSED", "订单已取消");
            result.put("terminal", true);
            return result;
        }
        return null;
    }

    /**
     * 判断订单当前状态是否满足业务条件。
     * @param qrCache qrCache 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示订单当前状态满足业务判断。
     */
    private boolean isPaymentQrReady(Object qrCache) {
        if (!(qrCache instanceof Map)) {
            return true;
        }
        Map<?, ?> value = (Map<?, ?>) qrCache;
        Object qrStatus = value.get("qrStatus");
        Object qrCode = value.get("qrCode");
        return "READY".equals(String.valueOf(qrStatus)) && qrCode != null && StringUtils.hasText(String.valueOf(qrCode));
    }

    /**
     * 完成订单中的 waitingPaymentStatus 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @param tradeStatus tradeStatus 字段，来源于当前接口入参或内部调用上下文。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> waitingPaymentStatus(String orderNo, String tradeStatus, String message) {
        Map<String, Object> result = new HashMap<>(6);
        result.put("orderNo", orderNo);
        result.put("paid", false);
        result.put("tradeStatus", tradeStatus);
        result.put("message", message);
        result.put("terminal", false);
        return result;
    }

    /**
     * 按主键删除订单记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void delete(Long id) {
        Order order = orderService.getById(id);
        orderService.removeById(id);
        invalidateOrderRelatedGameCaches(order);
    }

    /**
     * 完成订单中的 deleteMyOrder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    @Override
    public void deleteMyOrder(Long id, Integer userId) {
        Order order = orderService.getById(id);
        if (order == null || !userId.equals(order.getUserId())) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单不存在");
        }
        orderService.removeById(id);
        invalidateOrderRelatedGameCaches(order);
    }

    /**
     * 保存订单编辑后的内容，让前台展示和后台管理保持一致。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void update(Order order) {
        updateOrderAndInvalidate(order);
    }

    /**
     * 分页查询订单数据，返回列表内容和总数信息。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @param forcedUserId forcedUser 主键，用来定位关联业务数据。
     * @return 订单分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<Order> page(OrderVo dto, Integer forcedUserId) {
        expireStalePendingOrders();
        OrderVo query = dto == null ? new OrderVo() : dto;
        List<Integer> gameIds = findGameIds(query.getGameName());
        if (gameIds != null && gameIds.isEmpty()) {
            return PageResult.empty(0L);
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, false)
                .eq(forcedUserId != null, Order::getUserId, forcedUserId)
                .eq(forcedUserId == null && query.getUserId() != null, Order::getUserId, query.getUserId())
                .eq(query.getGameId() != null, Order::getGameId, query.getGameId())
                .eq(StringUtils.hasText(query.getOrderNo()), Order::getOrderNo, safeTrim(query.getOrderNo()))
                .eq(StringUtils.hasText(query.getStatus()), Order::getStatus, safeTrim(query.getStatus()))
                .eq(StringUtils.hasText(query.getRefundStatus()), Order::getRefundStatus, safeTrim(query.getRefundStatus()))
                .in(gameIds != null, Order::getGameId, gameIds)
                .orderByDesc(Order::getCreateTime);

        IPage<Order> page = orderService.page(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);
        page.getRecords().forEach(this::fillGameName);
        return new PageResult<>(page);
    }

    /**
     * 提交用户退款申请，等待后台管理员或审核员处理。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    @Override
    public void applyRefund(Integer id, OrderVo dto, Integer userId) {
        Order order = orderService.getById(id);
        if (order == null || !userId.equals(order.getUserId())) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单不存在");
        }
        if (!"购买成功".equals(order.getStatus())) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "只有购买成功的订单可以申请退款");
        }
        if ("PENDING".equals(order.getRefundStatus())) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "退款申请正在审核中");
        }

        order.setRefundStatus("PENDING");
        order.setRefundReason(dto == null ? null : dto.getRefundReason());
        order.setRefundReply(null);
        order.setRefundApplyTime(LocalDateTime.now());
        order.setRefundReviewTime(null);
        order.setStatus("退款审核中");
        updateOrderAndInvalidate(order);
    }

    /**
     * 审核退款申请并写入处理意见。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     */
    @Override
    public void reviewRefund(OrderVo dto) {
        if (dto == null || dto.getId() == null) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "请选择订单");
        }
        Order order = orderService.getById(dto.getId());
        if (order == null) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单不存在");
        }
        if (!"PENDING".equals(order.getRefundStatus())) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "该订单没有待审核的退款申请");
        }

        String lockKey = "order:refund:" + order.getOrderNo();
        String token = redisDistributedLock.tryLock(lockKey, ORDER_LOCK_TTL);
        if (token == null) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "订单退款正在处理，请稍后再试");
        }
        try {
            String decision = dto.getRefundStatus();
            order.setRefundReply(dto.getRefundReply());
            order.setRefundReviewTime(LocalDateTime.now());
            if ("APPROVED".equalsIgnoreCase(decision)) {
                boolean refundSuccess = alipayService.tradeRefund(order.getOrderNo(), order.getTotalPrice().toPlainString());
                order.setRefundStatus("APPROVED");
                order.setStatus("已退款");
                if (!refundSuccess) {
                    String reply = order.getRefundReply();
                    order.setRefundReply((reply == null || reply.trim().isEmpty() ? "退款申请已通过" : reply)
                            + "（支付宝沙箱退款接口返回失败，已记录为人工退款通过）");
                }
            } else if ("REJECTED".equalsIgnoreCase(decision)) {
                order.setRefundStatus("REJECTED");
                order.setStatus("购买成功");
            } else {
                /**
                 * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
                 * @return 订单在该步骤产出的业务结果。
                 */
                throw new ServiceException(500, "审核结果不正确");
            }
            orderService.updateById(order);
            invalidateOrderRelatedGameCaches(order);
        } finally {
            redisDistributedLock.unlock(lockKey, token);
        }
    }

    /**
     * 完成订单中的 createLocked 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 订单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, String> createLocked(Order order, Integer userId) {
        expirePendingUserGameOrders(userId, order.getGameId());
        Game game = gameService.getById(order.getGameId());
        if (game == null) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "游戏不存在");
        }
        if (hasBought(userId, order.getGameId())) {
            /**
             * 完成订单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 订单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "该游戏已购买，请勿重复下单");
        }
        int finalPrice = calculateFinalPrice(game.getPrice(), game.getDiscount());
        order.setUserId(userId);
        order.setGameName(game.getName());
        order.setTotalPrice(BigDecimal.valueOf(finalPrice));
        if (finalPrice <= 0) {
            cancelPendingUserGameOrders(userId, order.getGameId());
            Map<String, String> result = orderService.createOrder(order);
            gameUserCommerceService.clearCartItemsAfterPayment(Collections.singletonList(order));
            userExperienceService.award(userId, "PURCHASE", 30, 2, 60);
            invalidateOrderRelatedGameCaches(order);
            return result;
        }
        Order pendingOrder = findPendingOrder(userId, order.getGameId());
        if (pendingOrder != null) {
            fillGameName(pendingOrder);
            return orderService.continuePay(pendingOrder);
        }
        return orderService.createOrder(order);
    }

    /**
     * 完成订单中的 findGameIds 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameName gameName 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单列表数据。
     */
    private List<Integer> findGameIds(String gameName) {
        if (!StringUtils.hasText(gameName)) {
            return null;
        }
        return gameService.list(new LambdaQueryWrapper<Game>()
                        .select(Game::getId)
                        .like(Game::getName, safeTrim(gameName)))
                .stream()
                .map(Game::getId)
                .collect(Collectors.toList());
    }

    /**
     * 完成订单中的 fillGameName 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     */
    private void fillGameName(Order order) {
        if (order == null || order.getGameId() == null) {
            return;
        }
        Game game = gameService.getById(order.getGameId());
        if (game != null) {
            order.setGameName(game.getName());
        }
    }

    /**
     * 组装订单所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param orders orders 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单处理后的文本结果。
     */
    private String buildOrderSubject(List<Order> orders) {
        if (orders.size() > 1) {
            return "购物车结算";
        }
        Order order = orders.get(0);
        fillGameName(order);
        return StringUtils.hasText(order.getGameName()) ? order.getGameName() : "下单支付";
    }

    /**
     * 完成订单中的 safeTrim 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单处理后的文本结果。
     */
    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 完成订单中的 markOrderPaid 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     */
    private void markOrderPaid(String orderNo) {
        List<Order> payableOrders = orderService.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .ne(Order::getStatus, "购买成功"));
        if (payableOrders.isEmpty()) {
            return;
        }
        orderService.update(new UpdateWrapper<Order>()
                .eq("order_no", orderNo)
                .set("status", "购买成功")
                .set("refund_status", "NONE"));
        redisCacheService.delayedDoubleDelete(CacheKeys.paymentQr(orderNo), CacheKeys.paymentStatus(orderNo));
        gameUserCommerceService.clearCartItemsAfterPayment(payableOrders);
        payableOrders.forEach(order -> userExperienceService.award(order.getUserId(), "PURCHASE", 30, 2, 60));
        invalidateOrderRelatedGameCaches(payableOrders);
    }

    /**
     * 完成订单中的 expirePendingOrderIfNeeded 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     */
    private void expirePendingOrderIfNeeded(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return;
        }
        List<Order> pendingOrders = orderService.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, "待支付"));
        boolean expired = pendingOrders.stream()
                .filter(order -> order.getCreateTime() != null)
                .anyMatch(order -> order.getCreateTime().plusMinutes(30).isBefore(LocalDateTime.now()));
        if (expired) {
            orderService.update(new UpdateWrapper<Order>()
                    .eq("order_no", orderNo)
                    .eq("status", "待支付")
                    .set("status", "已取消")
                    .set("refund_status", "NONE"));
            redisCacheService.delayedDoubleDelete(CacheKeys.paymentQr(orderNo), CacheKeys.paymentStatus(orderNo));
        }
    }

    /**
     * 完成订单中的 expireStalePendingOrders 步骤，保证该环节的数据和状态可以继续向下流转。
     */
    private void expireStalePendingOrders() {
        orderService.update(new UpdateWrapper<Order>()
                .eq("status", "待支付")
                .le("create_time", LocalDateTime.now().minusMinutes(30))
                .set("status", "已取消")
                .set("refund_status", "NONE"));
    }

    /**
     * 完成订单中的 expirePendingUserGameOrders 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     */
    private void expirePendingUserGameOrders(Integer userId, Integer gameId) {
        orderService.update(new UpdateWrapper<Order>()
                .eq("user_id", userId)
                .eq("game_id", gameId)
                .eq("status", "待支付")
                .le("create_time", LocalDateTime.now().minusMinutes(30))
                .set("status", "已取消")
                .set("refund_status", "NONE"));
    }

    /**
     * 完成订单中的 cancelPendingUserGameOrders 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     */
    private void cancelPendingUserGameOrders(Integer userId, Integer gameId) {
        orderService.update(new UpdateWrapper<Order>()
                .eq("user_id", userId)
                .eq("game_id", gameId)
                .eq("status", "待支付")
                .set("status", "已取消")
                .set("refund_status", "NONE"));
    }

    /**
     * 完成订单中的 findPendingOrder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 订单在该步骤产出的业务结果。
     */
    private Order findPendingOrder(Integer userId, Integer gameId) {
        return orderService.getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getGameId, gameId)
                .eq(Order::getStatus, "待支付")
                .orderByDesc(Order::getCreateTime)
                .last("LIMIT 1"));
    }

    /**
     * 完成订单中的 hasBought 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示订单当前状态满足业务判断。
     */
    private boolean hasBought(Integer userId, Integer gameId) {
        return orderService.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getGameId, gameId)
                .eq(Order::getStatus, "购买成功")) > 0;
    }

    /**
     * 完成订单中的 updateOrderAndInvalidate 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     */
    private void updateOrderAndInvalidate(Order order) {
        orderService.updateById(order);
        invalidateOrderRelatedGameCaches(order);
    }

    /**
     * 完成订单中的 invalidateOrderRelatedGameCaches 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orders orders 字段，来源于当前接口入参或内部调用上下文。
     */
    private void invalidateOrderRelatedGameCaches(List<Order> orders) {
        orders.stream()
                .map(Order::getGameId)
                .distinct()
                .forEach(gameId -> redisCacheService.delayedDoubleDelete(CacheKeys.gameStats(gameId)));
        redisCacheService.delayedDoubleDeleteByPattern(CacheKeys.GAME_PATTERN, CacheKeys.RATING_PATTERN, CacheKeys.RANKING_PATTERN);
    }

    /**
     * 完成订单中的 invalidateOrderRelatedGameCaches 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param order order 字段，来源于当前接口入参或内部调用上下文。
     */
    private void invalidateOrderRelatedGameCaches(Order order) {
        if (order != null && order.getGameId() != null) {
            redisCacheService.delayedDoubleDelete(CacheKeys.gameStats(order.getGameId()));
        }
        redisCacheService.delayedDoubleDeleteByPattern(CacheKeys.GAME_PATTERN, CacheKeys.RATING_PATTERN, CacheKeys.RANKING_PATTERN);
    }

    /**
     * 完成订单中的 calculateFinalPrice 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param price price 字段，来源于当前接口入参或内部调用上下文。
     * @param discount discount 字段，来源于当前接口入参或内部调用上下文。
     * @return 订单统计值或主键结果。
     */
    private int calculateFinalPrice(Integer price, Integer discount) {
        if (price == null) {
            return 0;
        }
        int normalizedDiscount = discount == null ? 0 : Math.max(0, Math.min(discount, 100));
        if (normalizedDiscount <= 0) {
            return price;
        }
        return BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(100 - normalizedDiscount))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
