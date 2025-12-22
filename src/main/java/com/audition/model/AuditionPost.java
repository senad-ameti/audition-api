package com.audition.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a post in the audition system.
 */
@Getter
@Setter
public class AuditionPost {

    private int userId;
    private int id;
    private String title;
    private String body;
    private List<Comment> comments;

}
