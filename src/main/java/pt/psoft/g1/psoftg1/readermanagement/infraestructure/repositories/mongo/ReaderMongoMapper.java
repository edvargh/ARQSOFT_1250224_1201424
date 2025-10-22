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

  /**
   * Re-hydrate minimal domain object.
   * Note: we only set fields that exist in the domain and are used by services/controllers.
   * Reader entity is re-attached in the repository using UserRepository when possible.
   */
  public ReaderDetails toDomainSkeleton(ReaderDoc d, Reader reader, List<Genre> interests) {
    // Build ReaderDetails through its public constructor to enforce invariants
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
    // fix readerNumber to exact string (constructor builds from seq)
    try {
      var fNum = ReaderDetails.class.getDeclaredField("readerNumber");
      fNum.setAccessible(true);
      fNum.set(rd, new ReaderNumberStringBackdoor(d.getReaderNumber()));
    } catch (Exception ignored) {}

    // set version
    try {
      var fVer = ReaderDetails.class.getDeclaredField("version");
      fVer.setAccessible(true);
      fVer.set(rd, d.getVersion());
    } catch (Exception ignored) {}

    return rd;
  }

  private int extractSequential(String readerNumber) {
    if (readerNumber == null || readerNumber.length() < 6) return 1;
    return Integer.parseInt(readerNumber.substring(readerNumber.indexOf('/') + 1));
  }

  /** little helper to assign exact string into ReaderNumber via reflection */
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
