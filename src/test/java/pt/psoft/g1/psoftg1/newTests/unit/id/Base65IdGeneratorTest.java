package pt.psoft.g1.psoftg1.newTests.unit.id;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.shared.id.Base65IdGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class Base65IdGeneratorTest {

  private static final Pattern BASE65 =
      Pattern.compile("[0-9A-Za-z\\-_~]+");

  @Test
  void newId_returns22UrlSafeChars_noPrefix() {
    var gen = new Base65IdGenerator();

    String id = gen.newId();

    assertNotNull(id);
    assertEquals(22, id.length(), "Base65 should produce fixed length 22 chars");
    assertTrue(BASE65.matcher(id).matches(), "Only Base65 URL-safe alphabet expected");
  }

  @Test
  void newId_withPrefix_prependsPrefix_verbatim() {
    var gen = new Base65IdGenerator();

    String id = gen.newId("PRE-");

    assertNotNull(id);
    assertTrue(id.startsWith("PRE-"));
    String core = id.substring("PRE-".length());
    assertEquals(22, core.length());
    assertTrue(BASE65.matcher(core).matches());
  }

  @Test
  void newId_manyCalls_areUnique() {
    var gen = new Base65IdGenerator();
    Set<String> seen = new HashSet<>();

    for (int i = 0; i < 2_000; i++) {
      String id = gen.newId();
      assertTrue(seen.add(id), "duplicate detected at iteration " + i);
    }
  }
}
