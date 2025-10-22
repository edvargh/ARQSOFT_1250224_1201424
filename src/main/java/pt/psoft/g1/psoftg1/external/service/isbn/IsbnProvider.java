package pt.psoft.g1.psoftg1.external.service.isbn;

import java.util.List;

public interface IsbnProvider {
  List<String> findIsbnsByTitle(String title);
  String getName();
}
