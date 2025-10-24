package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.mongo;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mongo")
public interface SpringMongoPhotoRepo extends MongoRepository<PhotoDoc, String> {
  Optional<PhotoDoc> findByPhotoFile(String photoFile);
  void deleteByPhotoFile(String photoFile);
}