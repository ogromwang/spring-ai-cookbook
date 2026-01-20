package dev.dong4j.ai.spring.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 查询扩展示控制器
 *
 * <p>演示 Query Expansion（查询扩展）的相关技术。
 * 查询扩展示高级 RAG 中的重要环节，用于增强查询的覆盖范围和多样性。
 *
 * 主要技术包括：
 * 1. Multi-Query Expansion（多查询扩展）：将单个查询扩展为多个相关查询
 *    - 使用 LLM 生成查询的多种表述方式
 *    - 每个表述关注查询的不同方面
 *    - 并行执行多个查询，提高召回率
 *
 * 2. Related Query Generation（相关查询生成）：生成与原查询相关的补充查询
 *    - 基于原查询生成扩展查询
 *    - 覆盖更多的语义空间
 *    - 发现潜在的相关信息
 *
 * @author ogromwang
 * @version 1.0.0
 * @email mailto:ogromwang@gmail.com
 * @date 2025.11.29
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rag/expansion")
public class QueryExpansionController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    /**
     * 构造函数，注入相关组件
     *
     * @param chatClient 聊天客户端
     * @param vectorStore 向量存储
     */
    public QueryExpansionController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 多查询扩展示演示接口
     *
     * <p>演示 MultiQueryExpander 的功能：直接将复杂的问题转为多个子查询。
     *
     * 例如，对于查询"Spring AI 的核心特性是什么？"：
     * - 扩展查询1: "Spring AI 核心功能特性"
     * - 扩展查询2: "Spring AI Prompts API 介绍"
     * - 扩展查询3: "Spring AI 向量存储支持"
     *
     * 多查询扩展示的优势：
     * - 提高检索的召回率
     * - 覆盖查询的不同语义方面
     * - 减少单一查询可能遗漏重要信息的风险
     *
     * @param query 用户原始查询
     * @return 扩展后的多个查询及其检索结果
     */
    @GetMapping("/multi-query")
    public String multiQueryExpansion(@RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query) {
        // 使用 LLM 生成多个查询变体
        List<String> expandedQueries = expandQuery(query);

        // 并行执行多个查询检索
        List<List<Document>> allRetrievedDocs = new ArrayList<>();
        for (String expandedQuery : expandedQueries) {
            List<Document> docs = vectorStore.similaritySearch(expandedQuery);
            allRetrievedDocs.add(docs);
        }

        // 合并所有检索结果
        List<Document> mergedDocs = mergeDocuments(allRetrievedDocs);

        StringBuilder expandedQueryStr = new StringBuilder();
        for (int i = 0; i < expandedQueries.size(); i++) {
            expandedQueryStr.append("- ").append(expandedQueries.get(i)).append("\n");
        }

        StringBuilder retrievedContent = new StringBuilder();
        for (Document doc : mergedDocs) {
            retrievedContent.append("- ").append(doc.getText()).append("\n");
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Multi-Query Expansion（多查询扩展） ===

                原始查询: %s

                扩展后的查询变体:
                %s

                说明: MultiQueryExpander 使用 LLM 将单个查询扩展为多个相关查询，
                      每个查询关注原问题的不同方面，提高检索召回率。

                合并后的检索文档:
                %s

                AI 回复:
                %s
                """, query, expandedQueryStr, retrievedContent, response);
    }

    /**
     * 相关查询生成演示接口
     *
     * <p>演示基于原查询生成相关补充查询的功能。
     * 这种技术可以：
     * - 发现查询的潜在相关主题
     * - 扩展检索的覆盖范围
     * - 获取更全面的上下文信息
     *
     * @param query 用户查询
     * @return 原查询和相关查询的检索结果对比
     */
    @GetMapping("/related-query")
    public String relatedQueryGeneration(@RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query) {
        // 生成相关查询
        List<String> relatedQueries = generateRelatedQueries(query);

        // 合并原查询和相关查询
        List<String> allQueries = new ArrayList<>();
        allQueries.add(query);
        allQueries.addAll(relatedQueries);

        // 执行多查询检索
        List<List<Document>> allRetrievedDocs = new ArrayList<>();
        for (String q : allQueries) {
            List<Document> docs = vectorStore.similaritySearch(q);
            allRetrievedDocs.add(docs);
        }

        // 合并去重后的文档
        List<Document> mergedDocs = mergeDocuments(allRetrievedDocs);

        StringBuilder relatedQueryStr = new StringBuilder();
        for (String q : relatedQueries) {
            relatedQueryStr.append("- ").append(q).append("\n");
        }

        StringBuilder retrievedContent = new StringBuilder();
        for (Document doc : mergedDocs) {
            retrievedContent.append("- ").append(doc.getText()).append("\n");
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Related Query Generation（相关查询生成） ===

                原始查询: %s

                生成的相关查询:
                %s

                说明: 相关查询生成基于原查询扩展检索范围，
                      发现潜在的相关主题，获取更全面的上下文信息。

                合并去重后的检索文档:
                %s

                AI 回复:
                %s
                """, query, relatedQueryStr, retrievedContent, response);
    }

    /**
     * 查询扩展与融合检索演示接口
     *
     * <p>展示完整的查询扩展到结果融合的流程：
     * 1. 将单个查询扩展为多个查询变体
     * 2. 并行执行多个查询进行检索
     * 3. 对多个检索结果进行融合（去重、排序）
     * 4. 将融合后的结果提供给 LLM
     *
     * @param query 用户查询
     * @return 完整的扩展与融合结果
     */
    @GetMapping("/expand-and-fusion")
    public String expandAndFusion(@RequestParam(defaultValue = "Spring AI 支持哪些功能？") String query) {
        // 步骤1: 查询扩展
        List<String> expandedQueries = expandQuery(query);

        // 步骤2: 多查询并行检索
        List<List<Document>> allRetrievedDocs = new ArrayList<>();
        for (String expandedQuery : expandedQueries) {
            List<Document> docs = vectorStore.similaritySearch(expandedQuery);
            allRetrievedDocs.add(docs);
        }

        // 步骤3: 结果融合
        List<Document> fusedDocs = fuseResults(allRetrievedDocs);

        // 统计信息
        int totalRetrieved = allRetrievedDocs.stream().mapToInt(List::size).sum();
        int uniqueDocs = fusedDocs.size();

        StringBuilder expandedQueryStr = new StringBuilder();
        for (int i = 0; i < expandedQueries.size(); i++) {
            expandedQueryStr.append("- ").append(expandedQueries.get(i)).append("\n");
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === 查询扩展与结果融合 ===

                原始查询: %s

                扩展查询数量: %d 个
                %s

                检索统计:
                - 总检索文档数: %d
                - 融合去重后: %d

                AI 回复:
                %s

                说明: 多查询扩展解决了单一查询的覆盖度问题，
                      通过多个查询变体并行检索和结果融合，
                      显著提高检索的全面性和准确性。
                """, query, expandedQueries.size(), expandedQueryStr, totalRetrieved, uniqueDocs, response);
    }

    /**
     * 扩展单个查询为多个查询变体
     *
     * <p>使用 LLM 生成查询的多种表述方式和角度
     *
     * @param originalQuery 原始查询
     * @return 扩展后的查询列表
     */
    private List<String> expandQuery(String originalQuery) {
        // 使用 LLM 生成查询变体
        String expansionPrompt = String.format("""
            将以下查询扩展为3个不同的查询变体，每个变体关注查询的不同方面。
            原始查询: %s

            要求：
            1. 每个变体应该用不同的关键词或角度表达相同的查询意图
            2. 变体应该覆盖查询的不同方面
            3. 输出格式：每个变体一行，不要添加编号

            扩展查询:
            """, originalQuery);

        String expansion = chatClient.prompt()
                .user(expansionPrompt)
                .call()
                .content();

        // 解析扩展结果
        List<String> queries = new ArrayList<>();
        for (String line : expansion.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("-") && !trimmed.startsWith("*")
                    && !trimmed.toLowerCase().contains("扩展查询")) {
                queries.add(trimmed);
            }
        }

        // 如果解析失败，返回默认变体
        if (queries.isEmpty()) {
            queries.add(originalQuery);
            queries.add(originalQuery + " 的详细信息");
            queries.add(originalQuery + " 的核心要点");
        }

        return queries;
    }

    /**
     * 生成与原查询相关的补充查询
     *
     * @param query 原查询
     * @return 相关查询列表
     */
    private List<String> generateRelatedQueries(String query) {
        String relatedPrompt = String.format("""
            基于以下查询，生成2个相关的补充查询，这些查询应该扩展检索的范围。
            原始查询: %s

            相关查询:
            """, query);

        String related = chatClient.prompt()
                .user(relatedPrompt)
                .call()
                .content();

        List<String> queries = new ArrayList<>();
        for (String line : related.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                queries.add(trimmed.replaceAll("^[\\-\\*]\\s*", ""));
            }
        }

        return queries;
    }

    /**
     * 合并多个检索结果列表（去重）
     *
     * @param retrievedDocsLists 多个检索结果列表
     * @return 合并去重后的文档列表
     */
    private List<Document> mergeDocuments(List<List<Document>> retrievedDocsLists) {
        List<Document> merged = new ArrayList<>();
        Set<String> seenContents = new HashSet<>();

        for (List<Document> docs : retrievedDocsLists) {
            for (Document doc : docs) {
                String content = doc.getText();
                if (!seenContents.contains(content)) {
                    seenContents.add(content);
                    merged.add(doc);
                }
            }
        }
        return merged;
    }

    /**
     * 使用加权融合算法合并检索结果
     *
     * <p>多路召回与融合的另一种实现方式，根据不同查询来源的权重进行加权。
     *
     * @param retrievedDocsLists 多个检索结果列表
     * @return 融合后的文档列表
     */
    private List<Document> fuseResults(List<List<Document>> retrievedDocsLists) {
        // 简单实现：合并去重
        return mergeDocuments(retrievedDocsLists);
    }
}
