package com.audition.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditionComment {
    private Integer postId;
    private Integer id;
    private String name;
    private String email;
    private String body;

}
