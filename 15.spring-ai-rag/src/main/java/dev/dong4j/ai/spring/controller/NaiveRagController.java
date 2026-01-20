package dev.dong4j.ai.spring.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Naive RAG 控制器
 *
 * <p>演示最基础的 RAG 流程，也是最早期的 RAG 实现方式。
 * Naive RAG 的流程相对简单直接：
 * 1. 用户输入查询问题
 * 2. 将查询转换为向量
 * 3. 在向量数据库中检索相似文档
 * 4. 将检索到的文档作为上下文提供给 LLM
 * 5. LLM 基于上下文生成回答
 *
 * 这种方式虽然简单，但存在明显的局限性：
 * - 检索精度依赖向量相似度的准确性
 * - 缺乏对查询的优化处理
 * - 无法处理复杂的多轮对话
 * - 重排和精筛环节缺失
 *
 * @author ogromwang
 * @version 1.0.0
 * @email mailto:ogromwang@gmail.com
 * @date 2025.11.29
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rag/naive")
public class NaiveRagController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    /**
     * 构造函数，注入聊天客户端和向量存储
     *
     * @param chatClient 聊天客户端，用于与 AI 模型交互
     * @param vectorStore 向量存储，用于检索相似文档
     */
    public NaiveRagController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 基础 RAG 查询接口
     *
     * <p>演示 Naive RAG 的核心流程：查询 -> 向量化 -> 检索 -> 生成
     * 这是最原始的 RAG 实现方式，直接将用户查询转换为向量进行相似度检索，
     * 然后将检索结果作为上下文提供给 LLM。
     *
     * @param query 用户查询问题
     * @return AI 基于检索结果生成的回答
     */
    @GetMapping("/query")
    public String naiveRagQuery(@RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query) {
        // Naive RAG 流程：
        // 1. 用户输入查询
        // 2. 向量存储自动将查询转换为向量并检索相似文档
        // 3. 将检索结果作为上下文提供给 LLM
        // 4. LLM 基于上下文生成回答

        String response = chatClient.prompt()
                // 设置用户查询
                .user(query)
                // 启用 RAG 检索增强，从向量存储中检索相关文档
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Naive RAG 查询结果 ===

                用户问题: %s

                AI 回复:
                %s
                """, query, response);
    }

    /**
     * 添加文档到知识库接口
     *
     * <p>用于向向量存储中添加文档，构建知识库。
     * 在实际应用中，这些文档可以是产品手册、技术文档、FAQ 等。
     *
     * @param content 文档内容
     * @param metadata 文档元数据（如标题、来源等）
     * @return 添加结果
     */
    @GetMapping("/add-document")
    public String addDocument(
            @RequestParam String content,
            @RequestParam(defaultValue = "default") String metadata) {
        Document document = Document.builder()
                .text(content)
                .metadata(Map.of("source", metadata))
                .build();

        vectorStore.add(List.of(document));

        return String.format("文档添加成功！内容: %s", content);
    }

    /**
     * 批量添加示例文档接口
     *
     * <p>添加一组预定义的示例文档，用于演示 RAG 的检索效果。
     * 这些文档涵盖 Spring AI 的不同特性。
     *
     * @return 添加结果
     */
    @GetMapping("/add-sample-documents")
    public String addSampleDocuments() {
        List<Document> documents = List.of(
                Document.builder()
                        .text("Spring AI 是 Spring Framework 的一个模块，"
                                + "它为 AI 模型集成提供了统一的标准和抽象。"
                                + "主要特性包括：统一的 API 设计、跨模型兼容性、"
                                + "提示词模板支持、结构化输出等。")
                        .metadata(Map.of("topic", "spring-ai-intro", "title", "Spring AI 介绍"))
                        .build(),
                Document.builder()
                        .text("Spring AI 提供了 Prompts API，用于构建和管理提示词。"
                                + "核心类包括：Prompt、Message、PromptTemplate 等。"
                                + "支持模板渲染、消息类型定义、选项配置等功能。")
                        .metadata(Map.of("topic", "prompts", "title", "提示词 API"))
                        .build(),
                Document.builder()
                        .text("RAG（Retrieval-Augmented Generation，检索增强生成）"
                                + "是一种结合信息检索和文本生成的技术。"
                                + "它通过从外部知识库检索相关文档，"
                                + "为 LLM 提供上下文信息，减少幻觉，提高回答准确性。")
                        .metadata(Map.of("topic", "rag", "title", "RAG 技术介绍"))
                        .build(),
                Document.builder()
                        .text("Spring AI 的向量存储支持多种后端："
                                + "PgVector、Milvus、Elasticsearch、Chroma 等。"
                                + "向量存储用于存储文档的向量表示，"
                                + "支持高效的相似度搜索和检索。")
                        .metadata(Map.of("topic", "vector-store", "title", "向量存储"))
                        .build(),
                Document.builder()
                        .text("嵌入模型（Embedding Model）用于将文本转换为向量表示。"
                                + "Spring AI 支持多种嵌入模型：OpenAI、Azure OpenAI、"
                                + "HuggingFace、Ollama 等。"
                                + "向量表示能够捕捉文本的语义信息，"
                                + "支持相似度计算和语义搜索。")
                        .metadata(Map.of("topic", "embedding", "title", "嵌入模型"))
                        .build()
        );

        vectorStore.add(documents);

        return String.format("已添加 %d 个示例文档到知识库", documents.size());
    }
}
