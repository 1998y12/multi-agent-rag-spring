package com.ai.demo.edge;

import com.ai.demo.entity.RouteQueryEntity;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RouteQuestionEdge implements EdgeAction {

    private final ChatClient questionRouterChatClient;

    public RouteQuestionEdge(@Qualifier("QuestionRouterChatClient") ChatClient questionRouterChatClient) {
        this.questionRouterChatClient = questionRouterChatClient;
    }

    @Override
    public String apply(OverAllState state) {
        log.info("---------- 边：路由问题 ----------");

        String question = state.value("question", String.class).orElse("");

        // 决定数据源
        RouteQueryEntity response = questionRouterChatClient.prompt()
                .user(u -> u.param("question", question))
                .system(s -> s.param("knowledge_base", "关于Spring AI Alibaba的相关知识"))
                .call()
                .entity(RouteQueryEntity.class);

        log.info("路由到: {}", response);
        assert response != null;

        return response.dataSource();
    }
}
