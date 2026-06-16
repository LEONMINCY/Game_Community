package com.sys.pro.service;

import java.util.List;
import java.util.Map;

/**
 * 游戏榜单业务服务。
 */
public interface RankingService {
    /**
     * 读取榜单筛选可用平台，支持 PC、PS、Switch 等维度切换。
     * @return 游戏榜单列表数据。
     */

    List<String> platformOptions();
    /**
     * 按收入维度查询游戏销售榜，并返回分页和统计图数据。
     * @param period period 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> salesRanking(String period, String type, String platform, Integer pageNo, Integer pageSize);
    /**
     * 查询当前优惠游戏榜单，按折扣期间销量和收入排序。
     * @param period period 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> discountRanking(String period, String type, String platform, Integer pageNo, Integer pageSize);
    /**
     * 按好评率查询游戏评价榜单，并附带好评差评统计。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> reviewRanking(String type, String platform, Integer gameId, Integer pageNo, Integer pageSize);
    /**
     * 按愿望单数量查询最受期待游戏榜单。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> expectedRanking(String type, String platform, Integer pageNo, Integer pageSize);
    /**
     * 按游戏平台维度统计销售、评价和热度排行。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> platformRanking(String type, Integer pageNo, Integer pageSize);
}
