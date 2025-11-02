package pt.psoft.g1.psoftg1.genremanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Entity
@Table
public class Genre {
    @Transient
    private final int GENRE_MAX_LENGTH = 100;

    @Id
    @Column(name = "pk", length = 36, nullable = false, updatable = false,
        columnDefinition = "varchar(36)")
    @Getter
    private String pk;

    @Size(min = 1, max = GENRE_MAX_LENGTH, message = "Genre name must be between 1 and 100 characters")
    @Column(unique=true, nullable=false, length = GENRE_MAX_LENGTH)
    @Getter
    String genre;

    protected Genre(){}

    public Genre(String genre) {
        setGenre(genre);
    }

    /** Assign once on create (called by the service). */
    public void assignPk(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id cannot be blank");
        this.pk = id;
    }

    private void setGenre(String genre) {
        if(genre == null)
            throw new IllegalArgumentException("Genre cannot be null");
        if(genre.isBlank())
            throw new IllegalArgumentException("Genre cannot be blank");
        if(genre.length() > GENRE_MAX_LENGTH)
            throw new IllegalArgumentException("Genre has a maximum of 100 characters");
        this.genre = genre;
    }

    public String toString() {
        return genre;
    }
}
