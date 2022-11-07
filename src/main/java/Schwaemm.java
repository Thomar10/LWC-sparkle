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
  private static final int CONST_A0 = (1 << CAP_BRANS) << 24;
  private static final int CONST_A1 = (1 ^ (1 << CAP_BRANS)) << 24;
  private static final int CONST_M2 = (2 ^ (1 << CAP_BRANS)) << 24;
  private static final int CONST_M3 = (3 ^ (1 << CAP_BRANS)) << 24;
  private static final int CAP_WORDS = SPARKLE_CAPACITY / 32; // 4

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
  static byte[] encrypt(int[] state, byte[] message) {
    // TODO FIND OUT SIZE
    int[] cipher = new int[1000];
    int msgLength = message.length;
    int[] msgAsInt = createIntArrayFromBytes(message, (message.length - 1) / 4 + 1);
    int index = 0;
    int cipherIndex = 0;
    while (msgLength > RATE_BYTES) {
      rhoWhiEnc(state, Arrays.copyOfRange(msgAsInt, index, msgAsInt.length), cipher, cipherIndex);
      Sparkle.sparkle256Slim(state);
      msgLength -= RATE_BYTES;
      index += RATE_BYTES / 4;
      cipherIndex += RATE_BYTES / 4;
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
    System.arraycopy(data, 0, buffer, 0, data.length);
    if (length < RATE_BYTES) {
      buffer[getBufferIndex(length)] |= 128 << (8 * (length % 4));
    }

    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[i];
      int tmp2 = state[j];
      state[i] = state[j] ^ buffer[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp1 ^ buffer[j] ^ state[RATE_WORDS + capIndex(j)];
      buffer[i] ^= tmp1;
      buffer[j] ^= tmp2;
    }
    // TODO copy length amount of bytes into cipher at given index.
    byte[] dest = new byte[17];
//        memcpyIssh(buffer, dest, cipherIndex);
    return dest;
  }

  /**
   * Absorbs the data into the state.
   *
   * @param state state
   * @param data  data to be absorbed
   */
  static void associateData(int[] state, byte[] data) {
    int dataSize = data.length;
    int[] dataAsInt = createIntArrayFromBytes(data, (data.length - 1) / 4 + 1);
    int index = 0;
    while (dataSize > RATE_BYTES) {
      rhoWhiAut(state, Arrays.copyOfRange(dataAsInt, index, dataAsInt.length));
      Sparkle.sparkle256Slim(state);
      dataSize -= RATE_BYTES;
      index += RATE_BYTES / 4;
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

    System.arraycopy(data, 0, buffer, 0, data.length);

    if (length < RATE_BYTES) {
      buffer[getBufferIndex(length)] |= 128 << (8 * (length % 4));
    }

    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp = state[i];
      state[i] = state[j] ^ buffer[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp ^ buffer[j] ^ state[RATE_WORDS + capIndex(j)];
    }
  }

  // TODO this can prob be made a lot smarter
  private static int getBufferIndex(int length) {
    if (length < 4) {
      return 0;
    } else if (length < 8) {
      return 1;
    } else if (length < 12) {
      return 2;
    }
    return 3;
  }

  static void initialize(int[] state, byte[] key, byte[] nonce) {
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


  private static int[] createIntArrayFromBytes(byte[] bytes, int length) {
    int[] result = new int[length];
    for (int i = 0; i < result.length; i++) {
      result[i] = bytesToIntSafe(bytes, i * 4);
    }
    return result;
  }

  private static int bytesToIntSafe(byte[] bytes, int offset) {
    if ((bytes.length - offset) % 4 == 0 || (bytes.length - offset) >= 4) {
      return Byte.toUnsignedInt(bytes[3 + offset]) << 24
          | Byte.toUnsignedInt(bytes[2 + offset]) << 16
          | Byte.toUnsignedInt(bytes[1 + offset]) << 8
          | Byte.toUnsignedInt(bytes[offset]);
    }
    if ((bytes.length - offset) % 3 == 0) {
      return (Byte.toUnsignedInt(bytes[2 + offset]) << 16)
          | (Byte.toUnsignedInt(bytes[1 + offset]) << 8)
          | (Byte.toUnsignedInt(bytes[offset]));
    }
    if ((bytes.length - offset) % 2 == 0) {
      return Byte.toUnsignedInt(bytes[1 + offset]) << 8 | Byte.toUnsignedInt(bytes[offset]);
    }

    return Byte.toUnsignedInt(bytes[offset]);
  }

  public static void intToBytes(int value, byte[] writeBuffer, int offset) {
    writeBuffer[0] = (byte) (value >>> 24);
    writeBuffer[1 + offset] = (byte) (value >>> 16);
    writeBuffer[2 + offset] = (byte) (value >>> 8);
    writeBuffer[3 + offset] = (byte) value;
  }
}
