package com.ai.demo.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.Builder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
public class GenerationNode implements NodeAction {

    private final ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String query = state.value("question", "");
        List<Document> documents = state.value("documents", List.of());

        String generation = chatClient.prompt()
                .user(u -> u.param("question", query)
                        .param("context", documents
                                .stream()
                                .map(Document::getText)
                                .collect(Collectors.joining("\n\n"))))
                .call()
                .content();

        // 更新状态
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("generation", generation);

        return resultMap;
    }
}
