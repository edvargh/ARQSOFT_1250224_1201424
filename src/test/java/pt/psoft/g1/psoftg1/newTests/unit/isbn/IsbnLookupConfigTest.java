package pt.psoft.g1.psoftg1.newTests.unit.isbn;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.config.IsbnLookupConfig;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

class IsbnLookupConfigTest {

  @Test
  void isbnLookupExecutor_isFixedThreadPoolOf4() {
    Executor ex = new IsbnLookupConfig().isbnLookupExecutor();
    assertNotNull(ex);
    assertTrue(ex instanceof ThreadPoolExecutor);
    ThreadPoolExecutor tpe = (ThreadPoolExecutor) ex;
    assertEquals(4, tpe.getCorePoolSize());
    assertEquals(4, tpe.getMaximumPoolSize());
  }
}
