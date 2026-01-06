package com.audition.integration;

import com.audition.common.exception.SystemException;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AuditionIntegrationClient {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/posts";

    @Autowired
    private transient RestTemplate restTemplate;

    public List<AuditionPost> getPosts() {
        // make RestTemplate call to get Posts from https://jsonplaceholder.typicode.com/posts
        try {
            final AuditionPost[] posts = restTemplate.getForObject(BASE_URL, AuditionPost[].class);
            return posts == null ? List.of() : List.of(posts);
        } catch (final HttpClientErrorException e) {
            throw new SystemException(String.format("Error occurred while fetching posts: %s", e.getMessage()),
                "Integration Error", e.getStatusCode().value());
        }
    }

    public AuditionPost getPostById(final int id) {
        // get post by post ID call from https://jsonplaceholder.typicode.com/posts/
        try {
            final URI url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/{id}").buildAndExpand(id).encode().toUri();
            final AuditionPost post = restTemplate.getForObject(url, AuditionPost.class);
            return post == null ? new AuditionPost() : post;
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new SystemException("Cannot find a Post with id " + id, "Resource Not Found",
                    404);
            } else {
                // Find a better way to handle the exception so that the original error message is not lost.
                // Feel free to change this function.
                throw new SystemException(
                    String.format("Error occurred while fetching post with id %d: %s", id, e.getMessage()),
                    "Integration Error", e.getStatusCode().value());
            }
        }
    }

    // Write a method GET comments for a post from https://jsonplaceholder.typicode.com/posts/{postId}/comments - the comments must be returned as part of the post.
    public AuditionPost getPostWithCommentsByPostId(final int id) {
        final AuditionPost post = getPostById(id);
        final List<AuditionComment> comments = getCommentsByPostId(id);
        post.setComments(comments);

        return post;
    }

    // write a method. GET comments for a particular Post from https://jsonplaceholder.typicode.com/comments?postId={postId}.
    // The comments are a separate list that needs to be returned to the API consumers. Hint: this is not part of the AuditionPost pojo.
    public List<AuditionComment> getCommentsByPostId(final int id) {
        try {
            final URI url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/{id}/comments")
                .buildAndExpand(id)
                .encode()
                .toUri();
            final AuditionComment[] comments = restTemplate.getForObject(url, AuditionComment[].class);
            return comments == null ? List.of() : List.of(comments);
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new SystemException("Cannot find a Post with id " + id, "Resource Not Found",
                    404);
            } else {
                throw new SystemException(
                    String.format("Error occurred while fetching comments for post with id %d: %s", id, e.getMessage()),
                    "Integration Error", e.getStatusCode().value());
            }
        }
    }
}
