package pt.psoft.g1.psoftg1.bookmanagement.services;

import java.util.*;
import java.util.stream.Collectors;

public final class IsbnUtils {
  private IsbnUtils() {}

  public static String normalize(String isbn) {
    if (isbn == null) return null;
    return isbn.replaceAll("[^0-9Xx]", "").toUpperCase();
  }

  public static boolean isIsbn13(String s) { return s != null && s.matches("^\\d{13}$"); }
  public static boolean isIsbn10(String s) { return s != null && s.matches("^\\d{9}[\\dX]$"); }

  public static boolean looksValid(String s) { return isIsbn13(s) || isIsbn10(s); }

  public static List<String> normalizeAndOrder(List<String> raw) {
    if (raw == null) return List.of();
    LinkedHashSet<String> set = new LinkedHashSet<>();
    for (String r : raw) {
      String n = normalize(r);
      if (looksValid(n)) set.add(n);
    }
    List<String> list = new ArrayList<>(set);
    list.sort((a,b) -> Boolean.compare(!isIsbn13(a), !isIsbn13(b))); // put 13s first
    return list;
  }

  public static List<String> atLeastTwoAgree(List<List<String>> lists) {
    Map<String, Integer> freq = new HashMap<>();
    for (List<String> l : lists) {
      l.stream().map(IsbnUtils::normalize).filter(IsbnUtils::looksValid).distinct()
          .forEach(x -> freq.merge(x, 1, Integer::sum));
    }
    return freq.entrySet().stream()
        .filter(e -> e.getValue() >= 2)
        .map(Map.Entry::getKey)
        .sorted((a,b) -> Boolean.compare(!isIsbn13(a), !isIsbn13(b)))
        .collect(Collectors.toList());
  }

  public static String normalizeTitleKey(String t) {
    return t == null ? "" : t.trim().toLowerCase().replaceAll("\\s+", " ");
  }
}
