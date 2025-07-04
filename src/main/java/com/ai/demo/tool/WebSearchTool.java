package com.ai.demo.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class WebSearchTool {

    private final WebClient webClient;

    public WebSearchTool(WebClient.Builder webClientBuilder,
                         @Value("${tavily.base-url}") String baseUrl,
                         @Value("${tavily.api-key}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    /**
     * 使用 Tavily API 进行网络搜索
     *
     * @param query 搜索请求对象，包含查询参数和其他可选参数。
     * @return TavilyResponse 包含搜索结果和相关信息。
     */
    @Tool(description = "使用 Tavily API 进行网络搜索")
    public TavilyResponse search(@ToolParam(description = "search query to look up") String query) {

        TavilyRequest request = TavilyRequest.builder()
                .query(query)
                .maxResults(3) // 默认返回 3 条结果
                .searchDepth("basic") // 默认搜索深度为 basic
                .includeAnswer(false) // 默认不包含答案
                .includeRawContent(false) // 默认不包含原始内容
                .includeImages(false) // 默认不包含图片
                .build();

        if (request.getQuery() == null || request.getQuery().isEmpty()) {
            throw new IllegalArgumentException("Query parameter is required.");
        }
        log.info("Received TavilyRequest: {}", request);

        // Build the request payload with all parameters, setting defaults where necessary
        TavilyRequest requestWithApiKey = TavilyRequest.builder().query(request.getQuery())
                .searchDepth(request.getSearchDepth() != null ? request.getSearchDepth() : "basic")
                .topic(request.getTopic() != null ? request.getTopic() : "general")
                .days(request.getDays() != null ? request.getDays() : 300)
                .maxResults(request.getMaxResults() != 0 ? request.getMaxResults() : 10)
                .includeImages(request.isIncludeImages()).includeImageDescriptions(request.isIncludeImageDescriptions())
                .includeAnswer(request.isIncludeAnswer()).includeRawContent(request.isIncludeRawContent())
                .includeDomains(
                        request.getIncludeDomains() != null ? request.getIncludeDomains() : Collections.emptyList())
                .excludeDomains(
                        request.getExcludeDomains() != null ? request.getExcludeDomains() : Collections.emptyList())
                .build();

        log.debug("Sending request to Tavily API: query={}, searchDepth={}, topic={}, days={}, maxResults={}",
                requestWithApiKey.getQuery(), requestWithApiKey.getSearchDepth(), requestWithApiKey.getTopic(),
                requestWithApiKey.getDays(), requestWithApiKey.getMaxResults());

        try {
            TavilyResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/search").build())
                    .bodyValue(requestWithApiKey)
                    .retrieve()
                    .bodyToMono(TavilyResponse.class)
                    .block();

            log.info("Received response from Tavily API for query: {}", requestWithApiKey.getQuery());
            return response;
        } catch (Exception e) {
            log.error("Error occurred while calling Tavily API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch search results from Tavily API", e);
        }
    }

    /**
     * Request object for the Tavily API.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonClassDescription("Request object for the Tavily API")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TavilyRequest {

        @JsonProperty("query")
        @JsonPropertyDescription("The main search query.")
        private String query;

        @JsonProperty("api_key")
        @JsonPropertyDescription("API key for authentication with Tavily.")
        private String apiKey;

        @JsonProperty("search_depth")
        @JsonPropertyDescription("The depth of the search. Accepted values: 'basic', 'advanced'. Default is 'basic'.")
        private String searchDepth;

        @JsonProperty("topic")
        @JsonPropertyDescription("The category of the search. Accepted values: 'general', 'news'. Default is 'general'.")
        private String topic;

        @JsonProperty("days")
        @JsonPropertyDescription("The number of days back from the current date to include in search results. Default is 3. Only applies to 'news' topic.")
        private Integer days;

        @JsonProperty("time_range")
        @JsonPropertyDescription("The time range for search results. Accepted values: 'day', 'week', 'month', 'year' or 'd', 'w', 'm', 'y'. Default is none.")
        private String timeRange;

        @JsonProperty("max_results")
        @JsonPropertyDescription("The maximum number of search results to return. Default is 5.")
        private int maxResults;

        @JsonProperty("include_images")
        @JsonPropertyDescription("Whether to include a list of query-related images in the response. Default is false.")
        private boolean includeImages;

        @JsonProperty("include_image_descriptions")
        @JsonPropertyDescription("When 'include_images' is true, adds descriptive text for each image. Default is false.")
        private boolean includeImageDescriptions;

        @JsonProperty("include_answer")
        @JsonPropertyDescription("Whether to include a short answer to the query, generated from search results. Default is false.")
        private boolean includeAnswer;

        @JsonProperty("include_raw_content")
        @JsonPropertyDescription("Whether to include the cleaned and parsed HTML content of each search result. Default is false.")
        private boolean includeRawContent;

        @JsonProperty("include_domains")
        @JsonPropertyDescription("A list of domains to specifically include in search results. Default is an empty list.")
        private List<String> includeDomains;

        @JsonProperty("exclude_domains")
        @JsonPropertyDescription("A list of domains to specifically exclude from search results. Default is an empty list.")
        private List<String> excludeDomains;
    }

    /**
     * Response object for the Tavily API.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonClassDescription("Response object for the Tavily API")
    public static class TavilyResponse {
        @JsonProperty("query")
        private String query;

        @JsonProperty("follow_up_questions")
        private List<String> followUpQuestions;

        @JsonProperty("answer")
        private String answer;

        @JsonDeserialize(using = ImageDeserializer.class)
        @JsonProperty("images")
        private List<Image> images;

        @JsonProperty("results")
        private List<Result> results;

        @JsonProperty("response_time")
        private float responseTime;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Image {
            @JsonProperty("url")
            private String url;

            @JsonProperty("description")
            private String description;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Result {
            @JsonProperty("title")
            private String title;

            @JsonProperty("url")
            private String url;

            @JsonProperty("content")
            private String content;

            @JsonProperty("raw_content")
            private String rawContent;

            @JsonProperty("score")
            private float score;

            @JsonProperty("published_date")
            private String publishedDate;
        }
    }

    public static class ImageDeserializer extends JsonDeserializer<List<TavilyResponse.Image>> {
        @Override
        public List<TavilyResponse.Image> deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {

            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            List<TavilyResponse.Image> images = new ArrayList<>();

            if (node.isArray()) {
                for (JsonNode element : node) {
                    // If element is a string, treat it as a URL
                    if (element.isTextual()) {
                        images.add(new TavilyResponse.Image(element.asText(), null));
                    }
                    // If element is an object, map it to Image
                    else if (element.isObject()) {
                        String url = element.has("url") ? element.get("url").asText() : null;
                        String description = element.has("description") ? element.get("description").asText() : null;
                        images.add(new TavilyResponse.Image(url, description));
                    }
                }
            }

            return images;
        }
    }
}
