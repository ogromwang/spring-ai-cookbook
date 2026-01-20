package dev.dong4j.ai.spring.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI RAG 配置类
 *
 * <p>用于配置和初始化 RAG 相关的核心组件，包括聊天客户端、向量存储、嵌入模型等。
 * 这些组件是构建检索增强生成（RAG）应用的基础。
 *
 * @author ogromwang
 * @version 1.0.0
 * @email mailto:ogromwang@gmail.com
 * @date 2025.11.29
 * @since 1.0.0
 */
@Configuration
public class RagConfig {

    /**
     * 创建并返回一个配置好的 OpenAI 聊天客户端实例
     *
     * @param chatModel 用于构建聊天客户端的模型配置对象
     * @return 配置好的 ChatClient 实例
     */
    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.create(
                chatModel
                        .mutate()
                        .defaultOptions(
                                OpenAiChatOptions.builder()
                                        .model("Qwen/Qwen3-8B")
                                        .temperature(0.7)
                                        .build())
                        .build());
    }

    /**
     * 创建 SimpleVectorStore 向量存储实例
     *
     * <p>SimpleVectorStore 是 Spring AI 提供的轻量级内存向量存储实现。
     * 在 Spring AI 0.8.0 后，InMemoryVectorStore 和 SimplePersistentVectorStore
     * 被合并为 SimpleVectorStore。
     *
     * <p>特点：
     * - 数据存储在内存中（可选持久化）
     * - 无需外部数据库依赖
     * - 适合开发测试场景
     * - 零配置，开箱即用
     *
     * @param embeddingModel 嵌入模型，用于将文本转换为向量表示
     * @return 配置好的 SimpleVectorStore 实例
     */
    @Bean
    public VectorStore simpleVectorStore(OpenAiEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel)
                .build();
    }
}
