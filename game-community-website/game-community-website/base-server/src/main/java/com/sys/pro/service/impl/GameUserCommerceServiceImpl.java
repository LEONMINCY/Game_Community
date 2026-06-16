package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sys.pro.pojo.Game;
import com.sys.pro.pojo.Order;
import com.sys.pro.repository.GameUserCommerceRepository;
import com.sys.pro.service.AlipayService;
import com.sys.pro.service.GameService;
import com.sys.pro.service.GameUserCommerceService;
import com.sys.pro.service.OrderService;
import com.sys.pro.utils.CacheKeys;
import com.sys.pro.utils.RedisCacheService;
import com.sys.pro.utils.RedisDistributedLock;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户购物车、愿望单和已拥有游戏的业务编排。
 */
@Service
@RequiredArgsConstructor
public class GameUserCommerceServiceImpl implements GameUserCommerceService {

    private static final Duration CART_CHECKOUT_LOCK_TTL = Duration.ofSeconds(30);

    private final GameUserCommerceRepository commerceRepository;
    private final GameService gameService;
    private final OrderService orderService;
    private final AlipayService alipayService;
    private final RedisDistributedLock redisDistributedLock;
    private final RedisCacheService redisCacheService;


    /**
     * 完成愿望单、购物车和已购游戏中的 wishlistActive 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */
    @Override
    public boolean wishlistActive(Integer userId, Integer gameId) {
        return redisCacheService.getOrLoad(
                CacheKeys.commerceStatus("wishlist", userId, gameId),
                120,
                () -> commerceRepository.isActive(GameUserCommerceRepository.WISHLIST_TABLE, userId, gameId));
    }

    /**
     * 切换愿望单、购物车和已购游戏中的启用状态，让按钮和真实业务状态保持一致。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */
    @Override
    public boolean toggleWishlist(Integer userId, Integer gameId) {
        boolean active = toggle(GameUserCommerceRepository.WISHLIST_TABLE, userId, gameId);
        invalidateCommerceCaches(userId, gameId, true);
        return active;
    }

    /**
     * 汇总愿望单、购物车和已购游戏列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏列表数据。
     */
    @Override
    public List<Map<String, Object>> listWishlist(Integer userId) {
        return redisCacheService.getOrLoad(
                CacheKeys.commerceList("wishlist", userId),
                120,
                () -> listGames(GameUserCommerceRepository.WISHLIST_TABLE, userId));
    }

