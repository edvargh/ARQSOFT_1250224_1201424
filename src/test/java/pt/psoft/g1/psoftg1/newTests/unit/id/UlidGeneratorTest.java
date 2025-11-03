package pt.psoft.g1.psoftg1.newTests.unit.id;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.shared.id.UlidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class UlidGeneratorTest {

  private static final Pattern CROCKFORD_32 = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");

  @Test
  void newId_returns26CrockfordChars_noPrefix() {
    var gen = new UlidGenerator();

    String id = gen.newId();

    assertNotNull(id);
    assertEquals(26, id.length(), "ULID should produce fixed length 26 chars");
    assertTrue(CROCKFORD_32.matcher(id).matches(),
        "Only Crockford Base32 alphabet expected (0-9, A-H, J-K, M-N, P-T, V-Z)");
  }

  @Test
  void newId_withPrefix_prependsPrefix_verbatim() {
    var gen = new UlidGenerator();

    String id = gen.newId("PRE-");

    assertNotNull(id);
    assertTrue(id.startsWith("PRE-"));
    String core = id.substring("PRE-".length());
    assertEquals(26, core.length(), "ULID core should be 26 chars");
    assertTrue(CROCKFORD_32.matcher(core).matches(),
        "ULID core must use Crockford Base32 alphabet");
  }

  @Test
  void newId_manyCalls_areUnique() {
    var gen = new UlidGenerator();
    Set<String> seen = new HashSet<>();

    for (int i = 0; i < 2_000; i++) {
      String id = gen.newId();
      assertTrue(seen.add(id), "duplicate detected at iteration " + i);
    }
  }
}
