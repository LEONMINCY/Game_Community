package com.sys.pro.service;

/**
 * 内容治理服务，统一处理用户发布内容的敏感词和规则审核。
 */
public interface ContentReviewService {

    /**
     * 确保ContentReview依赖的数据或结构存在，避免运行时缺少基础配置。
     * @param scene scene 字段，来源于当前接口入参或内部调用上下文。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     */
    void ensurePublishable(String scene, String content);
}
