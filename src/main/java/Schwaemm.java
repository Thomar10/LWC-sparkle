import java.util.Arrays;

public final class Schwaemm {

  private static final int SCHWAEMM_KEY_LEN = 128;
  private static final int SCHWAEMM_NONCE_LEN = 128;
  private static final int SCHWAEMM_TAG_LEN = 128;
  private static final int SPARKLE_STATE = 256;
  private static final int SPARKLE_RATE = 128;
  private static final int SPARKLE_CAPACITY = 128;

  private static final int TAG_BYTES = SCHWAEMM_TAG_LEN / 8; // 16
  private static final int STATE_WORDS = SPARKLE_STATE / 32; // 8
  private static final int RATE_BYTES = SPARKLE_RATE / 8; // 16
  private static final int RATE_WORDS = SPARKLE_RATE / 32; // 4
  private static final int NONCE_BYTES = SCHWAEMM_NONCE_LEN / 8; // 16
  private static final int KEY_WORDS = SCHWAEMM_KEY_LEN / 32; // 4
  private static final int KEY_BYTES = SCHWAEMM_KEY_LEN / 8; // 16

  private static final int CAP_BRANS = SPARKLE_CAPACITY / 64; // 2
  private static final int CAP_WORDS = SPARKLE_CAPACITY / 32; // 4

  private static final int CONST_A0 = (1 << CAP_BRANS) << 24;
  private static final int CONST_A1 = (1 ^ (1 << CAP_BRANS)) << 24;
  private static final int CONST_M2 = (2 ^ (1 << CAP_BRANS)) << 24;
  private static final int CONST_M3 = (3 ^ (1 << CAP_BRANS)) << 24;

  public static byte[] encryptAndTag(byte[] message, byte[] assoData, byte[] key, byte[] nonce) {
    int[] state = new int[STATE_WORDS];
    initialize(state, key, nonce);
    associateData(state, assoData);
    byte[] cipherText = encrypt(state, message);
    int[] intKey = createIntArrayFromBytes(key, KEY_BYTES / 4);

    finalize(state, intKey);
    return generateTag(state, cipherText);
  }

  private static byte[] generateTag(int[] state, byte[] cipher) {
    byte[] tag = createByteArrayFromInts(state, TAG_BYTES, RATE_WORDS);
    System.out.println(Arrays.toString(tag));
    System.arraycopy(tag, 0, cipher, 1, TAG_BYTES);
    return cipher;
  }

  private static void finalize(int[] state, int[] key) {
    for (int i = 0; i < KEY_WORDS; i++) {
      state[RATE_WORDS + i] ^= key[i];
    }
  }

  // TODO should encrypt mutate a given buffer like the C or simply return cipher without space for
  // tag, and then genTag copies??
  private static byte[] encrypt(int[] state, byte[] message) {
    // TODO FIND OUT SIZE
    int[] cipher = new int[8];
    int msgLength = message.length;
    int[] msgAsInt = createIntArrayFromBytes(message, message.length);
    int index = 0;
    int cipherIndex = 0;
    while (msgLength > RATE_BYTES) {
      rhoWhiEnc(state, Arrays.copyOfRange(msgAsInt, index, msgAsInt.length), cipher, cipherIndex);
      Sparkle.sparkle256(state);
      msgLength -= RATE_BYTES;
      index++;
      cipherIndex++;
    }

    // Encryption of Last Block

    // addition of constant M2 or M3 to the state
    state[STATE_WORDS - 1] ^= msgLength < RATE_BYTES ? CONST_M2 : CONST_M3;
    // combined Rho and rate-whitening (incl. padding)
    byte[] cipherBytes =
        rhoWhiEncLast(
            state,
            Arrays.copyOfRange(msgAsInt, index, msgAsInt.length),
            msgLength,
            cipher,
            msgLength);
    Sparkle.sparkle256(state);

    return cipherBytes;
  }

