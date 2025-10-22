package pt.psoft.g1.psoftg1.readermanagement.infraestructure.repositories.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderBookCountDTO;
import pt.psoft.g1.psoftg1.readermanagement.services.SearchReadersQuery;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@Profile("mongo")
@RequiredArgsConstructor
public class ReaderMongoRepository implements ReaderRepository {

  private final SpringMongoReaderRepo repo;
  private final MongoTemplate mongo;
  private final ReaderMongoMapper mapper;
  private final GenreRepository genreRepo;

  /** Optional: only used to hydrate Reader entity if present (mixed stores). */
  private final Optional<pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository> userRepoOpt = Optional.empty();

  /* ------------ helpers ------------ */

  private List<Genre> toGenres(List<String> names) {
    if (names == null) return null;
    List<Genre> out = new ArrayList<>();
    for (String g : names) genreRepo.findByString(g).ifPresent(out::add);
    return out;
  }

  private ReaderDetails hydrate(ReaderDoc d) {
    var readerEntity = userRepoOpt
        .flatMap(u -> u.findById(d.getUserId()))
        .filter(u -> u instanceof pt.psoft.g1.psoftg1.usermanagement.model.Reader)
        .map(u -> (pt.psoft.g1.psoftg1.usermanagement.model.Reader) u)
        .orElse(null);

    return mapper.toDomainSkeleton(d, readerEntity, toGenres(d.getInterestGenres()));
  }

  private List<ReaderDetails> mapAll(List<ReaderDoc> docs) {
    return docs.stream().map(this::hydrate).toList();
  }

  private String ciStartsWith(String s) {
    return "^" + Pattern.quote(s);
  }

  private Pageable toPageable(pt.psoft.g1.psoftg1.shared.services.Page page) {
    int p = Math.max(0, page.getNumber() - 1);
    int size = Math.max(1, page.getLimit());
    return PageRequest.of(p, size);
  }

  /* ------------ contract ------------ */

  @Override
  public Optional<ReaderDetails> findByReaderNumber(String readerNumber) {
    return repo.findByReaderNumber(readerNumber).map(this::hydrate);
  }

  @Override
  public List<ReaderDetails> findByPhoneNumber(String phoneNumber) {
    return mapAll(repo.findByPhoneNumber(phoneNumber));
  }

  @Override
  public Optional<ReaderDetails> findByUsername(String username) {
    return repo.findByUsername(username).map(this::hydrate);
  }

  @Override
  public Optional<ReaderDetails> findByUserId(Long userId) {
    return repo.findByUserId(userId).map(this::hydrate);
  }

  @Override
  public int getCountFromCurrentYear() {
    int year = LocalDate.now().getYear();
    Query q = new Query(Criteria.where("readerNumber").regex("^" + year + "/"));
    return (int) mongo.count(q, ReaderDoc.class);
  }

  @Override
  public ReaderDetails save(ReaderDetails readerDetails) {
    ReaderDoc doc = repo.findByReaderNumber(readerDetails.getReaderNumber())
        .orElseGet(ReaderDoc::new);

    ReaderDoc fresh = mapper.toDoc(readerDetails);
    if (doc.getId() != null) fresh.setId(doc.getId());

    ReaderDoc saved = repo.save(fresh);
    return hydrate(saved);
  }

  @Override
  public Iterable<ReaderDetails> findAll() {
    return mapAll(repo.findAll());
  }

  @Override
  public org.springframework.data.domain.Page<ReaderDetails> findTopReaders(Pageable pageable) {
    Aggregation agg = newAggregation(
        group("readerNumber").count().as("cnt"),
        sort(Sort.Direction.DESC, "cnt"),
        limit(pageable.getPageSize()),
        skip((long) pageable.getPageNumber() * pageable.getPageSize())
    );
    record GroupOut(String id, long cnt) {}
    var res = mongo.aggregate(agg, "lendings", GroupOut.class).getMappedResults();

    List<ReaderDetails> readers = res.stream()
        .map(g -> repo.findByReaderNumber(g.id()).orElse(null))
        .filter(Objects::nonNull)
        .map(this::hydrate)
        .toList();

    return new PageImpl<>(readers, pageable, readers.size());
  }

  @Override
  public org.springframework.data.domain.Page<ReaderBookCountDTO> findTopByGenre(
      Pageable pageable, String genre, LocalDate startDate, LocalDate endDate) {

    Aggregation agg = newAggregation(
        match(Criteria.where("startDate").gte(startDate).lte(endDate)),
        lookup("books", "bookIsbn", "isbn", "book"),
        unwind("book"),
        match(Criteria.where("book.genre").is(genre)),
        group("readerNumber").count().as("cnt"),
        sort(Sort.Direction.DESC, "cnt"),
        skip((long) pageable.getPageNumber() * pageable.getPageSize()),
        limit(pageable.getPageSize())
    );
    record CountByReader(String id, long cnt) {}
    var res = mongo.aggregate(agg, "lendings", CountByReader.class).getMappedResults();

    List<ReaderBookCountDTO> list = new ArrayList<>();
    for (var row : res) {
      var rd = repo.findByReaderNumber(row.id()).map(this::hydrate);
      rd.ifPresent(r -> list.add(new ReaderBookCountDTO(r, row.cnt())));
    }
    return new PageImpl<>(list, pageable, list.size());
  }

  @Override
  public void delete(ReaderDetails readerDetails) {
    if (readerDetails == null) return;
    repo.findByReaderNumber(readerDetails.getReaderNumber()).ifPresent(d -> repo.deleteById(d.getId()));
  }

  @Override
  public List<ReaderDetails> searchReaderDetails(
      pt.psoft.g1.psoftg1.shared.services.Page page,
      SearchReadersQuery query) {

    Query q = new Query();
    if (query != null) {
      if (query.getName() != null && !query.getName().isBlank()) {
        q.addCriteria(Criteria.where("fullName").regex(ciStartsWith(query.getName()), "i"));
      }
      if (query.getEmail() != null && !query.getEmail().isBlank()) {
        q.addCriteria(Criteria.where("username").is(query.getEmail()));
      }
      if (query.getPhoneNumber() != null && !query.getPhoneNumber().isBlank()) {
        q.addCriteria(Criteria.where("phoneNumber").is(query.getPhoneNumber()));
      }
    }
    q.with(Sort.by(Sort.Direction.ASC, "fullName"));

    var pageable = toPageable(page);
    q.skip((long) pageable.getPageNumber() * pageable.getPageSize())
        .limit(pageable.getPageSize());

    return mapAll(mongo.find(q, ReaderDoc.class));
  }
}
