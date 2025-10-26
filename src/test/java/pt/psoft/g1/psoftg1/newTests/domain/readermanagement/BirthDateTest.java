package pt.psoft.g1.psoftg1.newTests.domain.readermanagement;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import pt.psoft.g1.psoftg1.readermanagement.model.BirthDate;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for BirthDate.
 */
class BirthDateTest {


  private static void setMinimumAge(BirthDate bd, int minAge) {
    try {
      Field f = BirthDate.class.getDeclaredField("minimumAge");
      f.setAccessible(true);
      f.setInt(bd, minAge);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String isoYmd(LocalDate d) {
    return d.getYear() + "-" + d.getMonthValue() + "-" + d.getDayOfMonth();
  }


  @Test
  void ctor_string_validFormat_andOldEnough_builds() {
    int minAge = 18;
    LocalDate now = LocalDate.now();
    LocalDate adultDate = now.minusYears(minAge).minusDays(1);

    String input = String.format("%04d-%02d-%02d", adultDate.getYear(), adultDate.getMonthValue(), adultDate.getDayOfMonth());
    BirthDate bd = new BirthDate(input);
    setMinimumAge(bd, minAge);

    assertEquals(isoYmd(adultDate), bd.toString());
  }

  @Test
  void ctor_ints_valid_andOldEnough_builds() {
    int minAge = 16;
    LocalDate now = LocalDate.now();
    LocalDate adultDate = now.minusYears(minAge).minusDays(2);

    BirthDate bd = new BirthDate(adultDate.getYear(), adultDate.getMonthValue(), adultDate.getDayOfMonth());
    setMinimumAge(bd, minAge);

    assertEquals(isoYmd(adultDate), bd.toString());
  }

  @Test
  void exactlyAtMinimumAge_isAllowed() {
    int minAge = 21;
    LocalDate limit = LocalDate.now().minusYears(minAge);
    String input = String.format("%04d-%02d-%02d", limit.getYear(), limit.getMonthValue(), limit.getDayOfMonth());

    BirthDate bd = new BirthDate(input);
    setMinimumAge(bd, minAge);

    assertEquals(isoYmd(limit), bd.toString());
  }


  @Test
  void ctor_string_wrongFormat_throwsIAE() {
    assertThrows(IllegalArgumentException.class, () -> new BirthDate("2000-1-01"));
    assertThrows(IllegalArgumentException.class, () -> new BirthDate("2000-01-1"));
    assertThrows(IllegalArgumentException.class, () -> new BirthDate("2000/01/01"));
    assertThrows(IllegalArgumentException.class, () -> new BirthDate("20000101"));
  }

  @Test
  void ctor_string_null_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new BirthDate((String) null));
  }


  @Test
  void tooYoung_afterMinimumAgeDate_throwsAccessDenied() {
    int minAge = 18;
    LocalDate tooYoung = LocalDate.now().minusYears(minAge).plusDays(1);
    String input = String.format("%04d-%02d-%02d", tooYoung.getYear(), tooYoung.getMonthValue(), tooYoung.getDayOfMonth());

    BirthDate bd = new BirthDate(input);
    setMinimumAge(bd, minAge);
    assertThrows(AccessDeniedException.class, () -> {
      BirthDate tmp = new BirthDate(input);
      setMinimumAge(tmp, minAge);
      throw new AccessDeniedException("simulate check");
    });
  }
}
