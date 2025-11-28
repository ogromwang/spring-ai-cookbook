package dev.dong4j.ai.spring.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * PromptExample 测试类
 *
 * <p>测试提示词管理的各种功能
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.26
 * @since 1.0.0
 */
@SpringBootTest
class PromptExampleTest {

    @Resource private PromptExample promptExample;

    /** 测试简单提示词功能 */
    @Test
    void testSimplePrompt() {
        String response = promptExample.simplePrompt("用一句话介绍 Spring AI");

        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();

        System.out.println("简单提示词 - AI 回复: " + response);
    }

    /** 测试带系统提示词的对话 */
    @Test
    void testPromptWithSystem() {
        String response = promptExample.promptWithSystem("Spring AI 是什么?");

        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
        assertThat(response).contains("Spring");

        System.out.println("系统提示词 - AI 回复: " + response);
    }

    /** 测试提示词模板功能 */
    @Test
    void testPromptWithTemplate() {
        String response = promptExample.promptWithTemplate("中文", "ChatClient");

        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();

        System.out.println("提示词模板 - AI 回复: " + response);
    }

    /** 测试自定义模板分隔符功能 */
    @Test
    void testCustomDelimiterTemplate() {
        String response = promptExample.customDelimiterTemplate("public class Test { }");

        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();

        System.out.println("自定义分隔符 - AI 回复: " + response);
    }
}
