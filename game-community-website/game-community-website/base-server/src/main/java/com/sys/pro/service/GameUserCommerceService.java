package com.sys.pro.service;

import com.sys.pro.pojo.Order;

import java.util.List;
import java.util.Map;

/**
 * 用户游戏资产、购物车和愿望单业务。
 */
public interface GameUserCommerceService {
    /**
     * 完成愿望单、购物车和已购游戏中的 wishlistActive 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */

    boolean wishlistActive(Integer userId, Integer gameId);
    /**
     * 切换愿望单、购物车和已购游戏中的启用状态，让按钮和真实业务状态保持一致。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */

    boolean toggleWishlist(Integer userId, Integer gameId);
    /**
     * 汇总愿望单、购物车和已购游戏列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏列表数据。
     */

    List<Map<String, Object>> listWishlist(Integer userId);
    /**
     * 移除愿望单、购物车和已购游戏中的指定数据，并同步清理关联展示状态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     */

    void removeWishlist(Integer userId, Integer gameId);
    /**
     * 完成愿望单、购物车和已购游戏中的 cartActive 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */

    boolean cartActive(Integer userId, Integer gameId);
    /**
     * 切换愿望单、购物车和已购游戏中的启用状态，让按钮和真实业务状态保持一致。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */

    boolean toggleCart(Integer userId, Integer gameId);
    /**
     * 汇总愿望单、购物车和已购游戏列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏列表数据。
     */

    List<Map<String, Object>> listCart(Integer userId);
    /**
     * 移除愿望单、购物车和已购游戏中的指定数据，并同步清理关联展示状态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     */

    void removeCart(Integer userId, Integer gameId);
    /**
     * 完成愿望单、购物车和已购游戏中的 checkoutCart 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameIds 游戏主键集合，表示本次批量处理的游戏范围。
     * @return 愿望单、购物车和已购游戏聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, String> checkoutCart(Integer userId, List<Integer> gameIds);
    /**
     * 生成已拥有游戏汇总缓存键，缓存数量、账号价值和游戏列表。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> ownedSummary(Integer userId);

    /**
     * 完成愿望单、购物车和已购游戏中的 clearCartItemsAfterPayment 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param orders orders 字段，来源于当前接口入参或内部调用上下文。
     */
    void clearCartItemsAfterPayment(List<Order> orders);
}
