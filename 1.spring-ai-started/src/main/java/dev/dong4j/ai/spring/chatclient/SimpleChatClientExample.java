package dev.dong4j.ai.spring.chatclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 简单的 ChatClient 示例
 *
 * <p>演示如何使用 Spring AI 的 ChatClient API 进行对话交互
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.26
 * @since 1.0.0
 */
@Service
public class SimpleChatClientExample {

    private final ChatClient chatClient;

    /**
     * 构造函数注入 ChatClient
     *
     * @param chatClientBuilder ChatClient.Builder 由 Spring AI 自动配置提供
     */
    public SimpleChatClientExample(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 简单的问答示例
     *
     * @param userMessage 用户消息
     * @return AI 回复内容
     */
    public String chat(String userMessage) {
        return chatClient.prompt().user(userMessage).call().content();
    }
}
