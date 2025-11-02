package pt.psoft.g1.psoftg1.newTests.domain.usermanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.usermanagement.model.Password;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Password.
 */
class PasswordTest {

  private static String getPwd(Password p) {
    try {
      Field f = Password.class.getDeclaredField("password");
      f.setAccessible(true);
      return (String) f.get(p);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void constructor_validPassword_ok() {
    Password p = new Password("Abcd1234!");
    assertEquals("Abcd1234!", getPwd(p));
  }

  @Test
  void constructor_invalidPasswords_throw() {
    assertThrows(IllegalArgumentException.class, () -> new Password(null));
    assertThrows(IllegalArgumentException.class, () -> new Password(""));

    assertThrows(IllegalArgumentException.class, () -> new Password("Ab1!a"));

    assertThrows(IllegalArgumentException.class, () -> new Password("abcdefgh"));
    assertThrows(IllegalArgumentException.class, () -> new Password("ABCDEFGH"));
    assertThrows(IllegalArgumentException.class, () -> new Password("Abcdefgh"));
    assertThrows(IllegalArgumentException.class, () -> new Password("ABCDEFG1"));
    assertThrows(IllegalArgumentException.class, () -> new Password("abcdefg1"));
  }

  @Test
  void updatePassword_valid_then_invalid_keepsOldValue() {
    Password p = new Password("Abcd1234!");
    assertEquals("Abcd1234!", getPwd(p));

    p.updatePassword("Zyxw9876#");
    assertEquals("Zyxw9876#", getPwd(p));

    assertThrows(IllegalArgumentException.class, () -> p.updatePassword("short1!"));
    assertEquals("Zyxw9876#", getPwd(p));
  }

  @Test
  void updatePassword_validPassword_doesNotThrow_andUpdates() {
    Password p = new Password("Start123!");
    assertDoesNotThrow(() -> p.updatePassword("GoodPass1#"));
    assertEquals("GoodPass1#", getPwd(p), "Password should update on valid input");
  }

  @Test
  void updatePassword_validPassword_updatesWithoutThrowing_andPersistsChange() {
    Password p = new Password("Start123!");
    String old = getPwd(p);

    try {
      p.updatePassword("GoodPass1#");
    } catch (Exception e) {
      fail("Should not throw for valid password, but threw: " + e);
    }

    assertNotEquals(old, getPwd(p), "Password should change on valid input");
    assertEquals("GoodPass1#", getPwd(p), "Updated password should be persisted");
  }

  @Test
  void updatePassword_validOnce_onlyCall_exercisesAssignmentPath() {
    Password p = new Password("Abcd1234!");
    String before = getPwd(p);

    p.updatePassword("Xyz98765!");

    String after = getPwd(p);
    assertNotEquals(before, after, "password must change on valid update");
    assertEquals("Xyz98765!", after);
  }

  @Test
  void updatePassword_empty_throws_and_keepsOldValue() {
    Password p = new Password("Abcd1234!");
    String before = getPwd(p);

    assertThrows(IllegalArgumentException.class, () -> p.updatePassword(""));

    assertEquals(before, getPwd(p), "on invalid update, password must not change");
  }
}
