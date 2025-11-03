package pt.psoft.g1.psoftg1.newTests.integration;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;

/**
 * Opaque-box integration tests for BookController against a real Mongo backend.
 * Controller → Service → Repos → Domain are all real.
 * FileStorageService is stubbed with @MockBean.
 */
@WithMockUser(roles = "READER")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","mongo"})
class BookControllerMongoIT extends MongoBackedITBase {

  @Autowired MockMvc mvc;

  @Autowired GenreRepository genreRepo;
  @Autowired AuthorRepository authorRepo;
  @Autowired BookRepository bookRepo;
  @Autowired IdGenerator idGen;
  @Autowired MongoTemplate mongo;

  @MockBean FileStorageService fileStorage;

  private Genre tech;

  @BeforeEach
  void setUp() {
    mongo.dropCollection("authors");
    mongo.dropCollection("books");
    mongo.dropCollection("genres");

    tech = new Genre("Tech");
    tech.assignPk(idGen.newId());
    tech = genreRepo.save(tech);

    when(fileStorage.getRequestPhoto(any())).thenReturn(null);
    when(fileStorage.getExtension(any())).thenReturn(Optional.of("png"));
    when(fileStorage.getFile(any())).thenReturn("IMG".getBytes(StandardCharsets.UTF_8));
    doNothing().when(fileStorage).deleteFile(any());
  }

  // ---------- helpers -------------------------------------------------------

  private Author author(String name) {
    var a = new Author(name, "bio", null);
    a.assignId(idGen.newId());
    return authorRepo.save(a);
  }

  private Book book(String isbn, String title, String desc, List<Author> authors) {
    var b = new Book(isbn, title, desc, tech, authors, null);
    b.assignPk(idGen.newId());
    return bookRepo.save(b);
  }

  // ---------- tests ---------------------------------------------------------

  @WithMockUser(roles = "LIBRARIAN")
  @Test
  void create_201_putWithPhoto_setsETag_andReturnsView() throws Exception {
    var a = author("RobertMartin");

    when(fileStorage.getRequestPhoto(any())).thenReturn("some-uuid.png");

    MockMultipartFile photo = new MockMultipartFile(
        "photo", "cover.png", "image/png", new byte[]{(byte)137, 80, 78, 71});

    mvc.perform(multipart("/api/books/{isbn}", "9780306406157")
            .file(photo)
            .param("title", "Clean Code")
            .param("description", "A Handbook of Agile Software Craftsmanship")
            .param("genre", tech.getGenre())
            .param("authors", a.getId())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .with(req -> { req.setMethod("PUT"); return req; }))
        .andExpect(status().isCreated())
        .andExpect(header().string("ETag", not(emptyString())))
        .andExpect(header().string("Location", containsString("/api/books/9780306406157/9780306406157")))
        .andExpect(jsonPath("$.isbn").value("9780306406157"))
        .andExpect(jsonPath("$.title").value("Clean Code"))
        .andExpect(jsonPath("$.genre").value("Tech"))
        .andExpect(jsonPath("$.authors[0]").value("RobertMartin"))
        .andExpect(jsonPath("$._links.self", notNullValue()))
        .andExpect(jsonPath("$._links.photo", notNullValue()));
  }

  @Test
  void findByIsbn_200_returnsView_andETag() throws Exception {
    var a = author("Eric Ries");
    var saved = book("9781861972712", "The Lean Startup", "Build-Measure-Learn", List.of(a));

    mvc.perform(get("/api/books/{isbn}", saved.getIsbn()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"0\""))
        .andExpect(jsonPath("$.isbn").value("9781861972712"))
        .andExpect(jsonPath("$.title").value("The Lean Startup"))
        .andExpect(jsonPath("$.genre").value("Tech"))
        .andExpect(jsonPath("$.authors[0]").value("Eric Ries"));
  }

  @Test
  void findByIsbn_404_whenMissing() throws Exception {
    mvc.perform(get("/api/books/{isbn}", "NOPE"))
        .andExpect(status().isNotFound());
  }

