package com.ai.demo.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

@RestController
@RequestMapping("/graph")
@Slf4j
public class GraphController {

    @Value("classpath:documents/faq.md")
    Resource file1;

    @Value("classpath:documents/overview.md")
    Resource file2;

    private final CompiledGraph compiledGraph;

    private final VectorStore vectorStore;

    private final String SAVE_PATH = System.getProperty("user.dir") + "/src/main/resources/vectorstore/vectorstore.json";

    @SneakyThrows
    public GraphController(@Qualifier("graph") StateGraph stateGraph, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.compiledGraph = stateGraph.compile();
    }

    @GetMapping(value = "/add")
    public void addDocuments() {
        // 如果存在则加载
        File file = new File(SAVE_PATH);
        if (file.exists()) {
            log.info("load vector store from {}", SAVE_PATH);
            ((SimpleVectorStore) vectorStore).load(file);
            return;
        }

        log.info("start add documents");
        var markdownReader1 = new MarkdownDocumentReader(file1, MarkdownDocumentReaderConfig.builder()
                .withAdditionalMetadata("title", "Spring AI Alibaba FAQ")
                .withAdditionalMetadata("summary", "关于Spring AI Alibaba的常见问题和解答")
                .build());
        List<Document> documents = new ArrayList<>(markdownReader1.get());

        var markdownReader2 = new MarkdownDocumentReader(file2, MarkdownDocumentReaderConfig.builder()
                .withAdditionalMetadata("title", "Spring AI Alibaba Overview")
                .withAdditionalMetadata("summary", "关于Spring AI Alibaba的概述")
                .build());
        documents.addAll(markdownReader2.get());

        // 将文档添加到向量库中
        vectorStore.add(documents);

        // 持久化
        ((SimpleVectorStore) vectorStore).save(file);
    }

    @GetMapping(value = "/chat")
    public Map<String, Object> chat(@RequestParam(value = "query", defaultValue = "你好，我想知道一些关于大模型的知识",
            required = false) String query) {
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId("001").build();
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("question", query);
        Optional<OverAllState> invoke = this.compiledGraph.invoke(objectMap, runnableConfig);
        return invoke.map(OverAllState::data).orElse(new HashMap<>());
    }
}