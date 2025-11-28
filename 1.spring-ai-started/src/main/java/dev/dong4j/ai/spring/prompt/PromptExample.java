package dev.dong4j.ai.spring.prompt;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.stereotype.Service;

/**
 * 提示词管理示例
 *
 * <p>演示 Spring AI 的提示词管理功能，包括模板变量替换和多种消息类型
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.26
 * @since 1.0.0
 */
@Service
public class PromptExample {

    private final ChatClient chatClient;

    /**
     * 构造函数注入 ChatClient
     *
     * @param chatClientBuilder ChatClient.Builder 由 Spring AI 自动配置提供
     */
    public PromptExample(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 简单提示词示例
     *
     * @param userMessage 用户消息
     * @return AI 回复内容
     */
    public String simplePrompt(String userMessage) {
        return chatClient.prompt(userMessage).call().content();
    }

    /**
     * 使用 System 提示词示例
     *
     * <p>设置系统提示词，定义 AI 的角色和行为
     *
     * @param userMessage 用户消息
     * @return AI 回复内容
     */
    public String promptWithSystem(String userMessage) {
        return chatClient
                .prompt()
                .system("你是一个专业的 Java 开发工程师，擅长 Spring 框架。请用一句话回答问题。")
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * 提示词模板示例
     *
     * <p>使用模板变量，运行时替换
     *
     * @param language 语言
     * @param topic 主题
     * @return AI 回复内容
     */
    public String promptWithTemplate(String language, String topic) {
        return chatClient
                .prompt()
                .user(
                        u ->
                                u.text("用 {language} 一句话解释 {topic}")
                                        .param("language", language)
                                        .param("topic", topic))
                .call()
                .content();
    }

    /**
     * 自定义模板分隔符示例
     *
     * <p>当提示词中包含 JSON 或其他使用 {} 的内容时，可以自定义模板分隔符
     *
     * @param code 代码内容
     * @return AI 回复内容
     */
    public String customDelimiterTemplate(String code) {
        return chatClient
                .prompt()
                .user(u -> u.text("请用一句话审查以下代码: <code>").param("code", "public class Test { }"))
                .templateRenderer(
                        StTemplateRenderer.builder()
                                .startDelimiterToken('<')
                                .endDelimiterToken('>')
                                .build())
                .call()
                .content();
    }
}
