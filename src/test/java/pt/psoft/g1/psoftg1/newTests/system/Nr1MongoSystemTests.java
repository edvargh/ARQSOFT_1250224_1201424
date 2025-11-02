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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.newTests.testutils.MongoBackedITBase;
import pt.psoft.g1.psoftg1.newTests.testutils.SystemTestsSeeds;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","mongo"})
class Nr1MongoSystemTests extends MongoBackedITBase {

  @Autowired MockMvc mvc;

  @Autowired GenreRepository genreRepo;

  private static RequestPostProcessor asLibrarian() {
    return jwt()
        .jwt(j -> j.claim("sub", "1,librarian@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
  }

  @Test
  void journey_librarian_registers_author_and_books_mongo() throws Exception {
    // 1) Create author
    final String authorName = "Tove Jansson";
    final String initialBio = "Finnish writer and artist.";
    String authorNumber = SystemTestsSeeds.createAuthor(mvc, asLibrarian(), authorName, initialBio);

    // 2) GET author & capture ETag
    MvcResult authorGet = mvc.perform(get("/api/authors/{authorNumber}", authorNumber).with(asLibrarian()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.authorNumber").value(authorNumber))
        .andExpect(jsonPath("$.name").value(authorName))
        .andExpect(jsonPath("$.bio").value(initialBio))
        .andReturn();

    String etag = authorGet.getResponse().getHeader("ETag");

    // 3) PATCH author (multipart) with If-Match
    final String updatedBio = "Beloved creator of the Moomins.";
    MockMultipartHttpServletRequestBuilder patchAuthor =
        (MockMultipartHttpServletRequestBuilder) multipart("/api/authors/{authorNumber}", authorNumber)
            .with(asLibrarian());
    patchAuthor.with(req -> { req.setMethod("PATCH"); return req; });
    patchAuthor.header("If-Match", etag);
    patchAuthor.param("bio", updatedBio);

    mvc.perform(patchAuthor)
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.authorNumber").value(authorNumber))
        .andExpect(jsonPath("$.bio").value(updatedBio));

    // 4) Create book linked to author (ensure genre exists)
    final String isbn = "9780132350884";
    final String title = "Moominland";
    final String initialGenre = "Fantasy";
    final String initialDesc = "A classic adventure in Moominvalley.";

    Genre genre = new Genre(initialGenre);
    genre.assignPk("12");
    genreRepo.save(genre);

    SystemTestsSeeds.putBook(mvc, asLibrarian(), isbn, title, initialGenre, initialDesc, authorNumber);

    // 5) GET book by ISBN; verify author + ETag
    mvc.perform(get("/api/books/{isbn}", isbn).with(asLibrarian()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.isbn").value(isbn))
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.authors", not(empty())))
        .andExpect(jsonPath("$.authors", hasItem(authorName)));

    // 6) GET /api/authors/{authorNumber}/books; verify listing (wrapped 'items' payload)
    mvc.perform(get("/api/authors/{authorNumber}/books", authorNumber).with(asLibrarian()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.items", not(empty())))
        .andExpect(jsonPath("$.items[*].isbn", hasItem(isbn)))
        .andExpect(jsonPath("$.items[*].title", hasItem(title)));
  }
}