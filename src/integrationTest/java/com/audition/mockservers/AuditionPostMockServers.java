package com.audition.mockservers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.audition.utils.FileUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.TooManyMethods")
public class AuditionPostMockServers {

    private static WireMockServer wireMockServer;
    private static final JsonMapper OBJECT_MAPPER = new JsonMapper();

    public static void start() {
        synchronized (AuditionPostMockServers.class) {
            if (wireMockServer != null && wireMockServer.isRunning()) {
                return;
            }
            wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
            wireMockServer.start();
            setupStubs();
        }
    }

    @PreDestroy
    public final void stopServer() {
        if (wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    public static int getPort() {
        return wireMockServer.port();
    }

    private static void setupStubs() {
        try {
            final String allPostsJson = FileUtils.loadResource("downstream/audition-post/all-posts.json");
            final String allCommentsJson = FileUtils.loadResource("downstream/audition-post/all-comments.json");

            final List<Map<String, Object>> allPosts = OBJECT_MAPPER.readValue(allPostsJson, new TypeReference<>() {});
            final List<Map<String, Object>> allComments = OBJECT_MAPPER.readValue(allCommentsJson, new TypeReference<>() {});

            stubAllPosts(allPostsJson);
            stubPostsByUserId(allPosts);
            stubPostById(allPosts);
            stubNotFoundPost();
            stubForbiddenPost();
            stubServerErrorPost();
            stubCommentsForPost(allComments);
            stubServerErrorComments();
            stubAllComments(allCommentsJson);
            stubForbiddenComments();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to set up WireMock stubs", e);
        }
    }

    private static void stubAllPosts(final String allPostsJson) {
        wireMockServer.stubFor(get(urlEqualTo("/posts"))
            .willReturn(okJson(allPostsJson)));
    }

    private static void stubPostsByUserId(final List<Map<String, Object>> allPosts) throws IOException {
        final List<Map<String, Object>> userId1Posts = allPosts.stream()
            .filter(p -> Integer.valueOf(1).equals(p.get("userId")))
            .collect(Collectors.toList());

        wireMockServer.stubFor(get(urlPathEqualTo("/posts"))
            .withQueryParam("userId", equalTo("1"))
            .willReturn(okJson(OBJECT_MAPPER.writeValueAsString(userId1Posts))));
    }

    private static void stubPostById(final List<Map<String, Object>> allPosts) throws IOException {
        wireMockServer.stubFor(get(urlEqualTo("/posts/1"))
            .willReturn(okJson(OBJECT_MAPPER.writeValueAsString(
                allPosts.stream()
                    .filter(p -> Integer.valueOf(1).equals(p.get("id")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Post with id=1 not found in all-posts.json"))))));
    }

    private static void stubNotFoundPost() {
        wireMockServer.stubFor(get(urlEqualTo("/posts/99999"))
            .willReturn(notFound().withBody("Not Found")));
    }

    private static void stubForbiddenPost() {
        wireMockServer.stubFor(get(urlEqualTo("/posts/403"))
            .willReturn(aResponse().withStatus(403).withBody("Forbidden: insufficient permissions")));
    }

    private static void stubServerErrorPost() {
        wireMockServer.stubFor(get(urlEqualTo("/posts/500"))
            .willReturn(serverError().withBody("Internal Server Error: database unavailable")));
    }

    private static void stubCommentsForPost(final List<Map<String, Object>> allComments) throws IOException {
        final List<Map<String, Object>> post1Comments = allComments.stream()
            .filter(c -> Integer.valueOf(1).equals(c.get("postId")))
            .collect(Collectors.toList());
        final String post1CommentsJson = OBJECT_MAPPER.writeValueAsString(post1Comments);

        wireMockServer.stubFor(get(urlEqualTo("/posts/1/comments"))
            .willReturn(okJson(post1CommentsJson)));

        wireMockServer.stubFor(get(urlEqualTo("/posts/99999/comments"))
            .willReturn(notFound().withBody("Not Found")));

        wireMockServer.stubFor(get(urlPathEqualTo("/comments"))
            .withQueryParam("postId", equalTo("1"))
            .willReturn(okJson(post1CommentsJson)));
    }

    private static void stubServerErrorComments() {
        wireMockServer.stubFor(get(urlEqualTo("/posts/500/comments"))
            .willReturn(serverError().withBody("Internal Server Error: comments service down")));
    }

    private static void stubAllComments(final String allCommentsJson) {
        wireMockServer.stubFor(get(urlEqualTo("/comments"))
            .willReturn(okJson(allCommentsJson)));
    }

    private static void stubForbiddenComments() {
        wireMockServer.stubFor(get(urlPathEqualTo("/comments"))
            .withQueryParam("postId", equalTo("403"))
            .willReturn(aResponse().withStatus(403).withBody("Forbidden: insufficient permissions")));
    }
}
