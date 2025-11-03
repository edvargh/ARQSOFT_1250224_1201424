package pt.psoft.g1.psoftg1.bookmanagement.services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.external.service.isbn.IsbnProvider;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static pt.psoft.g1.psoftg1.bookmanagement.services.IsbnUtils.*;

@Service
public class IsbnLookupService {

  private final List<IsbnProvider> providers;
  private final StringRedisTemplate redis;
  private final Executor isbnLookupExecutor;

  public IsbnLookupService(List<IsbnProvider> providers,
      StringRedisTemplate redis,
      Executor isbnLookupExecutor) {
    this.providers = providers;
    this.redis = redis;
    this.isbnLookupExecutor = isbnLookupExecutor;
  }

  public IsbnLookupResult getIsbnsByTitle(String title, IsbnLookupMode mode) {
    List<IsbnProvider> selected = selectProviders(mode);

    String providersKey = selected.stream()
        .map(IsbnProvider::getName)
        .sorted()
        .collect(Collectors.joining("+"));

    String key = "isbn:title:" + normalizeTitleKey(title)
        + ":mode:" + mode.name().toLowerCase()
        + ":providers:" + providersKey;

    String cached = redis.opsForValue().get(key);
    if (cached != null && !cached.isBlank()) {
      List<String> list = Arrays.stream(cached.split(","))
          .map(String::trim).filter(s -> !s.isBlank()).toList();
      return IsbnLookupResult.from(title, list, Set.of("cache"), mode, true);
    }

    if (selected.isEmpty()) {
      return IsbnLookupResult.from(title, List.of(), Set.of(), mode, false);
    }

    List<CompletableFuture<ProviderResult>> futures = selected.stream()
        .map(p -> CompletableFuture.supplyAsync(() -> safeCall(p, title), isbnLookupExecutor)
            .orTimeout(3, TimeUnit.SECONDS)
            .exceptionally(ex -> new ProviderResult(p.getName(), List.of())) )
        .toList();

    List<ProviderResult> results = futures.stream().map(CompletableFuture::join).toList();

    List<List<String>> lists = results.stream().map(ProviderResult::isbns).toList();
    Set<String> usedSources = results.stream()
        .filter(r -> !r.isbns().isEmpty())
        .map(ProviderResult::name)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    List<String> merged = switch (mode) {
      case ANY, GOOGLE_ONLY, OPENLIBRARY_ONLY ->
          normalizeAndOrder(results.stream().flatMap(r -> r.isbns().stream()).toList());
      case BOTH -> atLeastTwoAgree(lists);
    };

    if (!merged.isEmpty()) {
      redis.opsForValue().set(key, String.join(",", merged), Duration.ofDays(7));
    }
    return IsbnLookupResult.from(title, merged, usedSources, mode, false);
  }


  private ProviderResult safeCall(IsbnProvider p, String title) {
    try {
      List<String> raw = Optional.ofNullable(p.findIsbnsByTitle(title)).orElse(List.of());
      return new ProviderResult(p.getName(), normalizeAndOrder(raw));
    } catch (Exception e) {
      return new ProviderResult(p.getName(), List.of());
    }
  }

  private List<IsbnProvider> selectProviders(IsbnLookupMode mode) {
    return switch (mode) {
      case GOOGLE_ONLY -> providers.stream()
          .filter(p -> "google-books".equals(p.getName()))
          .toList();
      case OPENLIBRARY_ONLY -> providers.stream()
          .filter(p -> "openlibrary".equals(p.getName()))
          .toList();
      default -> providers;
    };
  }

  private record ProviderResult(String name, List<String> isbns) {}
}
