package dev.dong4j.ai.spring;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

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
@EnableAutoConfiguration
public class StartedApplication {

    /**
     * 主方法, 用于启动 Spring Boot 应用程序
     *
     * <p>该方法通过 SpringApplication 运行指定的 Spring Boot 应用类, 这是一个命令行应用, 不需要 Web 服务器.
     *
     * @param args 启动参数, 通常为命令行传入的参数
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(StartedApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);

        // spring-ai-starter-model-openai 自动注入了 OpenAiChatModel
        OpenAiChatModel chatModel = ctx.getBean(OpenAiChatModel.class);
        ChatClient client = ChatClient.create(chatModel);

        String reply = client.prompt("我说 ping, 你说 pong").call().content();
        System.out.println("AI 回复: " + reply);
        ctx.close();
    }
}
