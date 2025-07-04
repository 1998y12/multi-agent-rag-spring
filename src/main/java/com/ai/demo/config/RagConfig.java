package com.ai.demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    /**
     * 文本分割器，用于将文档切分成小片段
     * @return TextSplitter 实例
     */
    @Bean
    TextSplitter textSplitter() {
        return TokenTextSplitter.builder()
                // 可进一步设置分割的最大 token 数量、切分窗口大小等参数
                .build();
    }

    /**
     * 向量存储库，用于存储文档片段的向量表示
     * @return VectorStore 实例
     */
    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel)
                .build();
    }

    /**
     * 文档检索器，用于从向量存储中检索相关文档片段
     * @param vectorStore 向量存储库
     * @return DocumentRetriever 实例
     */
    @Bean
    DocumentRetriever documentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50)
                .build();
    }

    /**
     * 压缩查询转换器，将对话历史和后续查询压缩为捕获对话本质的独立查询
     * <p> <em>检索前增强</em> 适用于对话历史较长且后续查询与对话上下文相关时</p>
     * @param chatClient 聊天模型
     * @return CompressionQueryTransformer 实例
     */
    @Bean
    CompressionQueryTransformer compressionQueryTransformer(ChatClient chatClient) {
        return CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClient.mutate())
                .build();
    }

    /**
     * 查询重写转换器，重写用户查询
     * <p> <em>检索前增强</em> 适用于用户查询冗长、含歧义或包含可能影响搜索结果质量的无关信息时 </p>
     * @param chatClient 聊天模型
     * @return RewriteQueryTransformer 实例
     */
    @Bean
    RewriteQueryTransformer rewriteQueryTransformer(ChatClient chatClient) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClient.mutate())
                .build();
    }

    /**
     * 翻译查询转换器，将用户查询翻译为目标语言
     * <p> <em>检索前增强</em> 适用于用户查询使用非目标语言时</p>
     * @param chatClient 聊天模型
     * @return TranslationQueryTransformer 实例
     */
    @Bean
    TranslationQueryTransformer translationQueryTransformer(ChatClient chatClient) {
        return TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClient.mutate())
                .targetLanguage("chinese")
                .build();
    }

    /**
     * 多查询扩展器，生成多个查询以提高检索覆盖率
     * <p> <em>检索前增强</em> 利用大模型从不同视角生成多语义查询语句</p>
     * @param chatClient 聊天模型
     * @return MultiQueryExpander 实例
     */
    @Bean
    MultiQueryExpander multiQueryExpander(ChatClient chatClient) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClient.mutate())
                .numberOfQueries(3)
                .build();
    }
}
