package pt.psoft.g1.psoftg1.external.service.isbn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Component
public class OpenLibraryIsbnProvider implements IsbnProvider {

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public List<String> findIsbnsByTitle(String title) {
    try {
      if (title == null || title.isBlank()) return List.of();

      String encTitle = URLEncoder.encode(title.trim(), StandardCharsets.UTF_8);
      String url = "https://openlibrary.org/search.json"
          + "?title=" + encTitle
          + "&limit=5"
          + "&fields=isbn";

      HttpRequest req = HttpRequest.newBuilder(URI.create(url))
          .timeout(Duration.ofSeconds(3))
          .GET()
          .build();

      HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 400) return List.of();

      JsonNode root = mapper.readTree(resp.body());
      JsonNode docs = root.get("docs");
      if (docs == null || !docs.isArray()) return List.of();

      LinkedHashSet<String> isbn13 = new LinkedHashSet<>();
      LinkedHashSet<String> isbn10 = new LinkedHashSet<>();

      for (JsonNode doc : docs) {
        JsonNode isbns = doc.get("isbn");
        if (isbns == null || !isbns.isArray()) continue;

        for (JsonNode n : isbns) {
          String raw = n.asText(null);
          if (raw == null || raw.isBlank()) continue;
          String normalized = normalizeIsbn(raw);
          if (normalized == null) continue;

          if (normalized.length() == 13) {
            isbn13.add(normalized);
          } else if (normalized.length() == 10) {
            isbn10.add(normalized);
          }
        }
      }

      List<String> out = new ArrayList<>(isbn13.size() + isbn10.size());
      out.addAll(isbn13);
      out.addAll(isbn10);
      return out;

    } catch (Exception e) {
      return List.of();
    }
  }

  private String normalizeIsbn(String raw) {
    String s = raw.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
    if (s.length() == 10 || s.length() == 13) return s;
    return null;
  }

  @Override
  public String getName() {
    return "openlibrary";
  }
}