  private static void rhoWhiEnc(int[] state, int[] message, int[] cipher, int cipherIndex) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[i];
      int tmp2 = state[j];
      state[i] = state[j] ^ message[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp1 ^ message[j] ^ state[RATE_WORDS + capIndex(j)];
      cipher[i + cipherIndex] = message[i] ^ tmp1;
      cipher[j + cipherIndex] = message[j] ^ tmp2;
    }
  }

  private static byte[] rhoWhiEncLast(
      int[] state, int[] data, int length, int[] cipher, int cipherIndex) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, 0, buffer, 0, length);
    if (length < RATE_WORDS) {
      // Fatter ikke hvad der sker ved *bufptr = 0x80 pt.
      // Ser dog ud til dette virker, må tjekke senere.
      buffer[0] = 32818;
    }
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[i];
      int tmp2 = state[j];
      state[i] = state[j] ^ buffer[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp1 ^ buffer[j] ^ state[RATE_WORDS + capIndex(j)];
      buffer[i] ^= tmp1;
      buffer[j] ^= tmp2;
    }
    byte[] dest = new byte[17];
    memcpyIssh(buffer, dest, cipherIndex);
    return dest;
  }

  /**
   * Absorbs the data into the state.
   *
   * @param state state
   * @param data data to be absorbed
   */
  private static void associateData(int[] state, byte[] data) {
    int dataSize = data.length;
    // TODO make sure this does not die / work on % 4 =/ 0 cases.
    int[] dataAsInt = createIntArrayFromBytes(data, data.length);
    int index = 0;
    while (dataSize > RATE_BYTES) {
      rhoWhiAut(state, Arrays.copyOfRange(dataAsInt, index, dataAsInt.length));
      Sparkle.sparkle256Slim(state);
      dataSize -= RATE_BYTES;
      index++;
    }

    // LAST BLOCK
    state[STATE_WORDS - 1] ^= dataSize < RATE_BYTES ? CONST_A0 : CONST_A1;
    rhoWhiAutLast(state, Arrays.copyOfRange(dataAsInt, index, dataAsInt.length), dataSize);
    Sparkle.sparkle256(state);
  }

  private static void rhoWhiAut(int[] state, int[] data) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp = state[i];
      state[i] = state[j] ^ data[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp ^ data[j] ^ state[RATE_WORDS + capIndex(j)];
    }
  }

  private static int capIndex(int i) {
    if (RATE_WORDS > CAP_WORDS) {
      return i & (CAP_WORDS - 1);
    }
    return i;
  }

  private static void rhoWhiAutLast(int[] state, int[] data, int length) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, 0, buffer, 0, length);
    if (length < RATE_WORDS) {
      // Fatter ikke hvad der sker ved *bufptr = 0x80 pt.
      // Ser dog ud til dette virker, må tjekke senere.
      buffer[0] = 32768;
    }

    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp = state[i];
      state[i] = state[j] ^ buffer[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp ^ buffer[j] ^ state[RATE_WORDS + capIndex(j)];
    }
  }

  private static void initialize(int[] state, byte[] key, byte[] nonce) {
    int[] intNonce = createIntArrayFromBytes(nonce, NONCE_BYTES / 4);
    System.arraycopy(intNonce, 0, state, 0, intNonce.length);

    int[] intKey = createIntArrayFromBytes(key, KEY_BYTES / 4);
    System.arraycopy(intKey, 0, state, RATE_WORDS, intKey.length);
    Sparkle.sparkle256(state);
  }

  private static byte[] createByteArrayFromInts(int[] ints, int length, int startIndex) {
    byte[] result = new byte[length];
    for (int i = startIndex, j = 0; i < ints.length; i++, j++) {
      intToBytes(ints[i], result, j * 4);
    }
    return result;
  }

  // TODO ACTUALLY MAKE THIS XD
  private static byte[] memcpyIssh(int[] src, byte[] dest, int length) {
    dest[0] = getSomething(src[0], length);
    return dest;
  }

  private static int[] createIntArrayFromBytes(byte[] bytes, int length) {
    int[] result = new int[length];
    for (int i = 0; i < result.length; i++) {
      result[i] = bytesToIntSafe(bytes, i * 4);
    }
    return result;
  }

  private static byte getSomething(int number, int something) {
    if (something == 1) {
      return (byte) number;
    } else if (something == 2) {
      return (byte) (number >>> 8);
    } else if (something == 3) {
      return (byte) (number >>> 16);
    }
    return (byte) (number >>> 24);
  }

  private static int bytesToIntSafe(byte[] bytes, int offset) {
    if ((bytes.length - offset) % 4 == 0) {
      return bytesToInt(bytes, offset);
    }
    if ((bytes.length - offset) % 3 == 0) {
      return Byte.toUnsignedInt(bytes[offset]) << 24
          | Byte.toUnsignedInt(bytes[1 + offset]) << 16
          | Byte.toUnsignedInt(bytes[2 + offset]) << 8;
    }
    if ((bytes.length - offset) % 2 == 0) {
      return Byte.toUnsignedInt(bytes[offset]) << 24 | Byte.toUnsignedInt(bytes[1 + offset]) << 16;
    }

    return Byte.toUnsignedInt(bytes[offset]) << 24;
  }

  public static void intToBytes(int value, byte[] writeBuffer, int offset) {
    writeBuffer[0] = (byte) (value >>> 24);
    writeBuffer[1 + offset] = (byte) (value >>> 16);
    writeBuffer[2 + offset] = (byte) (value >>> 8);
    writeBuffer[3 + offset] = (byte) value;
  }

  private static int bytesToInt(byte[] bytes, int offset) {
    return Byte.toUnsignedInt(bytes[offset]) << 24
        | Byte.toUnsignedInt(bytes[1 + offset]) << 16
        | Byte.toUnsignedInt(bytes[2 + offset]) << 8
        | Byte.toUnsignedInt(bytes[3 + offset]);
  }

  public static void main(String[] args) {
    byte[] result =
        encryptAndTag(
            new byte[] {50},
            new byte[] {0},
            new byte[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            new byte[] {20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20});
    System.out.println(Arrays.toString(result));
  }
}
