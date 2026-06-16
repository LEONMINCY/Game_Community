package com.sys.pro.service.impl;

import com.sys.pro.mapper.BrowseHistoryMapper;
import com.sys.pro.mapper.CommentMapper;
import com.sys.pro.utils.CollaborativeFiltering;
import org.springframework.stereotype.Service;

import java.util.*;
/**
 * 推荐服务，结合帖子互动、游戏热度和用户行为计算推荐内容。
 */
@Service
public class RecommendationService {



    private final BrowseHistoryMapper browseHistoryMapper;

    private final CommentMapper commentMapper;



    /**
     * 注入推荐算法需要的用户浏览和评论行为数据源。
     *
     * @param browseHistoryMapper 浏览历史 Mapper，用来读取用户对内容的行为分。
     * @param commentMapper 评论 Mapper，保留给后续把评论互动纳入推荐权重。
     */
    public RecommendationService(BrowseHistoryMapper browseHistoryMapper, CommentMapper commentMapper) {

        this.browseHistoryMapper = browseHistoryMapper;

        this.commentMapper = commentMapper;

    }


    /**
     * 完成推荐中的 recommendGames 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 推荐列表数据。
     */
    public List<Long> recommendGames(Long userId) {
        Map<Long, Map<Long, Double>> userItemMatrix = getUserItemMatrix("game");
        if (userItemMatrix.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Map<Long, Double>> similarityMatrix = CollaborativeFiltering.calculateUserSimilarity(userItemMatrix);

        return CollaborativeFiltering.recommendForUser(userId, userItemMatrix, similarityMatrix, 5);
    }

    /**
     * 完成推荐中的 recommendNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 推荐列表数据。
     */
    public List<Long> recommendNews(Long userId) {
        Map<Long, Map<Long, Double>> userItemMatrix = getUserItemMatrix("news");
        Map<Long, Map<Long, Double>> similarityMatrix = CollaborativeFiltering.calculateUserSimilarity(userItemMatrix);

        return CollaborativeFiltering.recommendForUser(userId, userItemMatrix, similarityMatrix, 5);
    }

    /**
     * 读取推荐的 UserItemMatrix 数据，供页面展示或后续业务判断。
     * @param targetType targetType 字段，来源于当前接口入参或内部调用上下文。
     * @return 推荐聚合数据，键名与前端展示字段保持一致。
     */
    private Map<Long, Map<Long, Double>> getUserItemMatrix(String targetType) {
        List<Map<String, Object>> rawData = browseHistoryMapper.getUserScores(targetType);
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        for (Map<String, Object> row : rawData) {
            Number userId = (Number) row.get("user_id");
            Number targetId = (Number) row.get("target_id");
            Double score = ((Number) row.get("total_score")).doubleValue();

            matrix.computeIfAbsent(userId.longValue(), k -> new HashMap<>()).put(targetId.longValue(), score);
        }

        return matrix;
    }
}
