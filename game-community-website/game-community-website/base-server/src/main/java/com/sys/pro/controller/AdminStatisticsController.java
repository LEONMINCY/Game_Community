package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.service.AdminStatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后台统计接口，仅负责路由和响应封装。
 */
@Slf4j
@RequiredArgsConstructor
@Api(tags = "管理员统计接口")
@RestController
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    /**
     * 读取后台统计看板的 AdminDashboard 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取管理员首页聚合看板")
    @GetMapping("/dashboard")
    public CommonResult<Map<String, Object>> getAdminDashboard() {
        return CommonResult.success(adminStatisticsService.getAdminDashboard());
    }

    /**
     * 读取后台统计看板的 Statistics 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取系统统计数据")
    @GetMapping("/")
    public CommonResult<Map<String, Object>> getStatistics() {
        return CommonResult.success(adminStatisticsService.getStatistics());
    }

    /**
     * 读取后台统计看板的 GameTypes 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取游戏分类占比数据")
    @GetMapping("/game-types")
    public CommonResult<Map<String, Object>> getGameTypes() {
        return CommonResult.success(adminStatisticsService.getGameTypes());
    }

    /**
     * 读取后台统计看板的 UserGrowthTrend 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取用户增长趋势数据")
    @GetMapping("/user-growth")
    public CommonResult<Map<String, Object>> getUserGrowthTrend() {
        return CommonResult.success(adminStatisticsService.getUserGrowthTrend());
    }

    /**
     * 读取后台统计看板的 HotGames 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取热门游戏帖子TOP5")
    @GetMapping("/hot-games")
    public CommonResult<Map<String, Object>> getHotGames() {
        return CommonResult.success(adminStatisticsService.getHotGames());
    }

    /**
     * 读取后台统计看板的 ActivityAnalysis 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取用户活跃度分析数据")
    @GetMapping("/activity-analysis")
    public CommonResult<Map<String, Object>> getActivityAnalysis() {
        return CommonResult.success(adminStatisticsService.getActivityAnalysis());
    }

    /**
     * 读取后台统计看板的 GameSalesRanking 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取游戏销量排名")
    @GetMapping("/game-sales-ranking")
    public CommonResult<List<Map<String, Object>>> getGameSalesRanking() {
        return CommonResult.success(adminStatisticsService.getGameSalesRanking());
    }

    /**
     * 读取后台统计看板的 SalesOverview 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取平台游戏销售总额统计")
    @GetMapping("/sales-overview")
    public CommonResult<Map<String, Object>> getSalesOverview() {
        return CommonResult.success(adminStatisticsService.getSalesOverview());
    }

    /**
     * 读取后台统计看板的 PostInteractionHeatmap 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取帖子评论和点赞热力图")
    @GetMapping("/post-interaction-heatmap")
    public CommonResult<Map<String, Object>> getPostInteractionHeatmap() {
        return CommonResult.success(adminStatisticsService.getPostInteractionHeatmap());
    }

    /**
     * 读取后台统计看板的 UserAgeDistribution 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取用户年龄占比")
    @GetMapping("/user-age-distribution")
    public CommonResult<List<Map<String, Object>>> getUserAgeDistribution() {
        return CommonResult.success(adminStatisticsService.getUserAgeDistribution());
    }

    /**
     * 读取后台统计看板的 NewsViews 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取新闻浏览量统计")
    @GetMapping("/news-views")
    public CommonResult<List<Map<String, Object>>> getNewsViews() {
        return CommonResult.success(adminStatisticsService.getNewsViews());
    }

    /**
     * 读取后台统计看板的 GameGoodRates 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取游戏好评率统计")
    @GetMapping("/game-good-rates")
    public CommonResult<List<Map<String, Object>>> getGameGoodRates() {
        return CommonResult.success(adminStatisticsService.getGameGoodRates());
    }

    /**
     * 读取后台统计看板的 ModeratorDashboard 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取社区审核员首页数据")
    @GetMapping("/moderator-dashboard")
    public CommonResult<Map<String, Object>> getModeratorDashboard() {
        return CommonResult.success(adminStatisticsService.getModeratorDashboard());
    }
}
