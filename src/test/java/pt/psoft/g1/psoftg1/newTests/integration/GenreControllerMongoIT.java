package pt.psoft.g1.psoftg1.newTests.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.newTests.testutils.MongoBackedITBase;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;

/**
 * Opaque-box integration tests for GenreController against a real Mongo backend.
 * Controller → Service → Repos → Domain are all real.
 */
@WithMockUser(roles = "LIBRARIAN")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","mongo"})
class GenreControllerMongoIT extends MongoBackedITBase {

  @Autowired MockMvc mvc;

  @Autowired GenreRepository genreRepo;
  @Autowired AuthorRepository authorRepo;
  @Autowired BookRepository bookRepo;
  @Autowired IdGenerator idGen;
  @Autowired MongoTemplate mongo;

  private Genre tech;
  private Genre biz;

  @BeforeEach
  void setUp() {
    mongo.dropCollection("authors");
    mongo.dropCollection("books");
    mongo.dropCollection("genres");
    mongo.dropCollection("lendings");

    tech = new Genre("Tech");
    tech.assignPk(idGen.newId());
    tech = genreRepo.save(tech);

    biz = new Genre("Business");
    biz.assignPk(idGen.newId());
    biz = genreRepo.save(biz);
  }

  // ---------- helpers -------------------------------------------------------

  private Author author(String name) {
    var a = new Author(name, "bio", null);
    a.assignId(idGen.newId());
    return authorRepo.save(a);
  }

  private Book book(String isbn, String title, String desc, Genre genre, List<Author> authors) {
    var b = new Book(isbn, title, desc, genre, authors, null);
    b.assignPk(idGen.newId());
    return bookRepo.save(b);
  }

  // ---------- tests ---------------------------------------------------------

  @Test
  void top5_200_withBookCounts_sorted() throws Exception {
    var a1 = author("Robert Martin");
    var a2 = author("Eric Ries");

    book("9780306406157", "Clean Code", "D1", tech, List.of(a1));
    book("9780470059029", "Clean Architecture", "D2", tech, List.of(a1));
    book("9781861972712", "The Lean Startup", "D3", biz,  List.of(a2));

    mvc.perform(get("/api/genres/top5").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.items", not(empty())))
        .andExpect(jsonPath("$.items[*].genreView.genre", hasItems("Tech", "Business")))
        .andExpect(jsonPath("$.items[0].genreView.genre").value("Tech"))
        .andExpect(jsonPath("$.items[0].bookCount").value(2));
  }

  @Test
  void top5_404_whenNoBooks() throws Exception {
    mvc.perform(get("/api/genres/top5").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.message").value("Not found"))
        .andExpect(jsonPath("$.details[0]").value("No genres to show"));
  }

  @Test
  void avgLendingsPerGenre_200_emptyOk() throws Exception {
    String body = """
    {
      "page": { "number": 1, "limit": 10 },
      "query": { "month": 1, "year": 2025 }
    }
    """;

    mvc.perform(post("/api/genres/avgLendingsPerGenre")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").exists());
  }

  @Test
  void lendingsPerMonthLastTwelveMonths_404_whenNoData() throws Exception {
    mvc.perform(get("/api/genres/lendingsPerMonthLastTwelveMonths"))
        .andExpect(status().isNotFound());
  }

  @Test
  void lendingsAverageDurationPerMonth_404_whenNoData() throws Exception {
    mvc.perform(get("/api/genres/lendingsAverageDurationPerMonth")
            .param("startDate", "2020-01-01")
            .param("endDate", "2020-12-31"))
        .andExpect(status().isNotFound());
  }
}