    /**
     * 移除愿望单、购物车和已购游戏中的指定数据，并同步清理关联展示状态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     */
    @Override
    public void removeWishlist(Integer userId, Integer gameId) {
        commerceRepository.updateDeleted(GameUserCommerceRepository.WISHLIST_TABLE, userId, gameId, true);
        invalidateCommerceCaches(userId, gameId, true);
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 cartActive 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */
    @Override
    public boolean cartActive(Integer userId, Integer gameId) {
        return redisCacheService.getOrLoad(
                CacheKeys.commerceStatus("cart", userId, gameId),
                120,
                () -> commerceRepository.isActive(GameUserCommerceRepository.CART_TABLE, userId, gameId));
    }

    /**
     * 切换愿望单、购物车和已购游戏中的启用状态，让按钮和真实业务状态保持一致。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */
    @Override
    public boolean toggleCart(Integer userId, Integer gameId) {
        boolean active = toggle(GameUserCommerceRepository.CART_TABLE, userId, gameId);
        invalidateCommerceCaches(userId, gameId, false);
        return active;
    }

    /**
     * 汇总愿望单、购物车和已购游戏列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏列表数据。
     */
    @Override
    public List<Map<String, Object>> listCart(Integer userId) {
        return redisCacheService.getOrLoad(
                CacheKeys.commerceList("cart", userId),
                60,
                () -> listGames(GameUserCommerceRepository.CART_TABLE, userId));
    }

    /**
     * 移除愿望单、购物车和已购游戏中的指定数据，并同步清理关联展示状态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     */
    @Override
    public void removeCart(Integer userId, Integer gameId) {
        commerceRepository.updateDeleted(GameUserCommerceRepository.CART_TABLE, userId, gameId, true);
        invalidateCommerceCaches(userId, gameId, false);
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 checkoutCart 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameIds 游戏主键集合，表示本次批量处理的游戏范围。
     * @return 愿望单、购物车和已购游戏聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, String> checkoutCart(Integer userId, List<Integer> gameIds) {
        if (CollectionUtils.isEmpty(gameIds)) {
            /**
             * 完成愿望单、购物车和已购游戏中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 愿望单、购物车和已购游戏在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "请选择要结算的游戏");
        }
        String lockKey = "order:cartCheckout:" + userId;
        String token = redisDistributedLock.tryLock(lockKey, CART_CHECKOUT_LOCK_TTL);
        if (token == null) {
            /**
             * 完成愿望单、购物车和已购游戏中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 愿望单、购物车和已购游戏在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "购物车正在结算，请稍后再试");
        }
        try {
            return checkoutLocked(userId, gameIds);
        } finally {
            redisDistributedLock.unlock(lockKey, token);
        }
    }

    /**
     * 生成已拥有游戏汇总缓存键，缓存数量、账号价值和游戏列表。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> ownedSummary(Integer userId) {
        return redisCacheService.getOrLoad(CacheKeys.ownedSummary(userId), 180, () -> buildOwnedSummary(userId));
    }

    /**
     * 组装愿望单、购物车和已购游戏所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> buildOwnedSummary(Integer userId) {
        List<Map<String, Object>> games = commerceRepository.listOwnedGames(userId);
        games.forEach(this::fillPrice);
        int accountValue = games.stream()
                .map(row -> toInt(row.get("price")))
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gameCount", games.size());
        result.put("accountValue", accountValue);
        result.put("games", games);
        return result;
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 clearCartItemsAfterPayment 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orders orders 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void clearCartItemsAfterPayment(List<Order> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        Map<Integer, List<Integer>> userGameIds = orders.stream()
                .filter(order -> order.getUserId() != null && order.getGameId() != null)
                .collect(Collectors.groupingBy(Order::getUserId,
                        Collectors.mapping(Order::getGameId, Collectors.toList())));
        userGameIds.forEach((userId, gameIds) -> commerceRepository.clearCartItems(userId,
                gameIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList())));
        userGameIds.forEach((userId, gameIds) -> {
            redisCacheService.delayedDoubleDelete(CacheKeys.commerceList("cart", userId), CacheKeys.ownedSummary(userId));
            gameIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(gameId -> invalidateCommerceCaches(userId, gameId, false));
        });
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 checkoutLocked 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameIds 游戏主键集合，表示本次批量处理的游戏范围。
     * @return 愿望单、购物车和已购游戏聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, String> checkoutLocked(Integer userId, List<Integer> gameIds) {
        List<Integer> cartGameIds = commerceRepository.findActiveCartGameIds(userId, gameIds);
        if (cartGameIds.isEmpty()) {
            /**
             * 完成愿望单、购物车和已购游戏中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 愿望单、购物车和已购游戏在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "购物车中没有可结算的游戏");
        }

        expirePendingOrders(userId, cartGameIds);
        String orderNo = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + System.currentTimeMillis();
        BigDecimal total = BigDecimal.ZERO;
        List<Order> orders = new ArrayList<>();
        for (Integer gameId : cartGameIds) {
            if (hasBought(userId, gameId) || hasPendingOrder(userId, gameId)) {
                continue;
            }
            Game game = gameService.getById(gameId);
            if (game == null) {
                continue;
            }
            BigDecimal price = BigDecimal.valueOf(calculateFinalPrice(game.getPrice(), game.getDiscount()));
            total = total.add(price);

            Order order = new Order();
            order.setUserId(userId);
            order.setGameId(gameId);
            order.setOrderNo(orderNo);
            order.setStatus("待支付");
            order.setRefundStatus("NONE");
            order.setTotalPrice(price);
            orders.add(order);
        }

        if (orders.isEmpty()) {
            /**
             * 完成愿望单、购物车和已购游戏中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 愿望单、购物车和已购游戏在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "所选游戏已购买、已有待支付订单或不可结算");
        }
        orderService.saveBatch(orders);
        redisCacheService.delayedDoubleDeleteByPattern(CacheKeys.GAME_PATTERN, CacheKeys.RANKING_PATTERN);
        redisCacheService.delayedDoubleDelete(CacheKeys.commerceList("cart", userId));
        return alipayService.prepareTradePrecreate(orderNo, total.setScale(2, RoundingMode.HALF_UP).toPlainString(), "购物车结算");
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 invalidateCommerceCaches 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param rankingAffected rankingAffected 字段，来源于当前接口入参或内部调用上下文。
     */
    private void invalidateCommerceCaches(Integer userId, Integer gameId, boolean rankingAffected) {
        redisCacheService.delayedDoubleDelete(
                CacheKeys.commerceStatus("wishlist", userId, gameId),
                CacheKeys.commerceStatus("cart", userId, gameId),
                CacheKeys.commerceList("wishlist", userId),
                CacheKeys.commerceList("cart", userId),
                CacheKeys.ownedSummary(userId));
        if (rankingAffected) {
            redisCacheService.delayedDoubleDeleteByPattern(CacheKeys.RANKING_PATTERN);
        }
    }

    /**
     * 切换愿望单、购物车和已购游戏中的启用状态，让按钮和真实业务状态保持一致。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */
    private boolean toggle(String tableName, Integer userId, Integer gameId) {
        if (gameService.getById(gameId) == null) {
            return false;
        }
        if (commerceRepository.exists(tableName, userId, gameId)) {
            boolean active = commerceRepository.isActive(tableName, userId, gameId);
            commerceRepository.updateDeleted(tableName, userId, gameId, active);
            return !active;
        }
        commerceRepository.insertRelation(tableName, userId, gameId);
        return true;
    }

    /**
     * 汇总愿望单、购物车和已购游戏列表数据，供前端列表、下拉框或统计模块使用。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏列表数据。
     */
    private List<Map<String, Object>> listGames(String tableName, Integer userId) {
        List<Map<String, Object>> rows = commerceRepository.listRelationGames(tableName, userId);
        rows.forEach(this::fillPrice);
        return rows;
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 hasBought 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */
    private boolean hasBought(Integer userId, Integer gameId) {
        return orderService.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getGameId, gameId)
                .eq(Order::getStatus, "购买成功")) > 0;
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 hasPendingOrder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */
    private boolean hasPendingOrder(Integer userId, Integer gameId) {
        return orderService.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getGameId, gameId)
                .eq(Order::getStatus, "待支付")) > 0;
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 expirePendingOrders 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameIds 游戏主键集合，表示本次批量处理的游戏范围。
     */
    private void expirePendingOrders(Integer userId, List<Integer> gameIds) {
        if (CollectionUtils.isEmpty(gameIds)) {
            return;
        }
        orderService.update(new UpdateWrapper<Order>()
                .eq("user_id", userId)
                .in("game_id", gameIds)
                .eq("status", "待支付")
                .le("create_time", LocalDateTime.now().minusMinutes(30))
                .set("status", "已取消")
                .set("refund_status", "NONE"));
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 fillPrice 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param row row 字段，来源于当前接口入参或内部调用上下文。
     */
    private void fillPrice(Map<String, Object> row) {
        int price = toInt(row.get("price"), 0);
        int discount = toInt(row.get("discount"), 0);
        int finalPrice = calculateFinalPrice(price, discount);
        row.put("finalPrice", finalPrice);
        row.put("originalPrice", discount > 0 ? price : null);
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 calculateFinalPrice 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param price price 字段，来源于当前接口入参或内部调用上下文。
     * @param discount discount 字段，来源于当前接口入参或内部调用上下文。
     * @return 愿望单、购物车和已购游戏统计值或主键结果。
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

    /**
     * 转换愿望单、购物车和已购游戏字段格式，便于后续计算或接口返回。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 愿望单、购物车和已购游戏统计值或主键结果。
     */
    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    /**
     * 转换愿望单、购物车和已购游戏字段格式，便于后续计算或接口返回。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @param defaultValue defaultValue 字段，来源于当前接口入参或内部调用上下文。
     * @return 愿望单、购物车和已购游戏统计值或主键结果。
     */
    private int toInt(Object value, int defaultValue) {
        Integer parsed = toInt(value);
        return parsed == null ? defaultValue : parsed;
    }
}
