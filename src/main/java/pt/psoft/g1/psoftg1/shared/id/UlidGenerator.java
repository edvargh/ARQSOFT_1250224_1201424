package pt.psoft.g1.psoftg1.shared.id;

import java.security.SecureRandom;

public final class UlidGenerator implements IdGenerator {
  private static final char[] ALPH = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
  private static final SecureRandom RNG = new SecureRandom();
  private static long lastTs = -1L;

  @Override public String newId() { return newId(null); }

  @Override
  public synchronized String newId(String prefix) {
    long time = System.currentTimeMillis();

    int tries = 100;
    while (time == lastTs && tries-- > 0) {
      try { Thread.sleep(1); } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      }
      time = System.currentTimeMillis();
    }
    if (time == lastTs) {
      time = lastTs + 1;
    }
    lastTs = time;

    byte[] b = new byte[16];

    b[0]=(byte)(time>>>40); b[1]=(byte)(time>>>32); b[2]=(byte)(time>>>24);
    b[3]=(byte)(time>>>16); b[4]=(byte)(time>>>8);  b[5]=(byte) time;

    byte[] rand = new byte[10]; RNG.nextBytes(rand);
    System.arraycopy(rand, 0, b, 6, 10);

    char[] out = new char[26];
    int acc = 0, bits = 0, oi = 0, bi = 0;
    while (oi < 26) {
      if (bits < 5) {
        if (bi < 16) {
          acc = (acc << 8) | (b[bi++] & 0xFF);
          bits += 8;
          continue;
        } else {
          acc <<= (5 - bits);
          bits = 5;
        }
      }
      bits -= 5;
      out[oi++] = ALPH[(acc >>> bits) & 31];
    }

    String core = new String(out);
    return (prefix == null || prefix.isEmpty()) ? core : prefix + core;
  }
}
