package com.audition.dto;

import java.util.List;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "I prefer to suppress these FindBugs warnings"
)
public record AuditionPostDTO(
    int userId,
    int id,
    String title,
    String body,
    List<AuditionPostCommentDTO> comments
) {

}
