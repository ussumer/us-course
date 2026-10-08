package com.us.gateway.config;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

@Data
@Component
@ConfigurationProperties(prefix = "us.auth")
public class AuthProperties implements InitializingBean {

    private Set<String> excludePath = new HashSet<>();
    private Set<String> adminPaths = new HashSet<>();
    private Set<Long> adminRoleIds = new HashSet<>(Arrays.asList(1L));

    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加默认不拦截的路径
        excludePath.add("/error/**");
        excludePath.add("/jwks");
        excludePath.add("/accounts/login");
        excludePath.add("/accounts/admin/login");
        excludePath.add("/accounts/refresh");
    }
}
