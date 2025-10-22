package pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.mongo;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("mongo")
public interface SpringMongoGenreRepo extends MongoRepository<GenreDoc, String> {
  Optional<GenreDoc> findByGenre(String genre);
}
