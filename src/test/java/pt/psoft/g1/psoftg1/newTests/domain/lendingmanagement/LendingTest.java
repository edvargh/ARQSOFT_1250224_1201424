package pt.psoft.g1.psoftg1.newTests.domain.lendingmanagement;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional transparent-box tests for Lending.
 */
class LendingTest {

  private Book book;
  private ReaderDetails reader;

  @BeforeEach
  void setUp() {
    book = mock(Book.class);
    reader = mock(ReaderDetails.class);
    when(book.getTitle()).thenReturn(new Title("Clean Code"));
  }


  private static void setVersion(Lending l, long version) {
    try {
      Field f = Lending.class.getDeclaredField("version");
      f.setAccessible(true);
      f.set(l, version);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Test
  void constructor_valid_buildsWithDatesAndNoFine() {
    int duration = 14;
    int finePerDay = 100;

    Lending l = new Lending(book, reader, 1, duration, finePerDay);

    assertEquals(LocalDate.now(), l.getStartDate());
    assertEquals(l.getStartDate().plusDays(duration), l.getLimitDate());
    assertNull(l.getReturnedDate());

    assertEquals(Optional.of(duration), l.getDaysUntilReturn());
    assertEquals(Optional.empty(), l.getDaysOverdue());
    assertEquals(Optional.empty(), l.getFineValueInCents());
  }

  @Test
  void constructor_nullArguments_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Lending(null, reader, 1, 7, 50));
    assertThrows(IllegalArgumentException.class, () -> new Lending(book, null, 1, 7, 50));
  }

  @Test
  void assignId_setsAndValidates() {
    Lending l = new Lending(book, reader, 1, 7, 50);
    l.assignId("lend-1");
    assertEquals("lend-1", l.getId());

    assertThrows(IllegalArgumentException.class, () -> l.assignId("  "));
    assertThrows(IllegalArgumentException.class, () -> l.assignId(null));
  }


  @Test
  void setReturned_versionMismatch_throwsStale() {
    Lending l = new Lending(book, reader, 1, 7, 50);
    setVersion(l, 5L);

    assertThrows(StaleObjectStateException.class, () -> l.setReturned(4L, "ok"));
  }

  @Test
  void setReturned_twice_throwsIllegalArgument() {
    Lending l = new Lending(book, reader, 1, 7, 50);
    setVersion(l, 1L);

    l.setReturned(1L, "fine");
    assertNotNull(l.getReturnedDate());

    assertThrows(IllegalArgumentException.class, () -> l.setReturned(1L, "again"));
  }

  @Test
  void setReturned_setsDateClearsTransientCounters() {
    Lending l = new Lending(book, reader, 1, 7, 50);
    setVersion(l, 2L);

    l.setReturned(2L, "comment");
    assertNotNull(l.getReturnedDate());
    assertEquals(Optional.empty(), l.getDaysUntilReturn());
    assertEquals(0, l.getDaysDelayed());
    assertEquals(Optional.empty(), l.getDaysOverdue());
    assertEquals(Optional.empty(), l.getFineValueInCents());
  }


  @Test
  void notReturned_andBeforeLimit_noDelay_noFine() {
    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 1,
        LocalDate.now().minusDays(1), null,
        10, 123);

