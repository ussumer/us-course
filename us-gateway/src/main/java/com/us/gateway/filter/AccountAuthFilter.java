package com.us.gateway.filter;

import com.us.authsdk.gateway.util.AuthUtil;
import com.us.common.domain.R;
import com.us.common.domain.dto.LoginUserDTO;
import com.us.common.exceptions.UnauthorizedException;
import com.us.common.exceptions.ForbiddenException;
import com.us.gateway.config.AuthProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.us.auth.common.constants.JwtConstants.AUTHORIZATION_HEADER;
import static com.us.auth.common.constants.JwtConstants.USER_HEADER;

@Component
public class AccountAuthFilter implements GlobalFilter, Ordered {

    private final AuthUtil authUtil;
    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public AccountAuthFilter(AuthUtil authUtil, AuthProperties authProperties) {
        this.authUtil = authUtil;
        this.authProperties = authProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取请求request信息
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethodValue();
        String path = request.getPath().toString();
        String antPath = method + ":" + path;

        // 清除外部提交的身份头；只能由经过验证的 JWT 生成身份。
        exchange = exchange.mutate().request(builder -> builder.headers(headers -> headers.remove(USER_HEADER))).build();
        boolean publicPath = isExcludePath(antPath, path);

        // 3.尝试获取用户信息
        List<String> authHeaders = exchange.getRequest().getHeaders().get(AUTHORIZATION_HEADER);
        String token = authHeaders == null ? "" : authHeaders.get(0);
        R<LoginUserDTO> r = authUtil.parseToken(token);

        // 4.如果用户是登录状态，尝试更新请求头，传递用户信息
        if(r.success()){
            exchange = exchange.mutate()
                    .request(builder -> builder.header(USER_HEADER, r.getData().getUserId().toString()))
                    .build();
        }

        if (publicPath) {
            return chain.filter(exchange);
        }
        if (!r.success()) {
            throw new UnauthorizedException(r.getCode(), r.getMsg());
        }
        for (String adminPath : authProperties.getAdminPaths()) {
            if ((antPathMatcher.match(adminPath, antPath) || antPathMatcher.match(adminPath, path))
                    && !authProperties.getAdminRoleIds().contains(r.getData().getRoleId())) {
                throw new ForbiddenException("该接口需要管理员权限");
            }
        }

        // 5.校验权限
        authUtil.checkAuth(antPath, r);

        // 6.放行
        return chain.filter(exchange);
    }

    private boolean isExcludePath(String antPath, String path) {
        for (String pathPattern : authProperties.getExcludePath()) {
            if(antPathMatcher.match(pathPattern, antPath) || antPathMatcher.match(pathPattern, path)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 1000;
    }
}
