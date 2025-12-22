package com.audition.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a comment on a post.
 */
@Getter
@Setter
public class Comment {

    private int postId;
    private int id;
    private String name;
    private String email;
    private String body;

}
