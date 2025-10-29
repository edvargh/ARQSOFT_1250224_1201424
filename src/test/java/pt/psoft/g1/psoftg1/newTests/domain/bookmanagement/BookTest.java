package pt.psoft.g1.psoftg1.newTests.domain.bookmanagement;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.services.UpdateBookRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional transparent-box tests for Book.
 */
class BookTest {

  private Genre genre;
  private Author author1;
  private Author author2;

  @BeforeEach
  void setUp() {
    genre = mock(Genre.class);
    author1 = mock(Author.class);
    author2 = mock(Author.class);
  }

  private static void setVersion(Book book, long version) {
    try {
      Field f = Book.class.getDeclaredField("version");
      f.setAccessible(true);
      f.set(book, version);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Test
  void constructor_valid_minimal_buildsAggregate() {
    Book b = new Book("9780306406157", "Clean Code", "Great book",
        genre, List.of(author1), null);

    assertNotNull(b);
    assertEquals("9780306406157", b.getIsbn());
    assertEquals("Great book", b.getDescription());
    assertNotNull(b.getTitle());
    assertEquals(genre, b.getGenre());
    assertEquals(1, b.getAuthors().size());
  }

  @Test
  void constructor_nullGenre_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        new Book("9780306406157", "Clean Code", "x", null, List.of(author1), null));
    assertTrue(ex.getMessage().toLowerCase().contains("genre"));
  }

