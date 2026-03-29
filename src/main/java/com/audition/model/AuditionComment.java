package com.audition.model;

public record AuditionComment(int id, int postId, String name, String email, String body) {}
