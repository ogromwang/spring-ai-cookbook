package dev.dong4j.ai.spring.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RAG 核心概念与流程控制器
 *
 * <p>演示 RAG 的核心概念、基本流程和 Spring AI 组件映射。
 *
 * ## 为什么需要 RAG？
 *
 * ### 传统 LLM 的痛点
 *
 * 1. **知识截止日期问题**：LLM 的训练数据有截止日期，无法获取最新信息
 * 2. **幻觉问题**：LLM 可能生成不准确或不存在的信息
 * 3. **缺乏专业领域知识**：通用 LLM 可能不了解特定领域的专业知识
 * 4. **无法引用来源**：无法提供答案的依据和来源
 *
 * ### RAG 的解决方案
 *
 * RAG（Retrieval-Augmented Generation，检索增强生成）通过以下方式解决这些问题：
 *
 * 1. **检索阶段**：从外部知识库检索相关文档
 * 2. **增强阶段**：将检索结果作为上下文提供给 LLM
 * 3. **生成阶段**：LLM 基于检索到的真实信息生成回答
 *
 * ## RAG 基本流程
 *
 * ```
 * 用户 Query --> 检索 --> 文档匹配 --> 上下文拼接 --> LLM 生成
 * ```
 *
 * ## 高级 RAG 流程
 *
 * 1. **查询优化**：查询重写、查询分解、查询扩展
 * 2. **多路召回**：向量检索 + BM25 + 知识图谱等
 * 3. **结果融合**：RRF、加权投票等
 * 4. **重排精筛**：Cross-Encoder 重排
 * 5. **生成回答**：将精筛后的文档作为上下文
 *
 * @author ogromwang
 * @version 1.0.0
 * @email mailto:ogromwang@gmail.com
 * @date 2025.11.29
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    /**
     * 构造函数
     *
     * @param chatClient 聊天客户端
     * @param vectorStore 向量存储
     */
    public RagController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * Naive RAG 演示 - 最基础的 RAG 流程
     *
     * <p>最早的 RAG 是什么样子的：
     * 1. 用户输入查询
     * 2. 将查询转换为向量
     * 3. 在向量数据库中检索相似文档
     * 4. 将检索到的文档作为上下文提供给 LLM
     * 5. LLM 基于上下文生成回答
     *
     * <p>Naive RAG 的局限性：
     * - 检索精度依赖向量相似度的准确性
     * - 缺乏对查询的优化处理
     * - 无法处理复杂的多轮对话
     * - 重排和精筛环节缺失
     *
     * @param query 用户查询
     * @return AI 回复
     */
    @GetMapping("/naive")
    public String naiveRag(@RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query) {
        // Naive RAG 流程：直接使用向量检索 + 上下文拼接
        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Naive RAG（基础 RAG） ===

                流程: 用户 Query --> 向量检索 --> 上下文拼接 --> LLM 生成

                用户问题: %s

                AI 回复:
                %s

                说明: Naive RAG 是最早期的 RAG 实现，直接将查询转为向量进行检索，
                      缺乏查询优化、多路召回、重排等高级环节。
                """, query, response);
    }

    /**
     * 高级 RAG - 查询重写演示
     *
     * <p>Query Rewriting（查询重写）：
     * LLM 将口语化问题转为标准检索语句
     *
     * <p>示例：
     * - 原始查询："给我讲讲那个什么 Spring AI 的事儿"
     * - 重写后："Spring AI 的核心特性和功能"
     *
     * @param query 用户原始查询
     * @return 重写后的查询和检索结果
     */
    @GetMapping("/query-rewrite")
    public String queryRewrite(@RequestParam(defaultValue = "给我讲讲那个什么 Spring AI 的事儿") String query) {
        // 使用 LLM 进行查询重写
        String rewritePrompt = String.format("""
            将以下口语化的用户查询转换为清晰、专业的检索语句。
            保留查询的核心意图，但去除口语化表达，生成更适合向量检索的查询。

            原始查询: %s

            重写后的查询（只返回查询语句，不要其他内容）:
            """, query);

        String rewrittenQuery = chatClient.prompt()
                .user(rewritePrompt)
                .call()
                .content();

        // 使用重写后的查询进行检索
        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Query Rewriting（查询重写） ===

                原始查询: %s
                重写后查询: %s

                Spring AI 组件: RewriteQueryTransformer
                RAG 概念: Query Rewriting - LLM 将口语化问题转为标准检索语句

                AI 回复:
                %s
                """, query, rewrittenQuery, response);
    }

    /**
     * 高级 RAG - 查询分解演示
     *
     * <p>Query Decomposition（查询分解）：
     * 将复杂问题拆解为多个子查询
     *
     * <p>示例：
     * - 原始查询："Spring AI 和传统 Spring 框架有什么关系？它有哪些核心特性？"
     * - 分解后：
     *   1. Spring AI 与 Spring Framework 的关系
     *   2. Spring AI 的核心特性
     *
     * @param query 用户复杂查询
     * @return 分解后的子查询和检索结果
     */
    @GetMapping("/query-decompose")
    public String queryDecompose(@RequestParam(defaultValue = "Spring AI 和传统 Spring 框架有什么关系？它有哪些核心特性？") String query) {
        // 使用 LLM 进行查询分解
        String decomposePrompt = String.format("""
            将以下复杂查询分解为多个简单的子查询。
            每个子查询应该能够独立回答，然后综合形成完整答案。

            原始查询: %s

            子查询列表（每行一个，只返回子查询）：
            """, query);

        String decomposedQueries = chatClient.prompt()
                .user(decomposePrompt)
                .call()
                .content();

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Query Decomposition（查询分解） ===

                原始查询: %s

                分解后的子查询:
                %s

                Spring AI 组件: QueryDecomposer（概念）
                RAG 概念: Query Decomposition - 复杂问题拆解为多个子查询

                AI 回复:
                %s
                """, query, decomposedQueries, response);
    }

    /**
     * 高级 RAG - 查询扩展示演
     *
     * <p>Query Expansion（查询扩展示）：
     * 将单个查询扩展为多个相关查询，提高检索召回率
     *
     * <p>示例：
     * - 原查询："Spring AI 的核心特性是什么？"
     * - 扩展查询1: "Spring AI 核心功能特性"
     * - 扩展查询2: "Spring AI Prompts API 介绍"
     * - 扩展查询3: "Spring AI 向量存储支持"
     *
     * @param query 用户查询
     * @return 扩展后的查询和检索结果
     */
    @GetMapping("/query-expand")
    public String queryExpand(@RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query) {
        // 使用 LLM 进行查询扩展
        String expandPrompt = String.format("""
            将以下查询扩展为3个不同的查询变体，每个变体关注查询的不同方面。
            原始查询: %s

            扩展查询（每行一个，只返回查询语句）：
            """, query);

        String expandedQueries = chatClient.prompt()
                .user(expandPrompt)
                .call()
                .content();

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Query Expansion（查询扩展示） ===

                原始查询: %s

                扩展后的查询:
                %s

                Spring AI 组件: MultiQueryExpander
                RAG 概念: Query Expansion - 将单个查询扩展为多个变体，提高召回率

                AI 回复:
                %s
                """, query, expandedQueries, response);
    }

    /**
     * 高级 RAG - 多路召回与融合演示
     *
     * <p>多路召回：并行执行多个检索（向量检索 + 关键词检索等）
     * 结果融合：RRF（Reciprocal Rank Fusion）或加权投票
     *
     * <p>示例：
     * - 通道1: 向量检索（权重 0.7）
     * - 通道2: BM25 关键词检索（权重 0.3）
     * - 融合: 得分加权求和
     *
     * @param query 用户查询
     * @param vectorWeight 向量检索权重
     * @param bm25Weight BM25 权重
     * @return 融合后的检索结果
     */
    @GetMapping("/multi-channel-fusion")
    public String multiChannelFusion(
            @RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query,
            @RequestParam(defaultValue = "0.7") double vectorWeight,
            @RequestParam(defaultValue = "0.3") double bm25Weight) {
        // 模拟多路召回结果
        // 通道1: 向量检索
        List<Document> vectorResults = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(5).build()
        );

        // 简单模拟 BM25 结果（实际应用中应使用真正的 BM25 检索）
        List<Document> bm25Results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(5).build()
        );

        // 加权融合（简化实现）
        StringBuilder fusionResult = new StringBuilder();
        int i = 1;
        for (Document doc : vectorResults) {
            fusionResult.append(String.format("%d. [向量检索] %s\n", i++, doc.getText()));
        }
        for (Document doc : bm25Results) {
            fusionResult.append(String.format("%d. [BM25检索] %s\n", i++, doc.getText()));
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Multi-Channel Fusion（多路召回与融合） ===

                检索通道:
                - 通道1: 向量检索（权重 %.1f）
                - 通道2: BM25 检索（权重 %.1f）

                融合策略: 加权投票（向量权重 %.1f + BM25权重 %.1f）

                融合后的候选文档:
                %s

                Spring AI 组件: DocumentJoiner（用于连接多路检索结果）
                RAG 概念: 融合 - 解决"多样性"和"覆盖度"问题

                AI 回复:
                %s
                """, vectorWeight, bm25Weight, vectorWeight, bm25Weight, fusionResult, response);
    }

    /**
     * 高级 RAG - 重排演示
     *
     * <p>Re-ranking（重排）：
     * 对融合后的结果做精细打分，选出最相关的 top-K 送给 LLM
     *
     * <p>为什么需要重排？
     * - 初步检索（向量检索）是"近似最近邻"，速度快但精度有限
     * - 重排用更强的模型做"精筛"
     *
     * @param query 用户查询
     * @param topK 返回的文档数量
     * @return 重排后的结果
     */
    @GetMapping("/rerank")
    public String rerank(
            @RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query,
            @RequestParam(defaultValue = "5") int topK) {
        // 初步检索获取候选文档
        List<Document> candidates = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(10).build()
        );

        // 模拟重排（使用 LLM 进行相关性评分）
        StringBuilder rerankPrompt = new StringBuilder();
        rerankPrompt.append("请对以下文档与查询的相关性进行评分（0-1分），返回评分结果：\n");
        rerankPrompt.append("查询: ").append(query).append("\n\n");
        for (int i = 0; i < candidates.size(); i++) {
            rerankPrompt.append(String.format("文档%d: %s\n", i + 1, candidates.get(i).getText()));
        }

        // 获取重排结果（简化：直接使用相似度排序）
        StringBuilder rankedDocs = new StringBuilder();
        for (int i = 0; i < Math.min(topK, candidates.size()); i++) {
            Document doc = candidates.get(i);
            rankedDocs.append(String.format("%d. %s\n", i + 1, doc.getText()));
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Re-ranking（重排） ===

                重排流程:
                1. 多路召回获取 %d 个候选文档
                2. 使用 Cross-Encoder 等模型进行精细打分
                3. 按评分排序，选择 top-%d

                重排后的 Top-%d 文档:
                %s

                Spring AI 组件: CrossEncoderReranker（用于重排）
                RAG 概念: 重排 - 用更强的模型做"精筛"，选出最相关的 top-K

                AI 回复:
                %s
                """, candidates.size(), topK, topK, rankedDocs, response);
    }

    /**
     * 高级 RAG - 完整流程演示
     *
     * <p>展示完整的高级 RAG 流程：
     * 1. 查询优化（重写/分解/扩展）
     * 2. 多路召回（向量 + BM25）
     * 3. 结果融合（加权/RRF）
     * 4. 重排精筛（Cross-Encoder）
     * 5. LLM 生成回答
     *
     * @param query 用户查询
     * @return 完整的处理结果
     */
    @GetMapping("/advanced")
    public String advancedRag(@RequestParam(defaultValue = "Spring AI 支持哪些功能？") String query) {
        // 步骤1: 查询扩展（MultiQueryExpander）
        String expandPrompt = String.format("将以下查询扩展为2个不同的变体:\n%s", query);
        String expandedQueries = chatClient.prompt().user(expandPrompt).call().content();

        // 步骤2: 多路召回
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(10).build()
        );

        // 步骤3: 结果融合（简化）
        // 步骤4: 重排精筛（简化：取 top-5）
        List<Document> topDocs = docs.stream().limit(5).toList();

        StringBuilder context = new StringBuilder();
        for (Document doc : topDocs) {
            context.append("- ").append(doc.getText()).append("\n");
        }

        // 步骤5: 生成回答
        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Advanced RAG（高级 RAG）完整流程 ===

                用户查询: %s

                流程步骤:
                1. 查询优化: %s
                2. 多路召回: 向量检索 + BM25 检索
                3. 结果融合: 加权投票 / RRF
                4. 重排精筛: Cross-Encoder 重新打分
                5. LLM 生成: 基于精筛后的上下文生成回答

                最终上下文（top-5）:
                %s

                AI 回复:
                %s

                说明: 高级 RAG 通过查询优化、多路召回、结果融合、重排精筛等环节，
                      大幅提升了检索质量和生成准确性。
                """, query, expandedQueries, context, response);
    }

    /**
     * RAG 概念速查表
     *
     * @return Spring AI 组件与 RAG 概念的映射关系
     */
    @GetMapping("/concepts")
    public String ragConcepts() {
        return String.format("""
                === Spring AI RAG 组件与概念映射 ===

                ## 1. 查询转换（Query Transformation）

                | Spring AI 组件 | RAG 概念 | 说明 |
                |---------------|---------|------|
                | RewriteQueryTransformer | Query Rewriting | LLM 将口语化问题转为标准检索语句 |
                | QueryDecomposer | Query Decomposition | 复杂问题拆解为多个子查询 |
                | CompressionQueryTransformer | Query Compression | 查询压缩（对话场景） |
                | TranslationQueryTransformer | Query Translation | 多语言查询翻译 |

                ## 2. 查询扩展示（Query Expansion）

                | Spring AI 组件 | RAG 概念 | 说明 |
                |---------------|---------|------|
                | MultiQueryExpander | Query Expansion | 将单个查询扩展为多个变体 |

                ## 3. 文档检索（Document Retrieval）

                | Spring AI 组件 | RAG 概念 | 说明 |
                |---------------|---------|------|
                | VectorStoreDocumentRetriever | Vector Search | 向量存储检索器 |
                | VectorStoreRetriever | Similarity Search | 向量相似度搜索 |

                ## 4. 文档连接（Document Join）

                | Spring AI 组件 | RAG 概念 | 说明 |
                |---------------|---------|------|
                | ConcatenationDocumentJoiner | 文档拼接 | 连接多个检索源的文档，去重 |
                | ScoreDocListJoiner | 评分排序 | 根据评分排序选择 top-k |
                | WeightedDocumentJoiner | 加权融合 | 根据权重进行结果融合 |

                ## 5. 重排（Re-ranking）

                | Spring AI 组件 | RAG 概念 | 说明 |
                |---------------|---------|------|
                | CrossEncoderReranker | Cross-Encoder 重排 | 使用交叉编码器精细打分 |
                | CohereReranker | 第三方重排 | 使用 Cohere 等服务重排 |

                ## 6. 对话 RAG（Conversational RAG）

                | Spring AI 组件 | RAG 概念 | 说明 |
                |---------------|---------|------|
                | MessageChatMemoryAdvisor | Chat Memory | 对话历史记忆管理 |
                | RetrievalAugmentationAdvisor | 完整 RAG 管道 | 整合所有 RAG 组件 |
                """);
    }
}
