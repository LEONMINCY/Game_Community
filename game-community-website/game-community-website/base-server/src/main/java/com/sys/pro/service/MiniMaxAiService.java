package com.sys.pro.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sys.pro.pojo.AiMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI 助手模型调用服务。
 * <p>
 * 默认兼容原来的 MiniMax 调用方式；同时支持在 api-key 一行中写入
 * “服务商|模型|密钥” 或 “custom|模型|接口地址|密钥”，这样后续切换模型
 * 或服务商时，只需要调整配置中的 api-key 这一行。
 */
@Service
public class MiniMaxAiService {

    private static final Logger log = LoggerFactory.getLogger(MiniMaxAiService.class);

    private static final String SYSTEM_PROMPT =
            "你是游戏社区的AI助手，专注回答游戏攻略、社区帖子写作、游戏推荐、活动与社区规则相关问题。"
                    + "回答要简洁、友好、中文为主，不要编造未知数据。"
                    + "如果用户询问本站游戏、帖子、评价、销量、热门内容等信息，请优先依据系统提供的站内数据摘要回答，并说明数据来自当前社区摘要。"
                    + "如果用户发送图片，请结合图片理解结果分析图片，不确定时要明确说明。";

    private static final String MINIMAX_PROVIDER = "minimax";

    @Value("${minimax.key-line:}")
    private String keyLine;

    @Value("${minimax.api-key:}")
    private String legacyApiKey;

    @Value("${MINIMAX_API_KEY:}")
    private String environmentApiKey;

    @Value("${minimax.api-url:${MINIMAX_API_URL:https://api.minimaxi.com/v1/chat/completions}}")
    private String apiUrl;

    @Value("${minimax.model:${MINIMAX_MODEL:MiniMax-M2.7}}")
    private String model;

    @Value("${minimax.max-completion-tokens:${MINIMAX_MAX_COMPLETION_TOKENS:1024}}")
    private Integer maxCompletionTokens;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 使用默认站内上下文调用 AI 助手。
     *
     * @param history 当前会话消息历史。
     * @return AI 助手生成的回复文本。
     */
    public String chat(List<AiMessage> history) {
        return chat(history, null);
    }

    /**
     * 调用当前配置的对话模型，返回 AI 助手回复。
     *
     * @param history 当前会话消息历史。
     * @param siteContext 当前网站游戏、帖子、评论等数据摘要。
     * @return AI 助手生成的回复文本。
     */
    public String chat(List<AiMessage> history, String siteContext) {
        AiProviderConfig providerConfig = resolveProviderConfig();
        if (StringUtils.isBlank(providerConfig.getApiKey())) {
            throw new IllegalStateException("请先配置 AI API Key");
        }

        String latestImageAnalysis = providerConfig.isMiniMaxProvider()
                ? buildLatestImageAnalysis(history, providerConfig)
                : "";
        boolean includeImagesInChat = providerConfig.isSupportImageUrlContent()
                && !StringUtils.isNotBlank(latestImageAnalysis);

        JSONObject request = new JSONObject();
        request.put("model", providerConfig.getModelName());
        request.put("stream", false);
        request.put(providerConfig.getTokenLimitField(), getSafeMaxCompletionTokens());
        request.put("messages", buildMessages(history, siteContext, latestImageAnalysis, includeImagesInChat));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + providerConfig.getApiKey().trim());

