package pt.psoft.g1.psoftg1.newTests.domain.bookmanagement;

import org.hibernate.StaleObjectStateException;
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

    assertThrows(StaleObjectStateException.class, () -> b.applyPatch(4L, req));
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
}
