package com.audition.model;

public record AuditionPost(
    int userId,
    int id,
    String title,
    String body
) {

}
