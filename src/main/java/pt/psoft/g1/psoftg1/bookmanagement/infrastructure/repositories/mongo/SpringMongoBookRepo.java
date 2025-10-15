package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.mongo;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mongo")
public interface SpringMongoBookRepo extends MongoRepository<BookDoc, String> {
  Optional<BookDoc> findByIsbn(String isbn);

  List<BookDoc> findByGenreRegex(String genreRegex);
  List<BookDoc> findByTitleRegex(String titleRegex);

  List<BookDoc> findByAuthorNamesRegex(String authorNameRegex);
}
