package pt.psoft.g1.psoftg1.newTests.domain.readermanagement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.readermanagement.model.EmailAddress;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for EmailAddress.
 */
class EmailAddressTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void initValidator() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    factory.close();
  }


  @Test
  void validEmails_haveNoViolations() {
    List<String> samples = List.of(
        "user@example.com",
        "USER+tag@sub.example.co.uk",
        "first.last@domain.io",
        "u@d.pt"
    );

    for (String s : samples) {
      EmailAddress e = new EmailAddress(s);
      Set<ConstraintViolation<EmailAddress>> v = validator.validate(e);
      assertTrue(v.isEmpty(), "Expected no violations for: " + s);
    }
  }


  @Test
  void invalidEmails_raiseViolations() {
    List<String> samples = List.of(
        "plainaddress",
        "user@",
        "@example.com",
        "user@.com",
        "user@example..com"
    );

    for (String s : samples) {
      EmailAddress e = new EmailAddress(s);
      Set<ConstraintViolation<EmailAddress>> v = validator.validate(e);
      assertFalse(v.isEmpty(), "Expected violations for: " + s);
    }
  }


  @Test
  void nullAddress_isConsideredValidByBeanValidation() {
    EmailAddress e = new EmailAddress(null);
    Set<ConstraintViolation<EmailAddress>> v = validator.validate(e);
    assertTrue(v.isEmpty(), "Null is allowed because field lacks @NotNull");
  }


  @Test
  void allArgsConstructor_setsUnderlyingField() throws Exception {
    EmailAddress e = new EmailAddress("x@y.z");
    Field f = EmailAddress.class.getDeclaredField("address");
    f.setAccessible(true);
    assertEquals("x@y.z", f.get(e));
  }
}
