package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookCountDTO;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@Profile("mongo")
@RequiredArgsConstructor
public class BookMongoRepository implements BookRepository {

  private final SpringMongoBookRepo repo;
  private final MongoTemplate mongo;
  private final BookMongoMapper mapper;
  private final GenreRepository genreRepo;
  private final AuthorRepository authorRepo;

  /* ----------------------- helpers ------------------------ */

  private Book toDomain(BookDoc d) {
    Genre genre = genreRepo.findByString(d.getGenre())
        .orElseThrow(() -> new IllegalArgumentException("Genre not found: " + d.getGenre()));

    List<Author> authors = d.getAuthorNumbers() == null ? List.of() :
        d.getAuthorNumbers().stream()
            .map(authorRepo::findByAuthorNumber)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    return new Book(d.getIsbn(), d.getTitle(), d.getDescription(), genre, authors, null);
  }

  private List<Book> mapAll(List<BookDoc> docs) {
    return docs.stream().map(this::toDomain).toList();
  }

  private static String ciStartsWith(String s) {
    return "(?i)^" + Pattern.quote(s);
  }

  private static Pageable toPageable(pt.psoft.g1.psoftg1.shared.services.Page page) {
    int p = Math.max(0, page.getNumber() - 1);
    int size = Math.max(1, page.getLimit());
    return PageRequest.of(p, size);
  }


  @Override
  public List<Book> findByGenre(String genre) {
    if (genre == null || genre.isBlank()) return List.of();
    var regex = ciStartsWith(genre);
    return mapAll(repo.findByGenreRegex(regex));
  }

  @Override
  public List<Book> findByTitle(String title) {
    if (title == null || title.isBlank()) return List.of();
    var regex = ciStartsWith(title);
    return mapAll(repo.findByTitleRegex(regex));
  }

  @Override
  public List<Book> findByAuthorName(String authorName) {
    if (authorName == null || authorName.isBlank()) return List.of();
    var regex = ciStartsWith(authorName);
    return mapAll(repo.findByAuthorNamesRegex(regex));
  }

  @Override
  public Optional<Book> findByIsbn(String isbn) {
    return repo.findByIsbn(isbn).map(this::toDomain);
  }

  @Override
  public Page<BookCountDTO> findTop5BooksLent(LocalDate oneYearAgo, Pageable pageable) {
    MatchOperation match = match(Criteria.where("startDate").gte(oneYearAgo));
    GroupOperation group = group("bookIsbn").count().as("lendingCount");
    SortOperation sort = sort(Sort.Direction.DESC, "lendingCount");
    SkipOperation skip = skip((long) pageable.getPageNumber() * pageable.getPageSize());
    LimitOperation limit = limit(pageable.getPageSize());
    LookupOperation lookupBook = LookupOperation.newLookup()
        .from("books")
        .localField("_id")
        .foreignField("isbn")
        .as("book");
    UnwindOperation unwindBook = unwind("book");

    Aggregation agg = newAggregation(
        match,
        group,
        sort,
        skip,
        limit,
        lookupBook,
        unwindBook
    );

    AggregationResults<BookCountAgg> results =
        mongo.aggregate(agg, "lendings", BookCountAgg.class);

    var content = results.getMappedResults().stream()
        .map(r -> new BookCountDTO(toDomain(r.book()), r.lendingCount()))
        .toList();

    Aggregation totalAgg = newAggregation(
        match,
        group,
        count().as("total")
    );
    AggregationResults<TotalAgg> totalRes =
        mongo.aggregate(totalAgg, "lendings", TotalAgg.class);
    long total = Optional.ofNullable(totalRes.getUniqueMappedResult())
        .map(TotalAgg::total)
        .orElse(0L);

    return new PageImpl<>(content, pageable, total);
  }

  @Override
  public List<Book> findBooksByAuthorNumber(Long authorNumber) {
    if (authorNumber == null) return List.of();
    Query q = new Query(Criteria.where("authorNumbers").is(authorNumber));
    List<BookDoc> docs = mongo.find(q, BookDoc.class);
    return mapAll(docs);
  }

  @Override
  public List<Book> searchBooks(pt.psoft.g1.psoftg1.shared.services.Page page, SearchBooksQuery query) {
    Query q = new Query();

    if (query != null) {
      if (query.getTitle() != null && !query.getTitle().isBlank()) {
        q.addCriteria(Criteria.where("title").regex(ciStartsWith(query.getTitle()), "i"));
      }
      if (query.getGenre() != null && !query.getGenre().isBlank()) {
        q.addCriteria(Criteria.where("genre").regex(ciStartsWith(query.getGenre()), "i"));
      }
      if (query.getAuthorName() != null && !query.getAuthorName().isBlank()) {
        q.addCriteria(Criteria.where("authorNames").regex(ciStartsWith(query.getAuthorName()), "i"));
      }
    }

    q.with(Sort.by(Sort.Direction.ASC, "title"));

    if (page == null) page = new pt.psoft.g1.psoftg1.shared.services.Page(1, 10);
    int skip = (page.getNumber() - 1) * page.getLimit();
    q.skip(skip).limit(page.getLimit());

    List<BookDoc> docs = mongo.find(q, BookDoc.class);
    return mapAll(docs);
  }

  @Override
  public Book save(Book book) {
    var doc = BookDoc.builder()
        .isbn(book.getIsbn())
        .title(book.getTitle().getTitle())
        .description(book.getDescription())
        .genre(book.getGenre().toString())
        .authorNumbers(book.getAuthors().stream().map(Author::getId).toList())
        .authorNames(book.getAuthors().stream().map(Author::getName).toList())
        .photoFile(book.getPhoto() == null ? null : book.getPhoto().getPhotoFile())
        .build();

    var saved = repo.findByIsbn(book.getIsbn())
        .map(existing -> { doc.setId(existing.getId()); return repo.save(doc); })
        .orElseGet(() -> repo.save(doc));

    return toDomain(saved);
  }

  @Override
  public void delete(Book book) {
    if (book == null) return;
    repo.findByIsbn(book.getIsbn()).ifPresent(d -> repo.deleteById(d.getId()));
  }

  private record BookCountAgg(String _id, long lendingCount, BookDoc book) {}
  private record TotalAgg(long total) {}
}
