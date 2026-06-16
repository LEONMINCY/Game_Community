package com.sys.pro.config;

import com.sys.pro.socket.MyWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocketConfig 集中配置WebSocket实时推送相关Spring Bean和运行参数。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MyWebSocketHandler myWebSocketHandler;

    /**
     * 注册站内私信 WebSocket 地址，支持前后台实时接收消息。
     * @param registry registry 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler,"/ws").setAllowedOrigins("*");
    }
}
