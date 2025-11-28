package dev.dong4j.ai.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类
 *
 * <p>用于启动 Spring Boot 应用程序, 包含主方法以运行应用程序
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.23
 * @since 1.0.0
 */
@SpringBootApplication
public class StartedApplication {

    /**
     * 主方法, 用于启动 Spring Boot 应用程序
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(StartedApplication.class, args);
    }
}
