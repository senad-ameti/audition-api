package com.audition.service;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.client.AuditionIntegrationClient;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditionService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditionService.class);
    private final AuditionIntegrationClient client;
    private final AuditionLogger auditionLogger;

    public List<AuditionPost> getPosts() {
        auditionLogger.info(LOG, "Service: getPosts()");
        return client.getPosts();
    }

    public AuditionPost getPostById(final String id) {
        if (!StringUtils.hasText(id)) {
            auditionLogger.warn(LOG, "Service: getPostById called with invalid id");
            throw new SystemException("post id is required", 400, null);
        }
        auditionLogger.info(LOG, "Service: getPostById id={}", id);
        return client.getPostById(id);
    }

    public List<AuditionComment> getCommentsForPost(final String postId) {
        if (!StringUtils.hasText(postId)) {
            auditionLogger.warn(LOG, "Service: getCommentsForPost called with invalid postId");
            throw new SystemException("post id is required", 400, null);
        }
        auditionLogger.info(LOG, "Service: getCommentsForPost postId={}", postId);
        return client.getCommentsByPostId(postId);
    }

    public List<AuditionComment> getCommentsByQuery(final String postId) {
        if (!StringUtils.hasText(postId)) {
            auditionLogger.warn(LOG, "Service: getCommentsByQuery called with invalid postId");
            throw new SystemException("post id is required", 400, null);
        }
        auditionLogger.info(LOG, "Service: getCommentsByQuery postId={}", postId);
        return client.getCommentsByQuery(postId);
    }
}
