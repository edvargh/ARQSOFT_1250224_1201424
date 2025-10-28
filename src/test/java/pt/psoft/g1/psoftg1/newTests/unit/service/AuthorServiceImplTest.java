package pt.psoft.g1.psoftg1.newTests.unit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorLendingView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorMapper;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorServiceImpl;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (opaque-box) for AuthorServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AuthorServiceImplTest {

  @Mock AuthorRepository authorRepository;
  @Mock BookRepository bookRepository;
  @Mock AuthorMapper mapper;
  @Mock PhotoRepository photoRepository;
  @Mock IdGenerator idGenerator;

  @InjectMocks
  AuthorServiceImpl service;

  @Test
  void findAll_delegatesToRepo() {
    when(authorRepository.findAll()).thenReturn(List.of());
    Iterable<Author> it = service.findAll();
    assertNotNull(it);
    verify(authorRepository).findAll();
  }

  @Test
  void findByAuthorNumber_delegatesToRepo() {
    when(authorRepository.findByAuthorNumber("A1")).thenReturn(Optional.empty());
    assertTrue(service.findByAuthorNumber("A1").isEmpty());
    verify(authorRepository).findByAuthorNumber("A1");
  }

  @Test
  void findByName_delegatesToRepo() {
    when(authorRepository.findByName_NameStartsWithIgnoreCase("Jo")).thenReturn(List.of());
    assertNotNull(service.findByName("Jo"));
    verify(authorRepository).findByName_NameStartsWithIgnoreCase("Jo");
  }

  @Test
  void create_assignsId_saves_andClearsMismatchedPhotoCase_photoOnly() {
    CreateAuthorRequest req = new CreateAuthorRequest();
    MultipartFile mf = mock(MultipartFile.class);
    req.setPhoto(mf);
    req.setPhotoURI(null);

    Author author = mock(Author.class);
    when(mapper.create(req)).thenReturn(author);
    when(idGenerator.newId()).thenReturn("GEN-123");
    when(authorRepository.save(author)).thenReturn(author);

    Author result = service.create(req);

    assertSame(author, result);
    verify(idGenerator).newId();
    verify(author).assignId("GEN-123");
    verify(authorRepository).save(author);

    // request normalized
    assertNull(req.getPhoto(), "photo should be cleared");
    assertNull(req.getPhotoURI(), "photo URI should be cleared");
  }

  @Test
  void create_assignsId_saves_andClearsMismatchedPhotoCase_uriOnly() {
    CreateAuthorRequest req = new CreateAuthorRequest();
    req.setPhoto(null);
    req.setPhotoURI("file://x.png");

    Author author = mock(Author.class);
    when(mapper.create(req)).thenReturn(author);
    when(idGenerator.newId()).thenReturn("GEN-999");
    when(authorRepository.save(author)).thenReturn(author);

    Author result = service.create(req);

    assertSame(author, result);
    verify(idGenerator).newId();
    verify(author).assignId("GEN-999");
    verify(authorRepository).save(author);
    assertNull(req.getPhoto());
    assertNull(req.getPhotoURI());
  }

  @Test
  void partialUpdate_whenFound_appliesPatchAndSaves() {
    String id = "A-1";
    long desiredVersion = 7L;
    UpdateAuthorRequest patch = new UpdateAuthorRequest();

    Author author = mock(Author.class);
    when(authorRepository.findByAuthorNumber(id)).thenReturn(Optional.of(author));
    when(authorRepository.save(author)).thenReturn(author);

    Author out = service.partialUpdate(id, patch, desiredVersion);

    assertSame(author, out);
    verify(author).applyPatch(desiredVersion, patch);
    verify(authorRepository).save(author);
  }

  @Test
  void partialUpdate_whenMissing_throwsNotFound() {
    when(authorRepository.findByAuthorNumber("MISS")).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class,
        () -> service.partialUpdate("MISS", new UpdateAuthorRequest(), 1L));
    verify(authorRepository, never()).save(any());
  }

  @Test
  void findTopAuthorByLendings_usesFirstPageOfFive() {
    List<AuthorLendingView> expected = List.of(mock(AuthorLendingView.class));
    @SuppressWarnings("unchecked")
    Page<AuthorLendingView> page = (Page<AuthorLendingView>) mock(Page.class);
    when(page.getContent()).thenReturn(expected);

    ArgumentCaptor<Pageable> pageCap = ArgumentCaptor.forClass(Pageable.class);
    when(authorRepository.findTopAuthorByLendings(any())).thenReturn(page);

    List<AuthorLendingView> res = service.findTopAuthorByLendings();

    assertEquals(expected, res);
    verify(authorRepository).findTopAuthorByLendings(pageCap.capture());
    Pageable p = pageCap.getValue();
    assertEquals(0, p.getPageNumber());
    assertEquals(5, p.getPageSize());
  }

  @Test
  void findBooksByAuthorNumber_delegates() {
    when(bookRepository.findBooksByAuthorNumber("A1")).thenReturn(List.of(mock(Book.class)));
    List<Book> out = service.findBooksByAuthorNumber("A1");
    assertEquals(1, out.size());
    verify(bookRepository).findBooksByAuthorNumber("A1");
  }

  @Test
  void findCoAuthorsByAuthorNumber_delegates() {
    when(authorRepository.findCoAuthorsByAuthorNumber("A1")).thenReturn(List.of(mock(Author.class)));
    List<Author> out = service.findCoAuthorsByAuthorNumber("A1");
    assertEquals(1, out.size());
    verify(authorRepository).findCoAuthorsByAuthorNumber("A1");
  }

  @Test
  void removeAuthorPhoto_whenFound_removesPhoto_saves_andDeletesFile() {
    String id = "A-7";
    long desiredVersion = 3L;

    Photo photo = mock(Photo.class);
    when(photo.getPhotoFile()).thenReturn("p.png");

    Author author = mock(Author.class);
    when(author.getPhoto()).thenReturn(photo);

    when(authorRepository.findByAuthorNumber(id)).thenReturn(Optional.of(author));
    when(authorRepository.save(author)).thenReturn(author);

    Optional<Author> out = service.removeAuthorPhoto(id, desiredVersion);

    assertTrue(out.isPresent());
    assertSame(author, out.get());
    verify(author).removePhoto(desiredVersion);
    verify(authorRepository).save(author);
    verify(photoRepository).deleteByPhotoFile("p.png");
  }

  @Test
  void removeAuthorPhoto_whenMissing_throwsNotFound() {
    when(authorRepository.findByAuthorNumber("X")).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.removeAuthorPhoto("X", 1L));
    verify(photoRepository, never()).deleteByPhotoFile(anyString());
  }
}
