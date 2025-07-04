package com.ai.demo.config;

import com.ai.demo.tool.WebSearchTool;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@AllArgsConstructor
public class ChatClientConfig {

    private final WebSearchTool webSearchTool;

    /**
     * 记忆类型 固定容量的消息窗口
     * <p>此为 Spring AI 自动配置 ChatMemory Bean 时采用的默认消息类型（不配置也能使用）</p>
     * @return ChatMemory 实例
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
    }

    /**
     * 记忆存储 使用内存存储的 ChatMemoryRepository
     * <p>默认情况下，若未配置其他 Repository，Spring AI 将自动配置 InMemoryChatMemoryRepository 类型的 ChatMemoryRepository Bean供直接使用。（不配置也能使用）</p>
     * @return ChatMemoryRepository 实例
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * 通用的 OpenAI LLM 客户端
     * @param chatModel 模型配置
     * @return ChatClient 实例
     */
    @Bean
    @Primary
    public ChatClient openAiChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).defaultOptions(ChatOptions.builder().temperature(0.8).build()).build();
    }

    /**
     * 问题路由器 ChatClient
     * 负责将用户问题路由到不同的数据源（向量数据库或网络搜索）
     * @param chatModel 模型配置
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient QuestionRouterChatClient(ChatModel chatModel, ChatMemory chatMemory) {
        String systemPrompt = """
                    你是一个指令路由专家，负责将用户的输入/问题路由到以下对应的组件：
                
                    1. 向量数据库(vectorstore)
                    当用户的问题与知识库中的文档内容相关时，选择 vectorstore
                
                    知识库信息:
                    {knowledge_base}
                
                    2. 网络搜索(web_search)
                    当用户的问题涉及以下情况时，选择 web_search
                    - 需要最新实时信息（如新闻、天气、股价等）
                    - 问题超出知识库范围
                
                    请做出最佳路由决策。
                """;
        return ChatClient.builder(chatModel).defaultSystem(systemPrompt)
                .defaultUser(u -> u.text("用户问题: {question}"))
                .defaultOptions(ChatOptions.builder().temperature(0.0).build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId("QuestionRouter").build())
                .build();
    }

    /**
     * 网络搜索 ChatClient
     * 使用工具进行网络搜索，并返回相关信息
     * @param chatModel 模型配置
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient WebSearchChatClient(ChatModel chatModel, ChatMemory chatMemory) {

        String systemPrompt = """
               请根据用户的问题，使用工具进行网络搜索，并返回相关信息。
               
               今天的日期是: {date}
               """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultUser(u -> u.text("用户问题：{question}"))
                // 此处要用 ToolCallingChatOptions 而不是 ChatOptions
                .defaultOptions(ToolCallingChatOptions.builder().temperature(0.8).build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId("WebSearch").build())
                .defaultTools(webSearchTool)
                .build();
    }

    /**
     * 自适应 RAG ChatClient
     * 结合向量数据库和网络搜索的结果来回答用户问题
     * @param chatModel 模型配置
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient AdaptiveRagChatClient(ChatModel chatModel, ChatMemory chatMemory) {
        String systemPrompt = """
               你是一个专业的问答助手。请基于提供的上下文信息来回答用户问题。
               
               回答指南：
               1. 优先使用提供的上下文信息来回答问题
               2. 可以参考文档的名称和描述来理解内容背景
               3. 如果多个文档都相关，可以综合多个来源的信息
               4. 保持回答准确、简洁，通常使用2-3句话
               5. 在适当时候可以提及信息来源
               6. 如果你不确定或不知道答案，请诚实地说明
               """;

        String userPrompt = """
                问题:
                {question}
                
                检索到的上下文:
                {context}
                
                请基于上述信息回答问题。
                """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultUser(userPrompt)
                .defaultOptions(ChatOptions.builder().temperature(0.7).build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId("AdaptiveRag").build())
                .build();
    }

    /**
     * 用于评估 LLM 生成的回答是否基于检索到的事实
     * @param chatModel 模型配置
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient HallucinationChatClient(ChatModel chatModel, ChatMemory chatMemory) {

        String systemPrompt = """
                你是一个评分员，负责评估LLM生成的回答是否基于/支持一组检索到的事实。
                
                给出一个二分类分数 'yes'或'no'。 'yes'表示答案是基于/支持一组事实的。
                """;

        String userPrompt = """
                一组事实:
                {documents}
                
                LLM生成的回答:
                {generation}
                """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultUser(userPrompt)
                .defaultOptions(ChatOptions.builder().temperature(0.0).build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId("Hallucination").build())
                .build();
    }

    /**
     * 用于评估 LLM 生成的回答是否回应/解决了用户问题
     * @param chatModel 模型配置
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient AnswerGraderChatClient(ChatModel chatModel, ChatMemory chatMemory) {

        String systemPrompt = """
                你是一个评分员，负责评估回答是否回应/解决了问题。
                
                给出一个二分类分数 'yes'或'no'。 'yes'表示答案回应/解决了问题。
                """;

        String userPrompt = """
                用户问题:
                {question}
                
                LLM生成的回答:
                {generation}
                """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultUser(userPrompt)
                .defaultOptions(ChatOptions.builder().temperature(0.8).build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId("AnswerGrader").build())
                .build();
    }

    /**
     * 问题重写 ChatClient
     * 负责将用户的问题重写为更清晰、更具体的形式，以便于优化检索
     * @param chatModel 模型配置
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient QuestionRewriterChatClient(ChatModel chatModel, ChatMemory chatMemory) {

        String systemPrompt = """
                你是一个专业的问题重写专家，负责将用户的问题重写为更清晰、更具体的形式，以便于优化检索。
                
                重写规则：
                1. 保持原意，但使问题更明确
                2. 使用更具体、更有针对性的关键词，避免模糊或含糊的表述
                3. 如果问题过于宽泛，尝试将其细化为更具体的子问题
                """;

        String userPrompt = """
                原始问题:
                {question}
                
                请重写这个问题以提高检索效果：
                """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultUser(userPrompt)
                .defaultOptions(ChatOptions.builder().temperature(0.0).build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId("QuestionRewriter").build())
                .build();
    }
}
