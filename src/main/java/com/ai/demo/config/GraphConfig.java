package com.ai.demo.config;

import com.ai.demo.edge.GradeGenerationEdge;
import com.ai.demo.edge.RouteQuestionEdge;
import com.ai.demo.node.GenerationNode;
import com.ai.demo.node.RetrieveNode;
import com.ai.demo.node.TransformQueryNode;
import com.ai.demo.node.WebSearchNode;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@Slf4j
public class GraphConfig {

    private final RouteQuestionEdge routeQuestionEdge;

    private final GradeGenerationEdge gradeGenerationEdge;

    private final ChatClient commonChatClient;

    private final ChatClient webSearchClient;

    private final ChatClient ragChatClient;

    private final ChatClient questionRewriterChatClient;

    private final DocumentRetriever documentRetriever;

    private final CompressionQueryTransformer compressionQueryTransformer;

    private final RewriteQueryTransformer rewriteQueryTransformer;

    private final TranslationQueryTransformer translationQueryTransformer;

    public GraphConfig(RouteQuestionEdge routeQuestionEdge, GradeGenerationEdge gradeGenerationEdge,
            ChatClient commonChatClient,
            @Qualifier("WebSearchChatClient") ChatClient webSearchClient,
            @Qualifier("AdaptiveRagChatClient") ChatClient ragChatClient,
            @Qualifier("QuestionRewriterChatClient") ChatClient questionRewriterChatClient,
            DocumentRetriever documentRetriever,
            CompressionQueryTransformer compressionQueryTransformer,
            RewriteQueryTransformer rewriteQueryTransformer,
            TranslationQueryTransformer translationQueryTransformer) {
        this.routeQuestionEdge = routeQuestionEdge;
        this.gradeGenerationEdge = gradeGenerationEdge;
        this.commonChatClient = commonChatClient;
        this.webSearchClient = webSearchClient;
        this.ragChatClient = ragChatClient;
        this.questionRewriterChatClient = questionRewriterChatClient;
        this.documentRetriever = documentRetriever;
        this.compressionQueryTransformer = compressionQueryTransformer;
        this.rewriteQueryTransformer = rewriteQueryTransformer;
        this.translationQueryTransformer = translationQueryTransformer;
    }

    @Bean
    public StateGraph graph(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        OverAllStateFactory stateFactory = () -> {
            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy("question", new ReplaceStrategy());
            state.registerKeyAndStrategy("generation", new ReplaceStrategy());
            state.registerKeyAndStrategy("documents", new ReplaceStrategy());
            return state;
        };

        StateGraph stateGraph = new StateGraph("Spring AI Alibaba Graph Demo", stateFactory);

        // 添加节点
        stateGraph.addNode("prebuilt_rag_generation", AsyncNodeAction.node_async(RetrieveNode.builder()
                        .chatClient(commonChatClient)
                        .documentRetriever(documentRetriever)
                        .retrievalAugmentationAdvisor(RetrievalAugmentationAdvisor.builder()
                                .documentRetriever(documentRetriever)
                                .queryTransformers(compressionQueryTransformer, translationQueryTransformer, rewriteQueryTransformer)
                                .build())
                        .build()));
        stateGraph.addNode("web_search",
                AsyncNodeAction.node_async(WebSearchNode.builder().chatClient(webSearchClient).build()));
        stateGraph.addNode("self_rag_generation",
                AsyncNodeAction.node_async(GenerationNode.builder().chatClient(ragChatClient).build()));
        stateGraph.addNode("transform_query",
                AsyncNodeAction.node_async(TransformQueryNode.builder().chatClient(questionRewriterChatClient).build()));

        // 决定通过向量库检索还是网络搜索
        stateGraph.addConditionalEdges(StateGraph.START, AsyncEdgeAction.edge_async(routeQuestionEdge),
                Map.of("vectorstore", "prebuilt_rag_generation", "web_search", "web_search"));

        // 向量库chains
        stateGraph.addConditionalEdges("prebuilt_rag_generation", AsyncEdgeAction.edge_async(gradeGenerationEdge),
                Map.of("useful", StateGraph.END,
                        "unuseful", "transform_query",
                        "hallucination", "prebuilt_rag_generation"));

        // 网络搜索chains
        stateGraph.addEdge("web_search", "self_rag_generation");
        stateGraph.addConditionalEdges("self_rag_generation", AsyncEdgeAction.edge_async(gradeGenerationEdge),
                Map.of("useful", StateGraph.END,
                        "unuseful", "transform_query",
                        "hallucination", "self_rag_generation"));

        // 重写问题
        stateGraph.addEdge("transform_query", "self_rag_generation");

        // 添加 Mermaid 打印
        GraphRepresentation representation = stateGraph.getGraph(GraphRepresentation.Type.MERMAID,
                "Adaptive rag flow");
        log.info("\n=== Adaptive rag Flow ===");
        log.info(representation.content());
        log.info("==================================\n");

        return stateGraph;
    }
}
