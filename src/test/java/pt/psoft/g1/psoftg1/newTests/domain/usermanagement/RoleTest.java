package pt.psoft.g1.psoftg1.newTests.domain.usermanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Role.
 */
class RoleTest {

  @Test
  void constants_haveExpectedValues() {
    assertEquals("ADMIN", Role.ADMIN);
    assertEquals("LIBRARIAN", Role.LIBRARIAN);
    assertEquals("READER", Role.READER);
  }

  @Test
  void getAuthority_returnsBackingString() {
    Role r = new Role("ADMIN");
    assertEquals("ADMIN", r.getAuthority());
  }

  @Test
  void equalsAndHashCode_basedOnAuthority() {
    Role r1 = new Role("ADMIN");
    Role r2 = new Role("ADMIN");
    Role r3 = new Role("READER");

    assertEquals(r1, r2);
    assertEquals(r1.hashCode(), r2.hashCode());
    assertNotEquals(r1, r3);
  }

  @Test
  void setSemantics_deduplicateSameAuthority() {
    Set<Role> set = new HashSet<>();
    set.add(new Role("ADMIN"));
    set.add(new Role("ADMIN"));
    set.add(new Role("READER"));

    assertEquals(2, set.size());
    assertTrue(set.contains(new Role("ADMIN")));
    assertTrue(set.contains(new Role("READER")));
  }
}
