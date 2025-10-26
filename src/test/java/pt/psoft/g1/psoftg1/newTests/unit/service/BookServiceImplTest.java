package pt.psoft.g1.psoftg1.newTests.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.*;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.*;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (opaque-box) for BookServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

  @Mock private BookRepository bookRepository;
  @Mock private GenreRepository genreRepository;
  @Mock private AuthorRepository authorRepository;
  @Mock private PhotoRepository photoRepository;
  @Mock private ReaderRepository readerRepository;
  @Mock private IdGenerator idGenerator;

  @InjectMocks
  private BookServiceImpl service;

  // Valid ISBN-13s
  private static final String I1 = "9780132350884";
  private static final String I2 = "9780134685991";
  private static final String I3 = "9780306406157";

  @BeforeEach
  void tuneProperties() {
    ReflectionTestUtils.setField(service, "suggestionsLimitPerGenre", 2L);
  }

  @Test
  void create_ok_assignsPk_andSaves() {
    CreateBookRequest req = new CreateBookRequest();
    req.setTitle("Clean Code");
    req.setDescription("desc");
    req.setGenre("Software");
    req.setAuthors(List.of("A1","A2"));
    req.setPhotoURI("cover.png");

    when(bookRepository.findByIsbn("9780132350884")).thenReturn(Optional.empty());
    when(genreRepository.findByString("Software"))
        .thenReturn(Optional.of(new Genre("Software")));
    when(authorRepository.findByAuthorNumber("A1"))
        .thenReturn(Optional.of(mock(Author.class)));
    when(authorRepository.findByAuthorNumber("A2"))
        .thenReturn(Optional.of(mock(Author.class)));
    when(idGenerator.newId()).thenReturn("PK123");
    when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

    Book saved = service.create(req, "9780132350884");

    assertEquals("9780132350884", saved.getIsbn());
    verify(idGenerator, times(1)).newId();
    verify(bookRepository, times(1)).save(any(Book.class));
  }

  @Test
  void create_conflict_existingIsbn_throws() {
    when(bookRepository.findByIsbn("9780132350884")).thenReturn(Optional.of(mock(Book.class)));
    assertThrows(ConflictException.class,
        () -> service.create(new CreateBookRequest(), "9780132350884"));
    verify(bookRepository, never()).save(any());
  }

  @Test
  void create_genreNotFound_throws() {
    CreateBookRequest req = new CreateBookRequest();
    req.setTitle("T");
    req.setDescription("D");
    req.setGenre("Nope");
    req.setAuthors(List.of());
    req.setPhotoURI(null);

    when(bookRepository.findByIsbn(I1)).thenReturn(Optional.empty());
    when(genreRepository.findByString("Nope")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.create(req, I1));
    verify(bookRepository, never()).save(any());
  }

  @Test
  void update_ok_replacesAuthors_andGenre_andSaves() {
    Genre oldGenre = new Genre("Old");
    Book existing = new Book(I1, "t", "d", oldGenre, List.of(mock(Author.class)), null);

    ReflectionTestUtils.setField(existing, "version", 1L);

    when(bookRepository.findByIsbn(I1)).thenReturn(Optional.of(existing));

    UpdateBookRequest req = new UpdateBookRequest();
    req.setIsbn(I1);
    req.setAuthors(List.of("A1"));
    req.setGenre("New");

    Author a1 = mock(Author.class);
    when(authorRepository.findByAuthorNumber("A1")).thenReturn(Optional.of(a1));
    when(genreRepository.findByString("New")).thenReturn(Optional.of(new Genre("New")));
    when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

    Book out = service.update(req, "1");

    assertNotNull(out);
    verify(bookRepository).save(existing);
  }

  @Test
  void update_genreProvidedButMissing_throws() {
    Book existing = mock(Book.class);
    when(bookRepository.findByIsbn("ISBN")).thenReturn(Optional.of(existing));

    UpdateBookRequest req = new UpdateBookRequest();
    req.setIsbn("ISBN");
    req.setGenre("Ghost");

    when(genreRepository.findByString("Ghost")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.update(req, "7"));
    verify(bookRepository, never()).save(any());
  }

  @Test
  void save_delegates() {
    Book b = mock(Book.class);
    when(bookRepository.save(b)).thenReturn(b);
    assertSame(b, service.save(b));
    verify(bookRepository).save(b);
  }

  @Test
  void findTop5BooksLent_delegates() {
    var page = new PageImpl<>(List.of(mock(BookCountDTO.class)));
    when(bookRepository.findTop5BooksLent(any(), any(Pageable.class))).thenReturn(page);

    var list = service.findTop5BooksLent();

    assertEquals(1, list.size());
    verify(bookRepository).findTop5BooksLent(any(), any(Pageable.class));
  }

  @Test
  void findByGenre_delegates() {
    when(bookRepository.findByGenre("Thriller")).thenReturn(List.of(mock(Book.class)));
    assertEquals(1, service.findByGenre("Thriller").size());
  }

  @Test
  void findByTitle_delegates() {
    when(bookRepository.findByTitle("Title")).thenReturn(List.of(mock(Book.class)));
    assertEquals(1, service.findByTitle("Title").size());
  }

  @Test
  void findByAuthorName_delegates_withWildcard() {
    when(bookRepository.findByAuthorName("King%")).thenReturn(List.of(mock(Book.class)));
    assertEquals(1, service.findByAuthorName("King").size());
  }

  @Test
  void findByIsbn_found_returns() {
    Book b = new Book(I1, "t", "d", new Genre("g"), List.of(mock(Author.class)), null);
    when(bookRepository.findByIsbn(I1)).thenReturn(Optional.of(b));
    assertSame(b, service.findByIsbn(I1));
  }

  @Test
  void findByIsbn_missing_throws() {
    when(bookRepository.findByIsbn("MISS")).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.findByIsbn("MISS"));
  }

  @Test
  void removeBookPhoto_ok_removesAndDeletesFromRepo() {
    Book book = mock(Book.class);
    Photo photo = new Photo(Path.of("photo.png"));
    when(book.getPhoto()).thenReturn(photo);
    when(bookRepository.findByIsbn("I1")).thenReturn(Optional.of(book));
    when(bookRepository.save(book)).thenReturn(book);

    Book updated = service.removeBookPhoto("I1", 3L);

    assertSame(book, updated);
    verify(book).removePhoto(3L);
    verify(photoRepository).deleteByPhotoFile("photo.png");
    verify(bookRepository).save(book);
  }

  @Test
  void removeBookPhoto_noPhoto_throws() {
    Book book = mock(Book.class);
    when(book.getPhoto()).thenReturn(null);
    when(bookRepository.findByIsbn("I1")).thenReturn(Optional.of(book));

    assertThrows(NotFoundException.class, () -> service.removeBookPhoto("I1", 1L));
    verify(photoRepository, never()).deleteByPhotoFile(any());
  }

  @Test
  void getBooksSuggestions_readerNoInterests_throws() {
    ReaderDetails rd = mock(ReaderDetails.class);
    when(rd.getInterestList()).thenReturn(List.of());
    when(readerRepository.findByReaderNumber("R1")).thenReturn(Optional.of(rd));

    assertThrows(NotFoundException.class, () -> service.getBooksSuggestionsForReader("R1"));
  }

  @Test
  void getBooksSuggestions_respectsLimitAndSkipsEmptyGenres() {
    ReflectionTestUtils.setField(service, "suggestionsLimitPerGenre", 2L);

    Genre g1 = new Genre("G1");
    Genre g2 = new Genre("G2");

    ReaderDetails rd = mock(ReaderDetails.class);
    when(rd.getInterestList()).thenReturn(List.of(g1, g2));
    when(readerRepository.findByReaderNumber("R1")).thenReturn(Optional.of(rd));

    List<Book> g1Books = List.of(
        new Book(I1, "t", "d", g1, List.of(mock(Author.class)), null),
        new Book(I2, "t", "d", g1, List.of(mock(Author.class)), null),
        new Book(I3, "t", "d", g1, List.of(mock(Author.class)), null)
    );
    when(bookRepository.findByGenre("G1")).thenReturn(g1Books);
    when(bookRepository.findByGenre("G2")).thenReturn(List.of());

    List<Book> out = service.getBooksSuggestionsForReader("R1");

    assertEquals(2, out.size());
  }

  @Test
  void searchBooks_defaultsPageAndQuery_whenNulls() {
    pt.psoft.g1.psoftg1.shared.services.Page page =
        new pt.psoft.g1.psoftg1.shared.services.Page(1, 10);
    SearchBooksQuery q = new SearchBooksQuery("", "", "");
    when(bookRepository.searchBooks(eq(page), eq(q))).thenReturn(List.of());

    List<Book> list = service.searchBooks(null, null);

    assertNotNull(list);
    verify(bookRepository).searchBooks(eq(page), eq(q));
  }
}
