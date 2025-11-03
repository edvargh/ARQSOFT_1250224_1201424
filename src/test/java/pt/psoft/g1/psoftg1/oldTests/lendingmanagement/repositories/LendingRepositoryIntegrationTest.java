package pt.psoft.g1.psoftg1.oldTests.lendingmanagement.repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
public class LendingRepositoryIntegrationTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class TestAuditConfig {
        @Bean
        public AuditorAware<String> auditorAware() {
            return () -> Optional.of("test-user");
        }
    }

    @Autowired
    private LendingRepository lendingRepository;
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private IdGenerator idGenerator;

    private Lending lending;
    private ReaderDetails readerDetails;
    private Reader reader;
    private Book book;
    private Author author;
    private Genre genre;

    @BeforeEach
    void setUp() {
        // AUTHOR
        author = new Author(
            "Manuel Antonio Pina",
            "Manuel António Pina foi um jornalista e escritor português, premiado em 2011 com o Prémio Camões",
            null
        );
        author.assignId(idGenerator.newId("AUT_"));
        author = authorRepository.save(author);

        // GENRE
        genre = new Genre("Género");
        genre.assignPk(idGenerator.newId("GEN_"));
        genre = genreRepository.save(genre);

        // BOOK
        List<Author> authors = List.of(author);
        book = new Book(
            "9782826012092",
            "O Inspetor Max",
            "conhecido pastor-alemão que trabalha para a Judiciária, ...",
            genre,
            authors,
            null
        );
        book.assignPk(idGenerator.newId("BOOK_"));
        book = bookRepository.save(book);

        // READER / READER DETAILS
        reader = Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives");
        reader.assignId(idGenerator.newId("USR_"));
        reader = userRepository.save(reader);

        readerDetails = new ReaderDetails(
            1, reader, "2000-01-01", "919191919",
            true, true, true, null, null
        );
        readerDetails.assignId(idGenerator.newId("RED_"));
        readerDetails = readerRepository.save(readerDetails);

        // LENDING
        lending = Lending.newBootstrappingLending(
            book, readerDetails,
            LocalDate.now().getYear(), 999,
            LocalDate.of(LocalDate.now().getYear(), 1, 1),
            LocalDate.of(LocalDate.now().getYear(), 1, 11),
            15, 300
        );
        lending.assignId(idGenerator.newId("LND_"));
        lending = lendingRepository.save(lending);
    }

    @AfterEach
    void tearDown() {
        try { if (lending != null) lendingRepository.delete(lending); } catch (Exception ignored) {}
        try { if (readerDetails != null) readerRepository.delete(readerDetails); } catch (Exception ignored) {}
        try { if (reader != null) userRepository.delete(reader); } catch (Exception ignored) {}
        try { if (book != null) bookRepository.delete(book); } catch (Exception ignored) {}
        try { if (genre != null) genreRepository.delete(genre); } catch (Exception ignored) {}
        try { if (author != null) authorRepository.delete(author); } catch (Exception ignored) {}
    }

    @Test
    public void testSave() {
        Lending newLending = new Lending(lending.getBook(), lending.getReaderDetails(), 2, 14, 50);
        newLending.assignId(idGenerator.newId("LND_"));
        Lending savedLending = lendingRepository.save(newLending);
        assertThat(savedLending).isNotNull();
        assertThat(savedLending.getLendingNumber()).isEqualTo(newLending.getLendingNumber());
        lendingRepository.delete(savedLending);
    }

    @Test
    public void testFindByLendingNumber() {
        String ln = lending.getLendingNumber();
        Optional<Lending> found = lendingRepository.findByLendingNumber(ln);
        assertThat(found).isPresent();
        assertThat(found.get().getLendingNumber()).isEqualTo(ln);
    }

    @Test
    public void testListByReaderNumberAndIsbn() {
        List<Lending> lendings = lendingRepository.listByReaderNumberAndIsbn(lending.getReaderDetails().getReaderNumber(), lending.getBook().getIsbn());
        assertThat(lendings).isNotEmpty();
        assertThat(lendings).contains(lending);
    }

    @Test
    public void testGetCountFromCurrentYear() {
        int count = lendingRepository.getCountFromCurrentYear();
        assertThat(count).isEqualTo(1);
        var lending2 = Lending.newBootstrappingLending(book,
                readerDetails,
                LocalDate.now().getYear(),
                998,
                LocalDate.of(LocalDate.now().getYear(), 5,31),
                null,
                15,
                300);
        lending2.assignId(idGenerator.newId("LND_"));
        lendingRepository.save(lending2);
        count = lendingRepository.getCountFromCurrentYear();
        assertThat(count).isEqualTo(2);
    }

    @Test
    public void testListOutstandingByReaderNumber() {
        var lending2 = Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                998,
                LocalDate.of(2024, 5,31),
                null,
                15,
                300);
        lending2.assignId(idGenerator.newId("LND_"));
        lendingRepository.save(lending2);
        List<Lending> outstandingLendings = lendingRepository.listOutstandingByReaderNumber(lending.getReaderDetails().getReaderNumber());

        assertThat(outstandingLendings)
            .extracting(Lending::getLendingNumber)
            .contains(lending2.getLendingNumber());
    }

    @Test
    public void testGetAverageDuration() {
        double lendingDuration1 = ChronoUnit.DAYS.between(lending.getStartDate(), lending.getReturnedDate());
        Double averageDuration = lendingRepository.getAverageDuration();
        assertNotNull(averageDuration);
        assertEquals(lendingDuration1, lendingRepository.getAverageDuration(), 0.001);

        var lending2 = Lending.newBootstrappingLending(
            book, readerDetails, 2024, 998,
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 4, 4),
            15, 300);
        lending2.assignId(idGenerator.newId("LND_"));
        lendingRepository.save(lending2);
        double lendingDuration2 = ChronoUnit.DAYS.between(lending2.getStartDate(), lending2.getReturnedDate());
        double expectedAvg = (lendingDuration1 + lendingDuration2) / 2 ;
        assertEquals(expectedAvg, lendingRepository.getAverageDuration(), 0.001);

        var lending3 = Lending.newBootstrappingLending(
            book, readerDetails, 2024, 997,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 4, 25),
            15, 300);
        lending3.assignId(idGenerator.newId("LND_"));
        lendingRepository.save(lending3);
        double lendingDuration3 = ChronoUnit.DAYS.between(lending3.getStartDate(), lending3.getReturnedDate());
        expectedAvg = (lendingDuration1 + lendingDuration2 + lendingDuration3) / 3 ;
        assertEquals(expectedAvg, lendingRepository.getAverageDuration(), 0.001);

    }

    @Test
    public void testGetOverdue() {
        var returnedLateLending = Lending.newBootstrappingLending(
            book, readerDetails, 2024, 998,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 2, 1),
            15, 300);
        returnedLateLending.assignId(idGenerator.newId("LND_"));
        lendingRepository.save(returnedLateLending);

        var notReturnedLending = Lending.newBootstrappingLending(
            book, readerDetails, 2024, 997,
            LocalDate.of(2024, 3, 1),
            null,
            15, 300);
        notReturnedLending.assignId(idGenerator.newId("LND_"));
        lendingRepository.save(notReturnedLending);

        var today = LocalDate.now();
        var notReturnedAndNotOverdueLending = Lending.newBootstrappingLending(
            book, readerDetails, 2024, 996,
            today,
            null,
            15, 300);
        notReturnedAndNotOverdueLending.assignId(idGenerator.newId("LND_"));
        lendingRepository.save(notReturnedAndNotOverdueLending);
        Page page = new Page(1, 10);
        List<Lending> overdueLendings = lendingRepository.getOverdue(page);
        var overdueIds = overdueLendings.stream().map(Lending::getId).toList();

        assertThat(overdueIds).doesNotContain(returnedLateLending.getId());
        assertThat(overdueIds).contains(notReturnedLending.getId());
        assertThat(overdueIds).doesNotContain(notReturnedAndNotOverdueLending.getId());
    }
}
