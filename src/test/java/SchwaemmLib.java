import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.assertj.core.api.Assertions;

/**
 * Utility class for loading in C library of schwaemm and exposing desired functions.
 */
public final class SchwaemmLib {

  private final SchwaemmC schwaemmC;

  public SchwaemmLib(String schwaemm) {
    String library;
    if (System.getProperty("os.name").contains("Windows")) {
      library = "schwaemm/libschwaemm" + schwaemm + ".dll";
    } else {
      library = "schwaemm/libschwaemm" + schwaemm + ".so";
    }
    schwaemmC = (SchwaemmC) Native.synchronizedLibrary(Native.load(library, SchwaemmC.class));
  }

  public void initialize(int[] state, final byte[] key, final byte[] nonce) {
    schwaemmC.Initialize(state, key, nonce);
  }

  public void processAssocData(int[] state, final byte[] in, int inlen) {
    schwaemmC.ProcessAssocData(state, in, inlen);
  }

  public void ProcessPlainText(int[] state, byte[] out, final byte[] in, int inlen) {
    schwaemmC.ProcessPlainText(state, out, in, inlen);
  }

  public void ProcessCipherText(int[] state, byte[] out, final byte[] in, int inlen) {
    schwaemmC.ProcessCipherText(state, out, in, inlen);
  }

  public void finalize(int[] state, final byte[] key) {
    schwaemmC.Finalize(state, key);
  }

  public void generateTag(int[] state, byte[] tag) {
    schwaemmC.GenerateTag(state, tag);
  }

  public int verifyTag(int[] state, final byte[] tag) {
    return schwaemmC.VerifyTag(state, tag);
  }

  public void stagesWithFinalize(int[] state, final byte[] key, final byte[] nonce,
      final byte[] assocIn, int assocInLen, final byte[] msgIn, int msgInLen, byte[] out) {
    schwaemmC.Initialize(state, key, nonce);
    if (assocInLen > 0) {
      schwaemmC.ProcessAssocData(state, assocIn, assocInLen);
    }
    if (msgInLen > 0) {
      schwaemmC.ProcessPlainText(state, out, msgIn, msgInLen);
    }
    schwaemmC.Finalize(state, key);
  }

  public void stagesWithGenerateTag(int[] state, final byte[] key, final byte[] nonce,
      final byte[] assocIn, int assocInLen, final byte[] msgIn, int msgInLen, byte[] out) {
    schwaemmC.Initialize(state, key, nonce);
    if (assocInLen > 0) {
      schwaemmC.ProcessAssocData(state, assocIn, assocInLen);
    }
    if (msgInLen > 0) {
      schwaemmC.ProcessPlainText(state, out, msgIn, msgInLen);
    }
    schwaemmC.Finalize(state, key);
    byte[] tag = new byte[Schwaemm.TAG_BYTES];
    schwaemmC.GenerateTag(state, tag);
    System.arraycopy(tag, 0, out, msgInLen, Schwaemm.TAG_BYTES);
  }

  public void stagesWithProcessAssocData(int[] state, final byte[] key, final byte[] nonce,
      final byte[] assocIn, int assocInLen) {
    schwaemmC.Initialize(state, key, nonce);
    if (assocInLen > 0) {
      schwaemmC.ProcessAssocData(state, assocIn, assocInLen);
    }
  }

  public void stagesWithProcessPlainText(int[] state, final byte[] key, final byte[] nonce,
      final byte[] assocIn, int assocInLen, final byte[] msgIn, int msgInLen, byte[] out) {
    schwaemmC.Initialize(state, key, nonce);
    if (assocInLen > 0) {
      schwaemmC.ProcessAssocData(state, assocIn, assocInLen);
    }
    if (msgInLen > 0) {
      schwaemmC.ProcessPlainText(state, out, msgIn, msgInLen);
    }
  }

  public void encryptAndTag(byte[] c, final byte[] m, long mlen, final byte[] ad,
      long adlen, final byte[] npub, final byte[] k) {
    Memory memory = new Memory(64);
    int res = schwaemmC.crypto_aead_encrypt(c, memory, m, mlen, ad, adlen, null, npub, k);
    Assertions.assertThat(memory.getLong(0)).isEqualTo(c.length);
    if (res != 0) {
      throw new RuntimeException("Encrypting and tagging went wrong");
    }
  }

  public void decryptAndVerify(byte[] m, final byte[] c, long clen,
      final byte[] ad, long adlen, final byte[] npub, final byte[] k) {
    Memory memory = new Memory(64);
    int res = schwaemmC.crypto_aead_decrypt(m, memory, null, c, clen, ad, adlen, npub, k);
    Assertions.assertThat(memory.getLong(0)).isEqualTo(m.length);
    if (res != 0) {
      throw new RuntimeException("Verification went wrong on C side");
    }
  }


  interface SchwaemmC extends Library {

    void Initialize(int[] state, final byte[] key, final byte[] nonce);

    void ProcessAssocData(int[] state, final byte[] in, int inlen);

    void ProcessPlainText(int[] state, byte[] out, final byte[] in, int inlen);

    void ProcessCipherText(int[] state, byte[] out, final byte[] in, int inlen);

    void Finalize(int[] state, byte[] key);

    void GenerateTag(int[] state, byte[] tag);

    int VerifyTag(int[] state, final byte[] tag);

    int crypto_aead_encrypt(byte[] c, Memory clen, final byte[] m, long mlen, final byte[] ad,
        long adlen, final byte[] nsec, final byte[] npub, final byte[] k);

    int crypto_aead_decrypt(byte[] m, Memory mlen, final byte[] nsec, final byte[] c, long clen,
        final byte[] ad, long adlen, final byte[] npub, final byte[] k);
  }
}
