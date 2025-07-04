package com.ai.demo.node;

import com.ai.demo.tool.WebSearchTool;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Slf4j
public class WebSearchNode implements NodeAction {

    private final ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String query = state.value("question", "");
        WebSearchTool.TavilyResponse response = chatClient.prompt()
                .system(s -> s.param("date", LocalDate.now().toString())).user(u -> u.param("question", query)).call()
                .entity(WebSearchTool.TavilyResponse.class);


        assert response != null;
        log.debug("WebSearchNode response: {}", response);

        // 获取内容并转为文档对象
        List<Document> documents = response.getResults().stream()
                .map(result -> new Document(result.getContent(),
                        Map.of("origin", result.getUrl(),
                                "title", result.getTitle())))
                .collect(Collectors.toList());
        documents.addFirst(new Document(response.getAnswer(),
                Map.of("origin", "Web Search Answer", "title", "Web Search Answer")));

        // 更新状态
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("question", query);
        resultMap.put("documents", documents);
        return resultMap;
    }
}
