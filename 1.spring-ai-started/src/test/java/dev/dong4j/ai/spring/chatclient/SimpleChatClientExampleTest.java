package dev.dong4j.ai.spring.chatclient;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SimpleChatClientExample 测试类
 *
 * <p>测试 ChatClient 的基本功能
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.26
 * @since 1.0.0
 */
@SpringBootTest
class SimpleChatClientExampleTest {

    @Resource private SimpleChatClientExample chatClientExample;

    /** 测试简单对话功能 */
    @Test
    void testChat() {
        String response = chatClientExample.chat("你好");

        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();

        System.out.println("AI 回复: " + response);
    }
}
