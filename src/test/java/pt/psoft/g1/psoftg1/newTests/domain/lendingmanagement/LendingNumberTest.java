package pt.psoft.g1.psoftg1.newTests.domain.lendingmanagement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import pt.psoft.g1.psoftg1.lendingmanagement.model.LendingNumber;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for LendingNumber.
 */
class LendingNumberTest {


  @Test
  void yearSeq_valid_builds() {
    int now = LocalDate.now().getYear();
    int year = (now > 1970) ? now - 1 : 1970;
    LendingNumber ln = new LendingNumber(year, 0);

    assertEquals(year + "/0", ln.toString());
  }

  @Test
  void yearSeq_yearBefore1970_throws() {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(1969, 0));
  }

  @Test
  void yearSeq_yearAfterNow_throws() {
    int nextYear = LocalDate.now().getYear() + 1;
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(nextYear, 0));
  }

  @Test
  void yearSeq_negativeSequential_throws() {
    int now = LocalDate.now().getYear();
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(now, -1));
  }


  @Test
  void seqOnly_usesCurrentYear() {
    int now = LocalDate.now().getYear();
    LendingNumber ln = new LendingNumber(42);
    assertEquals(now + "/42", ln.toString());
  }

  @Test
  void seqOnly_negative_throws() {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(-5));
  }


  @Test
  void string_valid_builds() {
    int now = LocalDate.now().getYear();
    String s = now + "/123";
    LendingNumber ln = new LendingNumber(s);
    assertEquals(s, ln.toString());
  }

  @Test
  void string_null_throws() {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(null));
  }

  @Test
  void string_wrongSeparator_throws() {
    int now = LocalDate.now().getYear();
    String bad = now + "-123";
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(bad));
  }

  @Test
  void string_nonNumericYear_throws() {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber("abcd/10"));
  }

  @Test
  void string_tooShort_throws() {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber("2024"));
  }

  @Test
  void yearSeq_yearEquals1970_allowed() {
    assertDoesNotThrow(() -> new LendingNumber(1970, 0));
    assertEquals("1970/0", new LendingNumber(1970, 0).toString());
  }

  @Test
  void yearSeq_yearEqualsNow_allowed() {
    int now = LocalDate.now().getYear();
    assertDoesNotThrow(() -> new LendingNumber(now, 1));
    assertEquals(now + "/1", new LendingNumber(now, 1).toString());
  }

  @Test
  void yearSeq_sequentialZero_allowed() {
    int now = LocalDate.now().getYear();
    assertDoesNotThrow(() -> new LendingNumber(now, 0));
    assertEquals(now + "/0", new LendingNumber(now, 0).toString());
  }

  @Test
  void seqOnly_zero_allowed_usesCurrentYear() {
    int now = LocalDate.now().getYear();
    assertDoesNotThrow(() -> new LendingNumber(0));
    assertEquals(now + "/0", new LendingNumber(0).toString());
  }
}
