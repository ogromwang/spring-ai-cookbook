package dev.dong4j.ai.spring.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天客户端配置类
 * <p>
 * 用于配置和初始化不同的聊天客户端实例, 支持 OpenAI 和 Anthropic 等聊天模型的集成.
 * 提供了默认配置的 OpenAI 聊天客户端和直接使用的 Anthropic 聊天客户端.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.23
 * @since 1.0.0
 */
@Configuration
public class ChatClientConfig {

    /**
     * 创建并返回一个配置好的 OpenAI 聊天客户端实例
     * <p>
     * 该方法使用提供的 OpenAiChatModel 对象进行配置, 设置默认的模型和温度参数, 然后创建并返回一个 ChatClient 实例.
     *
     * @param chatModel 用于构建聊天客户端的模型配置对象
     * @return 配置好的 ChatClient 实例
     */
    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        final OpenAiChatModel build = chatModel.mutate().defaultOptions(OpenAiChatOptions.builder()
                                                                            .model("qwen2.5-14b-instruct")
                                                                            .temperature(0.7)
                                                                            // 注意：如果是思考模型, 因为 extraBody 会创建嵌套的 extra_body 对象
                                                                            // 但通义千问 API 需要参数直接在请求体顶层
                                                                            // 因此当前无法通过 extraBody 设置 enable_thinking 参数
                                                                            // 
                                                                            // 解决方案：
                                                                            // 1. 等待 Spring AI 未来版本支持将 extraBody 参数提升到顶层
                                                                            // 2. 自定义 OpenAiChatModel 实现来修改请求体
                                                                            // 3. 通过自定义 RestClient 拦截器修改请求（需要额外配置）
                                                                            // 4. 查看通义千问 API 文档，看是否支持通过其他方式控制思考功能
                                                                            .build()).build();
        return ChatClient.create(build);
    }

    /**
     * 创建并返回一个 Anthropic 聊天客户端实例
     * <p>
     * 根据提供的 Anthropic 聊天模型配置, 创建并初始化一个 ChatClient 对象
     *
     * @param chatModel Anthropic 聊天模型配置对象
     * @return 创建的 ChatClient 实例
     */
    @Bean
    public ChatClient anthropicChatClient(AnthropicChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}