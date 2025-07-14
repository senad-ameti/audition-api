package com.audition.model;


public record PostComment(
    int postId,
    int id,
    String name,
    String email,
    String body
) {

}
