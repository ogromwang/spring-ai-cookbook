package dev.dong4j.ai.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI 教程应用启动类
 * <p>
 * 该类用于启动 Spring AI 教程项目, 集成 Spring Boot 框架, 提供基础的项目运行环境和配置.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.23
 * @since 1.0.0
 */
@SpringBootApplication
public class SpringAiTutorialApplication {

    /**
     * 主方法, 用于启动 Spring Boot 应用程序
     * <p>
     * 该方法通过 SpringApplication 运行指定的 Spring Boot 应用类, 启动嵌入式服务器并开始应用运行.
     *
     * @param args 启动参数, 通常为命令行传入的参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringAiTutorialApplication.class, args);
    }

}
