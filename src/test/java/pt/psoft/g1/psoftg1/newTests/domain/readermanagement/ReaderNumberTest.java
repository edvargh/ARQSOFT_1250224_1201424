package pt.psoft.g1.psoftg1.newTests.domain.readermanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderNumber;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional transparent-box tests for ReaderNumber.
 */
class ReaderNumberTest {

  @Test
  void ctor_yearAndNumber_buildsExpectedString() {
    ReaderNumber rn = new ReaderNumber(1999, 42);
    assertEquals("1999/42", rn.toString());
  }

  @Test
  void ctor_numberOnly_usesCurrentYear() {
    int currentYear = LocalDate.now().getYear();
    ReaderNumber rn = new ReaderNumber(7);
    assertEquals(currentYear + "/7", rn.toString());
  }

  @Test
  void ctor_acceptsZeroAndNegativeNumbers_asIs() {
    ReaderNumber zero = new ReaderNumber(2020, 0);
    ReaderNumber negative = new ReaderNumber(2020, -3);

    assertEquals("2020/0", zero.toString());
    assertEquals("2020/-3", negative.toString());
  }
}
