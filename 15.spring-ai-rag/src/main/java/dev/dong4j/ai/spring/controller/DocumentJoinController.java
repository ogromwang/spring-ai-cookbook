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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档连接控制器
 *
 * <p>演示 Document Join（文档连接）的相关技术。
 * 文档连接是高级 RAG 中多路召回场景下的关键环节，用于将多个检索源的结果合并。
 *
 * 主要技术包括：
 * 1. ConcatenationDocumentJoiner（拼接文档连接器）
 *    - 通过将基于多个查询和来自多个数据源检索到的文档连接起来
 *    - 组合成一个文档集合
 *    - 如果存在重复文档，则保留第一次出现的内容
 *
 * 2. ScoreDocListJoiner（评分文档连接器）
 *    - 根据文档的相关性评分进行排序和连接
 *    - 支持升序或降序排列
 *
 * 3. WeightedDocumentJoiner（加权文档连接器）
 *    - 根据不同数据源的权重进行加权融合
 *    - 例如：向量检索权重0.7，BM25权重0.3
 *    - 得分加权求和后选择 top-k 文档
 *
 * @author ogromwang
 * @version 1.0.0
 * @email mailto:ogromwang@gmail.com
 * @date 2025.11.29
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rag/join")
public class DocumentJoinController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    /**
     * 构造函数，注入相关组件
     *
     * @param chatClient 聊天客户端
     * @param vectorStore 向量存储
     */
    public DocumentJoinController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 文档拼接连接演示接口
     *
     * <p>演示 ConcatenationDocumentJoiner 的功能：
     * 将多个检索结果按顺序拼接，合并为一个文档列表，自动去重。
     *
     * 适用场景：
     * - 多查询变体的结果合并
     * - 多数据源的结果合并
     * - 需要保留原始顺序的场景
     *
     * @param query 用户查询
     * @return 拼接合并后的文档列表和检索结果
     */
    @GetMapping("/concatenation")
    public String concatenationJoin(@RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query) {
        // 生成多个查询变体
        List<String> queryVariants = generateQueryVariants(query);

        // 模拟多路检索结果
        List<List<Document>> multiChannelResults = new ArrayList<>();
        for (String variant : queryVariants) {
            List<Document> docs = vectorStore.similaritySearch(variant);
            multiChannelResults.add(docs);
        }

        // 使用拼接方式合并文档（ConcatenationDocumentJoiner）
        List<Document> concatenatedDocs = concatenateDocuments(multiChannelResults);

        StringBuilder docsContent = new StringBuilder();
        for (int i = 0; i < concatenatedDocs.size(); i++) {
            Document doc = concatenatedDocs.get(i);
            docsContent.append(String.format("%d. %s\n", i + 1, doc.getText()));
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Concatenation Document Joiner（文档拼接连接） ===

                用户查询: %s
                查询变体数量: %d

                合并策略: 按检索顺序拼接，保留首次出现的文档

                合并后的文档列表:
                %s

                AI 回复:
                %s

                说明: ConcatenationDocumentJoiner 通过将基于多个查询和来自多个数据源
                      检索到的文档连接起来，组合成一个文档集合。如果存在重复文档，
                      则保留第一次出现的内容。
                """, query, queryVariants.size(), docsContent, response);
    }

    /**
     * 评分排序连接演示接口
     *
     * <p>演示 ScoreDocListJoiner 的功能：
     * 根据文档的相关性评分进行排序，选择 top-k 文档。
     *
     * 适用场景：
     * - 需要优先返回高相关性文档
     * - 上下文窗口有限，需要精选文档
     * - 需要控制输入 LLM 的文档数量
     *
     * @param query 用户查询
     * @param topK 返回的文档数量
     * @return 按评分排序后的文档列表
     */
    @GetMapping("/score-sort")
    public String scoreSortJoin(
            @RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query,
            @RequestParam(defaultValue = "3") int topK) {
        // 检索文档
        List<Document> docs = vectorStore.similaritySearch(query);

        // 使用评分排序方式合并（ScoreDocListJoiner）
        List<Document> sortedDocs = sortByScore(docs, topK);

        StringBuilder docsContent = new StringBuilder();
        for (int i = 0; i < sortedDocs.size(); i++) {
            Document doc = sortedDocs.get(i);
            Object score = doc.getMetadata().get("score");
            docsContent.append(String.format("%d. [相关性: %s] %s\n",
                    i + 1, score != null ? score : "N/A", doc.getText()));
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Score Doc List Joiner（评分排序连接） ===

                用户查询: %s
                Top-K: %d

                合并策略: 按相关性评分降序排序，选取 top-k

                排序后的文档列表:
                %s

                AI 回复:
                %s

                说明: ScoreDocListJoiner 根据文档的相关性评分进行排序，
                      选择得分最高的 top-k 文档，减少不相关文档的干扰。
                """, query, topK, docsContent, response);
    }

    /**
     * 加权融合连接演示接口
     *
     * <p>演示 WeightedDocumentJoiner 的功能：
     * 根据不同数据源的权重进行加权融合。
     *
     * 例如：
     * - 向量检索权重 0.7
     * - BM25 权重 0.3
     * - 得分加权求和后选择 top-k
     *
     适用场景：
     * - 多路召回（向量检索 + 关键词检索）
     * - 需要平衡不同检索源的贡献
     * - 需要精细控制各检索源的权重
     *
     * @param query 用户查询
     * @param vectorWeight 向量检索权重
     * @param bm25Weight BM25 权重
     * @return 加权融合后的文档列表
     */
    @GetMapping("/weighted-fusion")
    public String weightedFusionJoin(
            @RequestParam(defaultValue = "Spring AI 的核心特性是什么？") String query,
            @RequestParam(defaultValue = "0.7") double vectorWeight,
            @RequestParam(defaultValue = "0.3") double bm25Weight) {
        // 模拟多路召回结果
        // 实际场景中，这可能是向量检索和 BM25 检索的结果
        List<Document> vectorResults = vectorStore.similaritySearch(query);

        // 模拟 BM25 检索结果（这里用向量检索结果模拟）
        List<Document> bm25Results = vectorStore.similaritySearch(query);

        // 使用加权融合方式合并（WeightedDocumentJoiner）
        List<Document> fusedDocs = weightedFusion(
                vectorResults, bm25Results,
                vectorWeight, bm25Weight, 5);

        StringBuilder docsContent = new StringBuilder();
        for (int i = 0; i < fusedDocs.size(); i++) {
            Document doc = fusedDocs.get(i);
            docsContent.append(String.format("%d. %s\n", i + 1, doc.getText()));
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === Weighted Document Joiner（加权融合连接） ===

                用户查询: %s
                向量检索权重: %.1f
                BM25 权重: %.1f

                合并策略: 多路召回得分加权求和，选择 top-5

                加权融合后的文档列表:
                %s

                AI 回复:
                %s

                说明: WeightedDocumentJoiner 根据不同数据源的权重进行加权融合，
                      如向量检索权重 %.1f，BM25 权重 %.1f，得分加权求和后选择 top-k。
                      这种方式解决了"多样性"和"覆盖度"问题。
                """, query, vectorWeight, bm25Weight, docsContent, response, vectorWeight, bm25Weight);
    }

    /**
     * 多路召回融合完整演示接口
     *
     * <p>展示完整的多路召回与融合流程：
     * 1. 并行执行多个检索（向量检索 + 关键词检索等）
     * 2. 使用加权融合或 RRF 融合结果
     * 3. 选择 top-k 文档作为最终检索结果
     *
     * @param query 用户查询
     * @return 完整的多路召回融合结果
     */
    @GetMapping("/multi-channel-fusion")
    public String multiChannelFusion(@RequestParam(defaultValue = "Spring AI 支持哪些功能？") String query) {
        // 模拟多路召回
        List<List<Document>> channelResults = new ArrayList<>();

        // 通道1: 向量检索
        List<Document> vectorDocs = vectorStore.similaritySearch(query);
        channelResults.add(vectorDocs);

        // 通道2: 另一个查询变体的检索
        List<String> variants = generateQueryVariants(query);
        for (int i = 1; i < Math.min(3, variants.size()); i++) {
            List<Document> variantDocs = vectorStore.similaritySearch(variants.get(i));
            channelResults.add(variantDocs);
        }

        // 统计信息
        int totalDocs = channelResults.stream().mapToInt(List::size).sum();

        // 多路融合
        List<Document> fusedDocs = multiChannelFusion(channelResults, 5);

        StringBuilder channelInfo = new StringBuilder();
        channelInfo.append("通道1: 向量检索, 文档数: ").append(vectorDocs.size()).append("\n");
        for (int i = 1; i < channelResults.size(); i++) {
            channelInfo.append(String.format("通道%d: 查询变体检索, 文档数: %d\n",
                    i + 1, channelResults.get(i).size()));
        }

        StringBuilder docsContent = new StringBuilder();
        for (int i = 0; i < fusedDocs.size(); i++) {
            Document doc = fusedDocs.get(i);
            docsContent.append(String.format("%d. %s\n", i + 1, doc.getText()));
        }

        String response = chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("VectorStoreRetriever", vectorStore))
                .call()
                .content();

        return String.format("""
                === 多路召回与融合（Multi-Channel Recall & Fusion） ===

                用户查询: %s

                检索通道:
                %s

                融合策略: Reciprocal Rank Fusion (RRF) 或加权投票

                总检索文档数: %d
                融合后 top-5: %d

                融合后的文档列表:
                %s

                AI 回复:
                %s

                说明: 多路召回与融合并行执行多个检索（向量检索 + 关键词检索 + 图谱查询等），
                      然后使用 RRF 或加权投票融合结果，解决"多样性"和"覆盖度"问题。
                """, query, channelInfo, totalDocs, fusedDocs.size(), docsContent, response);
    }

    /**
     * 生成查询变体
     */
    private List<String> generateQueryVariants(String query) {
        String prompt = String.format("将以下查询扩展为3个不同的表述:\n%s", query);
        String expansion = chatClient.prompt().user(prompt).call().content();

        List<String> variants = new ArrayList<>();
        variants.add(query);
        for (String line : expansion.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("-") && !trimmed.startsWith("*")) {
                variants.add(trimmed);
            }
        }
        return variants;
    }

    /**
     * 拼接文档列表
     */
    private List<Document> concatenateDocuments(List<List<Document>> multiChannelResults) {
        List<Document> result = new ArrayList<>();
        Set<String> seenContents = new HashSet<>();

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
     * 按评分排序文档
     */
    private List<Document> sortByScore(List<Document> docs, int topK) {
        return docs.stream()
                .sorted((a, b) -> {
                    Double scoreA = extractScore(a);
                    Double scoreB = extractScore(b);
                    return scoreB.compareTo(scoreA); // 降序
                })
                .limit(topK)
                .toList();
    }

    /**
     * 加权融合多个检索结果
     */
    private List<Document> weightedFusion(
            List<Document> vectorResults,
            List<Document> bm25Results,
            double vectorWeight,
            double bm25Weight,
            int topK) {
        // 合并所有文档
        Set<String> allContents = new HashSet<>();
        List<DocumentWithWeight> weightedDocs = new ArrayList<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String content = doc.getText();
            if (!allContents.contains(content)) {
                allContents.add(content);
                weightedDocs.add(new DocumentWithWeight(doc, vectorWeight * (1.0 - i * 0.1)));
            }
        }

        for (int i = 0; i < bm25Results.size(); i++) {
            Document doc = bm25Results.get(i);
            String content = doc.getText();
            if (!allContents.contains(content)) {
                allContents.add(content);
                weightedDocs.add(new DocumentWithWeight(doc, bm25Weight * (1.0 - i * 0.1)));
            }
        }

        // 按权重排序
        weightedDocs.sort((a, b) -> Double.compare(b.weight, a.weight));

        return weightedDocs.stream()
                .limit(topK)
                .map(d -> d.document)
                .toList();
    }

    /**
     * 多通道融合（RRF 简化实现）
     */
    private List<Document> multiChannelFusion(List<List<Document>> channelResults, int topK) {
        // 简单实现：合并去重后取前 topK
        return concatenateDocuments(channelResults).stream().limit(topK).toList();
    }

    /**
     * 提取文档评分
     */
    private double extractScore(Document doc) {
        Object score = doc.getMetadata().get("score");
        if (score != null) {
            return Double.parseDouble(score.toString());
        }
        return 0.0;
    }

    /**
     * 带权重的文档记录
     */
    private record DocumentWithWeight(Document document, double weight) {}
}
