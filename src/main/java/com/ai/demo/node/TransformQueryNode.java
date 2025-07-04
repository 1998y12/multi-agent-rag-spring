package com.ai.demo.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;

@Builder
@Slf4j
public class TransformQueryNode implements NodeAction {

    private final ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String question = state.value("question", String.class).orElse("");
        String betterQuestion = chatClient.prompt()
                .user(u -> u.param("question", question))
                .call()
                .content();
        log.info("重写后的问题: {}", betterQuestion);
        // 更新状态
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("question", betterQuestion);
        return resultMap;
    }
}
