package pt.psoft.g1.psoftg1.newTests.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.external.service.ApiNinjasService;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.newTests.testutils.SqlBackedITBase;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderNumber;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Opaque-box integration tests for ReaderController against a real SQL backend.
 * Controller → Service → Repos → Domain are all real.
 * FileStorageService and ApiNinjasService are stubbed with @MockBean.
 */
@WithMockUser(roles = "READER")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","sql"})
@Transactional
class ReaderControllerSqlIT extends SqlBackedITBase {

  @Autowired MockMvc mvc;

  @Autowired GenreRepository genreRepo;
  @Autowired AuthorRepository authorRepo;
  @Autowired BookRepository bookRepo;
  @Autowired UserRepository userRepo;
  @Autowired ReaderRepository readerRepo;
  @Autowired IdGenerator idGen;

  @MockBean FileStorageService fileStorage;
  @MockBean ApiNinjasService apiNinjas;

  private Genre tech;

  @BeforeEach
  void setUp() {
    tech = new Genre("Tech");
    tech.assignPk(idGen.newId());
    tech = genreRepo.save(tech);

    when(fileStorage.getRequestPhoto(any())).thenReturn(null);
    when(fileStorage.getExtension(any())).thenReturn(Optional.of("png"));
    when(fileStorage.getFile(any())).thenReturn("IMG".getBytes(StandardCharsets.UTF_8));
    doNothing().when(fileStorage).deleteFile(any());

    when(apiNinjas.getRandomEventFromYearMonth(anyInt(), anyInt()))
        .thenReturn("Some fun historical tidbit");
  }

  // -------- helpers ------------------------


  private Reader persistReaderUser(String username, String fullName) {
    var r = Reader.newReader(username, "pwd", fullName);
    r.assignId(idGen.newId());
    return userRepo.save(r);
  }

  private Librarian persistLibrarianUser(String username, String fullName) {
    var l = Librarian.newLibrarian(username, "pwd", fullName);
    l.assignId(idGen.newId());
    return userRepo.save(l);
  }

  private ReaderDetails persistReaderDetails(Reader readerUser, int seqNumber) {
    readerUser = userRepo.save(readerUser);
    var rd = new ReaderDetails(
        seqNumber,
        readerUser,
        "2000-01-01",
        "912345678",
        true,
        false,
        false,
        null,
        List.of()
    );
    rd.assignId(idGen.newId());
    try {
      var field = ReaderDetails.class.getDeclaredField("readerNumber");
      field.setAccessible(true);
      field.set(rd, new ReaderNumber(LocalDate.now().getYear(), seqNumber));
    } catch (Exception e) {
      throw new RuntimeException("Failed to set readerNumber via reflection", e);
    }
    return readerRepo.save(rd);
  }

  private String sub(User u) {
    return u.getId() + "," + u.getUsername();
  }

  private RequestPostProcessor asReader(User u) {
    return jwt()
        .jwt(j -> j.claim("sub", sub(u)))
        .authorities(new SimpleGrantedAuthority("ROLE_READER"));
  }

  private RequestPostProcessor asLibrarian(User u) {
    return jwt()
        .jwt(j -> j.claim("sub", sub(u)))
        .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
  }

  // ------------------- tests ----------------------------------------------

