package dev.dong4j.ai.spring.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.annotation.Resource;

/**
 * 用于处理聊天助手相关请求的控制器
 *
 * <p>该控制器提供两个接口, 分别用于调用 Anthropic 和 OpenAI 的聊天客户端, 根据用户输入的提问返回相应的回答内容
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.23
 * @since 1.0.0
 */
@Controller
@ResponseBody
class AdoptionsController {

    /** OpenAI 聊天客户端实例, 用于与 OpenAI API 进行交互 */
    @Resource
    private ChatClient openAiChatClient;

    /** 用于与 Anthropic 聊天服务进行交互的客户端实例 */
    @Resource
    private ChatClient anthropicChatClient;

    /**
     * 处理向 Anthropic API 发送用户问题的 GET 请求
     *
     * <p>根据路径参数中的用户标识和请求参数中的问题内容, 调用 Anthropic 聊天接口获取回复内容
     *
     * @param user     用户标识
     * @param question 用户提出的问题内容
     * @return Anthropic API 返回的回复内容
     */
    @GetMapping("/anthropic/{user}/assistant")
    String anthropic(@PathVariable String user, @RequestParam String question) {
        return anthropicChatClient.prompt().user(question).call().content();
    }

    /**
     * 处理 OpenAI 聊天客户端的 GET 请求, 返回聊天内容
     *
     * <p>根据用户和问题调用 OpenAI 聊天客户端, 获取并返回聊天内容
     *
     * @param user     用户标识
     * @param question 用户提出的问题
     * @return 聊天内容
     */
    @GetMapping("/openAiChatClient/{user}/assistant")
    String openAiChatClient(@PathVariable String user, @RequestParam String question) {
        return openAiChatClient.prompt().user(question).call().content();
    }
}
