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
  public Optional<Author> findByAuthorNumber(String authorId) {
    return repo.findById(authorId).map(mapper::toDomain);
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
    if (author == null || author.getId() == null || author.getId().isBlank())
      throw new IllegalStateException("Author id must be assigned before saving");
    var saved = repo.save(mapper.toDoc(author));
    return mapper.toDomain(saved);
  }

  @Override
  public Iterable<Author> findAll() {
    return repo.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public Page<AuthorLendingView> findTopAuthorByLendings(Pageable pageable) {
    LookupOperation lookupBooks = lookup("books", "bookIsbn", "isbn", "book");
    UnwindOperation unwindBook = unwind("book");
    UnwindOperation unwindAuthors = unwind("book.authorIds");
    LookupOperation lookupAuthors = lookup("authors", "book.authorIds", "_id", "author");
    UnwindOperation unwindAuthor = unwind("author");

    GroupOperation groupByAuthor = group("author._id")
        .first("author.name").as("name")
        .count().as("lendingCount");

    SortOperation sortByCountDesc = sort(org.springframework.data.domain.Sort.Direction.DESC, "lendingCount");
    SkipOperation skip = skip((long) pageable.getPageNumber() * pageable.getPageSize());
    LimitOperation limit = limit(pageable.getPageSize());

    Aggregation agg = newAggregation(
        lookupBooks, unwindBook, unwindAuthors, lookupAuthors, unwindAuthor,
        groupByAuthor, sortByCountDesc, skip, limit
    );

    var results = mongoTemplate.aggregate(agg, "lendings", AuthorLendingAgg.class);

    var content = results.getMappedResults().stream()
        .map(r -> new AuthorLendingView(r.name(), r.lendingCount()))
        .toList();

    return new org.springframework.data.domain.PageImpl<>(content, pageable, content.size());
  }

  @Override
  public void delete(Author author) {
    if (author == null || author.getId() == null) return;
    repo.deleteById(author.getId());
  }

  @Override
  public List<Author> findCoAuthorsByAuthorNumber(String authorId) {
    MatchOperation matchBooks = match(Criteria.where("authorIds").is(authorId));
    UnwindOperation unwindAuthors = unwind("authorIds");
    MatchOperation matchCoauthors = match(Criteria.where("authorIds").ne(authorId));
    GroupOperation groupDistinct = group("authorIds"); // distinct coauthor ids
    ProjectionOperation projectIds = project().and("_id").as("authorId");
    LookupOperation lookupCoauthors = lookup("authors", "authorId", "_id", "author");
    UnwindOperation unwindAuthor = unwind("author");

    Aggregation agg = newAggregation(
        matchBooks,
        unwindAuthors,
        matchCoauthors,
        groupDistinct,
        projectIds,
        lookupCoauthors,
        unwindAuthor
    );

    var results = mongoTemplate.aggregate(agg, "books", CoAuthorAgg.class);
    return results.getMappedResults().stream()
        .map(CoAuthorAgg::author)
        .map(mapper::toDomain)
        .toList();
  }

  private record AuthorLendingAgg(String name, long lendingCount) {}
  private record CoAuthorAgg(AuthorDoc author) {}
}
