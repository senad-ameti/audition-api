package com.audition.service;

import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostFilter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditionService {

    private final AuditionIntegrationClient auditionIntegrationClient;

    public List<AuditionPost> getPosts(final AuditionPostFilter filter) {
        List<AuditionPost> posts = auditionIntegrationClient.getPosts(filter.userId());

        if (StringUtils.isNotBlank(filter.title())) {
            posts = posts.stream()
                .filter(p -> Objects.nonNull(p.title()) && p.title().toLowerCase(Locale.ENGLISH)
                    .contains(filter.title().toLowerCase(Locale.ENGLISH)))
                .toList();
        }

        return posts;
    }

    public AuditionPost getPostById(final String postId) {
        return auditionIntegrationClient.getPostById(postId);
    }

    public AuditionPost getPostWithComments(final String postId) {
        return auditionIntegrationClient.getPostWithComments(postId);
    }

    public List<AuditionComment> getComments(final String postId) {
        return auditionIntegrationClient.getComments(postId);
    }
}
