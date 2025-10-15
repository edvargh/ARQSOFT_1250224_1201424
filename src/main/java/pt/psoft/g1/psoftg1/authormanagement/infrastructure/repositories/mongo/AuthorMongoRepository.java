package pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import pt.psoft.g1.psoftg1.authormanagement.api.AuthorLendingView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@Profile("mongo")
public class AuthorMongoRepository implements AuthorRepository {

  private final SpringMongoAuthorRepo repo;
  private final MongoTemplate mongoTemplate;
  private final AuthorMongoMapper mapper = new AuthorMongoMapper();

  public AuthorMongoRepository(SpringMongoAuthorRepo repo, MongoTemplate mongoTemplate) {
    this.repo = repo;
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Optional<Author> findByAuthorNumber(Long authorNumber) {
    return repo.findByAuthorNumber(authorNumber).map(mapper::toDomain);
  }

  @Override
  public List<Author> findByName_NameStartsWithIgnoreCase(String name) {
    return repo.findByNameStartsWithIgnoreCase(name).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Author> findByName_NameIgnoreCase(String name) {
    return repo.findByNameIgnoreCase(name).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Author save(Author author) {
    var doc = mapper.toDoc(author);
    if (doc.getAuthorNumber() == null) {
      doc.setAuthorNumber(System.currentTimeMillis());
    }
    var saved = repo.save(doc);
    return mapper.toDomain(saved);
  }

  @Override
  public Iterable<Author> findAll() {
    return repo.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public Page<AuthorLendingView> findTopAuthorByLendings(Pageable pageable) {
    MatchOperation matchReturned = match(new Criteria());
    LookupOperation lookupBooks = LookupOperation.newLookup()
        .from("books")
        .localField("bookIsbn")
        .foreignField("isbn")
        .as("book");
    UnwindOperation unwindBook = unwind("book");
    UnwindOperation unwindAuthors = unwind("book.authors");
    LookupOperation lookupAuthors = LookupOperation.newLookup()
        .from("authors")
        .localField("book.authors")
        .foreignField("authorNumber")
        .as("author");
    UnwindOperation unwindAuthor = unwind("author");

    GroupOperation groupByAuthor = group("author.authorNumber")
        .first("author.name").as("name")
        .count().as("lendingCount");

    SortOperation sortByCountDesc = sort(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lendingCount"));
    SkipOperation skip = skip((long) pageable.getPageNumber() * pageable.getPageSize());
    LimitOperation limit = limit(pageable.getPageSize());

    Aggregation agg = newAggregation(
        matchReturned,
        lookupBooks,
        unwindBook,
        unwindAuthors,
        lookupAuthors,
        unwindAuthor,
        groupByAuthor,
        sortByCountDesc,
        skip,
        limit
    );

    AggregationResults<AuthorLendingAgg> results =
        mongoTemplate.aggregate(agg, "lendings", AuthorLendingAgg.class);

    var content = results.getMappedResults().stream()
        .map(r -> new AuthorLendingView(r.name(), r.lendingCount()))
        .toList();

    return new org.springframework.data.domain.PageImpl<>(content, pageable, content.size());
  }

  @Override
  public void delete(Author author) {
    if (author == null || author.getId() == null) return;
    repo.findByAuthorNumber(author.getId()).ifPresent(a -> repo.deleteById(a.getId()));
  }

  @Override
  public List<Author> findCoAuthorsByAuthorNumber(Long authorNumber) {
    MatchOperation matchBooks = match(Criteria.where("authors").is(authorNumber));
    UnwindOperation unwindAuthors = unwind("authors");
    MatchOperation matchCoauthors = match(Criteria.where("authors").ne(authorNumber));
    GroupOperation groupDistinct = group("authors"); // distinct authorNumbers
    LookupOperation lookupAuthors = LookupOperation.newLookup()
        .from("authors")
        .localField("_id")
        .foreignField("authorNumber")
        .as("author");
    UnwindOperation unwindAuthor = unwind("author");

    Aggregation agg = newAggregation(
        matchBooks,
        unwindAuthors,
        matchCoauthors,
        groupDistinct,
        lookupAuthors,
        unwindAuthor
    );

    AggregationResults<CoAuthorAgg> results =
        mongoTemplate.aggregate(agg, "books", CoAuthorAgg.class);

    return results.getMappedResults().stream()
        .map(CoAuthorAgg::author)
        .map(mapper::toDomain)
        .toList();
  }

  /** aggregation projections */
  private record AuthorLendingAgg(String name, long lendingCount) {}
  private record CoAuthorAgg(Long _id, AuthorDoc author) {}
}
