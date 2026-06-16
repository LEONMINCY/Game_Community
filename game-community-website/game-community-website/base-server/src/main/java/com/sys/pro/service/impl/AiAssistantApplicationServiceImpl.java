package com.sys.pro.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sys.pro.dto.AiChatRequest;
import com.sys.pro.dto.AiChatResponse;
import com.sys.pro.dto.AiConversationRequest;
import com.sys.pro.pojo.AiConversation;
import com.sys.pro.pojo.AiMessage;
import com.sys.pro.service.AiAssistantApplicationService;
import com.sys.pro.service.AiCommunityContextService;
import com.sys.pro.service.AiConversationService;
import com.sys.pro.service.AiMessageService;
import com.sys.pro.service.MiniMaxAiService;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 助手会话和消息编排实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantApplicationServiceImpl implements AiAssistantApplicationService {

    private final AiConversationService aiConversationService;
    private final AiMessageService aiMessageService;
    private final AiCommunityContextService aiCommunityContextService;
    private final MiniMaxAiService miniMaxAiService;

    /**
     * 读取 AI 助手历史会话，恢复用户之前的问答上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手列表数据。
     */
    @Override
    public List<AiConversation> conversations(Integer userId) {
        return aiConversationService.list(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getDeleted, false)
                .orderByDesc(AiConversation::getUpdateTime));
    }

    /**
     * 创建 AI 助手会话，保存用户与模型的上下文边界。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @return AI 助手在该步骤产出的业务结果。
     */
    @Override
    public AiConversation createConversation(Integer userId, AiConversationRequest request) {
        return createNewConversation(userId, request == null ? null : request.getTitle());
    }

    /**
     * 读取指定 AI 会话的消息记录。
     * @param conversationId conversation 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手列表数据。
     */
    @Override
    public List<AiMessage> messages(Long conversationId, Integer userId) {
        if (getOwnedConversation(conversationId, userId) == null) {
            /**
             * 完成AI 助手中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return AI 助手在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "会话不存在");
        }
        return listMessages(conversationId, userId);
    }

    /**
     * 清理指定时间范围内的数据记录，减少后台列表干扰。
     * @param conversationId conversation 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手列表数据。
     */
    @Override
    public List<AiMessage> clear(Long conversationId, Integer userId) {
        AiConversation conversation = getOwnedConversation(conversationId, userId);
        if (conversation == null) {
            /**
             * 完成AI 助手中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return AI 助手在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "会话不存在");
        }
        aiMessageService.remove(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId));
        conversation.setTitle("新对话");
        conversation.setUpdateTime(LocalDateTime.now());
        aiConversationService.updateById(conversation);
        return listMessages(conversationId, userId);
    }

    /**
     * 按主键删除AI 助手记录，并让业务层同步处理关联状态。
     * @param conversationId conversation 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    @Override
    public void delete(Long conversationId, Integer userId) {
        AiConversation conversation = getOwnedConversation(conversationId, userId);
        if (conversation == null) {
            /**
             * 完成AI 助手中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return AI 助手在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "会话不存在");
        }
        conversation.setDeleted(true);
        conversation.setUpdateTime(LocalDateTime.now());
        aiConversationService.updateById(conversation);
    }

    /**
     * 接收用户发给 AI 助手的消息，并返回模型回复结果。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手在该步骤产出的业务结果。
     */
    @Override
    public AiChatResponse chat(AiChatRequest request, Integer userId) {
        List<String> imageUrls = normalizeImages(request == null ? null : request.getImageUrls());
        if (request == null || (!StringUtils.hasText(request.getContent()) && imageUrls.isEmpty())) {
            /**
             * 完成AI 助手中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return AI 助手在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "请输入问题或选择图片");
        }
        AiConversation conversation = getOrCreateConversation(userId, request);
        if (conversation == null) {
            /**
             * 完成AI 助手中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return AI 助手在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "会话不存在");
        }

        String content = StringUtils.hasText(request.getContent()) ? request.getContent().trim() : "请分析我发送的图片。";
        AiMessage userMessage = buildMessage(conversation.getId(), userId, "user", content, imageUrls);
        List<AiMessage> history = listMessages(conversation.getId(), userId);
        history.add(userMessage);

        String answer;
        try {
            answer = miniMaxAiService.chat(history, aiCommunityContextService.buildContext());
        } catch (Exception e) {
            log.warn("AI assistant chat failed", e);
            /**
             * 完成AI 助手中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return AI 助手在该步骤产出的业务结果。
             */
            throw new ServiceException(500, e.getMessage());
        }

        AiMessage assistantMessage = buildMessage(conversation.getId(), userId, "assistant", answer, new ArrayList<>());
        aiMessageService.save(userMessage);
        aiMessageService.save(assistantMessage);

        conversation.setTitle(resolveConversationTitle(conversation.getTitle(), content));
        conversation.setUpdateTime(LocalDateTime.now());
        aiConversationService.updateById(conversation);

        AiChatResponse response = new AiChatResponse();
        response.setConversation(conversation);
        response.setMessages(listMessages(conversation.getId(), userId));
        response.setAnswer(answer);
        return response;
    }

    /**
     * 读取AI 助手的 OrCreateConversation 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @return AI 助手在该步骤产出的业务结果。
     */
    private AiConversation getOrCreateConversation(Integer userId, AiChatRequest request) {
        if (request.getConversationId() == null) {
            return createNewConversation(userId, buildTitle(request.getContent()));
        }
        return getOwnedConversation(request.getConversationId(), userId);
    }

    /**
     * 完成AI 助手中的 createNewConversation 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param title title 字段，来源于当前接口入参或内部调用上下文。
     * @return AI 助手在该步骤产出的业务结果。
     */
    private AiConversation createNewConversation(Integer userId, String title) {
        LocalDateTime now = LocalDateTime.now();
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(StringUtils.hasText(title) ? buildTitle(title) : "新对话");
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        conversation.setDeleted(false);
        aiConversationService.save(conversation);
        return conversation;
    }

    /**
     * 读取AI 助手的 OwnedConversation 数据，供页面展示或后续业务判断。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手在该步骤产出的业务结果。
     */
    private AiConversation getOwnedConversation(Long id, Integer userId) {
        return aiConversationService.getOne(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getId, id)
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getDeleted, false)
                .last("LIMIT 1"));
    }

    /**
     * 汇总AI 助手列表数据，供前端列表、下拉框或统计模块使用。
     * @param conversationId conversation 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手列表数据。
     */
    private List<AiMessage> listMessages(Long conversationId, Integer userId) {
        return aiMessageService.list(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getDeleted, false)
                .orderByAsc(AiMessage::getCreateTime));
    }

    /**
     * 组装AI 助手所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param conversationId conversation 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param role role 字段，来源于当前接口入参或内部调用上下文。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @param imageUrls imageUrls 字段，来源于当前接口入参或内部调用上下文。
     * @return AI 助手在该步骤产出的业务结果。
     */
    private AiMessage buildMessage(Long conversationId, Integer userId, String role, String content, List<String> imageUrls) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        if (imageUrls != null && !imageUrls.isEmpty()) {
            message.setImageUrls(JSON.toJSONString(imageUrls));
        }
        message.setCreateTime(LocalDateTime.now());
        message.setDeleted(false);
        return message;
    }

    /**
     * 完成AI 助手中的 normalizeImages 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param imageUrls imageUrls 字段，来源于当前接口入参或内部调用上下文。
     * @return AI 助手列表数据。
     */
    private List<String> normalizeImages(List<String> imageUrls) {
        if (imageUrls == null) {
            return new ArrayList<>();
        }
        return imageUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .limit(4)
                .collect(Collectors.toList());
    }

    /**
     * 完成AI 助手中的 resolveConversationTitle 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param oldTitle oldTitle 字段，来源于当前接口入参或内部调用上下文。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @return AI 助手处理后的文本结果。
     */
    private String resolveConversationTitle(String oldTitle, String content) {
        if (!StringUtils.hasText(oldTitle) || "新对话".equals(oldTitle)) {
            return buildTitle(content);
        }
        return oldTitle;
    }

    /**
     * 组装AI 助手所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @return AI 助手处理后的文本结果。
     */
    private String buildTitle(String content) {
        String text = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (text.length() > 24) {
            return text.substring(0, 24) + "...";
        }
        return StringUtils.hasText(text) ? text : "新对话";
    }
}
