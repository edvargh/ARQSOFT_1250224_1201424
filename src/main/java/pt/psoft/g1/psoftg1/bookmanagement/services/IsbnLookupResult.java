package pt.psoft.g1.psoftg1.bookmanagement.services;

import java.util.List;
import java.util.Set;

import static pt.psoft.g1.psoftg1.bookmanagement.services.IsbnUtils.isIsbn13;

public record IsbnLookupResult(
    String titleSearched,
    String primaryIsbn13,
    List<String> allIsbns,
    Set<String> sourcesUsed,
    IsbnLookupMode mode,
    boolean cached
) {
  static IsbnLookupResult from(String title, List<String> merged, Set<String> sources,
      IsbnLookupMode mode, boolean cached) {
    String primary13 = merged.stream().filter(IsbnUtils::isIsbn13).findFirst().orElse(null);
    return new IsbnLookupResult(title, primary13, merged, sources, mode, cached);
  }
}
