package pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringMongoAuthorRepo extends MongoRepository<AuthorDoc, String> {

  List<AuthorDoc> findByNameStartsWithIgnoreCase(String name);

  List<AuthorDoc> findByNameIgnoreCase(String name);

  Optional<AuthorDoc> findOneByNameIgnoreCase(String name); // optional convenience method
}