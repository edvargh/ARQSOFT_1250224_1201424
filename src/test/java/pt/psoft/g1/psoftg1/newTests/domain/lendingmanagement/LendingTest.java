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
}