  @Test
  void getData_200_asReader_returnsOwnDataWithETag() throws Exception {
    var user = persistReaderUser("reader@example.com", "R Example");
    var rd = persistReaderDetails(user, 11111);

    mvc.perform(get("/api/readers")
            .with(asReader(user)))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"" + rd.getVersion() + "\""))
        .andExpect(jsonPath("$.readerNumber").value(rd.getReaderNumber()))
        .andExpect(jsonPath("$.fullName", not(emptyString())));
  }


  @Test
  void getData_200_asLibrarian_returnsAll() throws Exception {
    var r1 = persistReaderUser("r1@example.com", "R1");
    persistReaderDetails(r1, 20001);
    var r2 = persistReaderUser("r2@example.com", "R2");
    persistReaderDetails(r2, 20002);

    var lib = persistLibrarianUser("lib@example.com", "Lib");

    mvc.perform(get("/api/readers")
            .with(asLibrarian(lib)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
  }

  @Test
  void findByReaderNumber_404_whenMissing() throws Exception {
    var lib = persistLibrarianUser("lib-find-missing@example.com", "LF");
    mvc.perform(get("/api/readers/{year}/{seq}", 2099, 999)
            .with(asLibrarian(lib)))
        .andExpect(status().isNotFound());
  }

  @Test
  void findByReaderNumber_200_returnsQuoteView_andETag() throws Exception {
    var user = persistReaderUser("q@example.com", "Quoted");
    var rd = persistReaderDetails(user, 12345);
    var parts = rd.getReaderNumber().split("/");
    String year = parts[0], seq = parts[1];

    var lib = persistLibrarianUser("lib-find-ok@example.com", "LF2");
    mvc.perform(get("/api/readers/{year}/{seq}", year, seq)
            .with(asLibrarian(lib)))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"" + rd.getVersion() + "\""))
        .andExpect(jsonPath("$.readerNumber").value(rd.getReaderNumber()))
        .andExpect(jsonPath("$.quote", not(emptyString())));
  }

  @Test
  void findByPhoneNumber_404_whenEmpty() throws Exception {
    mvc.perform(get("/api/readers").param("phoneNumber", "000000000"))
        .andExpect(status().isNotFound());
  }

  @Test
  void findByPhoneNumber_200_returnsItems() throws Exception {
    var u = persistReaderUser("phone@example.com", "Phone R");
    var rd = persistReaderDetails(u, 30001);
    mvc.perform(get("/api/readers").param("phoneNumber", "912345678"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].readerNumber", hasItem(rd.getReaderNumber())));
  }

  @Test
  void findByReaderName_200_asLibrarian() throws Exception {
    var u   = persistReaderUser("name@example.com", "John Finder");
    var rd  = persistReaderDetails(u, 40001);
    var lib = persistLibrarianUser("lib2@example.com", "Lib Two");

    mvc.perform(get("/api/readers").param("name", "John")
            .with(
                jwt()
                    .jwt(j -> j.claim("sub", sub(lib)))
                    .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
            ))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].readerNumber", hasItem(rd.getReaderNumber())));
  }

  @Test
  void getSpecificReaderPhoto_404_whenNoPhoto() throws Exception {
    var reader = persistReaderUser("p1@example.com", "P1");
    var rd = persistReaderDetails(reader, 50001);
    var parts = rd.getReaderNumber().split("/");
    String year = parts[0], seq = parts[1];

    var lib = persistLibrarianUser("lib3@example.com", "Lib Three");

    mvc.perform(get("/api/readers/{year}/{seq}/photo", year, seq)
            .with(asLibrarian(lib)))
        .andExpect(status().isNotFound());
  }

  @Test
  void getReaderOwnPhoto_404_whenNoPhoto() throws Exception {
    var reader = persistReaderUser("self@example.com", "Self R");
    persistReaderDetails(reader, 50002);

    mvc.perform(get("/api/readers/photo")
            .with(asReader(reader)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteReaderPhoto_404_whenNoPhoto() throws Exception {
    var reader = persistReaderUser("delnp@example.com", "No Photo");
    persistReaderDetails(reader, 70001);

    mvc.perform(delete("/api/readers/photo")
            .with(asReader(reader)))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateReader_400_withoutIfMatch() throws Exception {
    var reader = persistReaderUser("upd@example.com", "Upd R");
    persistReaderDetails(reader, 71001);

    mvc.perform(patch("/api/readers")
            .with(jwt()
                .jwt(j -> j.claim("sub", sub(reader)))
                .authorities(new SimpleGrantedAuthority("ROLE_READER")))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.detail",
            containsString("You must issue a conditional PATCH")));
  }

  @Test
  void createReader_201_multipart_setsETag_andLocation() throws Exception {
    when(fileStorage.getRequestPhoto(any())).thenReturn("reader-uuid.png");

    MockMultipartFile photo = new MockMultipartFile(
        "photo", "reader.png", "image/png", new byte[]{(byte)137, 80, 78, 71});

    mvc.perform(multipart("/api/readers")
            .file(photo)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .param("username", "newreader@example.com")
            .param("password", "Secret123!")
            .param("fullName", "New Reader")
            .param("phoneNumber", "933333333")
            .param("birthDate", "1998-01-02")
            .param("gdpr", "true")
        )
        .andExpect(status().isCreated())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(header().string("Location", containsString("/api/readers/")))
        .andExpect(jsonPath("$.email", is("newreader@example.com")));
  }



  @Test
  void getReaderLendings_403_readerAccessingAnotherReader() throws Exception {
    var owner = persistReaderUser("owner@example.com", "Owner");
    var ownerDetails = persistReaderDetails(owner, 72001);

    var other = persistReaderUser("other@example.com", "Other");
    persistReaderDetails(other, 72002);

    var parts = ownerDetails.getReaderNumber().split("/");
    String year = parts[0], seq = parts[1];

    mvc.perform(get("/api/readers/{year}/{seq}/lendings", year, seq)
            .param("isbn", "9780000000000")
            .with(asReader(other)))
        .andExpect(status().isForbidden());
  }


  @Test
  void getReaderLendings_404_librarianButNoLendings() throws Exception {
    var owner = persistReaderUser("owner2@example.com", "Owner2");
    var ownerDetails = persistReaderDetails(owner, 73001);

    var parts = ownerDetails.getReaderNumber().split("/");
    String year = parts[0], seq = parts[1];

    mvc.perform(get("/api/readers/{year}/{seq}/lendings", year, seq)
            .param("isbn", "9780000000001")
            .with(asReader(owner)))
        .andExpect(status().isNotFound());
  }

  @WithMockUser(roles = "LIBRARIAN")
  @Test
  void top5_200_ok() throws Exception {
    mvc.perform(get("/api/readers/top5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").exists());
  }

  @WithMockUser(roles = "LIBRARIAN")
  @Test
  void top5ByGenre_404_whenNoData() throws Exception {
    mvc.perform(get("/api/readers/top5ByGenre")
            .param("genre", "Tech")
            .param("startDate", "2024-01-01")
            .param("endDate", "2024-12-31"))
        .andExpect(status().isNotFound());
  }

  @WithMockUser(roles = "LIBRARIAN")
  @Test
  void searchReaders_200_emptyOk() throws Exception {
    String body = """
      { "page": { "number": 1, "limit": 10 }, "query": { } }
      """;
    mvc.perform(post("/api/readers/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Not found"))
        .andExpect(jsonPath("$.details[0]").value("No results match the search query"));
  }
}
