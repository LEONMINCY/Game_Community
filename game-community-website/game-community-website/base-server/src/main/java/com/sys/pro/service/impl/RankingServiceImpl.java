package com.sys.pro.service.impl;

import com.sys.pro.repository.RankingRepository;
import com.sys.pro.service.RankingService;
import com.sys.pro.utils.CacheKeys;
import com.sys.pro.utils.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 游戏榜单业务实现。
 * 负责缓存、排名变化计算和可视化数据组装；数据库查询委托给 RankingRepository。
 */
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private static final long RANKING_CACHE_SECONDS = 300;
    private static final Duration RANKING_CACHE_LOCK_TTL = Duration.ofSeconds(10);

    private final RankingRepository rankingRepository;
    private final RedisCacheService redisCacheService;

    /**
     * 读取榜单筛选可用平台，支持 PC、PS、Switch 等维度切换。
     * @return 游戏榜单列表数据。
     */
    @Override
    public List<String> platformOptions() {
        return redisCacheService.getOrLoadWithLock(
                CacheKeys.ranking("platform-options"),
                "cache:ranking:platform-options",
                RANKING_CACHE_LOCK_TTL,
                800,
                RANKING_CACHE_SECONDS,
                () -> rankingRepository.selectPlatformValues().stream()
                        .flatMap(value -> splitLabels(value).stream())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList()));
    }

    /**
     * 按收入维度查询游戏销售榜，并返回分页和统计图数据。
     * @param period period 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> salesRanking(String period, String type, String platform, Integer pageNo, Integer pageSize) {
        return cachedRanking("sales", period, type, platform, pageNo, pageSize, null, () -> {
            PeriodRange range = PeriodRange.of(period);
            List<Map<String, Object>> allRows = queryRevenueRanking(range.start, range.end, type, platform, false);
            Map<Integer, Integer> previousRanks = range.previousStart == null
                    ? Collections.emptyMap()
                    : rankByGameId(queryRevenueRanking(range.previousStart, range.start, type, platform, false));
            decorateRankChange(allRows, previousRanks);
            return pageResult(allRows, pageNo, pageSize, buildRevenueCharts(allRows));
        });
    }

    /**
     * 查询当前优惠游戏榜单，按折扣期间销量和收入排序。
     * @param period period 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> discountRanking(String period, String type, String platform, Integer pageNo, Integer pageSize) {
        return cachedRanking("discounts", period, type, platform, pageNo, pageSize, null, () -> {
            PeriodRange range = PeriodRange.of(period);
            List<Map<String, Object>> allRows = queryRevenueRanking(range.start, range.end, type, platform, true);
            Map<Integer, Integer> previousRanks = range.previousStart == null
                    ? Collections.emptyMap()
                    : rankByGameId(queryRevenueRanking(range.previousStart, range.start, type, platform, true));
            decorateRankChange(allRows, previousRanks);
            return pageResult(allRows, pageNo, pageSize, buildDiscountCharts(allRows));
        });
    }

    /**
     * 按好评率查询游戏评价榜单，并附带好评差评统计。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> reviewRanking(String type, String platform, Integer gameId, Integer pageNo, Integer pageSize) {
        return cachedRanking("reviews", null, type, platform, pageNo, pageSize, gameId, () -> {
            List<Map<String, Object>> allRows = queryReviewRanking(type, platform);
            Integer selectedGameId = gameId != null
                    ? gameId
                    : allRows.stream().findFirst().map(row -> number(row.get("id")).intValue()).orElse(null);
            return pageResult(allRows, pageNo, pageSize, buildReviewCharts(selectedGameId));
        });
    }

    /**
     * 按愿望单数量查询最受期待游戏榜单。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> expectedRanking(String type, String platform, Integer pageNo, Integer pageSize) {
        return cachedRanking("expected", null, type, platform, pageNo, pageSize, null, () -> {
            List<Map<String, Object>> allRows = queryExpectedRanking(type, platform);
            return pageResult(allRows, pageNo, pageSize, buildExpectedCharts(allRows));
        });
    }

    /**
     * 按游戏平台维度统计销售、评价和热度排行。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> platformRanking(String type, Integer pageNo, Integer pageSize) {
        return cachedRanking("platforms", null, type, null, pageNo, pageSize, null, () -> {
            List<Map<String, Object>> allRows = queryPlatformRanking(type);
            return pageResult(allRows, pageNo, pageSize, buildPlatformCharts(allRows));
        });
    }

    /**
     * 完成游戏榜单中的 cachedRanking 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param rankingType rankingType 字段，来源于当前接口入参或内部调用上下文。
     * @param period period 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param loader loader 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> cachedRanking(String rankingType,
                                              String period,
                                              String type,
                                              String platform,
                                              Integer pageNo,
                                              Integer pageSize,
                                              Integer gameId,
                                              java.util.function.Supplier<Map<String, Object>> loader) {
        String payload = String.join("|",
                rankingType,
                cachePart(period),
                cachePart(type),
                cachePart(platform),
                cachePart(pageNo),
                cachePart(pageSize),
                cachePart(gameId));
        String cacheKey = CacheKeys.ranking(payload);
        return redisCacheService.getOrLoadWithLock(
                cacheKey,
                "cache:ranking:" + cacheKey,
                RANKING_CACHE_LOCK_TTL,
                800,
                RANKING_CACHE_SECONDS,
                loader);
    }

    /**
     * 完成游戏榜单中的 queryRevenueRanking 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @param end end 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param discountsOnly discountsOnly 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    private List<Map<String, Object>> queryRevenueRanking(LocalDateTime start,
                                                          LocalDateTime end,
                                                          String type,
                                                          String platform,
                                                          boolean discountsOnly) {
        List<Map<String, Object>> rows = rankingRepository.queryRevenueRanking(start, end, type, platform, discountsOnly);
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = normalizeGameRow(rows.get(i));
            row.put("rank", i + 1);
            rows.set(i, row);
        }
        return rows;
    }

    /**
     * 完成游戏榜单中的 queryReviewRanking 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    private List<Map<String, Object>> queryReviewRanking(String type, String platform) {
        List<Map<String, Object>> rows = rankingRepository.queryReviewRanking(type, platform);
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = normalizeGameRow(rows.get(i));
            BigDecimal reviews = number(row.get("review_count"));
            BigDecimal good = number(row.get("good_count"));
            BigDecimal bad = number(row.get("bad_count"));
            row.put("rank", i + 1);
            row.put("reviewCount", reviews.intValue());
            row.put("goodCount", good.intValue());
            row.put("badCount", bad.intValue());
            row.put("goodRate", reviews.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : good.multiply(BigDecimal.valueOf(100)).divide(reviews, 1, RoundingMode.HALF_UP));
            row.put("avgRating", number(row.get("avg_rating")).setScale(1, RoundingMode.HALF_UP));
            rows.set(i, row);
        }
        return rows;
    }

    /**
     * 完成游戏榜单中的 queryExpectedRanking 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    private List<Map<String, Object>> queryExpectedRanking(String type, String platform) {
        List<Map<String, Object>> rows = rankingRepository.queryExpectedRanking(type, platform);
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = normalizeGameRow(rows.get(i));
            row.put("rank", i + 1);
            row.put("wishlistCount", number(row.get("wishlist_count")).intValue());
            rows.set(i, row);
        }
        return rows;
    }

    /**
     * 完成游戏榜单中的 queryPlatformRanking 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    private List<Map<String, Object>> queryPlatformRanking(String type) {
        List<Map<String, Object>> games = queryRevenueRanking(null, null, type, null, false);
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> game : games) {
            List<String> platforms = splitLabels((String) game.get("platforms"));
            if (platforms.isEmpty()) {
                platforms = Collections.singletonList("Unknown");
            }
            for (String platform : platforms) {
                Map<String, Object> row = grouped.computeIfAbsent(platform, key -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("platform", key);
                    value.put("gameCount", 0);
                    value.put("sales", 0);
                    value.put("revenue", BigDecimal.ZERO);
                    return value;
                });
                row.put("gameCount", number(row.get("gameCount")).intValue() + 1);
                row.put("sales", number(row.get("sales")).intValue() + number(game.get("sales")).intValue());
                row.put("revenue", number(row.get("revenue")).add(number(game.get("revenue"))));
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>(grouped.values());
        rows.sort((a, b) -> number(b.get("revenue")).compareTo(number(a.get("revenue"))));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).put("rank", i + 1);
        }
        return rows;
    }

    /**
     * 完成游戏榜单中的 normalizeGameRow 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param source source 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> normalizeGameRow(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>(source);
        Integer price = number(row.get("price")).intValue();
        Integer discount = number(row.get("discount")).intValue();
        if (discount < 0) {
            discount = 0;
        }
        if (discount > 100) {
            discount = 100;
        }
        int finalPrice = BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(100 - discount))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .intValue();
        row.put("price", price);
        row.put("discount", discount);
        row.put("finalPrice", finalPrice);
        row.put("revenue", number(row.get("revenue")).setScale(2, RoundingMode.HALF_UP));
        row.put("sales", number(row.get("sales")).intValue());
        row.put("platformList", splitLabels((String) row.get("platforms")));
        row.put("typeList", splitLabels((String) row.get("type")));
        return row;
    }

    /**
     * 完成游戏榜单中的 rankByGameId 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param rows rows 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<Integer, Integer> rankByGameId(List<Map<String, Object>> rows) {
        Map<Integer, Integer> ranks = new HashMap<>();
        for (Map<String, Object> row : rows) {
            ranks.put(number(row.get("id")).intValue(), number(row.get("rank")).intValue());
        }
        return ranks;
    }

    /**
     * 完成游戏榜单中的 decorateRankChange 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param rows rows 字段，来源于当前接口入参或内部调用上下文。
     * @param previousRanks previousRanks 字段，来源于当前接口入参或内部调用上下文。
     */
    private void decorateRankChange(List<Map<String, Object>> rows, Map<Integer, Integer> previousRanks) {
        for (Map<String, Object> row : rows) {
            int gameId = number(row.get("id")).intValue();
            Integer previousRank = previousRanks.get(gameId);
            int currentRank = number(row.get("rank")).intValue();
            row.put("previousRank", previousRank);
            row.put("rankChange", previousRank == null ? null : previousRank - currentRank);
            row.put("newListed", previousRank == null && number(row.get("revenue")).compareTo(BigDecimal.ZERO) > 0);
        }
    }

    /**
     * 完成游戏榜单中的 pageResult 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param rows rows 字段，来源于当前接口入参或内部调用上下文。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @param charts charts 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> pageResult(List<Map<String, Object>> rows,
                                           Integer pageNo,
                                           Integer pageSize,
                                           Map<String, Object> charts) {
        int currentPage = Math.max(1, pageNo == null ? 1 : pageNo);
        int size = Math.max(1, Math.min(pageSize == null ? 100 : pageSize, 100));
        int from = Math.min((currentPage - 1) * size, rows.size());
        int to = Math.min(from + size, rows.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", rows.subList(from, to));
        result.put("total", rows.size());
        result.put("pageNo", currentPage);
        result.put("pageSize", size);
        result.put("charts", charts);
        return result;
    }

    /**
     * 组装游戏榜单所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param rows rows 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> buildRevenueCharts(List<Map<String, Object>> rows) {
        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("topRevenue", rows.stream().limit(10).map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row.get("name"));
            item.put("value", row.get("revenue"));
            item.put("sales", row.get("sales"));
            return item;
        }).collect(Collectors.toList()));
        charts.put("typeRevenue", groupByLabels(rows, "typeList", "revenue"));
        charts.put("priceRanges", priceRanges(rows));
        return charts;
    }

    /**
     * 组装游戏榜单所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param rows rows 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> buildDiscountCharts(List<Map<String, Object>> rows) {
        Map<String, Object> charts = buildRevenueCharts(rows);
        charts.put("discountRanges", rows.stream().collect(Collectors.groupingBy(
                row -> discountBucket(number(row.get("discount")).intValue()),
                LinkedHashMap::new,
                Collectors.counting()
        )));
        return charts;
    }

    /**
     * 组装游戏榜单所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> buildReviewCharts(Integer gameId) {
        Map<String, Object> charts = new LinkedHashMap<>();
        if (gameId == null) {
            charts.put("pie", Collections.emptyList());
            charts.put("trend", Collections.emptyList());
            return charts;
        }
        Map<String, Object> counts = rankingRepository.queryReviewCounts(gameId);
        charts.put("pie", List.of(
                chartItem("good", number(counts.get("good_count"))),
                chartItem("bad", number(counts.get("bad_count")))
        ));
        charts.put("trend", rankingRepository.queryReviewTrend(gameId).stream().map(row -> {
            BigDecimal total = number(row.get("total"));
            BigDecimal good = number(row.get("good_count"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", row.get("month"));
            item.put("goodRate", total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : good.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP));
            item.put("total", total.intValue());
            return item;
        }).collect(Collectors.toList()));
        return charts;
    }

    /**
     * 组装游戏榜单所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param rows rows 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> buildExpectedCharts(List<Map<String, Object>> rows) {
        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("topExpected", rows.stream().limit(10).map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row.get("name"));
            item.put("value", row.get("wishlistCount"));
            return item;
        }).collect(Collectors.toList()));
        charts.put("typeWishlist", groupByLabels(rows, "typeList", "wishlistCount"));
        return charts;
    }

    /**
     * 组装游戏榜单所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param rows rows 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> buildPlatformCharts(List<Map<String, Object>> rows) {
        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("revenue", rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row.get("platform"));
            item.put("value", row.get("revenue"));
            item.put("sales", row.get("sales"));
            item.put("gameCount", row.get("gameCount"));
            return item;
        }).collect(Collectors.toList()));
        return charts;
    }

    /**
     * 完成游戏榜单中的 groupByLabels 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param rows rows 字段，来源于当前接口入参或内部调用上下文。
     * @param labelKey labelKey 字段，来源于当前接口入参或内部调用上下文。
     * @param valueKey valueKey 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> groupByLabels(List<Map<String, Object>> rows, String labelKey, String valueKey) {
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            List<String> labels = (List<String>) row.getOrDefault(labelKey, Collections.emptyList());
            if (labels.isEmpty()) {
                labels = Collections.singletonList("Unknown");
            }
            for (String label : labels) {
                grouped.merge(label, number(row.get(valueKey)), BigDecimal::add);
            }
        }
        return grouped.entrySet().stream().map(entry -> chartItem(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    /**
     * 完成游戏榜单中的 priceRanges 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param rows rows 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    private List<Map<String, Object>> priceRanges(List<Map<String, Object>> rows) {
        Map<String, Long> grouped = rows.stream().collect(Collectors.groupingBy(
                row -> priceBucket(number(row.get("finalPrice")).intValue()),
                LinkedHashMap::new,
                Collectors.counting()
        ));
        return grouped.entrySet().stream()
                .map(entry -> chartItem(entry.getKey(), BigDecimal.valueOf(entry.getValue())))
                .collect(Collectors.toList());
    }

    /**
     * 完成游戏榜单中的 chartItem 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param name name 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> chartItem(String name, BigDecimal value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    /**
     * 完成游戏榜单中的 priceBucket 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param price price 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单处理后的文本结果。
     */
    private String priceBucket(int price) {
        if (price <= 0) return "Free";
        if (price < 100) return "0-99";
        if (price < 200) return "100-199";
        if (price < 300) return "200-299";
        return "300+";
    }

    /**
     * 完成游戏榜单中的 discountBucket 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param discount discount 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单处理后的文本结果。
     */
    private String discountBucket(int discount) {
        if (discount <= 0) return "No discount";
        if (discount <= 20) return "1%-20%";
        if (discount <= 50) return "21%-50%";
        return "51%+";
    }

    /**
     * 完成游戏榜单中的 splitLabels 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    private List<String> splitLabels(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.split("[,，、|/]+");
        List<String> labels = new ArrayList<>();
        for (String part : parts) {
            String label = part.trim();
            if (StringUtils.hasText(label)) {
                labels.add(label);
            }
        }
        return labels;
    }

    /**
     * 完成游戏榜单中的 number 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单在该步骤产出的业务结果。
     */
    private BigDecimal number(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 完成游戏榜单中的 cachePart 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单处理后的文本结果。
     */
    private String cachePart(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static class PeriodRange {
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final LocalDateTime previousStart;

        /**
         * 保存当前榜单周期和上一周期边界，用来计算排名变化。
         *
         * @param start 当前周期开始时间；总榜为空。
         * @param end 当前周期结束时间；总榜为空。
         * @param previousStart 上一周期开始时间，用来计算排名升降。
         */
        private PeriodRange(LocalDateTime start, LocalDateTime end, LocalDateTime previousStart) {
            this.start = start;
            this.end = end;
            this.previousStart = previousStart;
        }

        /**
         * 完成游戏榜单中的 of 步骤，保证该环节的数据和状态可以继续向下流转。
         * @param period period 字段，来源于当前接口入参或内部调用上下文。
         * @return 游戏榜单在该步骤产出的业务结果。
         */
        private static PeriodRange of(String period) {
            String value = period == null ? "total" : period.toLowerCase(Locale.ROOT);
            LocalDate today = LocalDate.now();
            if ("year".equals(value)) {
                LocalDate start = today.withDayOfYear(1);
                return new PeriodRange(start.atStartOfDay(), start.plusYears(1).atStartOfDay(), start.minusYears(1).atStartOfDay());
            }
            if ("month".equals(value)) {
                LocalDate start = today.withDayOfMonth(1);
                return new PeriodRange(start.atStartOfDay(), start.plusMonths(1).atStartOfDay(), start.minusMonths(1).atStartOfDay());
            }
            if ("week".equals(value)) {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                return new PeriodRange(start.atStartOfDay(), start.plusWeeks(1).atStartOfDay(), start.minusWeeks(1).atStartOfDay());
            }
            return new PeriodRange(null, null, null);
        }
    }
}
