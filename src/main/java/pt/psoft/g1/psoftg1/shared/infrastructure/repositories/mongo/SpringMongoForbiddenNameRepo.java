package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.mongo;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mongo")
public interface SpringMongoForbiddenNameRepo extends MongoRepository<ForbiddenNameDoc, String> {

  Optional<ForbiddenNameDoc> findByForbiddenName(String forbiddenName);

  /** returns number of deleted docs (Spring Data Mongo supports delete count) */
  long deleteByForbiddenName(String forbiddenName);
}
