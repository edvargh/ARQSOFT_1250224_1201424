package pt.psoft.g1.psoftg1.newTests.unit.id;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.config.IdGeneratorConfig;
import pt.psoft.g1.psoftg1.shared.id.Base65IdGenerator;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.id.UlidGenerator;

import static org.junit.jupiter.api.Assertions.*;

class IdGeneratorConfigTest {

  @Test
  void base65IdGenerator_returnsBase65() {
    IdGenerator out = new IdGeneratorConfig().base65IdGenerator();
    assertNotNull(out);
    assertTrue(out instanceof Base65IdGenerator);
  }

  @Test
  void timestampIdGenerator_returnsUlid() {
    IdGenerator out = new IdGeneratorConfig().timestampIdGenerator();
    assertNotNull(out);
    assertTrue(out instanceof UlidGenerator);
  }
}
