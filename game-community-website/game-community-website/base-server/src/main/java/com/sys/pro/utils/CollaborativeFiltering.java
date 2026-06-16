package com.sys.pro.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CollaborativeFiltering 提供CollaborativeFiltering相关工具方法，供多个业务模块复用。
 */
@Slf4j
public class CollaborativeFiltering {

    /**
     * 完成CollaborativeFiltering中的 calculateUserSimilarity 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userItemMatrix userItemMatrix 字段，来源于当前接口入参或内部调用上下文。
     * @return CollaborativeFiltering聚合数据，键名与前端展示字段保持一致。
     */
    public static Map<Long, Map<Long, Double>> calculateUserSimilarity(Map<Long, Map<Long, Double>> userItemMatrix) {
        Map<Long, Map<Long, Double>> similarityMatrix = new HashMap<>();

        List<Long> userIds = new ArrayList<>(userItemMatrix.keySet());
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i; j < userIds.size(); j++) {
                Long userA = userIds.get(i);
                Long userB = userIds.get(j);

                double similarity = cosineSimilarity(userItemMatrix.get(userA), userItemMatrix.get(userB));

                similarityMatrix.computeIfAbsent(userA, k -> new HashMap<>()).put(userB, similarity);
                similarityMatrix.computeIfAbsent(userB, k -> new HashMap<>()).put(userA, similarity);
            }
        }

        return similarityMatrix;
    }

    /**
     * 完成CollaborativeFiltering中的 cosineSimilarity 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param vectorA vectorA 字段，来源于当前接口入参或内部调用上下文。
     * @param vectorB vectorB 字段，来源于当前接口入参或内部调用上下文。
     * @return CollaborativeFiltering在该步骤产出的业务结果。
     */
    private static double cosineSimilarity(Map<Long, Double> vectorA, Map<Long, Double> vectorB) {
        Set<Long> commonKeys = new HashSet<>(vectorA.keySet());
        commonKeys.retainAll(vectorB.keySet());

        double dotProduct = 0.0, normA = 0.0, normB = 0.0;

        for (Long key : commonKeys) {
            dotProduct += vectorA.get(key) * vectorB.get(key);
        }
        for (Double value : vectorA.values()) {
            normA += value * value;
        }
        for (Double value : vectorB.values()) {
            normB += value * value;
        }

        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 完成CollaborativeFiltering中的 recommendForUser 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     * @param userItemMatrix userItemMatrix 字段，来源于当前接口入参或内部调用上下文。
     * @param similarityMatrix similarityMatrix 字段，来源于当前接口入参或内部调用上下文。
     * @param topN topN 字段，来源于当前接口入参或内部调用上下文。
     * @return CollaborativeFiltering列表数据。
     */
    public static List<Long> recommendForUser(
            Long targetUserId,
            Map<Long, Map<Long, Double>> userItemMatrix,
            Map<Long, Map<Long, Double>> similarityMatrix,
            int topN) {

        log.info("targetUserId:{},userItemMatrix:{},similarityMatrix:{}", targetUserId, userItemMatrix, similarityMatrix);
        Map<Long, Double> predictedScores = new HashMap<>();

        Map<Long, Double> longDoubleMap = similarityMatrix.get(targetUserId);
        if (longDoubleMap == null) {
            return Collections.emptyList();
        }
        for (Long otherUserId : longDoubleMap.keySet()) {
            if (!targetUserId.equals(otherUserId)) {
                double similarity = longDoubleMap.get(otherUserId);
                Map<Long, Double> otherUserItems = userItemMatrix.get(otherUserId);

                for (Map.Entry<Long, Double> entry : otherUserItems.entrySet()) {
                    Long itemId = entry.getKey();
                    Double itemScore = entry.getValue();

                    if (!userItemMatrix.get(targetUserId).containsKey(itemId)) {
                        predictedScores.put(itemId, predictedScores.getOrDefault(itemId, 0.0) + similarity * itemScore);
                    }
                }
            }
        }

        // 按分数排序，返回前 N 个目标 ID
        return predictedScores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
