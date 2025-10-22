package pt.psoft.g1.psoftg1.readermanagement.infraestructure.repositories.mongo;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mongo")
public interface SpringMongoReaderRepo extends MongoRepository<ReaderDoc, String> {
  Optional<ReaderDoc> findByReaderNumber(String readerNumber);
  List<ReaderDoc> findByPhoneNumber(String phoneNumber);
  Optional<ReaderDoc> findByUsername(String username);
  Optional<ReaderDoc> findByUserId(Long userId);
}
