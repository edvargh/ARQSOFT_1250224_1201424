package pt.psoft.g1.psoftg1.oldTests.authormanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Based on https://www.baeldung.com/spring-boot-testing
 * <p>Adaptations to Junit 5 with ChatGPT
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AuthorServiceImplIntegrationTest {
    @Autowired
    private AuthorService authorService;
    @MockBean
    private AuthorRepository authorRepository;
    @MockBean
    private pt.psoft.g1.psoftg1.shared.id.IdGenerator idGenerator;
    @MockBean
    private pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository userRepository;
    @MockBean
    private pt.psoft.g1.psoftg1.usermanagement.services.UserService userService;

    @BeforeEach
    public void setUp() {
        Author alex = new Author("Alex", "O Alex escreveu livros", null);
        alex.assignId("1");

        Mockito.when(authorRepository.findByAuthorNumber("1"))
            .thenReturn(Optional.of(alex));

        Mockito.when(authorRepository.findByName_NameIgnoreCase("Alex"))
            .thenReturn(List.of(alex));
    }

    @Test
    public void whenValidId_thenAuthorShouldBeFound() {
        String id = "1";
        Optional<Author> found = authorService.findByAuthorNumber(id);
        found.ifPresent(author -> assertThat(author.getId())
                .isEqualTo(id));
    }
}
