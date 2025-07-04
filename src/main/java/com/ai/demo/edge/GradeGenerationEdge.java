package com.ai.demo.edge;

import com.ai.demo.entity.GradeScore;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GradeGenerationEdge implements EdgeAction {

    private final ChatClient hallucinationGrader;

    private final ChatClient answerGrader;

    public GradeGenerationEdge(@Qualifier("HallucinationChatClient") ChatClient hallucinationGrader,
            @Qualifier("AnswerGraderChatClient") ChatClient answerGrader) {
        this.hallucinationGrader = hallucinationGrader;
        this.answerGrader = answerGrader;
    }

    /**
     * 评估生成质量
     * @param state 图状态
     * @return "hallucination" 如果生成的回答不符合事实，需要重试；
     * "unuseful" 如果生成的回答没有回应问题，需要重写问题；
     * "useful" 如果生成的回答回应了问题。
     */
    @Override
    public String apply(OverAllState state) {
        log.info("---------- 边：检查生成的回答是否符合事实 ----------");
        String question = state.value("question", String.class).orElse("");
        String generation = state.value("generation", String.class).orElse("");
        List<Document> documents = state.value("documents", List.of());

        GradeScore hallucinationGradeScore = hallucinationGrader.prompt()
                .user(u -> u.param("documents", formatDocs(documents))
                        .param("generation", generation))
                .call()
                .entity(GradeScore.class);

        assert hallucinationGradeScore != null;
        if (!"yes".equals(hallucinationGradeScore.binaryScore())) {
            log.info("---------- 决策：生成的回答不符合事实，需要重试 ----------");
            return "hallucination";
        }

        log.info("---------- 决策：生成的回答符合事实 ----------");
        GradeScore answerGradeScore = answerGrader.prompt()
                .user(u -> u.param("question", question)
                        .param("generation", generation))
                .call()
                .entity(GradeScore.class);

        assert answerGradeScore != null;
        if ("yes".equals(answerGradeScore.binaryScore())) {
            log.info("---------- 决策：生成的回答回应了问题 ----------");
            return "useful";
        }

        log.info("---------- 决策：生成的回答没有回应问题 ----------");
        return "unuseful";
    }

    private String formatDocs(List<Document> documents) {
        return documents.stream().map(Document::getText).collect(Collectors.joining("\n\n"));
    }
}