  @WithMockUser(roles = "LIBRARIAN")
  @Test
  void updateBook_400_withoutIfMatch() throws Exception {
    var b = book("9780000000002", "Old Title", "Desc", List.of(author("A")));

    mvc.perform(patch("/api/books/{isbn}", b.getIsbn())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("title", "New Title"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.detail")
            .value(org.hamcrest.Matchers.containsString("You must issue a conditional PATCH")));
  }

  @WithMockUser(roles = "LIBRARIAN")
  @Test
  void getPhoto_200_imageBytes_whenPhotoExists() throws Exception {
    var a = author("Photo Author");

    when(fileStorage.getRequestPhoto(any())).thenReturn("photo-uuid.png");

    MockMultipartFile photo = new MockMultipartFile(
        "photo", "cover.png", "image/png", "PNG".getBytes(StandardCharsets.UTF_8));

    mvc.perform(multipart("/api/books/{isbn}", "9780000001009")
            .file(photo)
            .param("title", "Has Photo")
            .param("description", "D")
            .param("genre", tech.getGenre())
            .param("authors", a.getId())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .with(req -> { req.setMethod("PUT"); return req; }))
        .andExpect(status().isCreated());

    when(fileStorage.getExtension(any())).thenReturn(Optional.of("png"));
    when(fileStorage.getFile(any())).thenReturn(new byte[]{1,2,3});

    mvc.perform(get("/api/books/{isbn}/photo", "9780000001009"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", startsWith("image/")))
        .andExpect(content().bytes(new byte[]{1,2,3}));
  }

  @Test
  void getPhoto_200_emptyBody_whenNoPhoto() throws Exception {
    var saved = book("9780000001009", "No Photo", "D", List.of(author("X")));
    mvc.perform(get("/api/books/{isbn}/photo", saved.getIsbn()))
        .andExpect(status().isOk())
        .andExpect(content().string(isEmptyString()));
  }

  @WithMockUser(roles = "LIBRARIAN")
  @Test
  void deletePhoto_200_whenPhotoExists_thenGetReturnsEmpty() throws Exception {
    var a = author("Del Photo Author");
    when(fileStorage.getRequestPhoto(any())).thenReturn("del-uuid.jpeg");

    MockMultipartFile photo = new MockMultipartFile(
        "photo", "c.jpeg", "image/jpeg", new byte[]{1,2,3,4});

    mvc.perform(multipart("/api/books/{isbn}", "9780000001009")
            .file(photo)
            .param("title", "Del Photo")
            .param("description", "D")
            .param("genre", tech.getGenre())
            .param("authors", a.getId())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .with(req -> { req.setMethod("PUT"); return req; }))
        .andExpect(status().isCreated());

    mvc.perform(delete("/api/books/{isbn}/photo", "9780000001009"))
        .andExpect(status().isOk());

    mvc.perform(get("/api/books/{isbn}/photo", "9780000001009"))
        .andExpect(status().isOk())
        .andExpect(content().string(isEmptyString()));
  }

  @WithMockUser(roles = "LIBRARIAN")
  @Test
  void deletePhoto_404_whenNoPhoto() throws Exception {
    var b = book("9780000001009", "No Photo", "D", List.of(author("Y")));
    mvc.perform(delete("/api/books/{isbn}/photo", b.getIsbn()))
        .andExpect(status().isNotFound());
  }

  @Test
  void findBooks_orUnion_titleOrGenreOrAuthor_sortedByTitle() throws Exception {
    var a1 = author("Robert Martin");
    var a2 = author("Eric Ries");

    book("9780306406157", "Clean Code", "D1", List.of(a1));
    book("9781861972712", "The Lean Startup", "D2", List.of(a2));
    book("9780470059029", "Clean Architecture", "D3", List.of(a1));

    mvc.perform(get("/api/books")
            .param("title", "Clean")
            .param("genre", "Tech")
            .param("authorName", "Eric"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(3)))
        .andExpect(jsonPath("$.items[*].title",
            contains("Clean Architecture", "Clean Code", "The Lean Startup")));
  }
}
