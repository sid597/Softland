// Phase 4 plan validation probe (26 Sept 2026, no cluster): what the JDK's
// X25519 does with a 44-byte public key that is not a key, and with
// small-order points, when the read-out boxes to it. Checks the plan's
// request check ("the public key 44 bytes") against F2.
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import java.util.*;

public class Phase4ValidateX25519 {
  static final byte[] PREFIX = {0x30,0x2a,0x30,0x05,0x06,0x03,0x2b,0x65,0x6e,0x03,0x21,0x00};
  static byte[] spki(byte[] u) { byte[] b = new byte[44]; System.arraycopy(PREFIX,0,b,0,12); System.arraycopy(u,0,b,12,32); return b; }
  static String tryBox(String label, byte[] pub) {
    try {
      PublicKey p;
      try { p = KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(pub)); }
      catch (Throwable t) { return label + ": decode threw " + t.getClass().getName() + ": " + t.getMessage(); }
      label = label + ": decodes; then";
      KeyPair eph = KeyPairGenerator.getInstance("X25519").generateKeyPair();
      KeyAgreement ka = KeyAgreement.getInstance("XDH"); ka.init(eph.getPrivate()); ka.doPhase(p, true);
      byte[] s = ka.generateSecret();
      boolean zero = true; for (byte x : s) if (x != 0) zero = false;
      return label + ": agreement returned " + s.length + " bytes, all zero: " + zero;
    } catch (Throwable t) { return label + " agreement threw " + t.getClass().getName() + ": " + t.getMessage(); }
  }
  public static void main(String[] a) {
    System.out.println("java " + System.getProperty("java.version"));
    byte[] rnd = new byte[44]; new SecureRandom().nextBytes(rnd);
    System.out.println(tryBox("44 random bytes (not SPKI)", rnd));
    byte[] u0 = new byte[32];
    System.out.println(tryBox("SPKI, u = 0 (small order)", spki(u0)));
    byte[] u1 = new byte[32]; u1[0] = 1;
    System.out.println(tryBox("SPKI, u = 1 (small order)", spki(u1)));
    byte[] uff = new byte[32]; Arrays.fill(uff, (byte)0xff);
    System.out.println(tryBox("SPKI, u = 2^256-1 (non-canonical)", spki(uff)));
    KeyPair ok = null; try { ok = KeyPairGenerator.getInstance("X25519").generateKeyPair(); } catch (Exception e) {}
    System.out.println(tryBox("a real public key", ok.getPublic().getEncoded()));
  }
}