  @Test
  void constructor_nullAuthors_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        new Book("9780306406157", "Clean Code", "x", genre, null, null));
    assertTrue(ex.getMessage().toLowerCase().contains("author"));
  }

  @Test
  void constructor_emptyAuthors_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        new Book("9780306406157", "Clean Code", "x", genre, List.of(), null));
    assertTrue(ex.getMessage().toLowerCase().contains("empty"));
  }

  @Test
  void constructor_invalidIsbn_bubblesUpFromValueObject() {
    assertThrows(IllegalArgumentException.class, () ->
        new Book("BAD-ISBN", "Clean Code", "x", genre, List.of(author1), null));
  }


  @Test
  void assignPk_setsPk() {
    Book b = new Book("9780306406157", "Clean Code", "x",
        genre, List.of(author1), null);
    b.assignPk("id-123");
    assertEquals("id-123", b.getPk());
  }

  @Test
  void assignPk_blank_throws() {
    Book b = new Book("9780306406157", "Clean Code", "x",
        genre, List.of(author1), null);
    assertThrows(IllegalArgumentException.class, () -> b.assignPk("  "));
    assertThrows(IllegalArgumentException.class, () -> b.assignPk(null));
  }


  @Test
  void removePhoto_versionMismatch_throwsConflictException() {
    Book b = new Book("9780306406157", "Clean Code", "x",
        genre, List.of(author1), "photo://u/1");
    setVersion(b, 3L);

    assertThrows(ConflictException.class, () -> b.removePhoto(2L));
  }


  @Test
  void applyPatch_versionMismatch_throwsStale() {
    Book b = new Book("9780306406157", "Clean Code", "x",
        genre, List.of(author1), null);
    setVersion(b, 5L);

    UpdateBookRequest req = mock(UpdateBookRequest.class);
    when(req.getTitle()).thenReturn("New Title");

    OptimisticLockException ex =
        assertThrows(OptimisticLockException.class, () -> b.applyPatch(4L, req));
  }

  @Test
  void applyPatch_updatesOnlyProvidedFields() {
    Book b = new Book("9780306406157", "Clean Code", "Desc",
        genre, List.of(author1), null);
    setVersion(b, 7L);

    Genre newGenre = mock(Genre.class);
    List<Author> newAuthors = List.of(author1, author2);

    UpdateBookRequest req = mock(UpdateBookRequest.class);
    when(req.getTitle()).thenReturn("Even Cleaner Code");
    when(req.getDescription()).thenReturn("Updated desc");
    when(req.getGenreObj()).thenReturn(newGenre);
    when(req.getAuthorObjList()).thenReturn(newAuthors);
    when(req.getPhotoURI()).thenReturn(null);

    assertDoesNotThrow(() -> b.applyPatch(7L, req));

    assertEquals("Even Cleaner Code", b.getTitle().toString());
    assertEquals("Updated desc", b.getDescription());
    assertEquals(newGenre, b.getGenre());
    assertEquals(newAuthors, b.getAuthors());
  }

  @Test
  void applyPatch_withNulls_changesNothingForThoseFields() {
    Book b = new Book("9780306406157", "T1", "D1",
        genre, List.of(author1), null);
    setVersion(b, 1L);

    UpdateBookRequest req = mock(UpdateBookRequest.class);
    when(req.getTitle()).thenReturn(null);
    when(req.getDescription()).thenReturn(null);
    when(req.getGenreObj()).thenReturn(null);
    when(req.getAuthorObjList()).thenReturn(null);
    when(req.getPhotoURI()).thenReturn(null);

    b.applyPatch(1L, req);

    assertEquals("T1", b.getTitle().toString());
    assertEquals("D1", b.getDescription());
    assertEquals(genre, b.getGenre());
    assertEquals(1, b.getAuthors().size());
  }

  @Test
  void removePhoto_versionMatches_clearsPhoto() {
    Book b = new Book("9780306406157", "T", "D", genre, List.of(author1), "pics/cover.png");
    setVersion(b, 2L);

    b.removePhoto(2L);

    assertNull(b.getPhoto(), "Photo should be cleared when version matches");
  }

  @Test
  void applyPatch_setsPhoto_whenPhotoUriProvided() {
    Book b = new Book("9780306406157", "T", "D", genre, List.of(author1), null);
    setVersion(b, 10L);

    UpdateBookRequest req = mock(UpdateBookRequest.class);
    when(req.getTitle()).thenReturn(null);
    when(req.getDescription()).thenReturn(null);
    when(req.getGenreObj()).thenReturn(null);
    when(req.getAuthorObjList()).thenReturn(null);
    when(req.getPhotoURI()).thenReturn("images/new.jpg");

    b.applyPatch(10L, req);

    assertNotNull(b.getPhoto());
    assertTrue(b.getPhoto().getPhotoFile().endsWith("images/new.jpg") ||
            b.getPhoto().getPhotoFile().contains("images") ,
        "Photo file should reflect provided URI");
  }

  @Test
  void applyPatch_allowsUpdatingAuthorsToEmptyList() {
    Book b = new Book("9780306406157", "T", "D", genre, List.of(author1, author2), null);
    setVersion(b, 7L);

    UpdateBookRequest req = mock(UpdateBookRequest.class);
    when(req.getAuthorObjList()).thenReturn(List.of());
    b.applyPatch(7L, req);

    assertEquals(0, b.getAuthors().size(), "Patch should accept an empty author list");
  }

  @Test
  void applyPatch_whenBothVersionsNull_allowsPatch() throws Exception {
    Book b = new Book("9780306406157", "T", "D", genre, List.of(author1), null);
    Field f = Book.class.getDeclaredField("version");
    f.setAccessible(true);
    f.set(b, null);

    UpdateBookRequest req = mock(UpdateBookRequest.class);
    when(req.getTitle()).thenReturn("T2");

    b.applyPatch(null, req);
    assertEquals("T2", b.getTitle().toString());
  }

  @Test
  void constructor_nullDescription_keepsFieldNull_andGetDescriptionThrows() {
    Book b = new Book("9780306406157", "T", null, genre, List.of(author1), null);
    assertThrows(NullPointerException.class, b::getDescription);
  }

  @Test
  void applyPatch_updatesMultipleFieldsTogether() {
    Book b = new Book("9780306406157", "T", "D", genre, List.of(author1), null);
    setVersion(b, 3L);
    Genre g2 = mock(Genre.class);

    UpdateBookRequest req = mock(UpdateBookRequest.class);
    when(req.getTitle()).thenReturn("T2");
    when(req.getDescription()).thenReturn("D2");
    when(req.getGenreObj()).thenReturn(g2);
    when(req.getPhotoURI()).thenReturn("pics/p.png");

    b.applyPatch(3L, req);

    assertEquals("T2", b.getTitle().toString());
    assertEquals("D2", b.getDescription());
    assertEquals(g2, b.getGenre());
    assertNotNull(b.getPhoto());
  }

  @Test
  void constructor_withPhoto_setsPhoto() {
    Book b = new Book(
        "9780306406157", "Clean Code", "Great book",
        genre, List.of(author1), "covers/clean-code.jpg");

    assertNotNull(b.getPhoto(), "constructor should set photo when URI is provided");
    assertTrue(b.getPhoto().getPhotoFile().contains("covers"),
        "photo file should reflect the URI");
  }

  @Test
  void applyPatch_photoUriNull_keepsExistingPhoto() {
    Book b = new Book(
        "9780306406157", "T", "D",
        genre, List.of(author1), "existing/p.png");
    setVersion(b, 9L);

    UpdateBookRequest req = mock(UpdateBookRequest.class);
    when(req.getTitle()).thenReturn(null);
    when(req.getDescription()).thenReturn(null);
    when(req.getGenreObj()).thenReturn(null);
    when(req.getAuthorObjList()).thenReturn(null);
    when(req.getPhotoURI()).thenReturn(null);

    var originalPhoto = b.getPhoto();

    b.applyPatch(9L, req);

    assertNotNull(b.getPhoto(), "existing photo should remain when photoURI is null");
    assertSame(originalPhoto, b.getPhoto(), "photo reference should be unchanged");
  }

}
