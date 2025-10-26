package pt.psoft.g1.psoftg1.newTests.unit.isbn;

import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.external.service.isbn.OpenLibraryIsbnProvider;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpenLibraryIsbnProviderTest {

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  void findIsbnsByTitle_whenNullOrBlank_returnsEmpty() {
    var provider = new OpenLibraryIsbnProvider();
    assertTrue(provider.findIsbnsByTitle(null).isEmpty());
    assertTrue(provider.findIsbnsByTitle("   ").isEmpty());
  }

  @Test
  void findIsbnsByTitle_whenHttpError_returnsEmpty() throws Exception {
    var provider = new OpenLibraryIsbnProvider();

    HttpClient mockClient = mock(HttpClient.class);
    @SuppressWarnings("unchecked")
    HttpResponse<String> resp = (HttpResponse<String>) mock(HttpResponse.class);
    when(resp.statusCode()).thenReturn(429);
    when(resp.body()).thenReturn("{}");
    doReturn(resp).when(mockClient).send(
        any(HttpRequest.class),
        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
    );

    setField(provider, "http", mockClient);

    List<String> out = provider.findIsbnsByTitle("anything");
    assertTrue(out.isEmpty());
  }

  @Test
  void findIsbnsByTitle_whenException_returnsEmpty() throws Exception {
    var provider = new OpenLibraryIsbnProvider();

    HttpClient mockClient = mock(HttpClient.class);
    when(mockClient.send(any(), any())).thenThrow(new RuntimeException("boom"));
    setField(provider, "http", mockClient);

    List<String> out = provider.findIsbnsByTitle("anything");
    assertTrue(out.isEmpty());
  }

  @Test
  void findIsbnsByTitle_parsesAndNormalizes_dedupes_preservesOrder_13Then10() throws Exception {
    var provider = new OpenLibraryIsbnProvider();

    String json = """
      {
        "docs": [
          { "isbn": ["978-1-2345-6789-7", "012345678X", "garbage"] },
          { "isbn": ["9781234567890", "978-1-2345-6789-7"] }
        ]
      }
      """;

    HttpClient mockClient = mock(HttpClient.class);
    @SuppressWarnings("unchecked")
    HttpResponse<String> resp = (HttpResponse<String>) mock(HttpResponse.class);
    when(resp.statusCode()).thenReturn(200);
    when(resp.body()).thenReturn(json);
    doReturn(resp).when(mockClient).send(
        any(HttpRequest.class),
        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
    );

    setField(provider, "http", mockClient);

    List<String> out = provider.findIsbnsByTitle("title");
    assertEquals(List.of("9781234567897", "9781234567890", "012345678X"), out);
  }

  @Test
  void getName_isStable() {
    assertEquals("openlibrary", new OpenLibraryIsbnProvider().getName());
  }
}
