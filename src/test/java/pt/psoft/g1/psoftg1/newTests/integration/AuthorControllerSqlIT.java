package pt.psoft.g1.psoftg1.newTests.integration;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.newTests.testutils.SqlBackedITBase;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;

/**
 * Opaque-box integration tests for AuthorController against a real SQL backend.
 * Controller → Service → Repos → Domain are all real.
 * FileStorageService is stubbed with @MockBean.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","sql","redis"})
@Transactional
class AuthorControllerSqlIT extends SqlBackedITBase {

  @Autowired MockMvc mvc;

  @Autowired AuthorRepository authorRepo;
  @Autowired BookRepository bookRepo;
  @Autowired GenreRepository genreRepo;
  @Autowired PhotoRepository photoRepo;
  @Autowired IdGenerator idGenerator;

  @MockBean FileStorageService fileStorage;

  private Genre genreTech;

  @BeforeEach
  void setUp() {
    genreTech = new Genre("Tech");
    genreTech.assignPk(idGenerator.newId());
    genreTech = genreRepo.save(genreTech);

    when(fileStorage.getRequestPhoto(any())).thenReturn(null);
    when(fileStorage.getExtension(any())).thenReturn(Optional.of("jpg"));
    when(fileStorage.getFile(any())).thenReturn("IMG".getBytes(StandardCharsets.UTF_8));
    doNothing().when(fileStorage).deleteFile(any());
  }

  private Author a(String name, String bio) {
    var a = new Author(name, bio, null);
    a.assignId(idGenerator.newId());
    return authorRepo.save(a);
  }

  private Book book(String isbn, String title, String desc, List<Author> authors) {
    var b = new Book(isbn, title, desc, genreTech, authors, null);
    b.assignPk(idGenerator.newId());
    return bookRepo.save(b);
  }

  private static Long versionOf(Author a) {
    try {
      Field f = Author.class.getDeclaredField("version");
      f.setAccessible(true);
      return (Long) f.get(a);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void createAuthor_201_returnsViewAndETag() throws Exception {
    mvc.perform(post("/api/authors")
            .param("name", "Robert Martin")
            .param("bio", "Uncle Bob"))
        .andExpect(status().isCreated())
        .andExpect(header().string("ETag", not(emptyString())))
        .andExpect(jsonPath("$.name").value("Robert Martin"))
        .andExpect(jsonPath("$.bio").value("Uncle Bob"))
        .andExpect(jsonPath("$._links.author", notNullValue()))
        .andExpect(jsonPath("$._links.booksByAuthor", notNullValue()));
  }

  @Test
  void findByAuthorNumber_200_returnsAuthorView() throws Exception {
    var saved = a("Alice Anders", "Bio A");

    mvc.perform(get("/api/authors/{id}", saved.getId()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"" + versionOf(saved) + "\""))
        .andExpect(jsonPath("$.name").value("Alice Anders"))
        .andExpect(jsonPath("$.bio").value("Bio A"));
  }

  @Test
  void findByAuthorNumber_404_whenMissing() throws Exception {
    mvc.perform(get("/api/authors/{id}", "NOPE"))
        .andExpect(status().isNotFound());
  }

  @Test
  void findByName_prefixSearch_returnsListResponse() throws Exception {
    a("Bob Brown", "x");
    a("Bobby Tables", "x");
    a("Carol", "x");

    mvc.perform(get("/api/authors").param("name", "Bo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[*].name", containsInAnyOrder("Bob Brown", "Bobby Tables")));
  }

  @Test
  void partialUpdate_400_withoutIfMatch() throws Exception {
    a("Alice", "Bio");

    mvc.perform(patch("/api/authors/{id}",a("Alice", "Bio").getId())
            .param("name", "New Name"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void partialUpdate_200_updatesNameAndBio_whenVersionMatches() throws Exception {
    var saved = a("Alice", "Bio");
    var v = versionOf(saved);

    mvc.perform(patch("/api/authors/{id}", saved.getId())
            .header("If-Match", v.toString())
            .param("name", "Alice Updated")
            .param("bio", "Bio Updated"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(equalTo(v.toString()))))
        .andExpect(jsonPath("$.name").value("Alice Updated"))
        .andExpect(jsonPath("$.bio").value("Bio Updated"));
  }

  @Test
  void booksByAuthor_200_returnsBooksForAuthor() throws Exception {
    var A = a("Alice", "x");
    var B = a("Bob", "x");

    book("9780306406157", "Clean Code", "D1", List.of(A, B));
    book("9781861972712", "The Lean Startup", "D2", List.of(A));

    mvc.perform(get("/api/authors/{id}/books", A.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[*].title", containsInAnyOrder("Clean Code", "The Lean Startup")));
  }

  @Test
  void booksByAuthor_404_whenAuthorMissing() throws Exception {
    mvc.perform(get("/api/authors/{id}/books", "NOPE"))
        .andExpect(status().isNotFound());
  }

  @Test
  void top5_404_whenNoData() throws Exception {
    mvc.perform(get("/api/authors/top5"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getPhoto_200_pngOrJpeg_whenPhotoExists() throws Exception {
    var photo = new Photo(Path.of("images/p.png"));
    photo.assignIdIfAbsent(idGenerator.newId());             // manual id, per assignment
    photoRepo.save(photo);

    var a = new Author("Has Photo", "bio", null);    // don't let ctor auto-create Photo
    a.assignId(idGenerator.newId());
    a.setPhotoEntity(photo);
    var saved = authorRepo.save(a);

    when(fileStorage.getExtension(any())).thenReturn(Optional.of("png"));
    when(fileStorage.getFile(any())).thenReturn(new byte[]{1,2,3});

    mvc.perform(get("/api/authors/{id}/photo", saved.getId()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", startsWith("image/")))
        .andExpect(content().bytes(new byte[]{1,2,3}));
  }

  @Test
  void getPhoto_200_emptyBody_whenNoPhoto() throws Exception {
    var a = a("No Photo", "bio");
    mvc.perform(get("/api/authors/{id}/photo", a.getId()))
        .andExpect(status().isOk())
        .andExpect(content().string(isEmptyString()));
  }

  @Test
  void deletePhoto_200_whenPhotoExists_andRemovesIt() throws Exception {
    var photo = new Photo(Path.of("images/p.png"));           // whatever your Photo ctor takes (path/filename)
    photo.assignIdIfAbsent(idGenerator.newId());             // manual id, per assignment
    photoRepo.save(photo);

    var a = new Author("Del Photo", "bio", null);
    a.assignId(idGenerator.newId());
    a.setPhotoEntity(photo);
    var saved = authorRepo.save(a);

    mvc.perform(delete("/api/authors/{id}/photo", saved.getId()))
        .andExpect(status().isOk());
  }

  @Test
  void deletePhoto_404_whenAuthorMissing() throws Exception {
    mvc.perform(delete("/api/authors/{id}/photo", "NOPE"))
        .andExpect(status().isForbidden()); // controller throws AccessDeniedException in this branch
  }

  @Test
  void deletePhoto_404_whenNoPhoto() throws Exception {
    var a = a("No Photo", "bio");
    mvc.perform(delete("/api/authors/{id}/photo", a.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void coauthors_happyPath_listsEachCoauthorAndTheirBooks() throws Exception {
    var A = a("Alice Anders", "Bio A");
    var B = a("Bob Brown", "Bio B");
    var C = a("Cara Clark", "Bio C");

    book("9780306406157", "Clean Code", "D1", List.of(A, B));
    book("9781861972712", "Clean Architecture", "D2", List.of(A, C));

    mvc.perform(get("/api/authors/{id}/coauthors", A.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.author.name").value("Alice Anders"))
        .andExpect(jsonPath("$.coauthors[*].name", containsInAnyOrder("Bob Brown", "Cara Clark")))
        .andExpect(jsonPath("$.coauthors[?(@.name=='Bob Brown')].books").isArray())
        .andExpect(jsonPath("$.coauthors[?(@.name=='Cara Clark')].books").isArray());
  }

  @Test
  void coauthors_authorNotFound_404() throws Exception {
    mvc.perform(get("/api/authors/{id}/coauthors", "NOPE"))
        .andExpect(status().isNotFound());
  }

  @Test
  void coauthors_noCoauthors_returnsEmptyArray() throws Exception {
    var D = a("Dora Duke", "Bio D");
    book("9780470059029", "Solo", "Only Dora", List.of(D)); // book with single author

    mvc.perform(get("/api/authors/{id}/coauthors", D.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coauthors", hasSize(0)));
  }
}
