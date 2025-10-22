package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Profile("mongo")
@Repository
@RequiredArgsConstructor
public class LendingMongoRepository implements LendingRepository {

  private final SpringMongoLendingRepo repo;
  private final MongoTemplate mongoTemplate;
  private final BookRepository bookRepository;
  private final ReaderRepository readerRepository;

  @Override
  public Optional<Lending> findByLendingNumber(String lendingNumber) {
    return repo.findByLendingNumber(lendingNumber).map(this::toDomain);
  }

  @Override
  public List<Lending> listByReaderNumberAndIsbn(String readerNumber, String isbn) {
    return repo.findByReaderNumberAndBookIsbn(readerNumber, isbn).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public int getCountFromCurrentYear() {
    int year = LocalDate.now().getYear();
    MatchOperation match = match(Criteria.where("lendingNumber").regex("^" + year + "/"));
    GroupOperation count = group().count().as("count");
    Aggregation agg = newAggregation(match, count);
    AggregationResults<CountResult> result = mongoTemplate.aggregate(agg, "lendings", CountResult.class);
    return result.getUniqueMappedResult() != null ? result.getUniqueMappedResult().count() : 0;
  }

  @Override
  public List<Lending> listOutstandingByReaderNumber(String readerNumber) {
    return repo.findByReaderNumberAndReturnedDateIsNull(readerNumber).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Double getAverageDuration() {
    Aggregation agg = newAggregation(
        match(Criteria.where("returnedDate").ne(null)),
        project().andExpression("{$dateDiff: {startDate: '$startDate', endDate: '$returnedDate', unit: 'day'}}").as("duration"),
        group().avg("duration").as("avgDuration")
    );
    AggregationResults<AvgResult> result = mongoTemplate.aggregate(agg, "lendings", AvgResult.class);
    return result.getUniqueMappedResult() != null ? result.getUniqueMappedResult().avgDuration() : 0.0;
  }

  @Override
  public Double getAvgLendingDurationByIsbn(String isbn) {
    Aggregation agg = newAggregation(
        match(Criteria.where("bookIsbn").is(isbn).and("returnedDate").ne(null)),
        project().andExpression("{$dateDiff: {startDate: '$startDate', endDate: '$returnedDate', unit: 'day'}}").as("duration"),
        group().avg("duration").as("avgDuration")
    );
    AggregationResults<AvgResult> result = mongoTemplate.aggregate(agg, "lendings", AvgResult.class);
    return result.getUniqueMappedResult() != null ? result.getUniqueMappedResult().avgDuration() : 0.0;
  }

  @Override
  public List<Lending> getOverdue(Page page) {
    Query q = new Query();
    q.addCriteria(
        Criteria.where("returnedDate").is(null)
            .and("limitDate").lt(LocalDate.now())
    );
    q.with(toPageable(page));
    List<LendingDoc> docs = mongoTemplate.find(q, LendingDoc.class, "lendings");
    return docs.stream().map(this::toDomain).toList();
  }

  @Override
  public List<Lending> searchLendings(Page page,
      String readerNumber,
      String isbn,
      Boolean returned,
      LocalDate startDate,
      LocalDate endDate) {
    Query q = new Query();

    if (readerNumber != null && !readerNumber.isBlank()) {
      q.addCriteria(Criteria.where("readerNumber").is(readerNumber));
    }
    if (isbn != null && !isbn.isBlank()) {
      q.addCriteria(Criteria.where("bookIsbn").is(isbn));
    }
    if (returned != null) {
      if (returned) {
        q.addCriteria(Criteria.where("returnedDate").ne(null));
      } else {
        q.addCriteria(Criteria.where("returnedDate").is(null));
      }
    }
    if (startDate != null) {
      q.addCriteria(Criteria.where("startDate").gte(startDate));
    }
    if (endDate != null) {
      q.addCriteria(Criteria.where("startDate").lte(endDate));
    }

    q.with(toPageable(page));
    List<LendingDoc> docs = mongoTemplate.find(q, LendingDoc.class, "lendings");
    return docs.stream().map(this::toDomain).toList();
  }


  @Override
  public Lending save(Lending lending) {
    repo.save(LendingMongoMapper.toDoc(lending));
    return lending;
  }

  @Override
  public void delete(Lending lending) {
    repo.deleteById(lending.getLendingNumber());
  }

  private Pageable toPageable(Page page) {
    int defaultNumber = 1;
    int defaultLimit  = 10;

    int number1Based = (page == null) ? defaultNumber : page.getNumber();
    int limit        = (page == null) ? defaultLimit  : page.getLimit();

    int p = Math.max(0, number1Based - 1);
    int size = Math.max(1, limit);

    return PageRequest.of(p, size);
  }


  private Lending toDomain(LendingDoc doc) {
    Book book = bookRepository.findByIsbn(doc.getBookIsbn())
        .orElseThrow(() -> new IllegalArgumentException("Book not found for ISBN " + doc.getBookIsbn()));
    ReaderDetails reader = readerRepository.findByReaderNumber(doc.getReaderNumber())
        .orElseThrow(() -> new IllegalArgumentException("Reader not found for " + doc.getReaderNumber()));

    String ln = doc.getLendingNumber();
    int year = Integer.parseInt(ln.substring(0, 4));
    int seq = Integer.parseInt(ln.substring(5));

    int lendingDuration = (int) ChronoUnit.DAYS.between(doc.getStartDate(), doc.getLimitDate());

    return Lending.newBootstrappingLending(
        book,
        reader,
        year,
        seq,
        doc.getStartDate(),
        doc.getReturnedDate(),
        lendingDuration,
        doc.getFineValuePerDayInCents()
    );
  }

  private record CountResult(int count) {}
  private record AvgResult(double avgDuration) {}
}
