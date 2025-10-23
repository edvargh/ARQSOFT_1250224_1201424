package pt.psoft.g1.psoftg1.shared.id;

import java.security.SecureRandom;
import java.util.Base64;

public final class Base64UrlIdGenerator implements IdGenerator {
  private static final SecureRandom RNG = new SecureRandom();

  @Override public String newId() { return newId(null); }

  @Override
  public String newId(String prefix) {
    byte[] buf = new byte[16];
    RNG.nextBytes(buf);
    String core = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    return (prefix == null || prefix.isEmpty()) ? core : prefix + core;
  }
}
