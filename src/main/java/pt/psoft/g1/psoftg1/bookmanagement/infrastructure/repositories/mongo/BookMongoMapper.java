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
        .id(null)
        .isbn(b.getIsbn())
        .title(b.getTitle().getTitle())
        .description(b.getDescription())
        .genre(b.getGenre().toString())
        .authorNumbers(b.getAuthors().stream().map(a -> a.getId()).toList())
        .authorNames(b.getAuthors().stream().map(Author::getName).toList())
        .photoFile(b.getPhoto() == null ? null : b.getPhoto().getPhotoFile())
        .build();
  }

  public BookDoc toDocShallow(String isbn, String title, String description, String genre,
      List<Long> authorNumbers, List<String> authorNames, String photoFile) {
    return BookDoc.builder()
        .isbn(isbn).title(title).description(description).genre(genre)
        .authorNumbers(authorNumbers).authorNames(authorNames).photoFile(photoFile)
        .build();
  }
}
