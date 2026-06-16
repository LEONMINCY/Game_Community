package com.sys.pro.service;

import java.util.List;
import java.util.Map;

/**
 * 后台统计服务，负责管理员首页和审核员首页的数据聚合。
 */
public interface AdminStatisticsService {
    /**
     * 读取后台统计看板的 AdminDashboard 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> getAdminDashboard();
    /**
     * 读取后台统计看板的 Statistics 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> getStatistics();
    /**
     * 读取后台统计看板的 GameTypes 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> getGameTypes();
    /**
     * 读取后台统计看板的 UserGrowthTrend 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> getUserGrowthTrend();
    /**
     * 读取后台统计看板的 HotGames 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> getHotGames();
    /**
     * 读取后台统计看板的 ActivityAnalysis 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> getActivityAnalysis();
    /**
     * 读取后台统计看板的 GameSalesRanking 数据，供页面展示或后续业务判断。
     * @return 后台统计看板列表数据。
     */

    List<Map<String, Object>> getGameSalesRanking();
    /**
     * 读取后台统计看板的 SalesOverview 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> getSalesOverview();
    /**
     * 读取后台统计看板的 PostInteractionHeatmap 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> getPostInteractionHeatmap();
    /**
     * 读取后台统计看板的 UserAgeDistribution 数据，供页面展示或后续业务判断。
     * @return 后台统计看板列表数据。
     */

    List<Map<String, Object>> getUserAgeDistribution();
    /**
     * 读取后台统计看板的 NewsViews 数据，供页面展示或后续业务判断。
     * @return 后台统计看板列表数据。
     */

    List<Map<String, Object>> getNewsViews();
    /**
     * 读取后台统计看板的 GameGoodRates 数据，供页面展示或后续业务判断。
     * @return 后台统计看板列表数据。
     */

    List<Map<String, Object>> getGameGoodRates();
    /**
     * 读取后台统计看板的 ModeratorDashboard 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> getModeratorDashboard();
}
