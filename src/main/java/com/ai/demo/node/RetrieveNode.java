package com.ai.demo.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.Builder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Builder
public class RetrieveNode implements NodeAction {

    private final DocumentRetriever documentRetriever;

    private final ChatClient chatClient;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    private final ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();

    private final BaseChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
            .build();

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String query = state.value("question", "");

        // 检查 advisor 是否为空
        if (retrievalAugmentationAdvisor == null) {
            throw new IllegalStateException("RetrievalAugmentationAdvisor 不能为空");
        }

        // 检索文档
//        List<Document> documents = this.documentRetriever.retrieve(Query.builder()
//                .text(query)
//                .build());

        // other:
        ChatResponse response = chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .advisors(chatMemoryAdvisor, retrievalAugmentationAdvisor)
                .advisors(advisors -> advisors.param(CONVERSATION_ID,
                        "PrebuiltSpringRAG"))
                .user(query)
                .call()
                .chatResponse();

        // 结果
        String generation = Optional.ofNullable(response).map(ChatResponse::getResult).map(Generation::getOutput).map(AbstractMessage::getText).orElse("");
        System.out.println("Generation: " + generation);

        // 获取检索结果
        assert response != null;
        List<Document> retrievedDocuments = response.getMetadata().get("rag_document_context");

        System.out.println("Documents: " + retrievedDocuments);

        // 更新状态
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("question", query);
        resultMap.put("documents", retrievedDocuments);
        resultMap.put("generation", generation);
        return resultMap;
    }
}
