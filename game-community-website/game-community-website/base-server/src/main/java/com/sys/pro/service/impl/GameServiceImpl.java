package com.sys.pro.service.impl;

import com.sys.pro.pojo.Game;
import com.sys.pro.service.GameService;
import com.sys.pro.mapper.GameMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * GameServiceImpl 承接游戏资料核心业务规则，协调数据访问、缓存、通知和外部服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameServiceImpl extends ServiceImpl<GameMapper, Game> implements GameService {

}
