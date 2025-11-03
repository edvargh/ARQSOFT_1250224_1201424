package pt.psoft.g1.psoftg1.newTests.testutils;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SystemTestsSeeds {

  /** Create an author via multipart form; returns authorNumber (e.g. "A-12345"). */
  public static String createAuthor(MockMvc mvc, RequestPostProcessor asLibrarian,
      String name, String bio) throws Exception {
    var res = mvc.perform(multipart("/api/authors")
                .with(asLibrarian)
                .param("name", name)
                .param("bio", bio)
            // ensure no photo is sent (controller handles nulls)
            // .file(new MockMultipartFile("photo", new byte[0])) // omit entirely
        )
        .andExpect(status().isCreated())
        .andExpect(header().string("ETag", not("")))
        .andExpect(header().string("Location", containsString("/api/authors")))
        .andReturn();

    String body = res.getResponse().getContentAsString();
    // AuthorView should include authorNumber
    return JsonPath.read(body, "$.authorNumber");
  }

  /** Create a reader via multipart; returns readerNumber (e.g. "2025/1"). */
    public static String createReader(MockMvc mvc, String email, String fullName, String phone) throws Exception {
      var res = mvc.perform(multipart("/api/readers")
              .param("username", email)
              .param("password", "Secret123!")
              .param("fullName", fullName)
              .param("phoneNumber", phone)
              .param("birthDate", "1999-01-01")
              .param("gdpr", "true"))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", containsString("/api/readers/")))
          .andReturn();

      String loc = res.getResponse().getHeader("Location");
      String[] parts = java.net.URI.create(loc).getPath().split("/");
      return parts[parts.length - 2] + "/" + parts[parts.length - 1];
    }

  /**
   * Create a book via multipart **PUT** with @ModelAttribute CreateBookRequest.
   * 'authors' is a list of authorNumbers. Returns the Location header.
   */
  public static String putBook(MockMvc mvc, RequestPostProcessor asLibrarian,
      String isbn, String title, String genre,
      String description, String... authorNumbers) throws Exception {
    // Build a multipart request and switch it to PUT
    MockMultipartHttpServletRequestBuilder builder = (MockMultipartHttpServletRequestBuilder)
        multipart("/api/books/{isbn}", isbn).with(asLibrarian);

    builder.with(req -> { req.setMethod("PUT"); return req; });

    builder.param("title", title);
    builder.param("genre", genre);
    if (description != null) builder.param("description", description);
    if (authorNumbers != null && authorNumbers.length > 0) {
      builder.param("authors", authorNumbers);
      builder.param("authorNumbers", authorNumbers);
      for (int i = 0; i < authorNumbers.length; i++) {
        builder.param("authors[" + i + "]", authorNumbers[i]);
        builder.param("authorNumbers[" + i + "]", authorNumbers[i]);
      }
    }

    var res = mvc.perform(builder)
        .andExpect(status().isCreated())
        .andExpect(header().string("ETag", not("")))
        .andExpect(header().string("Location", containsString("/api/books/" + isbn)))
        .andReturn();

    return res.getResponse().getHeader("Location");
  }

  /** Create a lending via JSON body; returns lending Location (e.g. /api/lendings/2025/7). */
  public static String createLending(MockMvc mvc, RequestPostProcessor asLibrarian,
      String readerNumber, String isbn) throws Exception {
    var res = mvc.perform(post("/api/lendings")
            .with(asLibrarian)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {
                "isbn": "%s",
                "readerNumber": "%s"
              }
            """.formatted(isbn, readerNumber)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/lendings/")))
        .andReturn();

    return res.getResponse().getHeader("Location");
  }

  /** Utility to get the trailing segment from a Location URL. */
  private static String tail(String location) {
    return location.substring(location.lastIndexOf('/') + 1);
  }
}
