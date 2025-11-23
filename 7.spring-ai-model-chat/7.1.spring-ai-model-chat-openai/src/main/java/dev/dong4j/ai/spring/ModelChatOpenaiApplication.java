package dev.dong4j.ai.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ModelChatOpenaiApplication
 *
 * <p>Spring Boot 应用启动类, 用于启动 Chat Model OpenAI 应用程序. 该类通过 SpringApplication.run 方法启动 Spring Boot 应用, 初始化相关配置和组件.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.23
 * @since 1.0.0
 */
@SpringBootApplication
public class ModelChatOpenaiApplication {

    /**
     * 程序的入口方法, 用于启动 Spring Boot 应用
     *
     * <p>该方法通过 SpringApplication 运行 ModelChatOpenaiApplication 类, 启动 Chat Model OpenAI 应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ModelChatOpenaiApplication.class, args);
    }
}

