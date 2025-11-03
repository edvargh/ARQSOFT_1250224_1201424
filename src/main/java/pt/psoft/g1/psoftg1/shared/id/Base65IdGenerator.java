package pt.psoft.g1.psoftg1.shared.id;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class Base65IdGenerator implements IdGenerator {
  // 65 URL-safe symbols: 0-9, A-Z, a-z, '-', '_', '~'
  private static final char[] ALPH = (
      "0123456789" +
          "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
          "abcdefghijklmnopqrstuvwxyz" +
          "-_" +
          "~"
  ).toCharArray();
  private static final int RADIX = ALPH.length;
  private static final int BYTES = 16;
  private static final int FIXED_LEN = 22;
  private static final SecureRandom RNG = new SecureRandom();

  @Override public String newId() { return newId(null); }

  @Override
  public String newId(String prefix) {
    byte[] buf = new byte[BYTES];
    RNG.nextBytes(buf);

    BigInteger x = new BigInteger(1, buf);

    char[] tmp = new char[64];
    int i = 0;
    while (x.signum() != 0) {
      BigInteger[] qr = x.divideAndRemainder(BigInteger.valueOf(RADIX));
      tmp[i++] = ALPH[qr[1].intValue()];
      x = qr[0];
    }
    if (i == 0) tmp[i++] = ALPH[0];

    char[] out = new char[Math.max(FIXED_LEN, i)];
    int outLen = Math.max(FIXED_LEN, i);
    int p = outLen - i;
    for (int k = 0; k < p; k++) out[k] = ALPH[0];
    while (i > 0) out[p++] = tmp[--i];

    String core = new String(out);
    return (prefix == null || prefix.isEmpty()) ? core : prefix + core;
  }
}
