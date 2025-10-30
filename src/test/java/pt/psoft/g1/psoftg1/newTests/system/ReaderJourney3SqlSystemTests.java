package pt.psoft.g1.psoftg1.newTests.system;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.transaction.Transactional;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.newTests.testutils.SqlBackedITBase;
import pt.psoft.g1.psoftg1.newTests.testutils.SystemTestsSeeds;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","sql"})
@Transactional
class ReaderJourney3SqlSystemTests extends SqlBackedITBase {

  @Autowired MockMvc mvc;
  @Autowired UserRepository userRepo;
  @Autowired GenreRepository genreRepo;

  private static RequestPostProcessor asReader(String email) {
    return jwt()
        .jwt(j -> {
          j.claim("sub", "3," + email);
          j.claim("preferred_username", email);
        })
        .authorities(new SimpleGrantedAuthority("ROLE_READER"));
  }

  private static RequestPostProcessor asLibrarian() {
    return jwt()
        .jwt(j -> j.claim("sub", "1,librarian@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
  }

  @Test
  void journey_reader_requests_and_returns_lending_sql() throws Exception {
    // Seed minimal data
    Genre fantasy = new Genre("Fantasy"); fantasy.assignPk("g-fantasy"); genreRepo.save(fantasy);

    final String authorNumber = SystemTestsSeeds.createAuthor(
        mvc, asLibrarian(), "Tove Jansson", "Finnish writer and artist.");

    final String isbn  = "9780306406157";
    final String title = "Moominland";
    SystemTestsSeeds.putBook(mvc, asLibrarian(), isbn, title, "Fantasy",
        "A classic adventure in Moominvalley.", authorNumber);

    final String readerEmail = "reader4@example.com";
    final String readerNumber = SystemTestsSeeds.createReader(
        mvc, readerEmail, "Reader Four", "900000004");

    // 1) Librarian creates lending
    String lendingLocation = SystemTestsSeeds.createLending(mvc, asLibrarian(), readerNumber, isbn);
    String lendingPath = URI.create(lendingLocation).getPath();               // /api/lendings/{year}/{seq}
    String lendingNumber = lendingPath.substring("/api/lendings/".length());  // {year}/{seq}

    // 2) Reader GET their own lending
    MvcResult getAsReader = mvc.perform(get(lendingPath).with(asReader(readerEmail)))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.lendingNumber", is(lendingNumber)))
        .andExpect(jsonPath("$.bookTitle", is(title)))
        // assert links point to the right resources
        .andExpect(jsonPath("$._links.book.href", containsString("/api/books/" + isbn)))
        .andExpect(jsonPath("$._links.reader.href", containsString("/api/readers/" + readerNumber)))
        // returnedDate field exists and is null initially
        .andExpect(jsonPath("$.returnedDate", nullValue()))
        .andReturn();

    String etagV0 = getAsReader.getResponse().getHeader("ETag");

    // 3) Reader PATCH with If-Match to mark as returned
    MvcResult patchReturn = mvc.perform(
            patch(lendingPath)
                .with(asReader(readerEmail))
                .contentType(MediaType.APPLICATION_JSON)
                .header("If-Match", etagV0)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.lendingNumber", is(lendingNumber)))
        .andExpect(jsonPath("$.bookTitle", is(title)))
        .andExpect(jsonPath("$.returnedDate", notNullValue()))
        .andReturn();

    String etagV1 = patchReturn.getResponse().getHeader("ETag");

    // 4) Reader tries to PATCH again with stale ETag => 409, 412 or 400
    mvc.perform(
            patch(lendingPath)
                .with(asReader(readerEmail))
                .contentType(MediaType.APPLICATION_JSON)
                .header("If-Match", etagV0)
                .content("{}"))
        .andExpect(result -> {
          int s = result.getResponse().getStatus();
          org.hamcrest.MatcherAssert.assertThat(
              "Expected 409 Conflict, 412 Precondition Failed, or 400 Bad Request",
              s, anyOf(is(409), is(412), is(400)));
        });

    Librarian lib = new Librarian("librarian@example.com", "x");
    lib.assignId("1");
    userRepo.save(lib);

    // 5) Librarian GET any lending
    mvc.perform(get(lendingPath).with(asLibrarian()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.lendingNumber", is(lendingNumber)))
        .andExpect(jsonPath("$.bookTitle", is(title)))
        .andExpect(jsonPath("$.returnedDate", notNullValue()));
  }
}
