package com.audition.model;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditionComment {

    private int postId;
    private int id;
    private String name;
    private String email;
    private String body;

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AuditionComment that = (AuditionComment) o;
        return postId == that.postId && id == that.id && Objects.equals(name, that.name)
            && Objects.equals(email, that.email) && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, id, name, email, body);
    }
}
