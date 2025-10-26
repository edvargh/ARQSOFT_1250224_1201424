package pt.psoft.g1.psoftg1.newTests.domain.readermanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.readermanagement.model.PhoneNumber;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for PhoneNumber.
 */
class PhoneNumberTest {


  @Test
  void ctor_valid_startsWith9_andLength9() {
    PhoneNumber p = new PhoneNumber("912345678");
    assertEquals("912345678", p.toString());
  }

  @Test
  void ctor_valid_startsWith2_andLength9() {
    PhoneNumber p = new PhoneNumber("212345678");
    assertEquals("212345678", p.toString());
  }


  @Test
  void ctor_invalid_prefix_throws() {
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("712345678"));
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("112345678"));
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("012345678"));
  }

  @Test
  void ctor_invalid_length_throws() {
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("91234567"));   // 8 chars
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("9123456789")); // 10 chars
  }

  @Test
  void ctor_null_throwsNullPointerFromStartsWith() {
    assertThrows(NullPointerException.class, () -> new PhoneNumber(null));
  }


  @Test
  void ctor_nonDigitCharacters_areCurrentlyAccepted_ifPrefixAndLengthMatch() {
    PhoneNumber p = new PhoneNumber("9A2345678");
    assertEquals("9A2345678", p.toString());
  }
}
