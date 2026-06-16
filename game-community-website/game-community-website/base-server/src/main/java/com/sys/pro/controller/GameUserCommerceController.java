package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.service.GameUserCommerceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户侧游戏资产接口。
 * Controller 只负责参数转换和响应包装，购物车/愿望单/订单结算业务下沉到 Service。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/game-user")
public class GameUserCommerceController extends BaseController {

    private final GameUserCommerceService commerceService;

    /**
     * 查询当前游戏是否已在用户愿望单中，用来控制按钮状态。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/wishlist/status/{gameId}")
    public CommonResult<Map<String, Boolean>> wishlistStatus(@PathVariable Integer gameId) {
        return CommonResult.success(Collections.singletonMap("active", commerceService.wishlistActive(getUserId(), gameId)));
    }

    /**
     * 切换愿望单、购物车和已购游戏中的启用状态，让按钮和真实业务状态保持一致。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/wishlist/toggle/{gameId}")
    public CommonResult<Map<String, Boolean>> toggleWishlist(@PathVariable Integer gameId) {
        return CommonResult.success(Collections.singletonMap("active", commerceService.toggleWishlist(getUserId(), gameId)));
    }

    /**
     * 读取用户愿望单列表，供右上角愿望单管理页展示。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/wishlist/list")
    public CommonResult<List<Map<String, Object>>> wishlistList() {
        return CommonResult.success(commerceService.listWishlist(getUserId()));
    }

    /**
     * 移除愿望单、购物车和已购游戏中的指定数据，并同步清理关联展示状态。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @DeleteMapping("/wishlist/{gameId}")
    public CommonResult<Void> removeWishlist(@PathVariable Integer gameId) {
        commerceService.removeWishlist(getUserId(), gameId);
        return CommonResult.success();
    }

    /**
     * 查询当前游戏是否已在用户购物车中，用来控制按钮状态。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/cart/status/{gameId}")
    public CommonResult<Map<String, Boolean>> cartStatus(@PathVariable Integer gameId) {
        return CommonResult.success(Collections.singletonMap("active", commerceService.cartActive(getUserId(), gameId)));
    }

    /**
     * 切换愿望单、购物车和已购游戏中的启用状态，让按钮和真实业务状态保持一致。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/cart/toggle/{gameId}")
    public CommonResult<Map<String, Boolean>> toggleCart(@PathVariable Integer gameId) {
        return CommonResult.success(Collections.singletonMap("active", commerceService.toggleCart(getUserId(), gameId)));
    }

    /**
     * 读取用户购物车列表，供结算页展示待购买游戏。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/cart/list")
    public CommonResult<List<Map<String, Object>>> cartList() {
        return CommonResult.success(commerceService.listCart(getUserId()));
    }

    /**
     * 移除愿望单、购物车和已购游戏中的指定数据，并同步清理关联展示状态。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @DeleteMapping("/cart/{gameId}")
    public CommonResult<Void> removeCart(@PathVariable Integer gameId) {
        commerceService.removeCart(getUserId(), gameId);
        return CommonResult.success();
    }

    /**
     * 结算购物车中选中的游戏，生成订单并返回支付信息。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/cart/checkout")
    public CommonResult<Map<String, String>> checkout(@RequestBody Map<String, Object> body) {
        List<Integer> gameIds = extractGameIds(body == null ? null : body.get("gameIds"));
        return CommonResult.success(commerceService.checkoutCart(getUserId(), gameIds));
    }

    /**
     * 生成已拥有游戏汇总缓存键，缓存数量、账号价值和游戏列表。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/owned/summary/{userId}")
    public CommonResult<Map<String, Object>> ownedSummary(@PathVariable Integer userId) {
        return CommonResult.success(commerceService.ownedSummary(userId));
    }

    /**
     * 从请求体中提取游戏主键集合，供购物车批量结算使用。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 愿望单、购物车和已购游戏列表数据。
     */
    private List<Integer> extractGameIds(Object value) {
        if (!(value instanceof List<?>)) {
            return Collections.emptyList();
        }
        return ((List<?>) value).stream()
                .map(item -> {
                    if (item instanceof Number) {
                        return ((Number) item).intValue();
                    }
                    try {
                        return Integer.valueOf(String.valueOf(item));
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}
