package com.audition.model;

import jakarta.validation.constraints.Size;

public record AuditionPostFilter(
    String userId,
    @Size(min = 3, message = "Title must be at least 3 characters")
    String title
) {}
