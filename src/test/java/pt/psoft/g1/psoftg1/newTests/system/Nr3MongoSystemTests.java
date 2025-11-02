package pt.psoft.g1.psoftg1.newTests.system;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.newTests.testutils.MongoBackedITBase;
import pt.psoft.g1.psoftg1.newTests.testutils.SystemTestsSeeds;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","mongo"})
class Nr3MongoSystemTests extends MongoBackedITBase {

  @Autowired MockMvc mvc;

  @Autowired GenreRepository genreRepo;

  private static RequestPostProcessor asReader() {
    return jwt()
        .jwt(j -> {
          j.claim("sub", "2,reader@example.com");
          j.claim("preferred_username", "reader@example.com");
        })
        .authorities(new SimpleGrantedAuthority("ROLE_READER"));
  }

  private static RequestPostProcessor asLibrarian() {
    return jwt()
        .jwt(j -> j.claim("sub", "1,librarian@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
  }

  @Test
  void journey_reader_browses_searches_gets_suggestions_mongo() throws Exception {
    // Genres
    Genre fantasy = new Genre("Fantasy"); fantasy.assignPk("g-fantasy"); genreRepo.save(fantasy);
    Genre mystery = new Genre("Mystery"); mystery.assignPk("g-mystery"); genreRepo.save(mystery);

    // Authors
    final String author1Name = "Tove Jansson";
    final String author1Bio  = "Finnish writer and artist.";
    String author1Number = SystemTestsSeeds.createAuthor(mvc, asLibrarian(), author1Name, author1Bio);

    final String author2Name = "Jo Nesbø";
    final String author2Bio  = "Norwegian writer of crime fiction.";
    String author2Number = SystemTestsSeeds.createAuthor(mvc, asLibrarian(), author2Name, author2Bio);

    // Books linked to authors
    final String isbn1 = "9780306406157";
    final String title1 = "Moominland";
    final String desc1  = "A classic adventure in Moominvalley.";
    SystemTestsSeeds.putBook(mvc, asLibrarian(), isbn1, title1, "Fantasy", desc1, author1Number);

    final String isbn2 = "9780306406164";
    final String title2 = "The Snowman";
    final String desc2  = "Harry Hole investigates a chilling case.";
    SystemTestsSeeds.putBook(mvc, asLibrarian(), isbn2, title2, "Mystery", desc2, author2Number);

    // Reader (returns "YYYY/N")
    String readerNumber = SystemTestsSeeds.createReader(mvc,
        "reader2@example.com", "Reader Two", "900000000");

    // Lendings
    SystemTestsSeeds.createLending(mvc, asLibrarian(), readerNumber, isbn1);
    SystemTestsSeeds.createLending(mvc, asLibrarian(), readerNumber, isbn2);

    // ========================================================================
    // 1) GET /api/books with filters (title, genre)
    // ========================================================================

    // ---- 1a) Filter by partial title ----
    mvc.perform(get("/api/books").param("title", "moo").with(asReader()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.items", notNullValue()))
        .andExpect(jsonPath("$.items[*].title", hasItem(containsStringIgnoringCase("moo"))));

    // ---- 1b) Filter by genre name ----
    mvc.perform(get("/api/books").param("genre", "Fantasy").with(asReader()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.items", not(empty())))
        .andExpect(jsonPath("$.items[*].isbn", hasItem(isbn1)));

    // ========================================================================
    // 2) GET /api/authors?name= — search author by partial name
    // ========================================================================
    mvc.perform(get("/api/authors").param("name", "jo").with(asReader()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.items", not(empty())))
        .andExpect(jsonPath("$.items[*].name", hasItem(containsStringIgnoringCase("jo"))));

    // ========================================================================
    // 3) GET /api/authors/top5 — reader can access
    // ========================================================================
    mvc.perform(get("/api/authors/top5").with(asReader()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.items", isA(java.util.List.class)))
        .andExpect(jsonPath("$.items.length()", lessThanOrEqualTo(5)));

    // ========================================================================
    // 4) GET /api/genres/top5 — forbidden to reader
    // ========================================================================
    mvc.perform(get("/api/genres/top5").with(asReader()))
        .andExpect(status().isForbidden());
  }
}
