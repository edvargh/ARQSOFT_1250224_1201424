package pt.psoft.g1.psoftg1.readermanagement.infraestructure.repositories.mongo;

import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.*;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.util.List;

@Component
public class ReaderMongoMapper {

  public ReaderDoc toDoc(ReaderDetails rd) {
    return ReaderDoc.builder()
        .id(extractReaderPk(rd))
        .readerNumber(rd.getReaderNumber())
        .userId(rd.getReader() == null ? null : rd.getReader().getId())
        .username(rd.getReader() == null ? null : rd.getReader().getUsername())
        .fullName(rd.getReader() == null ? null : rd.getReader().getName().toString())
        .birthDate(rd.getBirthDate() == null ? null : rd.getBirthDate().getBirthDate())
        .phoneNumber(rd.getPhoneNumber())
        .gdprConsent(rd.isGdprConsent())
        .marketingConsent(rd.isMarketingConsent())
        .thirdPartySharingConsent(rd.isThirdPartySharingConsent())
        .interestGenres(rd.getInterestList() == null ? null :
            rd.getInterestList().stream().map(Genre::toString).toList())
        .photoFile(rd.getPhoto() == null ? null : rd.getPhoto().getPhotoFile())
        .version(rd.getVersion())
        .build();
  }

  public ReaderDetails toDomainSkeleton(ReaderDoc d, Reader reader, List<Genre> interests) {
    ReaderDetails rd = new ReaderDetails(
        extractSequential(d.getReaderNumber()),
        reader,
        d.getBirthDate() == null ? null : d.getBirthDate().toString(),
        d.getPhoneNumber(),
        d.isGdprConsent(),
        d.isMarketingConsent(),
        d.isThirdPartySharingConsent(),
        d.getPhotoFile() == null ? null : d.getPhotoFile(),
        interests
    );
    try {
      var fNum = ReaderDetails.class.getDeclaredField("readerNumber");
      fNum.setAccessible(true);
      fNum.set(rd, new ReaderNumberStringBackdoor(d.getReaderNumber()));
    } catch (Exception ignored) {}

    try {
      var fVer = ReaderDetails.class.getDeclaredField("version");
      fVer.setAccessible(true);
      fVer.set(rd, d.getVersion());
    } catch (Exception ignored) {}

    return rd;
  }

  private String extractReaderPk(ReaderDetails rd) {
    try {
      var f = ReaderDetails.class.getDeclaredField("pk");
      f.setAccessible(true);
      Object v = f.get(rd);
      return v == null ? null : v.toString();
    } catch (Exception e) {
      return null;
    }
  }

  private int extractSequential(String readerNumber) {
    if (readerNumber == null || readerNumber.length() < 6) return 1;
    return Integer.parseInt(readerNumber.substring(readerNumber.indexOf('/') + 1));
  }

  static class ReaderNumberStringBackdoor extends ReaderNumber {
    public ReaderNumberStringBackdoor(String rn) {
      super();
      try {
        var f = ReaderNumber.class.getDeclaredField("readerNumber");
        f.setAccessible(true);
        f.set(this, rn);
      } catch (Exception ignored) {}
    }
  }
}
