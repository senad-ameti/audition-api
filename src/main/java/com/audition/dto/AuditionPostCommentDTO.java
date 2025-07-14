package com.audition.dto;


public record AuditionPostCommentDTO(
    int postId,
    int id,
    String name,
    String email,
    String body
) {

}