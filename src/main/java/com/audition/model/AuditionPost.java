package com.audition.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "All lists are defensively copied via List.copyOf() in the canonical constructor"
)
public record AuditionPost(int userId, int id, String title, String body, List<AuditionComment> comments) {

    public AuditionPost {
        comments = comments != null ? List.copyOf(comments) : List.of();
    }

    public AuditionPost(int userId, int id, String title, String body) {
        this(userId, id, title, body, List.of());
    }

    public AuditionPost withComments(List<AuditionComment> comments) {
        return new AuditionPost(this.userId, this.id, this.title, this.body, comments);
    }
}