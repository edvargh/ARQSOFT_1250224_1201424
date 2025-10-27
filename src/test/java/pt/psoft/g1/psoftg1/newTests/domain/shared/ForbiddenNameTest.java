package pt.psoft.g1.psoftg1.newTests.domain.shared;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for ForbiddenName.
 */
class ForbiddenNameTest {

  @Test
  void constructor_setsIdAndName() {
    ForbiddenName fn = new ForbiddenName("id-1", "banned");

    assertEquals("id-1", fn.getId());
    assertEquals("banned", fn.getForbiddenName());
  }

  @Test
  void assignIdIfAbsent_setsId_whenNull() {
    ForbiddenName fn = new ForbiddenName(null, "curse");

    fn.assignIdIfAbsent("abc123");

    assertEquals("abc123", fn.getId());
  }

  @Test
  void assignIdIfAbsent_doesNotOverwrite_existingId() {
    ForbiddenName fn = new ForbiddenName("orig", "badword");

    fn.assignIdIfAbsent("new");
    assertEquals("orig", fn.getId(), "Existing ID must not be overwritten");
  }

  @Test
  void setters_and_getters_workCorrectly() {
    ForbiddenName fn = new ForbiddenName("id", "spam");
    fn.setForbiddenName("scam");

    assertEquals("scam", fn.getForbiddenName());
  }

  @Test
  void noArgConstructor_createsObjectWithNullFields() {
    ForbiddenName fn = new ForbiddenName();

    assertNull(fn.getId());
    assertNull(fn.getForbiddenName());
  }

  @Test
  void setter_allowsNull_evenThoughSizeAnnotationWouldForbidViaBeanValidation() {
    ForbiddenName fn = new ForbiddenName("id", "spam");
    fn.setForbiddenName(null);
    assertNull(fn.getForbiddenName());
  }

}
