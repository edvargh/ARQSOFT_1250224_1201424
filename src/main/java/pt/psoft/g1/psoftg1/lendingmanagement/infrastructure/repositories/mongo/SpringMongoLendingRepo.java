package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SpringMongoLendingRepo extends MongoRepository<LendingDoc, String> {
  Optional<LendingDoc> findByLendingNumber(String lendingNumber);
  List<LendingDoc> findByReaderNumberAndBookIsbn(String readerNumber, String isbn);
  List<LendingDoc> findByReaderNumberAndReturnedDateIsNull(String readerNumber);
}
