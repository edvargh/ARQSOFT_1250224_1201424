package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.mongo;

import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.List;

@Component
public class BookMongoMapper {

  public BookDoc toDoc(Book b) {
    return BookDoc.builder()
        .id(b.getPk())
        .version(b.getVersion())
        .isbn(b.getIsbn())
        .title(b.getTitle().getTitle())
        .description(b.getDescription())
        .genre(b.getGenre().toString())
        .authorIds(b.getAuthors().stream().map(Author::getId).toList())
        .authorNames(b.getAuthors().stream().map(Author::getName).toList())
        .photoFile(b.getPhoto() == null ? null : b.getPhoto().getPhotoFile())
        .build();
  }

  public BookDoc toDocShallow(String id, String isbn, String title, String description, String genre,
      List<String> authorIds, List<String> authorNames, String photoFile) {
    return BookDoc.builder()
        .id(id)
        .isbn(isbn)
        .title(title)
        .description(description)
        .genre(genre)
        .authorIds(authorIds)
        .authorNames(authorNames)
        .photoFile(photoFile)
        .build();
  }
}
