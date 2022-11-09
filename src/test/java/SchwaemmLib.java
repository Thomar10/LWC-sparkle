import com.sun.jna.Library;
import com.sun.jna.Native;

/** Utility class for loading in C library of schwaemm and exposing desired functions. */
public final class SchwaemmLib {

  private static final SchwaemmC schwaemmCC =
      (SchwaemmC)
          Native.synchronizedLibrary(Native.load("sparkle/libschwaemm.so", SchwaemmC.class));

  public void initialize(int[] state, final byte[] key, final byte[] nonce) {
    schwaemmCC.Initialize(state, key, nonce);
  }

  interface SchwaemmC extends Library {
    void Initialize(int[] state, final byte[] key, final byte[] nonce);

    void ProcessAssocData(int[] state, final byte[] in, int inlen);

    void ProcessPlainText(int[] state, byte[] out, final byte[] in, int inlen);

    void ProcessCipherText(int[] state, byte[] out, final byte[] in, int inlen);

    void Finalize(int[] state, byte[] key);

    void GenerateTag(int[] state, byte[] tag);

    int VerifyTag(int[] state, final byte[] tag);
  }
}
