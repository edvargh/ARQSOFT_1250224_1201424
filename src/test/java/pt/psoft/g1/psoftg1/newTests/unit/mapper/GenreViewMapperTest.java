package pt.psoft.g1.psoftg1.newTests.unit.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreViewMapper;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDTO;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional opaque-box tests for GenreViewMapper.
 */
class GenreViewMapperTest {

  private GenreViewMapper mapper;

  @BeforeEach
  void setup() {
    mapper = Mappers.getMapper(GenreViewMapper.class);
    assertNotNull(mapper);
  }

  @Test
  void toGenreView_mapsGenreField() {
    Genre g = mock(Genre.class);
    when(g.getGenre()).thenReturn("Sci-Fi");

    Object view = mapper.toGenreView(g);

    assertEquals("Sci-Fi", extract(view, "genre"));
  }

  @Test
  void mapStringToGenreView_mapsPlainStringToGenreView() {
    Object view = mapper.mapStringToGenreView("Fantasy");
    assertEquals("Fantasy", extract(view, "genre"));
  }

  @Test
  void toGenreBookCountView_wrapsGenreIntoNestedGenreView() {
    GenreBookCountDTO dto = mock(GenreBookCountDTO.class);
    when(dto.getGenre()).thenReturn("Horror");

    Object out = mapper.toGenreBookCountView(dto);

    Object nested = extract(out, "genreView");
    assertNotNull(nested);
    assertEquals("Horror", extract(nested, "genre"));
  }

  @Test
  void toGenreBookCountView_list_preservesOrderAndSize() {
    GenreBookCountDTO d1 = mock(GenreBookCountDTO.class);
    GenreBookCountDTO d2 = mock(GenreBookCountDTO.class);
    when(d1.getGenre()).thenReturn("Drama");
    when(d2.getGenre()).thenReturn("Poetry");

    List<?> out = mapper.toGenreBookCountView(List.of(d1, d2));

    assertEquals(2, out.size());
    assertEquals("Drama",  extract(extract(out.get(0), "genreView"), "genre"));
    assertEquals("Poetry", extract(extract(out.get(1), "genreView"), "genre"));
  }

  @Test
  void toGenreLendingsCountPerMonthView_mapsValuesToLendingsCount() {
    var values = List.of(
        new GenreLendingsDTO("Sci-Fi", 1L),
        new GenreLendingsDTO("Drama", 4L),
        new GenreLendingsDTO("Poetry", 2L)
    );
    var dto = new GenreLendingsPerMonthDTO(2024, 5, values);

    Object view = mapper.toGenreLendingsCountPerMonthView(dto);

    @SuppressWarnings("unchecked")
    List<Object> lendingsCount = (List<Object>) extract(view, "lendingsCount");
    assertNotNull(lendingsCount);
    assertEquals(3, lendingsCount.size());
    assertEquals("Sci-Fi", extract(lendingsCount.get(0), "genre"));
    assertEquals(1L,      extract(lendingsCount.get(0), "value"));
    assertEquals("Poetry", extract(lendingsCount.get(2), "genre"));
    assertEquals(2L,       extract(lendingsCount.get(2), "value"));
  }

  @Test
  void toGenreLendingsCountPerMonthView_list_preservesItems() {
    var dto1 = new GenreLendingsPerMonthDTO(2024, 1, List.of(new GenreLendingsDTO("X", 2L)));
    var dto2 = new GenreLendingsPerMonthDTO(2024, 2, List.of(new GenreLendingsDTO("Y", 3L), new GenreLendingsDTO("Z", 1L)));

    List<?> out = mapper.toGenreLendingsCountPerMonthView(List.of(dto1, dto2));

    assertEquals(2, out.size());

    @SuppressWarnings("unchecked")
    List<Object> c1 = (List<Object>) extract(out.get(0), "lendingsCount");
    @SuppressWarnings("unchecked")
    List<Object> c2 = (List<Object>) extract(out.get(1), "lendingsCount");

    assertEquals(1, c1.size());
    assertEquals("X", extract(c1.get(0), "genre"));
    assertEquals(2L,  extract(c1.get(0), "value"));

    assertEquals(2, c2.size());
    assertEquals("Y", extract(c2.get(0), "genre"));
    assertEquals(3L,  extract(c2.get(0), "value"));
    assertEquals("Z", extract(c2.get(1), "genre"));
    assertEquals(1L,  extract(c2.get(1), "value"));
  }

