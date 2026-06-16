package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 榜单接口层。
 * 只转发查询条件并包装响应，榜单缓存、统计聚合和 SQL 查询分别由 Service/Repository 负责。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ranking")
public class RankingController {

    private final RankingService rankingService;

    /**
     * 读取榜单筛选可用平台，支持 PC、PS、Switch 等维度切换。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/platform-options")
    public CommonResult<List<String>> platformOptions() {
        return CommonResult.success(rankingService.platformOptions());
    }

    /**
     * 按收入维度查询游戏销售榜，并返回分页和统计图数据。
     * @param period period 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/sales")
    public CommonResult<Map<String, Object>> salesRanking(@RequestParam(defaultValue = "total") String period,
                                                          @RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String platform,
                                                          @RequestParam(defaultValue = "1") Integer pageNo,
                                                          @RequestParam(defaultValue = "100") Integer pageSize) {
        return CommonResult.success(rankingService.salesRanking(period, type, platform, pageNo, pageSize));
    }

    /**
     * 查询当前优惠游戏榜单，按折扣期间销量和收入排序。
     * @param period period 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/discounts")
    public CommonResult<Map<String, Object>> discountRanking(@RequestParam(defaultValue = "total") String period,
                                                             @RequestParam(required = false) String type,
                                                             @RequestParam(required = false) String platform,
                                                             @RequestParam(defaultValue = "1") Integer pageNo,
                                                             @RequestParam(defaultValue = "100") Integer pageSize) {
        return CommonResult.success(rankingService.discountRanking(period, type, platform, pageNo, pageSize));
    }

    /**
     * 按好评率查询游戏评价榜单，并附带好评差评统计。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/reviews")
    public CommonResult<Map<String, Object>> reviewRanking(@RequestParam(required = false) String type,
                                                           @RequestParam(required = false) String platform,
                                                           @RequestParam(required = false) Integer gameId,
                                                           @RequestParam(defaultValue = "1") Integer pageNo,
                                                           @RequestParam(defaultValue = "100") Integer pageSize) {
        return CommonResult.success(rankingService.reviewRanking(type, platform, gameId, pageNo, pageSize));
    }

    /**
     * 按愿望单数量查询最受期待游戏榜单。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/expected")
    public CommonResult<Map<String, Object>> expectedRanking(@RequestParam(required = false) String type,
                                                             @RequestParam(required = false) String platform,
                                                             @RequestParam(defaultValue = "1") Integer pageNo,
                                                             @RequestParam(defaultValue = "100") Integer pageSize) {
        return CommonResult.success(rankingService.expectedRanking(type, platform, pageNo, pageSize));
    }

    /**
     * 按游戏平台维度统计销售、评价和热度排行。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/platforms")
    public CommonResult<Map<String, Object>> platformRanking(@RequestParam(required = false) String type,
                                                             @RequestParam(defaultValue = "1") Integer pageNo,
                                                             @RequestParam(defaultValue = "100") Integer pageSize) {
        return CommonResult.success(rankingService.platformRanking(type, pageNo, pageSize));
    }
}
