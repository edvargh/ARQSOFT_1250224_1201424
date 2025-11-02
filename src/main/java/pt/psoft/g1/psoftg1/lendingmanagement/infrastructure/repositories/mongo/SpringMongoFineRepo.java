package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SpringMongoFineRepo extends MongoRepository<FineDoc, String> {
  Optional<FineDoc> findByLendingId(String lendingId);
}
