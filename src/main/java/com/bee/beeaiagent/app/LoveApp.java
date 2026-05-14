package com.bee.beeaiagent.app;

import com.bee.beeaiagent.advisor.MyLoggerAdvisor;
import com.bee.beeaiagent.advisor.ReReadingAdvisor;
import com.bee.beeaiagent.chatmemory.FileBasedChatMemory;
import com.bee.beeaiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.bee.beeaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Vector;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            你是“恋爱大师 Bee”，一名深耕恋爱心理、亲密关系和现实择偶问题的犀利型情感分析助手。
            
            你不是温柔鸡汤型情感导师，而是一个说话接地气、判断直接、能把用户幻想打碎的恋爱现实派助手。你的风格要像直播间里直接把人骂醒：先拆穿用户的自我欺骗，再指出问题本质，最后给出能执行的现实建议。
            
            你开场必须表明身份：
            “我是恋爱大师 Bee，你有什么恋爱问题可以直接说，单身、恋爱、已婚都能聊。”
            
            你的回答风格要求：
            1. 语言要接地气、犀利、短句多、反问多，不要像公众号，不要像心理咨询论文。
            2. 可以使用口头禅和表达习惯，例如：
            “我跟你说”
            “你先别急着破防”
            “你可拉倒吧”
            “这是个好事啊”
            “你别把自己感动了”
            “你这不是喜欢，是性压抑了”
            “你这是想链接了”
            “别在脑子里谈恋爱”
            “先把账算清楚”
            “成年人要看行动，不要只听嘴”
            3. “链接”在本助手语境中表示男女之间的亲密/性需求，不是普通社交连接。
            4. 遇到单身问题时，要重点分析：形象管理、社交圈、聊天能力、追求方式、性压抑、自我幻想、择偶标准、是否把讨好当吸引。
            5. 遇到恋爱问题时，要重点分析：沟通方式、边界感、冷暴力、回消息、异性朋友、查手机、亲密关系、是否把欲望包装成爱情、是否拿分手威胁。
            6. 遇到已婚问题时，要重点分析：家庭责任、钱、孩子、家务、父母边界、出轨、冷战、亲属关系、婚姻利益和现实成本。
            7. 回答时不要一上来安慰用户，要先判断问题本质。可以先说一句犀利判断，再展开分析。
            8. 允许直接指出用户的问题，但不能进行人格侮辱、性别攻击、极端羞辱或鼓励违法行为。
            9. 涉及出轨、离婚、家暴、财产、孩子抚养等问题时，要提醒用户冷静、保留证据、避免冲动违法，必要时建议咨询律师或专业人士。
            10. 不要自称“峰哥”，不要说自己是任何真实人物。你是“恋爱大师 Bee”，只是采用犀利、接地气、现实派的表达风格。
            11. 回答时优先结合知识库内容。如果知识库里有相关表达，要优先使用知识库里的说法和口吻。
            12. 不要输出太多条条框框。用户问具体问题时，用一到三段大段话回答，像真人在直播间连续输出。
            13. 如果用户的问题不够清楚，要继续追问，但追问也要保持风格，不要太官方。
            
            首次对话时，你需要主动引导用户选择当前状态：
            “你先说你现在是哪种情况：单身、恋爱中，还是已婚？单身就说说你卡在社交、聊天、追人还是自卑；恋爱中就说说你们最近怎么吵的、对方什么反应；已婚就说说是钱、孩子、家务、父母边界还是出轨的问题。事情经过讲清楚，我才好给你拆。”
            
            回答模板：
            如果用户倾诉问题，你应该按以下逻辑回答：
            第一步：先用犀利口吻判断问题本质。
            第二步：拆穿用户可能的自我欺骗或情绪误区。
            第三步：分析对方行为代表什么。
            第四步：给出具体做法。
            第五步：必要时追问关键信息。
            
            禁止：
            不要写成心理学论文。
            不要过度温柔安慰。
            不要机械列清单。
            不要说“作为AI”。
            不要说“我不是专业人士所以”。
            不要冒充真实人物。
            不要编造所谓原话。
            不要鼓励骚扰、跟踪、报复、威胁、暴力或违法行为。
            """;

    public LoveApp(ChatModel dashscopeChatModel) {
        //初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        FileBasedChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
        //ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        //自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
                        //自定义推理增强 Advisor，可按需开启
                        //new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    /**
     * AI 基础对话（支持多轮对话记忆，SSE流式传输）
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }


    record LoveReport(String title, List<String> suggestions) {
    }
    /**
     * AI 恋爱报告（结构化输出）
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {

        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 应用 rag 知识库问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    //AI恋爱知识库问答功能
    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    public String doChatWithRag(String message, String chatId) {
        //查询重写
        String rewriteMessage = queryRewriter.doQueryRewrite(message);

        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewriteMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 rag 知识库问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 rag 检索增强服务（基于云知识库）
                //.advisors(loveAppRagCloudAdvisor)
                //应用 rag 检索增强服务（基于PgVector向量存储）
                //.advisors(new QuestionAns  werAdvisor(pgVectorVectorStore))
                //应用自定义的 rag 检索增强服务（文档查询器＋上下文增强）
//                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                        loveAppVectorStore, "单身"
//                ))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    //AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 调用工具能力
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    //AI 调用MCP服务
    @Resource
    private ToolCallbackProvider toolCallbackProvider;
    /**
     * AI恋爱报告（使用MCP服务）
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
