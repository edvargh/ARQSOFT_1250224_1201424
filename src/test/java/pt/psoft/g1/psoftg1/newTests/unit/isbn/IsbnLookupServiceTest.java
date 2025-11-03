package pt.psoft.g1.psoftg1.newTests.unit.isbn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import pt.psoft.g1.psoftg1.bookmanagement.services.IsbnLookupMode;
import pt.psoft.g1.psoftg1.bookmanagement.services.IsbnLookupResult;
import pt.psoft.g1.psoftg1.bookmanagement.services.IsbnLookupService;
import pt.psoft.g1.psoftg1.external.service.isbn.IsbnProvider;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pt.psoft.g1.psoftg1.bookmanagement.services.IsbnUtils.normalizeTitleKey;

class IsbnLookupServiceTest {

  private StringRedisTemplate redis;
  private ValueOperations<String, String> valueOps;
  private Executor executor;

  @BeforeEach
  void setup() {
    redis = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(valueOps);
    executor = Executors.newFixedThreadPool(4);
  }

  private IsbnProvider provider(String name, List<String> returns) {
    IsbnProvider p = mock(IsbnProvider.class);
    when(p.getName()).thenReturn(name);
    when(p.findIsbnsByTitle(anyString())).thenReturn(returns);
    return p;
  }

  private IsbnProvider throwingProvider(String name) {
    IsbnProvider p = mock(IsbnProvider.class);
    when(p.getName()).thenReturn(name);
    when(p.findIsbnsByTitle(anyString())).thenThrow(new RuntimeException("boom"));
    return p;
  }

  private IsbnLookupService serviceWithProviders(IsbnProvider... providers) {
    return new IsbnLookupService(List.of(providers), redis, executor);
  }