    assertEquals(0, l.getDaysDelayed());
    assertTrue(l.getDaysUntilReturn().isPresent());
    assertEquals(Optional.empty(), l.getDaysOverdue());
    assertEquals(Optional.empty(), l.getFineValueInCents());
  }

  @Test
  void notReturned_andAfterLimit_delayEqualsDaysPastLimit_fineAccrues() {
    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 2,
        LocalDate.now().minusDays(10), null,
        5, 200);

    assertEquals(5, l.getDaysDelayed());
    assertEquals(Optional.of(5), l.getDaysOverdue());
    assertEquals(Optional.of(5 * 200), l.getFineValueInCents());
  }

  @Test
  void returnedBeforeLimit_noDelay_noFine() {
    LocalDate start = LocalDate.now().minusDays(5);
    int duration = 10;
    LocalDate returned = start.plusDays(2);

    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 3,
        start, returned, duration, 150);

    assertEquals(0, l.getDaysDelayed());
    assertEquals(Optional.empty(), l.getDaysOverdue());
    assertEquals(Optional.empty(), l.getFineValueInCents());
  }

  @Test
  void returnedAfterLimit_delayAndFineComputedFromReturnDate() {
    LocalDate start = LocalDate.now().minusDays(10);
    int duration = 5;
    LocalDate returned = start.plusDays(8);

    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 4,
        start, returned, duration, 250);

    assertEquals(3, l.getDaysDelayed());
    assertEquals(Optional.of(3), l.getDaysOverdue());
    assertEquals(Optional.of(3 * 250), l.getFineValueInCents());
  }


  @Test
  void getTitle_delegatesToBookTitle() {
    Lending l = new Lending(book, reader, 1, 7, 50);
    assertEquals("Clean Code", l.getTitle());
  }

  @Test
  void setReturned_withComment_storesComment() throws Exception {
    Lending l = new Lending(book, reader, 1, 7, 50);
    setVersion(l, 3L);

    l.setReturned(3L, "nice book");

    var f = Lending.class.getDeclaredField("commentary");
    f.setAccessible(true);
    assertEquals("nice book", f.get(l));
  }

  @Test
  void setReturned_withNullComment_leavesCommentNull() throws Exception {
    Lending l = new Lending(book, reader, 1, 7, 50);
    setVersion(l, 2L);

    l.setReturned(2L, null);

    var f = Lending.class.getDeclaredField("commentary");
    f.setAccessible(true);
    assertNull(f.get(l));
  }

  @Test
  void notReturned_afterLimit_daysUntilReturnEmpty() {
    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 2,
        LocalDate.now().minusDays(10), null,
        5, 200);

    assertTrue(l.getDaysUntilReturn().isEmpty(), "past due should return empty daysUntilReturn");
  }

  @Test
  void setReturned_whenPastLimit_appliesFineFromNow() {
    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 5,
        LocalDate.now().minusDays(10), null,
        5, 123);
    setVersion(l, 1L);

    l.setReturned(1L, "late");

    assertTrue(l.getDaysDelayed() > 0);
    assertTrue(l.getFineValueInCents().isPresent());
    assertTrue(l.getFineValueInCents().get() > 0);
  }

  @Test
  void factory_nullArguments_throw() {
    assertThrows(IllegalArgumentException.class, () ->
        Lending.newBootstrappingLending(null, reader, 2024, 1, LocalDate.now(), null, 7, 10));
    assertThrows(IllegalArgumentException.class, () ->
        Lending.newBootstrappingLending(book, null, 2024, 1, LocalDate.now(), null, 7, 10));
  }

  @Test
  void getLendingNumber_matchesYearSlashSeq_constructorUsesCurrentYear() {
    Lending l = new Lending(book, reader, 42, 7, 10);
    String num = l.getLendingNumber();
    assertTrue(num.matches("\\d{4}/\\d+"));
  }

  @Test
  void getLendingNumber_matchesYearSlashSeq_factoryUsesProvidedYear() {
    Lending l = Lending.newBootstrappingLending(
        book, reader, 1999, 7, LocalDate.now(), null, 14, 10);
    assertEquals("1999/7", l.getLendingNumber());
  }

  @Test
  void returnedExactlyOnLimit_noDelay_noFine() {
    LocalDate start = LocalDate.now().minusDays(5);
    int duration = 5;
    LocalDate returned = start.plusDays(5);

    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 9, start, returned, duration, 300);

    assertEquals(0, l.getDaysDelayed());
    assertTrue(l.getFineValueInCents().isEmpty());
  }

  @Test
  void overdueWithZeroFinePerDay_returnsZeroFine() {
    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 10,
        LocalDate.now().minusDays(10), null,
        5, 0);

    assertEquals(5, l.getDaysDelayed());
    assertEquals(Optional.of(0), l.getFineValueInCents(), "fine should be zero when per-day fine is zero");
  }

  @Test
  void notReturned_exactlyOnLimit_zeroDaysUntilReturn_noOverdue_noFine() {
    int duration = 5;
    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 11,
        LocalDate.now().minusDays(duration),
        null, duration, 200);

    assertEquals(0, l.getDaysDelayed());
    assertEquals(Optional.of(0), l.getDaysUntilReturn());
    assertTrue(l.getDaysOverdue().isEmpty());
    assertTrue(l.getFineValueInCents().isEmpty());
  }

  @Test
  void notReturned_oneDayOverdue_overdueIsOne_fineIsPerDay() {
    int perDay = 123;
    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 12,
        LocalDate.now().minusDays(6),
        null, 5, perDay);

    assertEquals(1, l.getDaysDelayed());
    assertEquals(Optional.of(1), l.getDaysOverdue());
    assertEquals(Optional.of(perDay), l.getFineValueInCents());
  }

  @Test
  void setReturned_whenLimitWasYesterday_overdueOne_fineIsPerDay() {
    int perDay = 200;
    Lending l = Lending.newBootstrappingLending(
        book, reader, 2024, 13,
        LocalDate.now().minusDays(6), null, 5, perDay);
    setVersion(l, 1L);

    l.setReturned(1L, "late");

    assertEquals(1, l.getDaysDelayed());
    assertEquals(Optional.of(1), l.getDaysOverdue());
    assertEquals(Optional.of(perDay), l.getFineValueInCents());
  }

  @Test
  void constructor_initializesTransientCounters_withoutCallingGetters() throws Exception {
    Lending l = new Lending(book, reader, 1, 7, 100);

    Field fUntil = Lending.class.getDeclaredField("daysUntilReturn");
    fUntil.setAccessible(true);
    Field fOverdue = Lending.class.getDeclaredField("daysOverdue");
    fOverdue.setAccessible(true);

    Integer daysUntilReturn = (Integer) fUntil.get(l);
    Integer daysOverdue = (Integer) fOverdue.get(l);

    assertNotNull(daysUntilReturn, "constructor should initialize daysUntilReturn");
    assertTrue(daysUntilReturn >= 0, "daysUntilReturn should be non-negative");
    assertNull(daysOverdue, "constructor should set daysOverdue to null when not overdue");
  }

  @Test
  void constructor_withPastLimit_initializesDaysOverdueWithoutGetter() throws Exception {
    int negativeDuration = -3;
    int perDay = 50;

    Lending l = new Lending(book, reader, 1, negativeDuration, perDay);

    Field fOverdue = Lending.class.getDeclaredField("daysOverdue");
    fOverdue.setAccessible(true);
    Field fUntil = Lending.class.getDeclaredField("daysUntilReturn");
    fUntil.setAccessible(true);

    Integer daysOverdue = (Integer) fOverdue.get(l);
    Integer daysUntilReturn = (Integer) fUntil.get(l);

    assertEquals(Integer.valueOf(3), daysOverdue, "constructor should initialize daysOverdue when already overdue");
    assertNull(daysUntilReturn, "daysUntilReturn should be null when already overdue");
  }
}
