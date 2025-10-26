package pt.psoft.g1.psoftg1.newTests.domain.lendingmanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional transparent-box tests for Fine.
 */
class FineTest {


  @Test
  void constructor_overdue_calculatesFromLending() {
    Lending lending = mock(Lending.class);
    when(lending.getDaysDelayed()).thenReturn(3);
    when(lending.getFineValuePerDayInCents()).thenReturn(250);

    Fine fine = new Fine(lending);

    assertSame(lending, fine.getLending());
    assertEquals(250, fine.getFineValuePerDayInCents());
    assertEquals(3 * 250, fine.getCentsValue());
  }

  @Test
  void constructor_notOverdue_throws() {
    Lending lending = mock(Lending.class);
    when(lending.getDaysDelayed()).thenReturn(0);

    assertThrows(IllegalArgumentException.class, () -> new Fine(lending));
  }

  @Test
  void constructor_nullLending_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new Fine(null));
  }


  @Test
  void secondaryConstructor_setsValues_andClampsNegativesToZero() {
    Lending lending = mock(Lending.class);

    Fine fine = new Fine(lending, -100, -5);

    assertSame(lending, fine.getLending());
    assertEquals(0, fine.getFineValuePerDayInCents(), "per-day fine is clamped to >= 0");
    assertEquals(0, fine.getCentsValue(), "total fine is clamped to >= 0");
  }

  @Test
  void secondaryConstructor_nullLending_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new Fine(null, 100, 500));
  }


  @Test
  void assignId_setsId() {
    Lending lending = mock(Lending.class);
    when(lending.getDaysDelayed()).thenReturn(2);
    when(lending.getFineValuePerDayInCents()).thenReturn(100);

    Fine fine = new Fine(lending);
    fine.assignId("fine-123");

    assertEquals("fine-123", fine.getId());
  }

  @Test
  void assignId_blankOrNull_throws() {
    Lending lending = mock(Lending.class);
    when(lending.getDaysDelayed()).thenReturn(1);
    when(lending.getFineValuePerDayInCents()).thenReturn(100);

    Fine fine = new Fine(lending);

    assertThrows(IllegalArgumentException.class, () -> fine.assignId("  "));
    assertThrows(IllegalArgumentException.class, () -> fine.assignId(null));
  }


  @Test
  void setLending_reassignsAssociation() {
    Lending lending1 = mock(Lending.class);
    when(lending1.getDaysDelayed()).thenReturn(1);
    when(lending1.getFineValuePerDayInCents()).thenReturn(50);

    Fine fine = new Fine(lending1);

    Lending lending2 = mock(Lending.class);
    fine.setLending(lending2);

    assertSame(lending2, fine.getLending());
  }
}
