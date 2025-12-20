package com.audition.service;

import com.audition.common.logging.AuditionLogger;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionPost;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditionService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditionService.class);

    @Autowired
    private transient AuditionIntegrationClient auditionIntegrationClient;

    @Autowired
    private transient AuditionLogger logger;

    public List<AuditionPost> getPosts(final Integer userId, final Integer id) {
        if (LOG.isDebugEnabled()) {
            logger.debug(LOG, String.format("Filtering posts with userId=%s, id=%s", userId, id));
        }

        final List<AuditionPost> allPosts = auditionIntegrationClient.getPosts();

        return allPosts.stream()
            .filter(post -> userId == null || post.getUserId() == userId)
            .filter(post -> id == null || post.getId() == id)
            .collect(Collectors.toList());
    }

    public AuditionPost getPostById(final String postId) {
        return auditionIntegrationClient.getPostById(postId);
    }

}
