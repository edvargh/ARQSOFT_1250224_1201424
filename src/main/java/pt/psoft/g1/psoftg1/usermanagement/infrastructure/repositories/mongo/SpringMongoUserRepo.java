package pt.psoft.g1.psoftg1.usermanagement.infrastructure.repositories.mongo;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mongo")
public interface SpringMongoUserRepo extends MongoRepository<UserDoc, String> {
  Optional<UserDoc> findByUserId(Long userId);
  Optional<UserDoc> findByUsername(String username);

  List<UserDoc> findByFullName(String fullName);

  List<UserDoc> findByFullNameRegex(String fullNameRegex);
}
