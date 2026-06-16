package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.mapper.AiConversationMapper;
import com.sys.pro.pojo.AiConversation;
import com.sys.pro.service.AiConversationService;
import org.springframework.stereotype.Service;

/**
 * AiConversationServiceImpl 承接AI会话核心业务规则，协调数据访问、缓存、通知和外部服务。
 */
@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation> implements AiConversationService {
}
