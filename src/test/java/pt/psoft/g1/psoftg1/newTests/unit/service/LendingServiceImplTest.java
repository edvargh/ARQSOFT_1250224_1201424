package pt.psoft.g1.psoftg1.newTests.unit.service;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingServiceImpl;
import pt.psoft.g1.psoftg1.lendingmanagement.services.SearchLendingQuery;
import pt.psoft.g1.psoftg1.lendingmanagement.services.SetLendingReturnedRequest;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (opaque-box) for LendingServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class LendingServiceImplTest {

  @Mock LendingRepository lendingRepository;
  @Mock FineRepository fineRepository;
  @Mock BookRepository bookRepository;
  @Mock ReaderRepository readerRepository;
  @Mock IdGenerator idGenerator;

  LendingServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new LendingServiceImpl(lendingRepository, fineRepository, bookRepository, readerRepository, idGenerator);
    ReflectionTestUtils.setField(service, "lendingDurationInDays", 14);
    ReflectionTestUtils.setField(service, "fineValuePerDayInCents", 25);
  }

  @Test
  void findByLendingNumber_delegates() {
    when(lendingRepository.findByLendingNumber("L-1")).thenReturn(Optional.empty());
    assertTrue(service.findByLendingNumber("L-1").isEmpty());
    verify(lendingRepository).findByLendingNumber("L-1");
  }

  @Test
  void listByReaderNumberAndIsbn_whenReturnedParamEmpty_returnsAll() {
    Lending a = mock(Lending.class);
    Lending b = mock(Lending.class);
    when(lendingRepository.listByReaderNumberAndIsbn("R1", "ISBN1")).thenReturn(List.of(a, b));

    List<Lending> out = service.listByReaderNumberAndIsbn("R1", "ISBN1", Optional.empty());

    assertEquals(2, out.size());
    verify(lendingRepository).listByReaderNumberAndIsbn("R1", "ISBN1");
  }

  @Test
  void listByReaderNumberAndIsbn_whenReturnedTrue_keepsOnlyReturned() {
    Lending notReturned = mock(Lending.class);
    Lending returned = mock(Lending.class);
    when(notReturned.getReturnedDate()).thenReturn(null);
    when(returned.getReturnedDate()).thenReturn(LocalDate.now().minusDays(1));

    when(lendingRepository.listByReaderNumberAndIsbn("R1", "ISBN1"))
        .thenReturn(new ArrayList<>(List.of(notReturned, returned)));

    List<Lending> out = service.listByReaderNumberAndIsbn("R1", "ISBN1", Optional.of(true));

    assertEquals(1, out.size());
    assertSame(returned, out.get(0));
  }

  @Test
  void listByReaderNumberAndIsbn_whenReturnedFalse_keepsOnlyNotReturned() {
    Lending notReturned = mock(Lending.class);
    Lending returned = mock(Lending.class);
    when(notReturned.getReturnedDate()).thenReturn(null);
    when(returned.getReturnedDate()).thenReturn(LocalDate.now());

    when(lendingRepository.listByReaderNumberAndIsbn("R2", "ISBN2"))
        .thenReturn(new ArrayList<>(List.of(notReturned, returned))); // MUTABLE

    List<Lending> out = service.listByReaderNumberAndIsbn("R2", "ISBN2", Optional.of(false));

    assertEquals(1, out.size());
    assertSame(notReturned, out.get(0));
  }

  @Test
  void create_whenOutstandingHasDelay_throwsForbidden() {
    CreateLendingRequest req = new CreateLendingRequest();
    req.setReaderNumber("R1");
    req.setIsbn("X");

    Lending delayed = mock(Lending.class);
    when(delayed.getDaysDelayed()).thenReturn(2);
    when(lendingRepository.listOutstandingByReaderNumber("R1")).thenReturn(List.of(delayed));

    assertThrows(LendingForbiddenException.class, () -> service.create(req));
    verify(lendingRepository, never()).save(any());
  }

  @Test
  void create_whenOutstandingCountReachesThree_throwsForbidden() {
    CreateLendingRequest req = new CreateLendingRequest();
    req.setReaderNumber("R1");
    req.setIsbn("X");

    Lending l1 = mock(Lending.class);
    Lending l2 = mock(Lending.class);
    Lending l3 = mock(Lending.class);
    when(l1.getDaysDelayed()).thenReturn(0);
    when(l2.getDaysDelayed()).thenReturn(0);
    when(l3.getDaysDelayed()).thenReturn(0);
    when(lendingRepository.listOutstandingByReaderNumber("R1")).thenReturn(List.of(l1, l2, l3));

    assertThrows(LendingForbiddenException.class, () -> service.create(req));
    verify(lendingRepository, never()).save(any());
  }

  @Test
  void create_whenBookMissing_throwsNotFound() {
    CreateLendingRequest req = new CreateLendingRequest();
    req.setReaderNumber("R1");
    req.setIsbn("ISBN1");

    when(lendingRepository.listOutstandingByReaderNumber("R1")).thenReturn(List.of());
    when(bookRepository.findByIsbn("ISBN1")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.create(req));
  }

  @Test
  void create_whenReaderMissing_throwsNotFound() {
    CreateLendingRequest req = new CreateLendingRequest();
    req.setReaderNumber("R1");
    req.setIsbn("ISBN1");

    when(lendingRepository.listOutstandingByReaderNumber("R1")).thenReturn(List.of());
    when(bookRepository.findByIsbn("ISBN1")).thenReturn(Optional.of(mock(Book.class)));
    when(readerRepository.findByReaderNumber("R1")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.create(req));
  }

  @Test
  void create_happyPath_assignsId_andSaves() {
    CreateLendingRequest req = new CreateLendingRequest();
    req.setReaderNumber("R2");
    req.setIsbn("ISBN2");

    when(lendingRepository.listOutstandingByReaderNumber("R2")).thenReturn(List.of());
    when(bookRepository.findByIsbn("ISBN2")).thenReturn(Optional.of(mock(Book.class)));
    when(readerRepository.findByReaderNumber("R2")).thenReturn(Optional.of(mock(ReaderDetails.class)));
    when(lendingRepository.getCountFromCurrentYear()).thenReturn(41);
    when(idGenerator.newId()).thenReturn("LEN-777");

    ArgumentCaptor<Lending> cap = ArgumentCaptor.forClass(Lending.class);
    when(lendingRepository.save(any(Lending.class))).thenAnswer(inv -> inv.getArgument(0));

    Lending out = service.create(req);

    assertNotNull(out);
    verify(idGenerator).newId();
    verify(lendingRepository).save(cap.capture());
    Lending saved = cap.getValue();
    assertNotNull(saved, "Lending created and saved");
  }

  @Test
  void setReturned_whenMissing_throwsNotFound() {
    when(lendingRepository.findByLendingNumber("MISS")).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class,
        () -> service.setReturned("MISS", new SetLendingReturnedRequest(), 1L));
    verify(fineRepository, never()).save(any());
  }

  @Test
  void setReturned_whenNotDelayed_savesWithoutFine() {
    Lending lending = mock(Lending.class);
    when(lendingRepository.findByLendingNumber("L-1")).thenReturn(Optional.of(lending));
    when(lending.getDaysDelayed()).thenReturn(0);
    when(lendingRepository.save(lending)).thenReturn(lending);

    SetLendingReturnedRequest req = new SetLendingReturnedRequest();
    req.setCommentary("ok");

    Lending out = service.setReturned("L-1", req, 5L);

    assertSame(lending, out);
    verify(lending).setReturned(5L, "ok");
    verify(fineRepository, never()).save(any());
    verify(lendingRepository).save(lending);
  }

  @Test
  void setReturned_whenDelayed_createsFine_assignsId_andSaves() {
    Lending lending = mock(Lending.class);
    when(lendingRepository.findByLendingNumber("L-2")).thenReturn(Optional.of(lending));
    when(lending.getDaysDelayed()).thenReturn(3);
    when(idGenerator.newId()).thenReturn("FINE-1");
    when(lendingRepository.save(lending)).thenReturn(lending);

    SetLendingReturnedRequest req = new SetLendingReturnedRequest();
    req.setCommentary("late");

    ArgumentCaptor<pt.psoft.g1.psoftg1.lendingmanagement.model.Fine> fineCap =
        ArgumentCaptor.forClass(pt.psoft.g1.psoftg1.lendingmanagement.model.Fine.class);

    Lending out = service.setReturned("L-2", req, 2L);

    assertSame(lending, out);
    verify(lending).setReturned(2L, "late");
    verify(fineRepository).save(fineCap.capture());
    assertNotNull(fineCap.getValue());
    verify(idGenerator).newId();
    verify(lendingRepository).save(lending);
  }

  @Test
  void getAverageDuration_formatsToSingleDecimal_UsLocale() {
    when(lendingRepository.getAverageDuration()).thenReturn(3.14159);
    Double out = service.getAverageDuration();
    assertEquals(3.1, out.doubleValue(), 0.0001);
  }

  @Test
  void getOverdue_whenPageNull_usesDefault() {
    when(lendingRepository.getOverdue(any())).thenReturn(List.of());

    List<Lending> out = service.getOverdue(null);

    assertNotNull(out);
    ArgumentCaptor<pt.psoft.g1.psoftg1.shared.services.Page> pageCap =
        ArgumentCaptor.forClass(pt.psoft.g1.psoftg1.shared.services.Page.class);
    verify(lendingRepository).getOverdue(pageCap.capture());
    assertNotNull(pageCap.getValue(), "Default Page should be created when null is passed");
  }

  @Test
  void getOverdue_whenPageProvided_passesThrough() {
    pt.psoft.g1.psoftg1.shared.services.Page page =
        new pt.psoft.g1.psoftg1.shared.services.Page(3, 50);
    when(lendingRepository.getOverdue(page)).thenReturn(List.of());

    List<Lending> out = service.getOverdue(page);

    assertNotNull(out);
    verify(lendingRepository).getOverdue(page);
  }

  @Test
  void getAvgLendingDurationByIsbn_formatsToSingleDecimal() {
    when(lendingRepository.getAvgLendingDurationByIsbn("ISBN9")).thenReturn(12.9876);
    Double out = service.getAvgLendingDurationByIsbn("ISBN9");
    assertEquals(13.0, out.doubleValue(), 0.0001);
  }

  @Test
  void searchLendings_whenPageNull_defaultsPage() {
    SearchLendingQuery q = new SearchLendingQuery("R1", "ISBN1", null, null, null);
    when(lendingRepository.searchLendings(any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of());

    service.searchLendings(null, q);

    ArgumentCaptor<pt.psoft.g1.psoftg1.shared.services.Page> pageCap =
        ArgumentCaptor.forClass(pt.psoft.g1.psoftg1.shared.services.Page.class);
    verify(lendingRepository).searchLendings(pageCap.capture(), eq("R1"), eq("ISBN1"),
        isNull(), isNull(), isNull());
    assertNotNull(pageCap.getValue(), "Default Page should be created when null is passed");
  }

  @Test
  void searchLendings_whenQueryNull_buildsDefaultAndDelegates() {
    when(lendingRepository.searchLendings(any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of());

    List<Lending> out = service.searchLendings(
        new pt.psoft.g1.psoftg1.shared.services.Page(1, 10),
        null);

    assertNotNull(out);
    verify(lendingRepository).searchLendings(any(), anyString(), anyString(), any(), any(), any());
  }

  @Test
  void searchLendings_parsesDates_andPassesParams() {
    pt.psoft.g1.psoftg1.shared.services.Page page =
        new pt.psoft.g1.psoftg1.shared.services.Page(2, 20);
    SearchLendingQuery q = new SearchLendingQuery("R7", "ISBN7", Boolean.TRUE, "2024-01-10", "2024-02-01");

    when(lendingRepository.searchLendings(any(), anyString(), anyString(), any(), any(), any()))
        .thenReturn(List.of());

    ArgumentCaptor<pt.psoft.g1.psoftg1.shared.services.Page> pageCap =
        ArgumentCaptor.forClass(pt.psoft.g1.psoftg1.shared.services.Page.class);
    ArgumentCaptor<LocalDate> startCap = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<LocalDate> endCap = ArgumentCaptor.forClass(LocalDate.class);

    service.searchLendings(page, q);

    verify(lendingRepository).searchLendings(pageCap.capture(),
        eq("R7"), eq("ISBN7"), eq(Boolean.TRUE), startCap.capture(), endCap.capture());

    assertSame(page, pageCap.getValue());
    assertEquals(LocalDate.of(2024, 1, 10), startCap.getValue());
    assertEquals(LocalDate.of(2024, 2, 1), endCap.getValue());
  }

  @Test
  void searchLendings_whenDateParseFails_throwsIllegalArgument() {
    pt.psoft.g1.psoftg1.shared.services.Page page =
        new pt.psoft.g1.psoftg1.shared.services.Page(1, 10);
    SearchLendingQuery q = new SearchLendingQuery("R", "I", null, "2024/01/01", null);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.searchLendings(page, q));

    assertEquals("Expected format is YYYY-MM-DD", ex.getMessage());
    verify(lendingRepository, never()).searchLendings(any(), any(), any(), any(), any(), any());
  }
}
