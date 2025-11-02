package pt.psoft.g1.psoftg1.newTests.unit.isbn;

import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.external.service.isbn.GoogleBooksIsbnProvider;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleBooksIsbnProviderTest {

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  void findIsbnsByTitle_whenNullOrBlank_returnsEmpty() {
    var provider = new GoogleBooksIsbnProvider();
    assertTrue(provider.findIsbnsByTitle(null).isEmpty());
    assertTrue(provider.findIsbnsByTitle("   ").isEmpty());
  }

  @Test
  void findIsbnsByTitle_whenHttpError_returnsEmpty() throws Exception {
    var provider = new GoogleBooksIsbnProvider();

    HttpClient mockClient = mock(HttpClient.class);
    @SuppressWarnings("unchecked")
    HttpResponse<String> resp = (HttpResponse<String>) mock(HttpResponse.class);
    when(resp.statusCode()).thenReturn(500);
    when(resp.body()).thenReturn("{}");
    doReturn(resp).when(mockClient).send(
        any(HttpRequest.class),
        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
    );

    setField(provider, "http", mockClient);

    List<String> out = provider.findIsbnsByTitle("anything");
    assertNotNull(out);
    assertTrue(out.isEmpty());
  }

  @Test
  void findIsbnsByTitle_whenException_returnsEmpty() throws Exception {
    var provider = new GoogleBooksIsbnProvider();

    HttpClient mockClient = mock(HttpClient.class);
    when(mockClient.send(any(), any())).thenThrow(new RuntimeException("boom"));
    setField(provider, "http", mockClient);

    List<String> out = provider.findIsbnsByTitle("anything");
    assertNotNull(out);
    assertTrue(out.isEmpty());
  }

  @Test
  void findIsbnsByTitle_parsesIsbn13_thenIsbn10_inOrder() throws Exception {
    var provider = new GoogleBooksIsbnProvider();

    String json = """
      {
        "items": [
          {
            "volumeInfo": {
              "industryIdentifiers": [
                {"type":"ISBN_10", "identifier":"0123456789"},
                {"type":"ISBN_13", "identifier":"9781234567897"}
              ]
            }
          },
          {
            "volumeInfo": {
              "industryIdentifiers": [
                {"type":"OTHER", "identifier":"ignore-me"}
              ]
            }
          }
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
    assertEquals(List.of("9781234567897", "0123456789"), out, "expects ISBN_13 first, then ISBN_10");
  }

  @Test
  void getName_isStable() {
    assertEquals("google-books", new GoogleBooksIsbnProvider().getName());
  }
}
