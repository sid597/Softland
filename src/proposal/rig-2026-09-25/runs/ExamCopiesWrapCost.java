// Probe for EXAMINATION-copies.md: the cost of wrapping a 32-byte value lock
// under a person's lock, symmetric (AES-256-GCM under a 32-byte person lock,
// as rig.store.locks does) against public-key (X25519 ephemeral agreement,
// SHA-256 of the shared secret as the wrapping lock, then AES-256-GCM).
// One JVM thread, no cluster. Run: java ExamCopiesWrapCost.java
import javax.crypto.*; import javax.crypto.spec.*; import java.security.*;
public class ExamCopiesWrapCost {
  static SecureRandom rnd = new SecureRandom();
  static byte[] seal(byte[] k, byte[] plain) throws Exception {
    byte[] n = new byte[12]; rnd.nextBytes(n);
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, "AES"), new GCMParameterSpec(128, n));
    return c.doFinal(plain);
  }
  public static void main(String[] a) throws Exception {
    byte[] K = new byte[32]; rnd.nextBytes(K);
    byte[] person = new byte[32]; rnd.nextBytes(person);
    KeyPairGenerator g = KeyPairGenerator.getInstance("X25519");
    KeyPair bob = g.generateKeyPair();
    MessageDigest sha = MessageDigest.getInstance("SHA-256");
    int warm = 3000, n = 10000;
    for (int round = 0; round < 2; round++) {
      int it = round == 0 ? warm : n;
      long t0 = System.nanoTime();
      for (int i = 0; i < it; i++) seal(person, K);
      long t1 = System.nanoTime();
      for (int i = 0; i < it; i++) {           // write side: ephemeral + agreement + seal
        KeyPair e = g.generateKeyPair();
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(e.getPrivate()); ka.doPhase(bob.getPublic(), true);
        seal(sha.digest(ka.generateSecret()), K);
      }
      long t2 = System.nanoTime();
      KeyPair e = g.generateKeyPair();
      for (int i = 0; i < it; i++) {           // open side: one agreement with the secret
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(bob.getPrivate()); ka.doPhase(e.getPublic(), true);
        sha.digest(ka.generateSecret());
      }
      long t3 = System.nanoTime();
      if (round == 1) {
        System.out.printf("java %s, %d iterations each%n", System.getProperty("java.version"), it);
        System.out.printf("symmetric wrap (AES-256-GCM of 32 bytes):        %8.2f us%n", (t1 - t0) / 1e3 / it);
        System.out.printf("public-key wrap (X25519 ephemeral + agree + GCM): %8.2f us%n", (t2 - t1) / 1e3 / it);
        System.out.printf("public-key open step (X25519 agree + SHA-256):    %8.2f us%n", (t3 - t2) / 1e3 / it);
      }
    }
  }
}
