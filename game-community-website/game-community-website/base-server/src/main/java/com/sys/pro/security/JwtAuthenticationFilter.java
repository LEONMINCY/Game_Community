package com.sys.pro.security;

import com.alibaba.fastjson.JSONObject;
import com.sys.pro.common.CommonResult;
import com.sys.pro.common.GlobalErrorCodeConstants;
import com.sys.pro.pojo.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 Authorization Bearer Token 中恢复 Spring Security 登录态。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> PUBLIC_ALL_METHOD_PATHS = List.of(
            "/noLogin/**"
    );

    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/game/get/**", "/game/listAll", "/game/listHot", "/game/hotCommunities", "/game/types", "/game/discounts", "/game/stats/**", "/game/recommend",
            "/game-user/owned/summary/**",
            "/gameRating/average-rating/**", "/gameRating/rating-count/**", "/gameRating/list/**",
            "/post/get/**", "/post/search", "/post/hotTopics", "/post/recommend", "/post/game/**", "/post/user/**", "/post/posts/**", "/post/postsName",
            "/comment/get/**", "/comment/reply/list/**",
            "/news/get/**", "/news/comment/reply/list/**",
            "/user-info/get-real/**", "/user-info/search", "/noLogin/user-info/search",
            "/userRelation/following/**", "/userRelation/followers/**"
    );

    private final JwtTokenProvider jwtTokenProvider;


    /**
     * 解析 JWT 并写入 Spring Security 上下文，放行游客可访问接口。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     * @param filterChain filterChain 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UserInfo userInfo = jwtTokenProvider.parseUser(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userInfo,
                    null,
                    authorities(userInfo)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            if (isPublicRequest(request) || isPublicGet(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().print(JSONObject.toJSONString(CommonResult.error(GlobalErrorCodeConstants.UNAUTHORIZED)));
        }
    }

    /**
     * 判断当前请求是否属于所有 HTTP 方法都允许游客访问的接口。
     * @param request 当前 HTTP 请求。
     * @return true 表示即使携带过期 Token 也应该继续放行。
     */
    private boolean isPublicRequest(HttpServletRequest request) {
        String finalPath = resolveRequestPath(request);
        return PUBLIC_ALL_METHOD_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, finalPath));
    }

    /**
     * 判断当前 GET 请求是否属于允许游客访问的查询接口。
     * @param request 当前 HTTP 请求。
     * @return true 表示 GET 接口允许游客访问。
     */
    private boolean isPublicGet(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String finalPath = resolveRequestPath(request);
        return PUBLIC_GET_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, finalPath));
    }

    /**
     * 去掉应用上下文路径，得到用于权限匹配的请求路径。
     * @param request 当前 HTTP 请求。
     * @return 不包含 contextPath 的接口路径。
     */
    private String resolveRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path;
    }

    /**
     * 根据账号角色生成 Spring Security 权限集合。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return JwtAuthentication列表数据。
     */
    private List<SimpleGrantedAuthority> authorities(UserInfo userInfo) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (userInfo == null) {
            return authorities;
        }
        if (userInfo.getRoleId() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + userInfo.getRoleId()));
        }
        if (userInfo.getPermissions() != null) {
            userInfo.getPermissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        }
        return authorities;
    }
}
