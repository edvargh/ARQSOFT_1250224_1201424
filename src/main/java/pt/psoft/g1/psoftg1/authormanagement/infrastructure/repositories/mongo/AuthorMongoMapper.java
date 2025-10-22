package pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.mongo;

import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.authormanagement.model.Bio;

public class AuthorMongoMapper {

  public AuthorDoc toDoc(Author a) {
    if (a == null) return null;
    AuthorDoc d = new AuthorDoc();
    d.setAuthorNumber(a.getId());
    d.setName(a.getName());
    d.setBio(a.getBio());
    return d;
  }

  public Author toDomain(AuthorDoc d) {
    if (d == null) return null;
    Author a = new Author(d.getName(), d.getBio(), null);
    try {
      var f = Author.class.getDeclaredField("authorNumber");
      f.setAccessible(true);
      f.set(a, d.getAuthorNumber());
    } catch (Exception ignored) {}

    try {
      var f = Author.class.getDeclaredField("version");
      f.setAccessible(true);
      f.set(a, d.getVersion() == null ? 0L : d.getVersion());
    } catch (Exception ignored) {}

    return a;
  }
}
