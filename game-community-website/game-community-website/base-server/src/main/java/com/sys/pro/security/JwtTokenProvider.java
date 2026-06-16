package com.sys.pro.security;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sys.pro.pojo.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Spring Security 使用的 JWT 工具。Token 只保存登录态所需的轻量用户信息。
 */
@Component
public class JwtTokenProvider {

    @Value("${security.jwt.secret:dev-only-change-me}")
    private String secret;

    @Value("${security.jwt.expire-hours:72}")
    private long expireHours;

    /**
     * 根据账号身份生成 JWT，供前端后续请求携带。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return JwtTokenProvider处理后的文本结果。
     */
    public String createToken(UserInfo userInfo) {
        Date expiresAt = new Date(System.currentTimeMillis() + expireHours * 60 * 60 * 1000);
        return JWT.create()
                .withClaim("user", JSON.toJSONString(userInfo))
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 解析JwtTokenProvider相关输入，把原始字符串或请求参数转换成业务对象。
     * @param bearerToken bearerToken 字段，来源于当前接口入参或内部调用上下文。
     * @return JwtTokenProvider在该步骤产出的业务结果。
     */
    public UserInfo parseUser(String bearerToken) {
        String token = stripBearer(bearerToken);
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
        String userJson = jwt.getClaim("user").asString();
        if (userJson != null) {
            return JSON.parseObject(userJson, UserInfo.class);
        }

        // 兼容旧版 Shiro JWT 的 claims.user 结构，减少用户升级过程中的强制下线。
        if (jwt.getClaim("claims").isNull() || jwt.getClaim("claims").asMap() == null) {
            /**
             * 完成JwtTokenProvider中的 IllegalArgumentException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return JwtTokenProvider在该步骤产出的业务结果。
             */
            throw new IllegalArgumentException("JWT user claim is missing");
        }
        Object legacyUser = jwt.getClaim("claims").asMap().get("user");
        return JSONObject.parseObject(String.valueOf(legacyUser), UserInfo.class);
    }

    /**
     * 去掉请求头中的 Bearer 前缀，得到原始 JWT。
     * @param bearerToken bearerToken 字段，来源于当前接口入参或内部调用上下文。
     * @return JwtTokenProvider处理后的文本结果。
     */
    public String stripBearer(String bearerToken) {
        if (bearerToken == null) {
            return "";
        }
        return bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;
    }
}
