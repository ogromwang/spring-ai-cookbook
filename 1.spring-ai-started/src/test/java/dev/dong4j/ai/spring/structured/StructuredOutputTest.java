package dev.dong4j.ai.spring.structured;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 结构化输出测试类
 *
 * <p>测试 Spring AI 的结构化输出功能，包括：
 * <ul>
 *   <li>使用 entity() 方法返回 Java 对象</li>
 *   <li>返回 List 类型</li>
 *   <li>使用 BeanOutputConverter</li>
 *   <li>流式响应转结构化</li>
 * </ul>
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.26
 * @since 1.0.0
 */
@SpringBootTest
class StructuredOutputTest {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    /**
     * 演员电影记录类
     */
    record ActorFilms(String actor, List<String> movies) {
    }

    /**
     * 产品记录类
     */
    record Product(String name, String description, Double price, List<String> features) {
    }

    /**
     * 测试：使用 entity() 方法返回 Java 对象
     *
     * <p>对应 README.md 中的示例：使用 entity() 方法
     */
    @Test
    void testEntityMethod() {
        ChatClient client = chatClientBuilder.build();

        // 直接返回 Java 对象
        ActorFilms result = client.prompt()
            .user("生成一个随机演员的电影作品列表，返回 JSON 格式")
            .call()
            .entity(ActorFilms.class);

        assertThat(result).isNotNull();
        assertThat(result.actor()).isNotNull().isNotEmpty();
        assertThat(result.movies()).isNotNull().isNotEmpty();

        System.out.println("演员: " + result.actor());
        System.out.println("电影: " + result.movies());
    }

    /**
     * 测试：返回 List 类型
     *
     * <p>对应 README.md 中的示例：返回 List 类型
     */
    @Test
    void testEntityList() {
        ChatClient client = chatClientBuilder.build();

        // 返回 List 需要使用 ParameterizedTypeReference
        List<ActorFilms> results = client.prompt()
            .user("生成 3 个演员的电影作品列表，包括 Tom Hanks 和 Bill Murray，返回 JSON 数组格式")
            .call()
            .entity(new ParameterizedTypeReference<>() {
            });

        assertThat(results).isNotNull().isNotEmpty();
        assertThat(results.size()).isGreaterThanOrEqualTo(1);

        results.forEach(actor -> {
            assertThat(actor.actor()).isNotNull().isNotEmpty();
            assertThat(actor.movies()).isNotNull().isNotEmpty();
            System.out.println(actor.actor() + ": " + actor.movies());
        });
    }

    /**
     * 测试：使用 BeanOutputConverter
     *
     * <p>对应 README.md 中的示例：使用 BeanOutputConverter
     */
    @Test
    void testBeanOutputConverter() {
        ChatClient client = chatClientBuilder.build();
        BeanOutputConverter<Product> converter = new BeanOutputConverter<>(Product.class);

        // 在提示词中包含格式说明
        String format = converter.getFormat();
        String prompt = """
            请为 Spring AI 创建一个产品介绍。
            格式要求：
            %s
            """.formatted(format);

        Product product = client.prompt()
            .user(prompt)
            .call()
            .entity(Product.class);

        assertThat(product).isNotNull();
        assertThat(product.name()).isNotNull().isNotEmpty();
        assertThat(product.description()).isNotNull().isNotEmpty();
        assertThat(product.price()).isNotNull();
        assertThat(product.features()).isNotNull().isNotEmpty();

        System.out.println("产品名称: " + product.name());
        System.out.println("产品描述: " + product.description());
        System.out.println("产品价格: " + product.price());
        System.out.println("产品特性: " + product.features());
    }

    /**
     * 测试：流式响应转结构化
     *
     * <p>对应 README.md 中的示例：流式响应转结构化
     */
    @Test
    void testStreamToStructured() {
        ChatClient client = chatClientBuilder.build();
        BeanOutputConverter<List<ActorFilms>> converter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
            });

        // 流式获取响应
        Flux<String> flux = client.prompt()
            .user(u -> u
                .text("生成 2 个演员的电影作品列表。格式要求：\n{format}")
                .param("format", converter.getFormat()))
            .stream()
            .content();

        // 聚合流式响应并转换
        String content = flux.collectList()
            .block()
            .stream()
            .collect(Collectors.joining());

        assertThat(content).isNotNull().isNotEmpty();

        List<ActorFilms> results = converter.convert(content);

        assertThat(results).isNotNull().isNotEmpty();
        assertThat(results.size()).isGreaterThanOrEqualTo(1);

        results.forEach(actor -> {
            assertThat(actor.actor()).isNotNull().isNotEmpty();
            assertThat(actor.movies()).isNotNull().isNotEmpty();
            System.out.println(actor.actor() + ": " + actor.movies());
        });
    }
}

