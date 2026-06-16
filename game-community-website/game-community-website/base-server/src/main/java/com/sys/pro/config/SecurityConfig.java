package com.sys.pro.config;

import com.alibaba.fastjson.JSONObject;
import com.sys.pro.common.CommonResult;
import com.sys.pro.common.GlobalErrorCodeConstants;
import com.sys.pro.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

/**
 * Spring Security 鉴权配置。
 * <p>
 * 查询类接口允许游客访问，发帖、评论、购买、管理等写操作统一走 JWT 登录校验。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 提供密码哈希组件，统一处理登录和注册时的密码加密。
     * @return 安全认证在该步骤产出的业务结果。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2 是当前推荐的抗 GPU/ASIC 暴力破解密码哈希方案，替代旧的 MD5。
        return new Argon2PasswordEncoder(16, 32, 1, 1 << 14, 3);
    }

    /**
     * 声明接口访问权限、JWT 过滤器和登录失败处理策略。
     * @param http http 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, ex) -> writeJson(response, CommonResult.error(GlobalErrorCodeConstants.UNAUTHORIZED)))
                .accessDeniedHandler((request, response, ex) -> writeJson(response, CommonResult.error(GlobalErrorCodeConstants.FORBIDDEN)))
                .and()
                .authorizeRequests()
                .antMatchers("/noLogin/**", "/ws", "/favicon.ico", "/swagger/**", "/v2/api-docs", "/doc.html", "/swagger-resources/**", "/webjars/**").permitAll()
                .antMatchers(HttpMethod.POST, "/alipay/notify").permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.GET,
                        "/game/get/**", "/game/listAll", "/game/listHot", "/game/hotCommunities", "/game/types", "/game/discounts", "/game/stats/**", "/game/recommend",
                        "/game-user/owned/summary/**",
                        "/gameRating/average-rating/**", "/gameRating/rating-count/**", "/gameRating/list/**",
                        "/post/get/**", "/post/search", "/post/hotTopics", "/post/recommend", "/post/game/**", "/post/user/**", "/post/posts/**", "/post/postsName",
                        "/comment/get/**", "/comment/reply/list/**",
                        "/news/get/**", "/news/comment/reply/list/**",
                        "/user-info/get-real/**", "/user-info/search", "/userRelation/following/**", "/userRelation/followers/**")
                .permitAll()
                .antMatchers(HttpMethod.POST, "/game/list", "/news/page", "/comment/list", "/news/comment/list")
                .permitAll()
                .anyRequest().authenticated();

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 向认证失败或权限失败的响应中写入 JSON 错误信息。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     * @param result result 字段，来源于当前接口入参或内部调用上下文。
     */
    private void writeJson(HttpServletResponse response, CommonResult<?> result) throws java.io.IOException {
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().print(JSONObject.toJSONString(result));
    }
}
