package pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.mongo;

import pt.psoft.g1.psoftg1.authormanagement.model.Author;
public class AuthorMongoMapper {

  public AuthorDoc toDoc(Author a) {
    if (a == null) return null;
    AuthorDoc d = new AuthorDoc();
    d.setId(a.getId());
    d.setName(a.getName());
    d.setBio(a.getBio());
    return d;
  }

  public Author toDomain(AuthorDoc d) {
    if (d == null) return null;
    Author a = new Author(d.getName(), d.getBio(), null);
    a.assignId(d.getId());
    return a;
  }
}