  @Test
  void cacheHit_returnsCachedList_and_skipsProviders() {
    // Arrange
    String title = "  Clean   Code ";
    String modePart = "any";
    String providersKey = "google-books+openlibrary";
    String key = "isbn:title:" + normalizeTitleKey(title) + ":mode:" + modePart + ":providers:" + providersKey;

    when(valueOps.get(key)).thenReturn("9780306406157,0306406152");

    IsbnProvider google = provider("google-books", List.of("junk"));
    IsbnProvider openlib = provider("openlibrary", List.of("junk"));
    var sut = serviceWithProviders(google, openlib);

    IsbnLookupResult out = sut.getIsbnsByTitle(title, IsbnLookupMode.ANY);

    verify(google, never()).findIsbnsByTitle(anyString());
    verify(openlib, never()).findIsbnsByTitle(anyString());

    assertEquals(title, out.titleSearched());
    assertTrue(out.cached());
    assertEquals(Set.of("cache"), out.sourcesUsed());
    assertEquals(List.of("9780306406157", "0306406152"), out.allIsbns());
    assertEquals("9780306406157", out.primaryIsbn13());
    assertEquals(IsbnLookupMode.ANY, out.mode());

    verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  void anyMode_mergesAllNormalized_and_writesCache_withSortedProvidersInKey() {
    // Arrange
    String title = " The Pragmatic  Programmer ";
    IsbnProvider google = provider("google-books", List.of(
        "978-0201616224",
        "0 201 61622 x",
        "foo"
    ));
    IsbnProvider openlib = provider("openlibrary", List.of(
        "020161622X",
        "9780201616224"
    ));

    var sut = serviceWithProviders(openlib, google);
    when(valueOps.get(anyString())).thenReturn(null);

    IsbnLookupResult out = sut.getIsbnsByTitle(title, IsbnLookupMode.ANY);

    assertEquals(List.of("9780201616224", "020161622X"), out.allIsbns());
    assertEquals("9780201616224", out.primaryIsbn13());
    assertEquals(Set.of("openlibrary", "google-books"), out.sourcesUsed());
    assertFalse(out.cached());
    assertEquals(IsbnLookupMode.ANY, out.mode());
    assertEquals(title, out.titleSearched());

    ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> valCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Duration> ttlCap = ArgumentCaptor.forClass(Duration.class);
    verify(valueOps).set(keyCap.capture(), valCap.capture(), ttlCap.capture());

    assertEquals("isbn:title:" + normalizeTitleKey(title) +
        ":mode:any:providers:google-books+openlibrary", keyCap.getValue());
    assertEquals("9780201616224,020161622X", valCap.getValue());
    assertEquals(Duration.ofDays(7), ttlCap.getValue());
  }

  @Test
  void bothMode_includesOnlyIsbnsPresentInAtLeastTwoProviders_and_writesCache() {
    String title = "Domain-Driven Design";
    IsbnProvider google = provider("google-books", List.of("9780321125217", "0321125215", "noise"));
    IsbnProvider openlib = provider("openlibrary", List.of("9780321125217", "other"));
    IsbnProvider other = provider("some-other", List.of("0321125215"));

    var sut = serviceWithProviders(google, openlib, other);
    when(valueOps.get(anyString())).thenReturn(null);

    IsbnLookupResult out = sut.getIsbnsByTitle(title, IsbnLookupMode.BOTH);

    assertEquals(List.of("9780321125217", "0321125215"), out.allIsbns());
    assertEquals("9780321125217", out.primaryIsbn13());
    assertEquals(Set.of("google-books", "openlibrary", "some-other"), out.sourcesUsed());
    assertFalse(out.cached());
    assertEquals(IsbnLookupMode.BOTH, out.mode());
    assertEquals(title, out.titleSearched());

    verify(valueOps).set(anyString(), eq("9780321125217,0321125215"), eq(Duration.ofDays(7)));
  }

  @Test
  void googleOnly_selectsOnlyGoogleProvider_and_usesItInKey() {
    String title = "Refactoring";
    IsbnProvider google = provider("google-books", List.of("9780201485677", "0201485672"));
    IsbnProvider openlib = provider("openlibrary", List.of("should not be called"));
    IsbnProvider other = provider("x", List.of("should not be called"));

    var sut = serviceWithProviders(google, openlib, other);
    when(valueOps.get(anyString())).thenReturn(null);

    IsbnLookupResult out = sut.getIsbnsByTitle(title, IsbnLookupMode.GOOGLE_ONLY);

    assertEquals(List.of("9780201485677", "0201485672"), out.allIsbns());
    assertEquals("9780201485677", out.primaryIsbn13());
    assertEquals(Set.of("google-books"), out.sourcesUsed());
    assertEquals(IsbnLookupMode.GOOGLE_ONLY, out.mode());
    assertEquals(title, out.titleSearched());

    ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
    verify(valueOps).set(keyCap.capture(), anyString(), any(Duration.class));
    assertTrue(keyCap.getValue().contains(":providers:google-books"));
    assertFalse(keyCap.getValue().contains("openlibrary"));
  }

  @Test
  void providerException_isHandled_and_othersStillUsed_noCacheWhenMergedEmpty() {
    String title = "Some Title";
    IsbnProvider bad = throwingProvider("google-books");
    IsbnProvider empty = provider("openlibrary", List.of("junk", "not an isbn"));
    var sut = serviceWithProviders(bad, empty);
    when(valueOps.get(anyString())).thenReturn(null);

    IsbnLookupResult out = sut.getIsbnsByTitle(title, IsbnLookupMode.ANY);

    assertEquals(List.of(), out.allIsbns());
    assertNull(out.primaryIsbn13());
    assertEquals(Set.of(), out.sourcesUsed());
    assertFalse(out.cached());
    assertEquals(IsbnLookupMode.ANY, out.mode());
    assertEquals(title, out.titleSearched());
    verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  void noSelectedProviders_returnsEmpty_and_readsCacheOnce_but_skipsWrite() {
    String title = "Whatever";
    IsbnProvider onlyGoogle = provider("google-books", List.of("9780306406157"));
    var sut = serviceWithProviders(onlyGoogle);
    when(valueOps.get(anyString())).thenReturn(null);

    IsbnLookupResult out = sut.getIsbnsByTitle(title, IsbnLookupMode.OPENLIBRARY_ONLY);

    assertEquals(List.of(), out.allIsbns());
    assertNull(out.primaryIsbn13());
    assertEquals(Set.of(), out.sourcesUsed());
    assertFalse(out.cached());
    assertEquals(IsbnLookupMode.OPENLIBRARY_ONLY, out.mode());
    assertEquals(title, out.titleSearched());

    verify(valueOps, times(1)).get(anyString());
    verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
  }
}
