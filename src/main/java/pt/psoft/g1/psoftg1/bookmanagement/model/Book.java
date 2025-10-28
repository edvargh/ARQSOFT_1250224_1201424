package pt.psoft.g1.psoftg1.bookmanagement.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.StaleObjectStateException;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.services.UpdateBookRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "Book", uniqueConstraints = {
        @UniqueConstraint(name = "uc_book_isbn", columnNames = {"ISBN"})
})
public class Book extends EntityWithPhoto {
    @Id
    @Column(name = "BOOK_ID", length = 36, nullable = false, updatable = false)
    @Getter
    private String pk;

    @Version
    @Getter
    private Long version;

    @Embedded
    Isbn isbn;

    @Getter
    @Embedded
    @NotNull
    Title title;

    @Getter
    @ManyToOne
    @NotNull
    Genre genre;

    @Getter
    @ManyToMany
    private List<Author> authors = new ArrayList<>();

    @Embedded
    Description description;

    private void setTitle(String title) {this.title = new Title(title);}

    private void setIsbn(String isbn) {
        this.isbn = new Isbn(isbn);
    }

    private void setDescription(String description) {this.description = new Description(description); }

    private void setGenre(Genre genre) {this.genre = genre; }

    private void setAuthors(List<Author> authors) {this.authors = authors; }

    public String getDescription(){ return this.description.toString(); }

    public Book(String isbn, String title, String description, Genre genre, List<Author> authors, String photoURI) {
        setTitle(title);
        setIsbn(isbn);
        if(description != null)
            setDescription(description);
        if(genre==null)
            throw new IllegalArgumentException("Genre cannot be null");
        setGenre(genre);
        if(authors == null)
            throw new IllegalArgumentException("Author list is null");
        if(authors.isEmpty())
            throw new IllegalArgumentException("Author list is empty");

        setAuthors(authors);
        setPhotoInternal(photoURI);
    }

    protected Book() {
        // got ORM only
    }

    public void removePhoto(long desiredVersion) {
        if(desiredVersion != this.version) {
            throw new ConflictException("Provided version does not match latest version of this object");
        }

        setPhotoInternal(null);
    }


    public void applyPatch(final Long desiredVersion, UpdateBookRequest request) {
        long expected = (desiredVersion == null) ? 0L : desiredVersion;
        long current  = (this.version == null) ? 0L : this.version;

        if (current != expected) {
            throw new OptimisticLockException("ETag/version mismatch");
        }

        if (request.getTitle() != null)        setTitle(request.getTitle());
        if (request.getDescription() != null)   setDescription(request.getDescription());
        if (request.getGenreObj() != null)      setGenre(request.getGenreObj());
        if (request.getAuthorObjList() != null) setAuthors(request.getAuthorObjList());
        if (request.getPhotoURI() != null)      setPhotoInternal(request.getPhotoURI());
    }

    public String getIsbn(){
        return this.isbn.toString();
    }

    public void assignPk(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id cannot be blank");
        this.pk = id;
    }
}
