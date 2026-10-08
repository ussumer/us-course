package com.us.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 只在 Demo 配置启用时设置样例账号密码，不记录密码或 token。 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "us.demo.enabled", havingValue = "true")
public class DemoAccountInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        update("admin", "DEMO_ADMIN_PASSWORD");
        update("jack", "DEMO_STUDENT_PASSWORD");
    }

    private void update(String username, String variable) {
        String password = environment.getRequiredProperty(variable);
        if (jdbc.update("UPDATE `user` SET password=? WHERE username=?", encoder.encode(password), username) != 1) {
            throw new IllegalStateException("初始化 SQL 缺少 Demo 账号: " + username);
        }
    }
}
