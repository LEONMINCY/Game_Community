package com.sys.pro.service.impl;

import com.sys.pro.service.ContentReviewService;
import com.sys.pro.web.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 轻量内容治理实现。
 * 当前先接入敏感词规则，词库可通过 APP_SENSITIVE_WORDS 配置；后续可在这里继续接入 AI 审核。
 */
@Service
public class ContentReviewServiceImpl implements ContentReviewService {

    @Value("${app.content-review.enabled:true}")
    private boolean enabled;

    @Value("${app.content-review.sensitive-words:赌博,诈骗,色情,违法交易,人身攻击}")
    private String sensitiveWords;

    /**
     * 确保ContentReview依赖的数据或结构存在，避免运行时缺少基础配置。
     * @param scene scene 字段，来源于当前接口入参或内部调用上下文。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void ensurePublishable(String scene, String content) {
        if (!enabled || StringUtils.isBlank(content)) {
            return;
        }
        String normalizedContent = normalize(content);
        for (String word : loadSensitiveWords()) {
            if (StringUtils.isBlank(word)) {
                continue;
            }
            String normalizedWord = normalize(word);
            if (StringUtils.isNotBlank(normalizedWord) && normalizedContent.contains(normalizedWord)) {
                /**
                 * 完成ContentReview中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
                 * @return ContentReview在该步骤产出的业务结果。
                 */
                throw new ServiceException(500, scene + "包含敏感内容，请修改后再提交");
            }
        }
    }

    /**
     * 完成ContentReview中的 loadSensitiveWords 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return ContentReview列表数据。
     */
    private List<String> loadSensitiveWords() {
        return Arrays.stream(StringUtils.defaultString(sensitiveWords).split("[,，\\n\\r]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    /**
     * 完成ContentReview中的 normalize 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return ContentReview处理后的文本结果。
     */
    private String normalize(String value) {
        return Normalizer.normalize(StringUtils.defaultString(value), Normalizer.Form.NFKC)
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }
}
