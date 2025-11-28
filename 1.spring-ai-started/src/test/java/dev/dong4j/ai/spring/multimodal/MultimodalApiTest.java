package dev.dong4j.ai.spring.multimodal;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

/**
 * 多模态 API 测试类
 *
 * <p>测试 Spring AI 的多模态功能，包括：
 *
 * <ul>
 *   <li>文本 + 图像输入
 *   <li>多张图像输入
 *   <li>从 URL 加载图像
 *   <li>图像 + 结构化输出
 * </ul>
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.26
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("multimodal")
class MultimodalApiTest {

    @jakarta.annotation.Resource private ChatClient.Builder chatClientBuilder;

    /** 图像分析记录类 */
    record ImageAnalysis(String mainSubject, List<String> objects, String description) {}

    /**
     * 测试：文本 + 图像输入
     *
     * <p>对应 README.md 中的示例：文本 + 图像输入
     */
    @Test
    void testTextAndImageInput() {
        ChatClient client = chatClientBuilder.build();

        // 从类路径加载图像
        Resource imageResource = new ClassPathResource("images/test1.jpg");

        // 同时使用文本和图像
        String reply =
                client.prompt()
                        .user(
                                u ->
                                        u.text("请分析这张图片，描述其中的主要内容")
                                                .media(MimeTypeUtils.IMAGE_JPEG, imageResource))
                        .call()
                        .content();

        assertThat(reply).isNotNull().isNotEmpty();
        log.info("图像分析结果: {}", reply);
    }

    /**
     * 测试：多张图像输入
     *
     * <p>对应 README.md 中的示例：多张图像输入
     */
    @Test
    void testMultipleImagesInput() {
        ChatClient client = chatClientBuilder.build();

        Resource image1 = new ClassPathResource("images/test1.jpg");
        Resource image2 = new ClassPathResource("images/test2.png");

        // 同时分析多张图片
        String reply =
                client.prompt()
                        .user(
                                u ->
                                        u.text("请对比这两张图表，找出它们的差异")
                                                .media(MimeTypeUtils.IMAGE_JPEG, image1)
                                                .media(MimeTypeUtils.IMAGE_PNG, image2))
                        .call()
                        .content();

        assertThat(reply).isNotNull().isNotEmpty();
        log.info("图像对比结果: {}", reply);
    }

    /**
     * 测试：从 URL 加载图像
     *
     * <p>对应 README.md 中的示例：从 URL 加载图像
     */
    @Test
    void testImageFromUrl() {
        ChatClient client = chatClientBuilder.build();

        // 从 URL 加载图像
        try {
            Resource imageUrl = new UrlResource("https://cdn.dong4j.site/source/image/avatar.webp");

            String reply =
                    client.prompt()
                            .user(
                                    u ->
                                            u.text("这张图片展示了什么？")
                                                    .media(MimeTypeUtils.IMAGE_PNG, imageUrl))
                            .call()
                            .content();

            assertThat(reply).isNotNull().isNotEmpty();
            log.info("URL 图像分析结果: {}", reply);
        } catch (Exception e) {
            log.info("跳过测试：无法访问图像 URL - {}", e.getMessage());
        }
    }

    /**
     * 测试：图像 + 结构化输出
     *
     * <p>对应 README.md 中的示例：图像 + 结构化输出
     */
    @Test
    void testImageWithStructuredOutput() {
        ChatClient client = chatClientBuilder.build();
        Resource image = new ClassPathResource("images/test1.jpg");

        // 结合结构化输出分析图像
        ImageAnalysis analysis =
                client.prompt()
                        .user(
                                u ->
                                        u.text(
                                                        """
                                                        请分析这张产品图片，提取以下信息(使用中文)：
                                                        - 主要产品名称
                                                        - 图片中的对象列表
                                                        - 产品描述
                                                        """)
                                                .media(MimeTypeUtils.IMAGE_JPEG, image))
                        .call()
                        .entity(ImageAnalysis.class);

        assertThat(analysis).isNotNull();
        assertThat(analysis.mainSubject()).isNotNull().isNotEmpty();
        assertThat(analysis.objects()).isNotNull();
        assertThat(analysis.description()).isNotNull().isNotEmpty();

        log.info("产品: {}", analysis.mainSubject());
        log.info("对象: {}", analysis.objects());
        log.info("描述: {}", analysis.description());
    }
}
