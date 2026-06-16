package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.mapper.NewsCommentMapper;
import com.sys.pro.pojo.NewsComment;
import com.sys.pro.service.NewsCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * NewsCommentServiceImpl 承接新闻评论核心业务规则，协调数据访问、缓存、通知和外部服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCommentServiceImpl extends ServiceImpl<NewsCommentMapper, NewsComment> implements NewsCommentService {
}
