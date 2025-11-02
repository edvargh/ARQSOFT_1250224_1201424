package pt.psoft.g1.psoftg1.authormanagement.services;

import pt.psoft.g1.psoftg1.authormanagement.api.AuthorLendingView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;

import java.util.List;
import java.util.Optional;

public interface AuthorService {

    Iterable<Author> findAll();

    Optional<Author> findByAuthorNumber(String authorId);

    List<Author> findByName(String name);

    Author create(CreateAuthorRequest resource);

    Author partialUpdate(String authorId, UpdateAuthorRequest resource, long desiredVersion);

    List<AuthorLendingView> findTopAuthorByLendings();

    List<Book> findBooksByAuthorNumber(String authorId);

    List<Author> findCoAuthorsByAuthorNumber(String authorId);

    Optional<Author> removeAuthorPhoto(String authorId, long desiredVersion);
}
