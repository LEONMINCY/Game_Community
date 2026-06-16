package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.mapper.AiMessageMapper;
import com.sys.pro.pojo.AiMessage;
import com.sys.pro.service.AiMessageService;
import org.springframework.stereotype.Service;

/**
 * AiMessageServiceImpl 承接AI消息核心业务规则，协调数据访问、缓存、通知和外部服务。
 */
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements AiMessageService {
}
