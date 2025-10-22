package pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDTO;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@Profile("mongo")
@RequiredArgsConstructor
public class GenreMongoRepository implements GenreRepository {

  private final SpringMongoGenreRepo repo;
  private final MongoTemplate mongo;

  private Genre toDomain(GenreDoc d) { return new Genre(d.getGenre()); }

  private GenreDoc toDoc(Genre g) {
    return GenreDoc.builder().genre(g.toString()).build();
  }

  private Date toStartOfDay(LocalDate d) {
    return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private Date toEndOfDay(LocalDate d) {
    return Date.from(d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  @Override
  public Iterable<Genre> findAll() {
    return repo.findAll().stream().map(this::toDomain).toList();
  }

  @Override
  public Optional<Genre> findByString(String genreName) {
    if (genreName == null) return Optional.empty();
    return repo.findByGenre(genreName).map(this::toDomain);
  }

  @Override
  public Genre save(Genre genre) {
    var saved = repo.findByGenre(genre.toString())
        .map(existing -> { existing.setGenre(genre.toString()); return repo.save(existing); })
        .orElseGet(() -> repo.save(toDoc(genre)));
    return toDomain(saved);
  }

  @Override
  public void delete(Genre genre) {
    if (genre == null) return;
    repo.findByGenre(genre.toString()).ifPresent(d -> repo.deleteById(d.getId()));
  }

  @Override
  public Page<GenreBookCountDTO> findTop5GenreByBookCount(Pageable pageable) {
    GroupOperation group = group("genre").count().as("bookCount");
    SortOperation sort = sort(org.springframework.data.domain.Sort.Direction.DESC, "bookCount");
    SkipOperation skip = skip((long) pageable.getPageNumber() * pageable.getPageSize());
    LimitOperation limit = limit(pageable.getPageSize());

    Aggregation agg = newAggregation(group, sort, skip, limit);
    var results = mongo.aggregate(agg, "books", GenreBookCountAgg.class).getMappedResults();

    List<GenreBookCountDTO> mapped = results.stream()
        .map(r -> new GenreBookCountDTO(r.id, r.bookCount))
        .toList();

    var totalAgg = mongo.aggregate(newAggregation(group), "books", GenreBookCountAgg.class);
    long total = totalAgg.getMappedResults().size();

    return new PageImpl<>(mapped, pageable, total);
  }

  private record GenreBookCountAgg(String id, long bookCount) {}

  @Override
  public List<GenreLendingsDTO> getAverageLendingsInMonth(LocalDate month, pt.psoft.g1.psoftg1.shared.services.Page page) {
    int days = month.lengthOfMonth();
    Date start = toStartOfDay(month.withDayOfMonth(1));
    Date end = toEndOfDay(month.withDayOfMonth(days));

    MatchOperation match = match(Criteria.where("startDate").gte(start).lt(end));
    LookupOperation lookup = lookup("books", "bookIsbn", "isbn", "book");
    UnwindOperation unwind = unwind("book");
    GroupOperation group = group("$book.genre").count().as("cnt");
    ProjectionOperation project = project()
        .and("_id").as("genre")
        .andExpression("cnt / " + days).as("avg");

    Aggregation agg = newAggregation(match, lookup, unwind, group, project,
        sort(org.springframework.data.domain.Sort.by("genre").ascending()));

    var results = mongo.aggregate(agg, "lendings", GenreAvgAgg.class).getMappedResults();

    int from = Math.max(0, (page.getNumber() - 1) * page.getLimit());
    int to = Math.min(results.size(), from + page.getLimit());
    if (from >= to) return List.of();

    return results.subList(from, to).stream()
        .map(r -> new GenreLendingsDTO(r.genre, r.avg))
        .toList();
  }

  private record GenreAvgAgg(String genre, double avg) {}

  @Override
  public List<GenreLendingsPerMonthDTO> getLendingsPerMonthLastYearByGenre() {
    LocalDate now = LocalDate.now();
    LocalDate twelveMonthsAgo = now.minusMonths(12);

    MatchOperation match = match(Criteria.where("startDate")
        .gte(toStartOfDay(twelveMonthsAgo.withDayOfMonth(1)))
        .lt(toEndOfDay(now)));

    LookupOperation lookup = lookup("books", "bookIsbn", "isbn", "book");
    UnwindOperation unwind = unwind("book");

    ProjectionOperation projectYearMonthGenre = project()
        .andExpression("year($startDate)").as("year")
        .andExpression("month($startDate)").as("month")
        .and("book.genre").as("genre");

    GroupOperation group = group(fields().and("year").and("month").and("genre"))
        .count().as("count");

    SortOperation sort = sort(org.springframework.data.domain.Sort.by("year", "month", "genre").ascending());

    Aggregation agg = newAggregation(match, lookup, unwind, projectYearMonthGenre, group, sort);

    var rows = mongo.aggregate(agg, "lendings", YMGCountAgg.class).getMappedResults();

    Map<Integer, Map<Integer, List<GenreLendingsDTO>>> grouped =
        rows.stream().collect(Collectors.groupingBy(
            r -> r.year,
            Collectors.groupingBy(
                r -> r.month,
                Collectors.mapping(
                    r -> new GenreLendingsDTO(r.genre, r.count),
                    Collectors.toList()
                )
            )
        ));

    List<GenreLendingsPerMonthDTO> out = new ArrayList<>();
    grouped.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(yearEntry -> {
          int year = yearEntry.getKey();
          yearEntry.getValue().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .forEach(monthEntry -> out.add(new GenreLendingsPerMonthDTO(year, monthEntry.getKey(), monthEntry.getValue())));
        });

    return out;
  }

  private record YMGCountAgg(int year, int month, String genre, long count) {}

  @Override
  public List<GenreLendingsPerMonthDTO> getLendingsAverageDurationPerMonth(LocalDate startDate, LocalDate endDate) {
    MatchOperation match = match(Criteria.where("startDate").gte(toStartOfDay(startDate))
        .lte(toEndOfDay(endDate))
        .and("returnedDate").ne(null));

    LookupOperation lookup = lookup("books", "bookIsbn", "isbn", "isbnBook");
    UnwindOperation unwind = unwind("isbnBook");

    AddFieldsOperation addDuration = AddFieldsOperation.builder()
        .addFieldWithValue(
            "durationDays",
            new org.bson.Document("$dateDiff",
                new org.bson.Document()
                    .append("startDate", "$startDate")
                    .append("endDate", "$returnedDate")
                    .append("unit", "day")))
        .build();

    ProjectionOperation project = project()
        .andExpression("year($startDate)").as("year")
        .andExpression("month($startDate)").as("month")
        .and("isbnBook.genre").as("genre")
        .and("durationDays").as("durationDays");

    GroupOperation group = group(fields().and("year").and("month").and("genre"))
        .avg("durationDays").as("avgDuration");

    SortOperation sort = sort(org.springframework.data.domain.Sort.by("year", "month", "genre").ascending());

    Aggregation agg = newAggregation(match, lookup, unwind, addDuration, project, group, sort);

    var rows = mongo.aggregate(agg, "lendings", YMGAverageAgg.class).getMappedResults();

    Map<Integer, Map<Integer, List<GenreLendingsDTO>>> grouped =
        rows.stream().collect(Collectors.groupingBy(
            r -> r.year,
            Collectors.groupingBy(
                r -> r.month,
                Collectors.mapping(
                    r -> new GenreLendingsDTO(r.genre, r.avgDuration),
                    Collectors.toList()
                )
            )
        ));

    List<GenreLendingsPerMonthDTO> out = new ArrayList<>();
    grouped.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(yearEntry -> {
          int year = yearEntry.getKey();
          yearEntry.getValue().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .forEach(monthEntry -> out.add(new GenreLendingsPerMonthDTO(year, monthEntry.getKey(), monthEntry.getValue())));
        });

    return out;
  }

  private record YMGAverageAgg(int year, int month, String genre, double avgDuration) {}
}
