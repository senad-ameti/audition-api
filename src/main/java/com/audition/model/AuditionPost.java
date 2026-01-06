package com.audition.model;

import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditionPost {

    private int userId;
    private int id;
    private String title;
    private String body;
    private List<AuditionComment> comments;

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AuditionPost that = (AuditionPost) o;
        return userId == that.userId && id == that.id && Objects.equals(title, that.title)
            && Objects.equals(body, that.body) && Objects.equals(comments, that.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, id, title, body, comments);
    }
}