        Exception lastError = null;
        String lastDetail = "";
        for (String endpoint : providerConfig.getChatEndpoints()) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        endpoint,
                        HttpMethod.POST,
                        new HttpEntity<>(request.toJSONString(), headers),
                        String.class
                );
                return parseAnswer(response.getBody());
            } catch (HttpStatusCodeException e) {
                lastError = e;
                lastDetail = buildApiFailureDetail(e);
                log.warn("AI API request failed. provider=" + providerConfig.getProviderName()
                        + ", endpoint=" + endpoint
                        + ", key=" + maskSecret(providerConfig.getApiKey())
                        + ", status=" + e.getStatusCode()
                        + ", body=" + sanitizeApiError(e.getResponseBodyAsString()));
            } catch (Exception e) {
                lastError = e;
                lastDetail = sanitizeApiError(e.getMessage());
                log.warn("AI API request failed. provider=" + providerConfig.getProviderName()
                        + ", endpoint=" + endpoint, e);
            }
        }
        throw new IllegalStateException("调用AI模型失败：" + lastDetail, lastError);
    }

    /**
     * 解析 AI 服务配置。
     * <p>
     * 兼容两种写法：
     * 1. 旧写法：api-key 直接填写 MiniMax key。
     * 2. 一行切换写法：provider|model|key，或 custom|model|endpoint|key。
     *
     * @return 当前生效的 AI 服务配置。
     */
    private AiProviderConfig resolveProviderConfig() {
        String rawConfig = readEffectiveKeyLine();
        if (rawConfig.contains("|")) {
            return resolveInlineProviderConfig(rawConfig);
        }
        return buildMiniMaxConfig(rawConfig, model, buildRawMiniMaxEndpoints());
    }

    /**
     * 读取当前真正使用的 AI key 配置。
     * <p>
     * 优先使用 minimax.key-line，避免系统环境变量 MINIMAX_API_KEY 覆盖
     * application.yml 中刚替换的新 key；没有配置 key-line 时再兼容旧的
     * minimax.api-key 写法。
     *
     * @return 当前生效的一行 key 配置。
     */
    private String readEffectiveKeyLine() {
        if (StringUtils.isNotBlank(keyLine)) {
            return StringUtils.trimToEmpty(keyLine);
        }
        return StringUtils.trimToEmpty(legacyApiKey);
    }

    /**
     * 解析写在 api-key 一行里的模型供应商配置。
     *
     * @param rawConfig api-key 原始配置值。
     * @return 当前生效的 AI 服务配置。
     */
    private AiProviderConfig resolveInlineProviderConfig(String rawConfig) {
        String[] parts = rawConfig.split("\\|", -1);
        String providerName = StringUtils.trimToEmpty(parts[0]).toLowerCase(Locale.ROOT);

        if ("custom".equals(providerName)) {
            if (parts.length < 4) {
                throw new IllegalStateException("custom AI 配置格式应为：custom|模型名|接口地址|API Key");
            }
            List<String> endpoints = new ArrayList<>();
            addEndpoint(endpoints, parts[2]);
            return new AiProviderConfig(
                    "custom",
                    StringUtils.defaultIfBlank(parts[1], model),
                    StringUtils.trimToEmpty(parts[3]),
                    "max_tokens",
                    endpoints,
                    false,
                    false
            );
        }

        if (parts.length < 2) {
            throw new IllegalStateException("AI 配置格式应为：服务商|模型名|API Key");
        }
        String modelName = parts.length >= 3 ? parts[1] : "";
        String keyValue = parts.length >= 3 ? parts[2] : parts[1];
        return buildPresetProviderConfig(providerName, modelName, keyValue);
    }

    /**
     * 根据服务商名称生成内置的 OpenAI-compatible 调用配置。
     *
     * @param providerName 服务商名称。
     * @param modelName 模型名称。
     * @param keyValue API Key。
     * @return 当前服务商的调用配置。
     */
    private AiProviderConfig buildPresetProviderConfig(String providerName, String modelName, String keyValue) {
        List<String> endpoints = new ArrayList<>();
        String actualModel = modelName;
        String tokenField = "max_tokens";
        boolean miniMaxProvider = false;
        boolean supportImageUrlContent = false;

        switch (providerName) {
            case MINIMAX_PROVIDER:
                actualModel = StringUtils.defaultIfBlank(modelName, "MiniMax-M2.7");
                tokenField = "max_completion_tokens";
                miniMaxProvider = true;
                supportImageUrlContent = true;
                addEndpoint(endpoints, "https://api.minimaxi.com/v1/chat/completions");
                addEndpoint(endpoints, "https://api.minimax.io/v1/chat/completions");
                break;
            case "openai":
                actualModel = StringUtils.defaultIfBlank(modelName, "gpt-4o-mini");
                tokenField = "max_completion_tokens";
                addEndpoint(endpoints, "https://api.openai.com/v1/chat/completions");
                break;
            case "deepseek":
                actualModel = StringUtils.defaultIfBlank(modelName, "deepseek-chat");
                addEndpoint(endpoints, "https://api.deepseek.com/chat/completions");
                break;
            case "qwen":
                actualModel = StringUtils.defaultIfBlank(modelName, "qwen-plus");
                addEndpoint(endpoints, "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
                break;
            case "moonshot":
                actualModel = StringUtils.defaultIfBlank(modelName, "moonshot-v1-8k");
                addEndpoint(endpoints, "https://api.moonshot.cn/v1/chat/completions");
                break;
            case "siliconflow":
                actualModel = StringUtils.defaultIfBlank(modelName, "Qwen/Qwen2.5-7B-Instruct");
                addEndpoint(endpoints, "https://api.siliconflow.cn/v1/chat/completions");
                break;
            default:
                throw new IllegalStateException("不支持的 AI 服务商：" + providerName
                        + "，请使用 minimax/openai/deepseek/qwen/moonshot/siliconflow/custom");
        }

        return new AiProviderConfig(
                providerName,
                actualModel,
                StringUtils.trimToEmpty(keyValue),
                tokenField,
                endpoints,
                miniMaxProvider,
                supportImageUrlContent
        );
    }

    /**
     * 构建兼容旧配置的 MiniMax 调用配置。
     *
     * @param keyValue MiniMax API Key。
     * @param modelName MiniMax 模型名称。
     * @param endpoints MiniMax 文本对话接口候选地址。
     * @return MiniMax 调用配置。
     */
    private AiProviderConfig buildMiniMaxConfig(String keyValue, String modelName, List<String> endpoints) {
        return new AiProviderConfig(
                MINIMAX_PROVIDER,
                StringUtils.defaultIfBlank(modelName, "MiniMax-M2.7"),
                StringUtils.trimToEmpty(keyValue),
                "max_completion_tokens",
                endpoints,
                true,
                true
        );
    }

    /**
     * 生成旧 MiniMax 配置可尝试的网关列表，优先使用配置中的网关。
     *
     * @return MiniMax 文本对话接口候选列表。
     */
    private List<String> buildRawMiniMaxEndpoints() {
        List<String> endpoints = new ArrayList<>();
        addEndpoint(endpoints, apiUrl);
        addEndpoint(endpoints, "https://api.minimaxi.com/v1/chat/completions");
        addEndpoint(endpoints, "https://api.minimax.io/v1/chat/completions");
        return endpoints;
    }

    /**
     * 把候选网关加入列表，同时去掉空地址和重复地址。
     *
     * @param endpoints 当前候选网关列表。
     * @param endpoint 待加入的网关地址。
     */
    private void addEndpoint(List<String> endpoints, String endpoint) {
        if (StringUtils.isBlank(endpoint)) {
            return;
        }
        String value = endpoint.trim();
        if (!endpoints.contains(value)) {
            endpoints.add(value);
        }
    }

    /**
     * 构建对话模型需要的 messages 参数。
     *
     * @param history 当前会话消息历史。
     * @param siteContext 当前网站数据摘要。
     * @param latestImageAnalysis 最新图片分析结果。
     * @param includeImagesInChat 是否把图片链接作为多模态内容发送。
     * @return 对话模型 messages 数组。
     */
    private JSONArray buildMessages(List<AiMessage> history, String siteContext,
                                    String latestImageAnalysis, boolean includeImagesInChat) {
        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("name", "GameCommunityAI");
        system.put("content", buildSystemContent(siteContext));
        messages.add(system);

        int start = history == null ? 0 : Math.max(0, history.size() - 20);
        if (history != null) {
            for (int i = start; i < history.size(); i++) {
                AiMessage item = history.get(i);
                if (item == null || item.getContent() == null) {
                    continue;
                }
                String role = "assistant".equals(item.getRole()) ? "assistant" : "user";
                JSONObject message = new JSONObject();
                message.put("role", role);
                message.put("name", "assistant".equals(role) ? "MiniMaxAI" : "User");
                String content = item.getContent();
                boolean isLatest = i == history.size() - 1;
                List<String> imageUrls = parseImageUrls(item.getImageUrls());

                if ("user".equals(role) && isLatest && StringUtils.isNotBlank(latestImageAnalysis)) {
                    content = content + "\n\n[用户本轮上传图片的理解结果]\n" + latestImageAnalysis;
                }
                if ("user".equals(role) && includeImagesInChat && !imageUrls.isEmpty()) {
                    message.put("content", buildMultiModalContent(content, imageUrls));
                } else {
                    if ("user".equals(role) && !isLatest && !imageUrls.isEmpty()) {
                        content = content + "\n[历史消息包含 " + imageUrls.size() + " 张图片，已在当轮回复中分析。]";
                    }
                    message.put("content", content);
                }
                messages.add(message);
            }
        }
        return messages;
    }

    /**
     * 拼接系统提示词和网站数据摘要，让 AI 知道自己需要服务游戏社区场景。
     *
     * @param siteContext 当前网站数据摘要。
     * @return 发送给模型的 system 文本。
     */
    private String buildSystemContent(String siteContext) {
        StringBuilder systemContent = new StringBuilder(SYSTEM_PROMPT);
        if (StringUtils.isNotBlank(siteContext)) {
            systemContent.append("\n\n[当前网站数据摘要]\n").append(siteContext);
        }
        return systemContent.toString();
    }

    /**
     * 构建用户文本和图片链接组成的多模态消息内容。
     *
     * @param text 用户输入文本。
     * @param imageUrls 用户上传图片地址。
     * @return OpenAI-compatible 多模态 content 数组。
     */
    private JSONArray buildMultiModalContent(String text, List<String> imageUrls) {
        JSONArray content = new JSONArray();
        if (StringUtils.isNotBlank(text)) {
            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            textPart.put("text", text);
            content.add(textPart);
        }
        for (String imageUrl : imageUrls) {
            JSONObject imagePart = new JSONObject();
            imagePart.put("type", "image_url");
            JSONObject image = new JSONObject();
            image.put("url", imageUrl);
            imagePart.put("image_url", image);
            content.add(imagePart);
        }
        return content;
    }

    /**
     * 对用户最近一轮上传的图片做预分析，便于文本模型结合图片信息回答。
     *
     * @param history 当前会话消息历史。
     * @param providerConfig 当前 AI 服务商配置。
     * @return 最新图片的中文分析摘要。
     */
    private String buildLatestImageAnalysis(List<AiMessage> history, AiProviderConfig providerConfig) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        AiMessage latest = history.get(history.size() - 1);
        if (latest == null || !"user".equals(latest.getRole())) {
            return "";
        }
        List<String> imageUrls = parseImageUrls(latest.getImageUrls());
        if (imageUrls.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int index = 1;
        for (String imageUrl : imageUrls) {
            if (index > 4) {
                break;
            }
            String analysis = analyzeImage(imageUrl, latest.getContent(), index, providerConfig);
            if (StringUtils.isNotBlank(analysis)) {
                result.append("图片").append(index).append("：").append(analysis).append("\n");
            }
            index++;
        }
        return result.toString().trim();
    }

    /**
     * 调用 MiniMax 图片理解接口，提取图片中的游戏、界面或文字信息。
     *
     * @param imageUrl 图片访问地址。
     * @param userPrompt 用户本轮输入的问题。
     * @param index 图片序号，用于日志定位。
     * @param providerConfig 当前 AI 服务商配置。
     * @return 图片分析结果。
     */
    private String analyzeImage(String imageUrl, String userPrompt, int index, AiProviderConfig providerConfig) {
        JSONObject request = new JSONObject();
        request.put("prompt", StringUtils.defaultIfBlank(userPrompt, "请分析这张图片")
                + "\n请用中文描述图片内容，指出和游戏、社区帖子、截图、界面、文本或问题相关的信息。");
        request.put("image_url", imageUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + providerConfig.getApiKey().trim());

        for (String endpoint : buildCandidateVisionEndpoints(providerConfig)) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        endpoint,
                        HttpMethod.POST,
                        new HttpEntity<>(request.toJSONString(), headers),
                        String.class
                );
                return parseVisionAnswer(response.getBody());
            } catch (HttpStatusCodeException e) {
                log.warn("AI image analysis failed. imageIndex=" + index
                        + ", endpoint=" + endpoint
                        + ", status=" + e.getStatusCode()
                        + ", body=" + sanitizeApiError(e.getResponseBodyAsString()));
            } catch (Exception e) {
                log.warn("AI image analysis failed. imageIndex=" + index + ", endpoint=" + endpoint, e);
            }
        }
        return "";
    }

    /**
     * 根据文本接口候选地址推导 MiniMax 图片理解接口候选地址。
     *
     * @param providerConfig 当前 AI 服务商配置。
     * @return MiniMax 图片理解接口候选列表。
     */
    private List<String> buildCandidateVisionEndpoints(AiProviderConfig providerConfig) {
        List<String> endpoints = new ArrayList<>();
        if (!providerConfig.isMiniMaxProvider()) {
            return endpoints;
        }
        for (String endpoint : providerConfig.getChatEndpoints()) {
            if (endpoint.contains("/v1/chat/completions")) {
                addEndpoint(endpoints, endpoint.replace("/v1/chat/completions", "/v1/coding_plan/vlm"));
            }
        }
        addEndpoint(endpoints, "https://api.minimaxi.com/v1/coding_plan/vlm");
        addEndpoint(endpoints, "https://api.minimax.io/v1/coding_plan/vlm");
        return endpoints;
    }

    /**
     * 解析图片理解接口返回内容。
     *
     * @param responseBody 图片理解接口响应体。
     * @return 图片分析文本。
     */
    private String parseVisionAnswer(String responseBody) {
        JSONObject response = JSON.parseObject(responseBody);
        JSONObject baseResp = response.getJSONObject("base_resp");
        if (baseResp != null && baseResp.getIntValue("status_code") != 0) {
            throw new IllegalStateException(baseResp.getString("status_msg"));
        }
        String content = response.getString("content");
        if (StringUtils.isNotBlank(content)) {
            return stripReasoning(content);
        }
        JSONArray choices = response.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message != null) {
                return stripReasoning(readMessageContent(message));
            }
        }
        return "";
    }

    /**
     * 把数据库中保存的图片地址 JSON 转为图片地址列表。
     *
     * @param imageUrlsJson 图片地址 JSON 数组字符串。
     * @return 图片地址列表。
     */
    private List<String> parseImageUrls(String imageUrlsJson) {
        List<String> imageUrls = new ArrayList<>();
        if (StringUtils.isBlank(imageUrlsJson)) {
            return imageUrls;
        }
        try {
            JSONArray array = JSON.parseArray(imageUrlsJson);
            for (int i = 0; i < array.size(); i++) {
                String value = array.getString(i);
                if (StringUtils.isNotBlank(value)) {
                    imageUrls.add(value);
                }
            }
        } catch (Exception e) {
            log.warn("Invalid AI image url json", e);
        }
        return imageUrls;
    }

    /**
     * 解析文本对话接口响应。
     *
     * @param responseBody 文本对话接口响应体。
     * @return 模型回复文本。
     */
    private String parseAnswer(String responseBody) {
        JSONObject response = JSON.parseObject(responseBody);
        JSONObject baseResp = response.getJSONObject("base_resp");
        if (baseResp != null && baseResp.getIntValue("status_code") != 0) {
            throw new IllegalStateException(baseResp.getString("status_msg"));
        }
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("未获取到AI回复");
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        if (message == null) {
            throw new IllegalStateException("未获取到AI回复");
        }
        return stripReasoning(readMessageContent(message));
    }

    /**
     * 读取模型响应 message.content，兼容字符串和数组两种返回形态。
     *
     * @param message 模型响应中的 message 节点。
     * @return message.content 文本。
     */
    private String readMessageContent(JSONObject message) {
        Object content = message.get("content");
        if (content == null) {
            return "";
        }
        if (content instanceof String) {
            return (String) content;
        }
        return JSON.toJSONString(content);
    }

    /**
     * 把 HTTP 错误整理成前端可展示的简短错误格式。
     *
     * @param e HTTP 调用异常。
     * @return 形如“401 UNAUTHORIZED 未返回具体错误”的错误摘要。
     */
    private String buildApiFailureDetail(HttpStatusCodeException e) {
        return e.getStatusCode() + " " + sanitizeApiError(e.getResponseBodyAsString());
    }

    /**
     * 清理模型错误响应，避免前端展示过长内容或泄露 API Key。
     *
     * @param message 原始错误信息。
     * @return 可展示的错误摘要。
     */
    private String sanitizeApiError(String message) {
        if (StringUtils.isBlank(message)) {
            return "未返回具体错误";
        }
        String text = message.trim();
        text = maskKnownSecrets(text, readEffectiveKeyLine());
        text = maskKnownSecrets(text, legacyApiKey);
        text = maskKnownSecrets(text, environmentApiKey);
        if (text.length() > 300) {
            return text.substring(0, 300) + "...";
        }
        return text;
    }

    /**
     * 对日志和错误信息中的密钥做脱敏处理。
     *
     * @param text 原始文本。
     * @param rawConfig 可能包含密钥的一行配置。
     * @return 脱敏后的文本。
     */
    private String maskKnownSecrets(String text, String rawConfig) {
        if (StringUtils.isBlank(text) || StringUtils.isBlank(rawConfig)) {
            return text;
        }
        String masked = text.replace(StringUtils.trimToEmpty(rawConfig), "***");
        String secret = extractSecretFromApiKeyLine(rawConfig);
        if (StringUtils.isNotBlank(secret)) {
            masked = masked.replace(secret, "***");
        }
        return masked;
    }

    /**
     * 生成只显示首尾的密钥标识，方便确认运行时到底读了哪一串 key。
     *
     * @param secret API Key。
     * @return 脱敏后的 key 标识。
     */
    private String maskSecret(String secret) {
        String value = extractSecretFromApiKeyLine(secret);
        if (StringUtils.isBlank(value)) {
            return "未配置";
        }
        if (value.length() <= 14) {
            return "***";
        }
        return value.substring(0, 8) + "***" + value.substring(value.length() - 8);
    }

    /**
     * 从 api-key 一行配置中提取真正的密钥部分，用于日志脱敏。
     *
     * @param rawConfig api-key 原始配置值。
     * @return 真实 API Key。
     */
    private String extractSecretFromApiKeyLine(String rawConfig) {
        if (StringUtils.isBlank(rawConfig) || !rawConfig.contains("|")) {
            return rawConfig;
        }
        String[] parts = rawConfig.split("\\|", -1);
        return parts.length == 0 ? "" : StringUtils.trimToEmpty(parts[parts.length - 1]);
    }

    /**
     * 防止配置缺失或配置为非正数时，把 max token 限制恢复到安全默认值。
     *
     * @return 当前请求使用的最大回复 token 数。
     */
    private int getSafeMaxCompletionTokens() {
        if (maxCompletionTokens == null || maxCompletionTokens <= 0) {
            return 1024;
        }
        return maxCompletionTokens;
    }

    /**
     * 去掉模型返回中的思考标签，避免前端显示推理过程。
     *
     * @param content 模型原始回复。
     * @return 清理后的回复文本。
     */
    private String stripReasoning(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("(?s)<think>.*?</think>\\s*", "").trim();
    }

    /**
     * AI 服务商的最终生效配置。
     */
    private static class AiProviderConfig {

        private final String providerName;

        private final String modelName;

        private final String apiKey;

        private final String tokenLimitField;

        private final List<String> chatEndpoints;

        private final boolean miniMaxProvider;

        private final boolean supportImageUrlContent;

        /**
         * 创建一次模型调用所需的服务商配置。
         *
         * @param providerName 服务商名称。
         * @param modelName 模型名称。
         * @param apiKey API Key。
         * @param tokenLimitField token 限制字段名。
         * @param chatEndpoints 对话接口候选地址。
         * @param miniMaxProvider 是否为 MiniMax 服务商。
         * @param supportImageUrlContent 是否支持把图片链接直接放入对话消息。
         */
        private AiProviderConfig(String providerName, String modelName, String apiKey, String tokenLimitField,
                                 List<String> chatEndpoints, boolean miniMaxProvider,
                                 boolean supportImageUrlContent) {
            this.providerName = providerName;
            this.modelName = modelName;
            this.apiKey = apiKey;
            this.tokenLimitField = tokenLimitField;
            this.chatEndpoints = chatEndpoints;
            this.miniMaxProvider = miniMaxProvider;
            this.supportImageUrlContent = supportImageUrlContent;
        }

        /**
         * 获取服务商名称，主要用于日志定位。
         *
         * @return 服务商名称。
         */
        private String getProviderName() {
            return providerName;
        }

        /**
         * 获取模型名称。
         *
         * @return 模型名称。
         */
        private String getModelName() {
            return modelName;
        }

        /**
         * 获取 API Key。
         *
         * @return API Key。
         */
        private String getApiKey() {
            return apiKey;
        }

        /**
         * 获取当前服务商使用的 token 限制字段名。
         *
         * @return token 限制字段名。
         */
        private String getTokenLimitField() {
            return tokenLimitField;
        }

        /**
         * 获取对话接口候选地址。
         *
         * @return 对话接口候选地址。
         */
        private List<String> getChatEndpoints() {
            return chatEndpoints;
        }

        /**
         * 判断当前是否使用 MiniMax 服务商。
         *
         * @return 是 MiniMax 则返回 true。
         */
        private boolean isMiniMaxProvider() {
            return miniMaxProvider;
        }

        /**
         * 判断当前服务商是否支持把 image_url 直接放入 messages。
         *
         * @return 支持图片消息则返回 true。
         */
        private boolean isSupportImageUrlContent() {
            return supportImageUrlContent;
        }
    }
}
