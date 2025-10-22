package pt.psoft.g1.psoftg1.external.service.isbn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class GoogleBooksIsbnProvider implements IsbnProvider {

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${google.books.api.key:}")
  private String apiKey;

  @Override
  public List<String> findIsbnsByTitle(String title) {
    try {
      if (title == null || title.isBlank()) return List.of();

      String q = URLEncoder.encode("intitle:" + title.trim(), StandardCharsets.UTF_8);
      StringBuilder url = new StringBuilder("https://www.googleapis.com/books/v1/volumes?q=")
          .append(q)
          .append("&maxResults=5");
      if (apiKey != null && !apiKey.isBlank()) {
        url.append("&key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
      }

      HttpRequest req = HttpRequest.newBuilder(URI.create(url.toString()))
          .timeout(Duration.ofSeconds(3))
          .GET()
          .build();

      HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 400) return List.of();

      JsonNode root = mapper.readTree(resp.body());
      JsonNode items = root.get("items");
      if (items == null || !items.isArray()) return List.of();

      List<String> out = new ArrayList<>();
      for (JsonNode item : items) {
        JsonNode volumeInfo = item.path("volumeInfo");
        JsonNode ids = volumeInfo.path("industryIdentifiers");
        if (ids.isArray()) {
          for (JsonNode id : ids) {
            if ("ISBN_13".equals(id.path("type").asText())) {
              String v = id.path("identifier").asText();
              if (v != null && !v.isBlank()) out.add(v);
            }
          }
          for (JsonNode id : ids) {
            if ("ISBN_10".equals(id.path("type").asText())) {
              String v = id.path("identifier").asText();
              if (v != null && !v.isBlank()) out.add(v);
            }
          }
        }
      }
      return out;
    } catch (Exception e) {
      return List.of();
    }
  }

  @Override
  public String getName() {
    return "google-books";
  }
}
