package pt.psoft.g1.psoftg1.newTests.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreServiceImpl;
import pt.psoft.g1.psoftg1.genremanagement.services.GetAverageLendingsQuery;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (opaque-box) for GenreServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {

  @Mock GenreRepository genreRepository;
  @Mock IdGenerator idGenerator;

  GenreServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new GenreServiceImpl(genreRepository, idGenerator);
  }

  @Test
  void findByString_delegatesToRepo() {
    when(genreRepository.findByString("Sci-Fi")).thenReturn(Optional.empty());
    assertTrue(service.findByString("Sci-Fi").isEmpty());
    verify(genreRepository).findByString("Sci-Fi");
  }

  @Test
  void findAll_delegatesToRepo() {
    when(genreRepository.findAll()).thenReturn(List.of());
    Iterable<Genre> it = service.findAll();
    assertNotNull(it);
    verify(genreRepository).findAll();
  }

  @Test
  void findTopGenreByBooks_usesFirstPageOfFive() {
    List<GenreBookCountDTO> expected = List.of(mock(GenreBookCountDTO.class));
    @SuppressWarnings("unchecked")
    Page<GenreBookCountDTO> page = (Page<GenreBookCountDTO>) mock(Page.class);
    when(page.getContent()).thenReturn(expected);

    ArgumentCaptor<Pageable> pageCap = ArgumentCaptor.forClass(Pageable.class);
    when(genreRepository.findTop5GenreByBookCount(any())).thenReturn(page);

    List<GenreBookCountDTO> out = service.findTopGenreByBooks();

    assertEquals(expected, out);
    verify(genreRepository).findTop5GenreByBookCount(pageCap.capture());
    Pageable p = pageCap.getValue();
    assertEquals(0, p.getPageNumber());
    assertEquals(5, p.getPageSize());
  }

  @Test
  void save_whenNull_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> service.save(null));
    verify(genreRepository, never()).save(any());
  }

  @Test
  void save_whenNew_assignsPk_andSaves() {
    Genre g = mock(Genre.class);
    when(g.getPk()).thenReturn(null);
    when(idGenerator.newId()).thenReturn("G-123");
    when(genreRepository.save(g)).thenReturn(g);

    Genre out = service.save(g);

    assertSame(g, out);
    verify(idGenerator).newId();
    verify(g).assignPk("G-123");
    verify(genreRepository).save(g);
  }

  @Test
  void save_whenBlankPk_assignsPk_andSaves() {
    Genre g = mock(Genre.class);
    when(g.getPk()).thenReturn("  "); // blank
    when(idGenerator.newId()).thenReturn("G-456");
    when(genreRepository.save(g)).thenReturn(g);

    Genre out = service.save(g);

    assertSame(g, out);
    verify(idGenerator).newId();
    verify(g).assignPk("G-456");
    verify(genreRepository).save(g);
  }

  @Test
  void save_whenExisting_keepsPk_andSaves() {
    Genre g = mock(Genre.class);
    when(g.getPk()).thenReturn("EXISTING");
    when(genreRepository.save(g)).thenReturn(g);

    Genre out = service.save(g);

    assertSame(g, out);
    verify(idGenerator, never()).newId();
    verify(g, never()).assignPk(anyString());
    verify(genreRepository).save(g);
  }

  @Test
  void getLendingsPerMonthLastYearByGenre_delegatesToRepo() {
    List<GenreLendingsPerMonthDTO> expected = List.of(mock(GenreLendingsPerMonthDTO.class));
    when(genreRepository.getLendingsPerMonthLastYearByGenre()).thenReturn(expected);

    List<GenreLendingsPerMonthDTO> out = service.getLendingsPerMonthLastYearByGenre();

    assertEquals(expected, out);
    verify(genreRepository).getLendingsPerMonthLastYearByGenre();
  }

  @Test
  void getAverageLendings_whenPageNull_defaultsPage_andDelegates() {
    GetAverageLendingsQuery query = mock(GetAverageLendingsQuery.class);
    when(query.getYear()).thenReturn(2024);
    when(query.getMonth()).thenReturn(6);

    List<GenreLendingsDTO> expected = List.of(mock(GenreLendingsDTO.class));

    ArgumentCaptor<LocalDate> dateCap = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<pt.psoft.g1.psoftg1.shared.services.Page> pageCap =
        ArgumentCaptor.forClass(pt.psoft.g1.psoftg1.shared.services.Page.class);

    when(genreRepository.getAverageLendingsInMonth(any(), any())).thenReturn(expected);

    List<GenreLendingsDTO> out = service.getAverageLendings(query, null);

    assertEquals(expected, out);
    verify(genreRepository).getAverageLendingsInMonth(dateCap.capture(), pageCap.capture());
    assertEquals(LocalDate.of(2024, 6, 1), dateCap.getValue());
    assertNotNull(pageCap.getValue(), "Default Page should be created when null is passed");
  }

  @Test
  void getAverageLendings_whenPageProvided_passesThrough() {
    GetAverageLendingsQuery query = mock(GetAverageLendingsQuery.class);
    when(query.getYear()).thenReturn(2025);
    when(query.getMonth()).thenReturn(1);

    pt.psoft.g1.psoftg1.shared.services.Page pageArg =
        new pt.psoft.g1.psoftg1.shared.services.Page(2, 25);
    List<GenreLendingsDTO> expected = List.of();

    ArgumentCaptor<LocalDate> dateCap = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<pt.psoft.g1.psoftg1.shared.services.Page> pageCap =
        ArgumentCaptor.forClass(pt.psoft.g1.psoftg1.shared.services.Page.class);

    when(genreRepository.getAverageLendingsInMonth(any(), any())).thenReturn(expected);

    List<GenreLendingsDTO> out = service.getAverageLendings(query, pageArg);

    assertSame(expected, out);
    verify(genreRepository).getAverageLendingsInMonth(dateCap.capture(), pageCap.capture());
    assertEquals(LocalDate.of(2025, 1, 1), dateCap.getValue());
    assertSame(pageArg, pageCap.getValue(), "Provided Page instance should be passed through");
  }

  @Test
  void getLendingsAverageDurationPerMonth_parsesDates_callsRepo_andReturns() {
    List<GenreLendingsPerMonthDTO> expected = List.of(mock(GenreLendingsPerMonthDTO.class));
    ArgumentCaptor<LocalDate> startCap = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<LocalDate> endCap = ArgumentCaptor.forClass(LocalDate.class);

    when(genreRepository.getLendingsAverageDurationPerMonth(any(), any())).thenReturn(expected);

    List<GenreLendingsPerMonthDTO> out =
        service.getLendingsAverageDurationPerMonth("2024-01-01", "2024-12-31");

    assertEquals(expected, out);
    verify(genreRepository).getLendingsAverageDurationPerMonth(startCap.capture(), endCap.capture());
    assertEquals(LocalDate.of(2024, 1, 1), startCap.getValue());
    assertEquals(LocalDate.of(2024, 12, 31), endCap.getValue());
  }

  @Test
  void getLendingsAverageDurationPerMonth_whenDateParseFails_throwsIllegalArgument() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.getLendingsAverageDurationPerMonth("2024/01/01", "2024-12-31"));
    assertEquals("Expected format is YYYY-MM-DD", ex.getMessage());
    verify(genreRepository, never()).getLendingsAverageDurationPerMonth(any(), any());
  }

  @Test
  void getLendingsAverageDurationPerMonth_whenStartAfterEnd_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class,
        () -> service.getLendingsAverageDurationPerMonth("2024-12-31", "2024-01-01"));
    verify(genreRepository, never()).getLendingsAverageDurationPerMonth(any(), any());
  }

  @Test
  void getLendingsAverageDurationPerMonth_whenRepoReturnsEmpty_throwsNotFound() {
    when(genreRepository.getLendingsAverageDurationPerMonth(any(), any())).thenReturn(List.of());
    assertThrows(NotFoundException.class,
        () -> service.getLendingsAverageDurationPerMonth("2024-01-01", "2024-01-31"));
  }
}
