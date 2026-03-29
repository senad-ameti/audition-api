package com.audition.mockservers;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.audition.utils.FileUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuditionPostMockServers {

    private static WireMockServer wireMockServer;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static synchronized void start() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            return;
        }
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        setupStubs();
    }

    @PreDestroy
    public final void tearDown() {
        if (wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    public static int getPort() {
        return wireMockServer.port();
    }

    private static void setupStubs() {
        try {
            String allPostsJson = FileUtils.loadResource("downstream/audition-post/all-posts.json");
            String allCommentsJson = FileUtils.loadResource("downstream/audition-post/all-comments.json");

            List<Map<String, Object>> allPosts = objectMapper.readValue(allPostsJson, new TypeReference<>() {});
            List<Map<String, Object>> allComments = objectMapper.readValue(allCommentsJson, new TypeReference<>() {});

            stubAllPosts(allPostsJson);
            stubPostsByUserId(allPosts);
            stubPostById(allPosts);
            stubNotFoundPost();
            stubCommentsForPost(allComments);
            stubAllComments(allCommentsJson);

        } catch (IOException e) {
            throw new RuntimeException("Failed to set up WireMock stubs", e);
        }
    }

    private static void stubAllPosts(String allPostsJson) {
        wireMockServer.stubFor(get(urlEqualTo("/posts"))
            .willReturn(okJson(allPostsJson)));
    }

    private static void stubPostsByUserId(List<Map<String, Object>> allPosts) throws IOException {
        List<Map<String, Object>> userId1Posts = allPosts.stream()
            .filter(p -> Integer.valueOf(1).equals(p.get("userId")))
            .collect(Collectors.toList());

        wireMockServer.stubFor(get(urlPathEqualTo("/posts"))
            .withQueryParam("userId", equalTo("1"))
            .willReturn(okJson(objectMapper.writeValueAsString(userId1Posts))));
    }

    private static void stubPostById(List<Map<String, Object>> allPosts) throws IOException {
        Map<String, Object> post1 = allPosts.stream()
            .filter(p -> Integer.valueOf(1).equals(p.get("id")))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Post with id=1 not found in all-posts.json"));

        wireMockServer.stubFor(get(urlEqualTo("/posts/1"))
            .willReturn(okJson(objectMapper.writeValueAsString(post1))));
    }

    private static void stubNotFoundPost() {
        wireMockServer.stubFor(get(urlEqualTo("/posts/99999"))
            .willReturn(notFound().withBody("Not Found")));
    }

    private static void stubCommentsForPost(List<Map<String, Object>> allComments) throws IOException {
        List<Map<String, Object>> post1Comments = allComments.stream()
            .filter(c -> Integer.valueOf(1).equals(c.get("postId")))
            .collect(Collectors.toList());
        String post1CommentsJson = objectMapper.writeValueAsString(post1Comments);

        wireMockServer.stubFor(get(urlEqualTo("/posts/1/comments"))
            .willReturn(okJson(post1CommentsJson)));

        wireMockServer.stubFor(get(urlEqualTo("/posts/99999/comments"))
            .willReturn(notFound().withBody("Not Found")));

        wireMockServer.stubFor(get(urlPathEqualTo("/comments"))
            .withQueryParam("postId", equalTo("1"))
            .willReturn(okJson(post1CommentsJson)));
    }

    private static void stubAllComments(String allCommentsJson) {
        wireMockServer.stubFor(get(urlEqualTo("/comments"))
            .willReturn(okJson(allCommentsJson)));
    }
}
