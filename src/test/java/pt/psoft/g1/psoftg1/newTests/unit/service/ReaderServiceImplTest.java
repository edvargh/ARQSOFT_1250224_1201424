package pt.psoft.g1.psoftg1.newTests.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.*;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (opaque-box) for ReaderServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ReaderServiceImplTest {

  @Mock ReaderRepository readerRepo;
  @Mock UserRepository userRepo;
  @Mock ReaderMapper readerMapper;
  @Mock GenreRepository genreRepo;
  @Mock ForbiddenNameRepository forbiddenNameRepository;
  @Mock PhotoRepository photoRepository;
  @Mock IdGenerator idGenerator;

  ReaderServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ReaderServiceImpl(
        readerRepo, userRepo, readerMapper, genreRepo,
        forbiddenNameRepository, photoRepository, idGenerator
    );
  }

  @Test
  void create_whenUsernameExists_throwsConflict() {
    CreateReaderRequest req = new CreateReaderRequest();
    req.setUsername("alice");
    req.setFullName("Alice Smith");

    when(userRepo.findByUsername("alice")).thenReturn(Optional.of(mock(Reader.class)));

    assertThrows(ConflictException.class, () -> service.create(req, null));
    verify(readerRepo, never()).save(any());
  }

  @Test
  void create_whenNameContainsForbiddenWord_throwsIllegalArgument() {
    CreateReaderRequest req = new CreateReaderRequest();
    req.setUsername("bob");
    req.setFullName("Evil Bob");

    when(userRepo.findByUsername("bob")).thenReturn(Optional.empty());
    when(forbiddenNameRepository.findByForbiddenNameIsContained("Evil"))
        .thenReturn(List.of(mock(ForbiddenName.class)));

    assertThrows(IllegalArgumentException.class, () -> service.create(req, null));
    verify(readerRepo, never()).save(any());
  }

  @Test
  void create_whenInterestContainsUnknownGenre_throwsNotFound() {
    CreateReaderRequest req = new CreateReaderRequest();
    req.setUsername("charlie");
    req.setFullName("Charlie Day");
    req.setInterestList(List.of("UnknownGenre"));

    when(userRepo.findByUsername("charlie")).thenReturn(Optional.empty());
    when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString()))
        .thenReturn(List.of()); // no forbidden words
    when(genreRepo.findByString("UnknownGenre")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.create(req, null));
  }

  @Test
  void create_normalizesPhoto_whenMismatchedPhotoAndUri() {
    CreateReaderRequest req = new CreateReaderRequest();
    req.setUsername("dana");
    req.setFullName("Dana Scully");
    MultipartFile mf = mock(MultipartFile.class);
    req.setPhoto(mf);
    req.setInterestList(List.of());

    when(userRepo.findByUsername("dana")).thenReturn(Optional.empty());
    when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString())).thenReturn(List.of());
    when(readerRepo.getCountFromCurrentYear()).thenReturn(5);

    Reader reader = mock(Reader.class);
    ReaderDetails rd = mock(ReaderDetails.class);

    when(readerMapper.createReader(req)).thenReturn(reader);
    when(readerMapper.createReaderDetails(eq(6), eq(reader), eq(req), eq(null), any()))
        .thenReturn(rd);

    when(idGenerator.newId()).thenReturn("RID-1", "RDID-2");
    when(readerRepo.save(rd)).thenReturn(rd);

    ReaderDetails out = service.create(req, null);

    assertSame(rd, out);
    verify(reader).assignId("RID-1");
    verify(rd).assignId("RDID-2");
    verify(userRepo).save(reader);
    verify(readerRepo).save(rd);
    assertNull(req.getPhoto());
  }

  @Test
  void create_happyPath_assignsIds_mapsInterests_andSaves() {
    CreateReaderRequest req = new CreateReaderRequest();
    req.setUsername("ed");
    req.setFullName("Ed Green");
    req.setInterestList(List.of("Sci-Fi", "Fantasy"));

    when(userRepo.findByUsername("ed")).thenReturn(Optional.empty());
    when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString())).thenReturn(List.of());
    Genre g1 = mock(Genre.class);
    Genre g2 = mock(Genre.class);
    when(genreRepo.findByString("Sci-Fi")).thenReturn(Optional.of(g1));
    when(genreRepo.findByString("Fantasy")).thenReturn(Optional.of(g2));

    when(readerRepo.getCountFromCurrentYear()).thenReturn(9);

    Reader reader = mock(Reader.class);
    ReaderDetails rd = mock(ReaderDetails.class);

    when(readerMapper.createReader(req)).thenReturn(reader);
    when(readerMapper.createReaderDetails(eq(10), eq(reader), eq(req), eq("uri://photo"), any()))
        .thenReturn(rd);

    when(idGenerator.newId()).thenReturn("RID-10", "RDID-11");
    when(readerRepo.save(rd)).thenReturn(rd);

    ReaderDetails out = service.create(req, "uri://photo");

    assertSame(rd, out);
    verify(reader).assignId("RID-10");
    verify(rd).assignId("RDID-11");
    verify(userRepo).save(reader);
    verify(readerRepo).save(rd);

    verify(genreRepo).findByString("Sci-Fi");
    verify(genreRepo).findByString("Fantasy");
  }

  @Test
  void findTopByGenre_whenStartAfterEnd_throwsIllegalArgument() {
    LocalDate s = LocalDate.of(2024, 2, 1);
    LocalDate e = LocalDate.of(2024, 1, 1);
    assertThrows(IllegalArgumentException.class, () -> service.findTopByGenre("Sci-Fi", s, e));
  }

  @Test
  void findTopByGenre_usesFirstPageOfFive_andDelegates() {
    List<ReaderBookCountDTO> expected = List.of(mock(ReaderBookCountDTO.class));
    @SuppressWarnings("unchecked")
    Page<ReaderBookCountDTO> page = (Page<ReaderBookCountDTO>) mock(Page.class);
    when(page.getContent()).thenReturn(expected);

    ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
    when(readerRepo.findTopByGenre(any(), eq("Sci-Fi"),
        eq(LocalDate.of(2024,1,1)), eq(LocalDate.of(2024,12,31)))).thenReturn(page);

    List<ReaderBookCountDTO> out = service.findTopByGenre("Sci-Fi",
        LocalDate.of(2024,1,1), LocalDate.of(2024,12,31));

    assertEquals(expected, out);
    verify(readerRepo).findTopByGenre(cap.capture(),
        eq("Sci-Fi"), eq(LocalDate.of(2024,1,1)), eq(LocalDate.of(2024,12,31)));
    Pageable p = cap.getValue();
    assertEquals(0, p.getPageNumber());
    assertEquals(5, p.getPageSize());
  }

  @Test
  void update_whenMissing_throwsNotFound() {
    when(readerRepo.findById("X")).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class,
        () -> service.update("X", new UpdateReaderRequest(), 1L, null));
    verify(readerRepo, never()).save(any());
  }

  @Test
  void update_normalizesPhoto_whenMismatched() {
    UpdateReaderRequest req = new UpdateReaderRequest();
    MultipartFile mf = mock(MultipartFile.class);
    req.setPhoto(mf);
    req.setInterestList(List.of());

    ReaderDetails rd = mock(ReaderDetails.class);
    Reader userReader = mock(Reader.class);
    when(rd.getReader()).thenReturn(userReader);

    when(readerRepo.findById("ID-1")).thenReturn(Optional.of(rd));
    when(readerRepo.save(rd)).thenReturn(rd);

    ReaderDetails out = service.update("ID-1", req, 2L, null);

    assertSame(rd, out);
    verify(rd).applyPatch(eq(2L), eq(req), isNull(), anyList());
    verify(userRepo).save(userReader);
    verify(readerRepo).save(rd);
    assertNull(req.getPhoto(), "photo should be cleared when URI missing");
  }

  @Test
  void update_mapsInterests_andSaves() {
    UpdateReaderRequest req = new UpdateReaderRequest();
    req.setInterestList(List.of("Mystery"));

    ReaderDetails rd = mock(ReaderDetails.class);
    Reader userReader = mock(Reader.class);
    when(rd.getReader()).thenReturn(userReader);
    when(readerRepo.findById("ID-2")).thenReturn(Optional.of(rd));
    when(readerRepo.save(rd)).thenReturn(rd);

    Genre g = mock(Genre.class);
    when(genreRepo.findByString("Mystery")).thenReturn(Optional.of(g));

    ReaderDetails out = service.update("ID-2", req, 5L, "uri://x");

    assertSame(rd, out);
    verify(genreRepo).findByString("Mystery");
    verify(rd).applyPatch(5L, req, "uri://x", List.of(g));
    verify(userRepo).save(userReader);
    verify(readerRepo).save(rd);
  }

  @Test
  void findByReaderNumber_delegates() {
    when(readerRepo.findByReaderNumber("R1")).thenReturn(Optional.empty());
    assertTrue(service.findByReaderNumber("R1").isEmpty());
    verify(readerRepo).findByReaderNumber("R1");
  }

  @Test
  void findByPhoneNumber_delegates() {
    when(readerRepo.findByPhoneNumber("123")).thenReturn(List.of());
    assertNotNull(service.findByPhoneNumber("123"));
    verify(readerRepo).findByPhoneNumber("123");
  }

  @Test
  void findByUsername_delegates() {
    when(readerRepo.findByUsername("alice")).thenReturn(Optional.empty());
    assertTrue(service.findByUsername("alice").isEmpty());
    verify(readerRepo).findByUsername("alice");
  }

  @Test
  void findAll_delegates() {
    when(readerRepo.findAll()).thenReturn(List.of());
    assertNotNull(service.findAll());
    verify(readerRepo).findAll();
  }

  @Test
  void findTopReaders_whenMinTopLessThan1_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> service.findTopReaders(0));
  }

  @Test
  void findTopReaders_usesFirstPageOfMinTop() {
    List<ReaderDetails> expected = List.of(mock(ReaderDetails.class));
    @SuppressWarnings("unchecked")
    Page<ReaderDetails> page = (Page<ReaderDetails>) mock(Page.class);
    when(page.getContent()).thenReturn(expected);

    ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
    when(readerRepo.findTopReaders(any())).thenReturn(page);

    List<ReaderDetails> out = service.findTopReaders(7);

    assertEquals(expected, out);
    verify(readerRepo).findTopReaders(cap.capture());
    Pageable p = cap.getValue();
    assertEquals(0, p.getPageNumber());
    assertEquals(7, p.getPageSize());
  }

  @Test
  void removeReaderPhoto_whenMissing_throwsNotFound() {
    when(readerRepo.findByReaderNumber("X")).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.removeReaderPhoto("X", 1L));
    verify(photoRepository, never()).deleteByPhotoFile(anyString());
  }

  @Test
  void removeReaderPhoto_whenFound_removesPhoto_saves_andDeletesFile() {
    String id = "R-9";
    long desiredVersion = 4L;

    Photo photo = mock(Photo.class);
    when(photo.getPhotoFile()).thenReturn("p.png");

    ReaderDetails rd = mock(ReaderDetails.class);
    when(rd.getPhoto()).thenReturn(photo);

    when(readerRepo.findByReaderNumber(id)).thenReturn(Optional.of(rd));
    when(readerRepo.save(rd)).thenReturn(rd);

    Optional<ReaderDetails> out = service.removeReaderPhoto(id, desiredVersion);

    assertTrue(out.isPresent());
    assertSame(rd, out.get());
    verify(rd).removePhoto(desiredVersion);
    verify(readerRepo).save(rd);
    verify(photoRepository).deleteByPhotoFile("p.png");
  }

  @Test
  void searchReaders_whenPageNull_defaultsPage() {
    SearchReadersQuery q = new SearchReadersQuery("R1", "Alice", "123");
    when(readerRepo.searchReaderDetails(any(), any())).thenReturn(List.of(mock(ReaderDetails.class)));

    List<ReaderDetails> out = service.searchReaders(null, q);

    assertNotNull(out);
    ArgumentCaptor<pt.psoft.g1.psoftg1.shared.services.Page> pageCap =
        ArgumentCaptor.forClass(pt.psoft.g1.psoftg1.shared.services.Page.class);
    verify(readerRepo).searchReaderDetails(pageCap.capture(), eq(q));
    assertNotNull(pageCap.getValue(), "Default Page should be created when null is passed");
  }

  @Test
  void searchReaders_whenQueryNull_buildsDefaultAndDelegates() {
    pt.psoft.g1.psoftg1.shared.services.Page page =
        new pt.psoft.g1.psoftg1.shared.services.Page(1, 10);
    when(readerRepo.searchReaderDetails(eq(page), any())).thenReturn(List.of(mock(ReaderDetails.class)));

    List<ReaderDetails> out = service.searchReaders(page, null);

    assertNotNull(out);
    verify(readerRepo).searchReaderDetails(eq(page), any(SearchReadersQuery.class));
  }

  @Test
  void searchReaders_whenRepoReturnsEmpty_throwsNotFound() {
    pt.psoft.g1.psoftg1.shared.services.Page page =
        new pt.psoft.g1.psoftg1.shared.services.Page(1, 10);
    when(readerRepo.searchReaderDetails(eq(page), any())).thenReturn(new ArrayList<>());

    assertThrows(NotFoundException.class, () -> service.searchReaders(page, new SearchReadersQuery("","","")));
  }
}
