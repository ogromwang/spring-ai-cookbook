package dev.dong4j.ai.spring.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 查询转换控制器
 *
 * <p>演示 Query Transformation（查询转换）的相关技术。
 * 查询转换是高级 RAG 的核心环节之一，用于优化用户查询以提高检索质量。
 *
 * 主要技术包括：
 * 1. Query Rewrite（查询重写）：将口语化问题转换为标准检索语句
 *    - 使用 LLM 对查询进行理解和改写
 *    - 去除口语化表达，提取核心检索意图
 *    - 生成更适合向量检索的查询语句
 *
 * 2. Query Decomposition（查询分解）：将复杂问题拆分为多个子查询
 *    - 对于复杂问题，分解为多个简单子问题
 *    - 分别检索各个子问题的答案
 *    - 综合多个子查询结果形成最终回答
 *
 * @author ogromwang
 * @version 1.0.0
 * @email mailto:ogromwang@gmail.com
 * @date 2025.11.29
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rag/transformation")
public class QueryTransformationController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    /**
     * 构造函数，注入相关组件
     *
     * @param chatClient 聊天客户端
     * @param vectorStore 向量存储
     */
    public QueryTransformationController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 查询重写演示接口
     *
     * <p>演示 RewriteQueryTransformer 的功能：将口语化问题转为标准检索语句。
     * 例如：
     * - 原始查询："给我讲讲那个什么 Spring AI 的事儿"
     * - 重写后："Spring AI 的核心特性和功能"
     *
     * 这种转换能够：
     * - 去除口语化表达，提取核心意图
     * - 生成更适合向量检索的查询语句
     * - 提高检索的准确性和召回率
     *
     * @param query 用户原始查询
     * @return 重写后的查询语句和检索结果
     */
    @GetMapping("/rewrite")
    public String rewriteQuery(@RequestParam(defaultValue = "给我讲讲那个什么 Spring AI 的事儿") String query) {
        // 使用 LLM 进行查询重写
        String rewritePrompt = String.format("""
            将以下口语化的用户查询转换为清晰、专业的检索语句。
            保留查询的核心意图，但去除口语化表达，生成更适合向量检索的查询。

            原始查询: %s

            重写后的查询:
            """, query);

        String transformedQuery = chatClient.prompt()
                .user(rewritePrompt)
                .call()
                .content();

        String response = chatClient.prompt()
                .user(transformedQuery)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Query Rewrite（查询重写） ===

                原始查询: %s
                重写后查询: %s

                AI 回复:
                %s

                说明: 查询重写使用 LLM 将口语化问题转为标准检索语句，
                      提高检索的准确性和召回率。
                """, query, transformedQuery, response);
    }

    /**
     * 查询分解演示接口
     *
     * <p>演示 Query Decomposition（查询分解）的功能：将复杂问题拆分为多个子查询。
     * 例如：
     * - 原始查询："Spring AI 和传统 Spring 框架有什么关系？它有哪些核心特性？"
     * - 分解后子查询：
     *   1. Spring AI 与 Spring Framework 的关系
     *   2. Spring AI 的核心特性
     *
     * 复杂问题拆分为多个简单子问题后：
     * - 每个子问题可以独立进行检索
     * - 检索结果更加精准
     * - 最终综合多个子查询结果形成完整回答
     *
     * @param query 用户复杂查询
     * @return 分解后的子查询和综合回答
     */
    @GetMapping("/decompose")
    public String decomposeQuery(@RequestParam(defaultValue = "Spring AI 和传统 Spring 框架有什么关系？它有哪些核心特性？") String query) {
        // 使用 LLM 进行查询分解
        String decomposePrompt = String.format("""
            将以下复杂查询分解为多个简单的子查询。
            每个子查询应该能够独立回答，然后综合形成完整答案。

            原始查询: %s

            子查询列表（每行一个）:
            """, query);

        String decomposedQueries = chatClient.prompt()
                .user(decomposePrompt)
                .call()
                .content();

        // 解析子查询
        List<String> subQueries = List.of(decomposedQueries.split("\\n"));

        // 分别检索每个子查询的结果
        StringBuilder subResults = new StringBuilder();
        for (String subQuery : subQueries) {
            if (!subQuery.trim().isEmpty() && !subQuery.contains("子查询")) {
                List<Document> docs = vectorStore.similaritySearch(subQuery.trim());
                subResults.append("\n子查询: ").append(subQuery.trim());
                subResults.append("\n检索结果: ");
                for (Document doc : docs) {
                    subResults.append("- ").append(doc.getText()).append("\n");
                }
            }
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Query Decomposition（查询分解） ===

                原始复杂查询: %s

                分解后的子查询:
                %s

                子查询检索结果:
                %s

                AI 综合回复:
                %s

                说明: 查询分解将复杂问题拆分为多个子查询，
                         每个子查询独立检索后综合结果。
                """, query, decomposedQueries, subResults, response);
    }

    /**
     * 查询转换与检索流程演示接口
     *
     * <p>展示完整的查询转换到检索的流程：
     * 1. 接收用户原始查询
     * 2. 使用 LLM 进行查询转换（重写或分解）
     * 3. 使用转换后的查询进行向量检索
     * 4. 将检索结果提供给 LLM 生成最终回答
     *
     * @param query 用户查询
     * @param transformType 转换类型：rewrite（重写）或 decompose（分解）
     * @return 处理结果
     */
    @GetMapping("/transform-and-retrieve")
    public String transformAndRetrieve(
            @RequestParam(defaultValue = "Spring AI 支持哪些嵌入模型？") String query,
            @RequestParam(defaultValue = "rewrite") String transformType) {

        String transformedQuery;
        if ("rewrite".equals(transformType)) {
            String rewritePrompt = String.format("将以下查询转换为标准检索语句: %s", query);
            transformedQuery = chatClient.prompt().user(rewritePrompt).call().content();
        } else {
            String decomposePrompt = String.format("将以下查询分解为子查询: %s", query);
            transformedQuery = chatClient.prompt().user(decomposePrompt).call().content();
        }

        // 使用转换后的查询进行检索
        List<Document> retrievedDocs = vectorStore.similaritySearch(transformedQuery);

        StringBuilder retrievedContent = new StringBuilder();
        for (Document doc : retrievedDocs) {
            retrievedContent.append("- ").append(doc.getText()).append("\n");
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === 查询转换与检索流程 ===

                转换类型: %s
                原始查询: %s
                转换后查询: %s

                检索到的文档:
                %s

                AI 回复:
                %s
                """, transformType, query, transformedQuery, retrievedContent, response);
    }
}
