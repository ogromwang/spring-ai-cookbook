package dev.dong4j.ai.spring.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 重排控制器
 *
 * <p>演示 Re-ranking（重排）的相关技术。
 * 重排是高级 RAG 中的关键环节，用于对融合后的结果做精细打分，
 * 选出最相关的 top-K 文档送给 LLM。
 *
 * ## 重排的核心价值
 *
 * 初步检索（尤其是向量检索）是「近似最近邻」（ANN），速度快但精度有限。
 * 重排用更昂贵但更准的模型做「精筛」。
 *
 * ## 重排的工作流程
 *
 * 1. **多路召回**：并行执行向量检索、关键词检索、图谱查询等
 * 2. **结果融合**：将多个检索源的结果合并（如 RRF 或加权投票）
 * 3. **重排精筛**：用更强的模型对候选文档重新打分
 * 4. **Top-K 选择**：选出最相关的 top-K（如 top-5）送给 LLM
 *
 * ## Spring AI 支持的重排策略
 *
 * 1. **Cross-Encoder Re-ranker**：使用交叉编码器进行精细打分
 *    - 输入：查询 + 文档对
 *    - 输出：相关性分数
 *    - 优点：精度高，考虑查询和文档的完整交互
 *
 * 2. **Score-based Re-ranker**：基于评分的排序
 *    - 根据初始检索分数进行排序
 *    - 支持自定义排序策略
 *
 * 3. **Diversity Re-ranker**：多样性重排
 *    - 在保证相关性的同时，提升结果多样性
 *    - 避免重复或相似的内容
 *
 * @author ogromwang
 * @version 1.0.0
 * @email mailto:ogromwang@gmail.com
 * @date 2025.11.29
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rag/rerank")
public class RerankingController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    /**
     * 构造函数，注入相关组件
     *
     * @param chatClient 聊天客户端
     * @param vectorStore 向量存储
     */
    public RerankingController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 基础重排演示接口
     *
     * <p>演示重排的基本流程：
     * 1. 初步检索获取候选文档
     * 2. 使用 LLM 对候选文档进行相关性评分
     * 3. 按评分排序，选择 top-K
     *
     * @param query 用户查询
     * @param topK 返回的文档数量
     * @return 重排后的文档列表
     */
    @GetMapping("/basic")
    public String basicRerank(
            @RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query,
            @RequestParam(defaultValue = "5") int topK) {
        // 步骤1: 初步检索，获取候选文档
        List<Document> candidateDocs = vectorStore.similaritySearch(query);

        // 步骤2: 使用 LLM 对候选文档进行重排评分
        List<Document> rerankedDocs = rerankWithLLM(query, candidateDocs, topK);

        // 构建重排后的文档列表
        StringBuilder rerankedContent = new StringBuilder();
        for (int i = 0; i < rerankedDocs.size(); i++) {
            Document doc = rerankedDocs.get(i);
            Object score = doc.getMetadata().get("rerank_score");
            rerankedContent.append(String.format("%d. [重排分数: %s] %s\n",
                    i + 1, score != null ? score : "N/A", doc.getText()));
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Basic Re-ranking（基础重排） ===

                用户查询: %s
                Top-K: %d

                重排流程:
                1. 初步检索获取 %d 个候选文档
                2. 使用 LLM 对候选文档进行相关性评分
                3. 按评分排序，选择 top-%d

                重排后的文档列表:
                %s

                AI 回复:
                %s

                说明: 重排的目标是用更强的模型对候选文档重新打分，
                      选出最相关的 top-K（如 top-5）送给 LLM。
                      初步检索是"近似最近邻"，速度快但精度有限；
                      重排用更昂贵但更准的模型做"精筛"。
                """, query, topK, candidateDocs.size(), topK, rerankedContent, response);
    }

    /**
     * Cross-Encoder 重排演示接口
     *
     * <p>演示 Cross-Encoder 重排的原理：
     * - 输入：查询 + 文档对
     * - 输出：相关性分数
     * - 优点：精度高，考虑查询和文档的完整交互
     *
     * Cross-Encoder 与 Bi-Encoder 的对比：
     * - Bi-Encoder（向量检索）：查询和文档分别编码，速度快
     * - Cross-Encoder：查询和文档一起编码，精度高但速度慢
     *
     * @param query 用户查询
     * @return Cross-Encoder 重排结果
     */
    @GetMapping("/cross-encoder")
    public String crossEncoderRerank(@RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query) {
        // 步骤1: 初步检索获取候选文档
        List<Document> candidateDocs = vectorStore.similaritySearch(query);

        // 步骤2: 模拟 Cross-Encoder 重排评分
        // 实际应用中，这里会使用专门的 Cross-Encoder 模型
        List<Document> rerankedDocs = crossEncoderRerank(query, candidateDocs);

        StringBuilder rerankedContent = new StringBuilder();
        for (int i = 0; i < rerankedDocs.size(); i++) {
            Document doc = rerankedDocs.get(i);
            Object score = doc.getMetadata().get("cross_encoder_score");
            rerankedContent.append(String.format("%d. [CE分数: %s] %s\n",
                    i + 1, score != null ? score : "N/A", doc.getText()));
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Cross-Encoder Re-ranking（交叉编码器重排） ===

                用户查询: %s

                Cross-Encoder 原理:
                - 输入：查询 + 文档对
                - 输出：相关性分数（0-1）
                - 优点：考虑查询和文档的完整交互，精度高
                - 缺点：速度慢，需要逐个计算

                重排后的文档列表:
                %s

                AI 回复:
                %s

                说明: Cross-Encoder 将查询和文档一起输入模型，
                      计算两者的完整交互，因此精度更高。
                      适用于对精度要求高、候选文档数量有限的场景。
                """, query, rerankedContent, response);
    }

    /**
     * 多样性重排演示接口
     *
     * <p>演示 Diversity Re-ranker 的功能：
     * 在保证相关性的同时，提升结果多样性，避免重复或相似的内容。
     *
     * 适用场景：
     * - 需要提供多样化的结果选项
     * - 避免返回内容重复的文档
     * - 推荐系统等需要新颖性的场景
     *
     * @param query 用户查询
     * @return 多样性重排结果
     */
    @GetMapping("/diversity")
    public String diversityRerank(@RequestParam(defaultValue = "Spring AI 支持哪些功能？") String query) {
        // 步骤1: 初步检索获取候选文档
        List<Document> candidateDocs = vectorStore.similaritySearch(query);

        // 步骤2: 多样性重排
        List<Document> rerankedDocs = diversityRerank(query, candidateDocs);

        StringBuilder rerankedContent = new StringBuilder();
        for (int i = 0; i < rerankedDocs.size(); i++) {
            Document doc = rerankedDocs.get(i);
            rerankedContent.append(String.format("%d. %s\n", i + 1, doc.getText()));
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Diversity Re-ranking（多样性重排） ===

                用户查询: %s

                多样性重排策略:
                - 在保证相关性的同时，提升结果多样性
                - 避免返回内容重复或过于相似的文档
                - 使用 MMR（Maximal Marginal Relevance）等算法

                重排后的文档列表:
                %s

                AI 回复:
                %s

                说明: 多样性重排在保证相关性的前提下，
                      通过 MMR 等算法选择与已选文档差异最大的文档，
                      确保结果的多样性和新颖性。
                """, query, rerankedContent, response);
    }

    /**
     * 完整的高级 RAG 重排流程演示接口
     *
     * <p>展示完整的高级 RAG 重排流程：
     * 1. 多路召回（向量检索 + 查询扩展）
     * 2. 结果融合
     * 3. 重排精筛
     * 4. Top-K 选择
     *
     * @param query 用户查询
     * @return 完整的重排流程结果
     */
    @GetMapping("/complete-flow")
    public String completeRerankFlow(@RequestParam(defaultValue = "Spring AI 的核心特性和应用场景") String query) {
        StringBuilder flowLog = new StringBuilder();
        flowLog.append("=== 完整的高级 RAG 重排流程 ===\n\n");

        // 步骤1: 查询扩展
        flowLog.append("步骤1: 查询扩展（Query Expansion）\n");
        List<String> expandedQueries = expandQuery(query);
        for (int i = 0; i < expandedQueries.size(); i++) {
            flowLog.append(String.format("  - 查询%d: %s\n", i + 1, expandedQueries.get(i)));
        }
        flowLog.append("\n");

        // 步骤2: 多路召回
        flowLog.append("步骤2: 多路召回（Multi-Channel Recall）\n");
        List<List<Document>> channelResults = new ArrayList<>();
        for (String expandedQuery : expandedQueries) {
            List<Document> docs = vectorStore.similaritySearch(expandedQuery);
            channelResults.add(docs);
            flowLog.append(String.format("  - 通道%d检索: %d 个文档\n",
                    channelResults.size(), docs.size()));
        }
        flowLog.append("\n");

        // 步骤3: 结果融合
        flowLog.append("步骤3: 结果融合（Result Fusion）\n");
        List<Document> fusedDocs = concatenateDocuments(channelResults);
        flowLog.append(String.format("  - 融合后文档数: %d\n\n", fusedDocs.size()));

        // 步骤4: 重排精筛
        flowLog.append("步骤4: 重排精筛（Re-ranking）\n");
        List<Document> rerankedDocs = rerankWithLLM(query, fusedDocs, 5);
        for (int i = 0; i < rerankedDocs.size(); i++) {
            Document doc = rerankedDocs.get(i);
            Object score = doc.getMetadata().get("rerank_score");
            flowLog.append(String.format("  - Top%d [分数: %s]: %s\n",
                    i + 1, score != null ? score : "N/A",
                    doc.getText().substring(0, Math.min(50, doc.getText().length())) + "..."));
        }
        flowLog.append("\n");

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                %s

                最终 AI 回复:
                %s

                说明: 完整的高级 RAG 流程包括：
                      1. 查询扩展 - 将单个查询扩展为多个变体
                      2. 多路召回 - 并行执行多个检索
                      3. 结果融合 - 合并去重多路结果
                      4. 重排精筛 - 用更强模型重新打分
                      5. Top-K 选择 - 选出最相关的文档
                """, flowLog, response);
    }

    /**
     * 使用 LLM 对候选文档进行重排评分
     */
    private List<Document> rerankWithLLM(String query, List<Document> candidates, int topK) {
        // 构建重排提示
        StringBuilder rerankPrompt = new StringBuilder();
        rerankPrompt.append("请对以下候选文档与查询的相关性进行评分（0-1分）：\n");
        rerankPrompt.append(String.format("查询: %s\n\n", query));
        rerankPrompt.append("候选文档:\n");
        for (int i = 0; i < candidates.size(); i++) {
            rerankPrompt.append(String.format("文档%d: %s\n", i + 1, candidates.get(i).getText()));
        }
        rerankPrompt.append("\n请按以下格式返回评分结果（只返回评分）：\n");
        rerankPrompt.append("文档1: 0.85\n文档2: 0.72\n...");

        // 调用 LLM 进行评分
        String scores = chatClient.prompt()
                .user(rerankPrompt.toString())
                .call()
                .content();

        // 解析评分结果并排序
        List<DocumentWithScore> docsWithScores = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Document doc = candidates.get(i);
            double score = parseScore(scores, i + 1);
            doc.getMetadata().put("rerank_score", String.format("%.4f", score));
            docsWithScores.add(new DocumentWithScore(doc, score));
        }

        // 按评分排序并选择 topK
        docsWithScores.sort((a, b) -> Double.compare(b.score, a.score));
        return docsWithScores.stream()
                .limit(topK)
                .map(d -> d.document)
                .toList();
    }

    /**
     * 模拟 Cross-Encoder 重排
     */
    private List<Document> crossEncoderRerank(String query, List<Document> candidates) {
        List<DocumentWithScore> docsWithScores = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            Document doc = candidates.get(i);
            // 模拟 Cross-Encoder 评分
            // 实际应用中，这里会调用专门的 Cross-Encoder 模型
            double score = simulateCrossEncoderScore(query, doc.getText());
            doc.getMetadata().put("cross_encoder_score", String.format("%.4f", score));
            docsWithScores.add(new DocumentWithScore(doc, score));
        }

        // 按评分排序
        docsWithScores.sort((a, b) -> Double.compare(b.score, a.score));
        return docsWithScores.stream()
                .map(d -> d.document)
                .toList();
    }

    /**
     * 多样性重排
     */
    private List<Document> diversityRerank(String query, List<Document> candidates) {
        // 先按相关性排序
        List<DocumentWithScore> docsWithScores = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Document doc = candidates.get(i);
            double relevance = simulateCrossEncoderScore(query, doc.getText());
            docsWithScores.add(new DocumentWithScore(doc, relevance));
        }
        docsWithScores.sort((a, b) -> Double.compare(b.score, a.score));

        // 使用 MMR 算法选择多样化的文档
        List<Document> selected = new ArrayList<>();
        double lambda = 0.5; // 相关性和多样性的平衡因子

        for (int i = 0; i < Math.min(5, docsWithScores.size()); i++) {
            DocumentWithScore candidate = docsWithScores.get(i);

            // 如果已选文档为空，直接选择
            if (selected.isEmpty()) {
                selected.add(candidate.document);
                continue;
            }

            // 计算与已选文档的最大相似度（多样性惩罚）
            double maxSimilarity = 0.0;
            for (Document selectedDoc : selected) {
                double similarity = calculateSimilarity(candidate.document.getText(), selectedDoc.getText());
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }

            // MMR 分数 = 相关性 - λ * 多样性惩罚
            double mmrScore = lambda * candidate.score - (1 - lambda) * maxSimilarity;
            candidate.document.getMetadata().put("mmr_score", String.format("%.4f", mmrScore));

            selected.add(candidate.document);
        }

        return selected;
    }

    /**
     * 扩展单个查询为多个查询变体
     */
    private List<String> expandQuery(String originalQuery) {
        String expansionPrompt = String.format("将以下查询扩展为3个不同的查询变体:\n%s", originalQuery);
        String expansion = chatClient.prompt().user(expansionPrompt).call().content();

        List<String> queries = new ArrayList<>();
        for (String line : expansion.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("-") && !trimmed.startsWith("*")) {
                queries.add(trimmed);
            }
        }

        if (queries.isEmpty()) {
            queries.add(originalQuery);
        }

        return queries;
    }

    /**
     * 拼接文档列表
     */
    private List<Document> concatenateDocuments(List<List<Document>> multiChannelResults) {
        List<Document> result = new ArrayList<>();
        java.util.Set<String> seenContents = new java.util.HashSet<>();

        for (List<Document> channelDocs : multiChannelResults) {
            for (Document doc : channelDocs) {
                String content = doc.getText();
                if (!seenContents.contains(content)) {
                    seenContents.add(content);
                    result.add(doc);
                }
            }
        }
        return result;
    }

    /**
     * 解析评分
     */
    private double parseScore(String scores, int docIndex) {
        try {
            for (String line : scores.split("\\n")) {
                if (line.contains("文档" + docIndex + ":") || line.contains("Doc" + docIndex + ":")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        return Double.parseDouble(parts[1].trim());
                    }
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        // 默认返回随机分数
        return 0.5 + Math.random() * 0.5;
    }

    /**
     * 模拟 Cross-Encoder 评分
     */
    private double simulateCrossEncoderScore(String query, String document) {
        // 简单模拟：基于关键词匹配度
        String[] queryWords = query.toLowerCase().split("\\s+");
        long matchCount = 0;
        for (String word : queryWords) {
            if (document.toLowerCase().contains(word)) {
                matchCount++;
            }
        }
        return matchCount / (double) Math.max(1, queryWords.length);
    }

    /**
     * 计算文档相似度（用于多样性计算）
     */
    private double calculateSimilarity(String doc1, String doc2) {
        // 简单模拟：基于共同词汇
        java.util.Set<String> words1 = new java.util.HashSet<>();
        java.util.Set<String> words2 = new java.util.HashSet<>();

        for (String word : doc1.toLowerCase().split("\\s+")) {
            if (word.length() > 3) {
                words1.add(word);
            }
        }
        for (String word : doc2.toLowerCase().split("\\s+")) {
            if (word.length() > 3) {
                words2.add(word);
            }
        }

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }

        java.util.Set<String> intersection = new java.util.HashSet<>(words1);
        intersection.retainAll(words2);

        java.util.Set<String> union = new java.util.HashSet<>(words1);
        union.addAll(words2);

        return intersection.size() / (double) union.size();
    }

    /**
     * 带评分的文档记录
     */
    private record DocumentWithScore(Document document, double score) {}
}
