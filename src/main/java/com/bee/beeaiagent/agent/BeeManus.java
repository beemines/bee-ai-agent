package com.bee.beeaiagent.agent;

import com.bee.beeaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Bee 的AI超级智能体，（拥有自主规划能力，可以直接使用）
 */
@Component
public class BeeManus extends ToolCallAgent {

    public BeeManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("beeManus");
        String SYSTEM_PROMPT = """  
                你是 BeeManus，一个能够自主规划并调用工具完成复杂任务的 AI 助手。
                除非用户明确要求英文，否则你的思考、工具参数、文件内容、PDF 内容和最终回答都必须使用简体中文。
                生成文件时，要优先保证内容完整、格式清晰，并在完成后说明文件保存位置。
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """  
                根据用户需求，主动选择最合适的工具或工具组合。
                对复杂任务，要先拆解步骤，再按顺序执行，不要过早结束。
                使用工具后，要根据工具结果判断下一步；如果某个工具失败，不要反复无意义重试，应分析原因并换用可行方案。
                生成 PDF、HTML、文本等文件时，除非用户明确要求英文，否则文件内容必须使用简体中文。
                任务完成并确认结果后，再调用 `terminate` / `doTerminate` 工具结束。
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(40);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