  @Test
  void toGenreLendingsAveragePerMonthView_mapsValuesToDurationAverages() {
    var values = List.of(
        new GenreLendingsDTO("Sci-Fi", 2.52),
        new GenreLendingsDTO("Drama", 3.06),
        new GenreLendingsDTO("Poetry", 1.74)
    );
    var dto = new GenreLendingsPerMonthDTO(2025, 2, values);

    Object view = mapper.toGenreLendingsAveragePerMonthView(dto);

    @SuppressWarnings("unchecked")
    List<Object> avg = (List<Object>) extract(view, "durationAverages");
    assertEquals(3, avg.size());
    assertEquals("Sci-Fi", extract(avg.get(0), "genre"));
    assertEquals(2.5,      extract(avg.get(0), "value"));
    assertEquals("Drama",  extract(avg.get(1), "genre"));
    assertEquals(3.1,      extract(avg.get(1), "value"));
    assertEquals("Poetry", extract(avg.get(2), "genre"));
    assertEquals(1.7,      extract(avg.get(2), "value"));
  }

  @Test
  void toGenreLendingsAveragePerMonthView_list_preservesItems() {
    var d1 = new GenreLendingsPerMonthDTO(2024, 9, List.of(new GenreLendingsDTO("A", 1.2)));
    var d2 = new GenreLendingsPerMonthDTO(2024,10, List.of(new GenreLendingsDTO("B", 5.0), new GenreLendingsDTO("C", 6.5)));

    List<?> out = mapper.toGenreLendingsAveragePerMonthView(List.of(d1, d2));

    assertEquals(2, out.size());

    @SuppressWarnings("unchecked")
    List<Object> a1 = (List<Object>) extract(out.get(0), "durationAverages");
    @SuppressWarnings("unchecked")
    List<Object> a2 = (List<Object>) extract(out.get(1), "durationAverages");

    assertEquals(1, a1.size());
    assertEquals("A", extract(a1.get(0), "genre"));
    assertEquals(1.2, extract(a1.get(0), "value"));

    assertEquals(2, a2.size());
    assertEquals("B", extract(a2.get(0), "genre"));
    assertEquals(5.0, extract(a2.get(0), "value"));
    assertEquals("C", extract(a2.get(1), "genre"));
    assertEquals(6.5, extract(a2.get(1), "value"));
  }

  private Object extract(Object dto, String prop) {
    String accessor = prop;
    String getter = "get" + capitalize(prop);
    try {
      Method m = dto.getClass().getMethod(accessor);
      return m.invoke(dto);
    } catch (NoSuchMethodException ignored) {
      try {
        Method m2 = dto.getClass().getMethod(getter);
        return m2.invoke(dto);
      } catch (NoSuchMethodException ignored2) {
        try {
          Field f = dto.getClass().getDeclaredField(prop);
          f.setAccessible(true);
          return f.get(dto);
        } catch (Exception e) {
          fail("Cannot access '" + prop + "' on " + dto.getClass().getName() + ": " + e.getMessage());
          return null;
        }
      } catch (Exception e2) {
        fail("Error invoking getter '" + getter + "': " + e2.getMessage());
        return null;
      }
    } catch (Exception e) {
      fail("Error invoking accessor '" + accessor + "': " + e.getMessage());
      return null;
    }
  }

  private String capitalize(String p) {
    if (p == null || p.isEmpty()) return p;
    if (p.startsWith("_")) return "_" + capitalize(p.substring(1));
    return Character.toUpperCase(p.charAt(0)) + p.substring(1);
  }
}
