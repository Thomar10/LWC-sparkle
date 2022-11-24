package schwaemm;

import java.util.Arrays;
import java.util.function.Consumer;
import sparkle.Sparkle;
import util.ConversionUtil;

public final class Schwaemm {

  private final int TAG_BYTES;
  private final Consumer<int[]> sparkleSlim;
  private final Consumer<int[]> sparkle;
  private final int STATE_WORDS;
  private final int RATE_BYTES;
  private final int RATE_WORDS;
  private final int NONCE_BYTES;
  private final int KEY_WORDS;
  private final int KEY_BYTES;
  private final int CONST_A0;
  private final int CONST_A1;
  private final int CONST_M2;
  private final int CONST_M3;
  private final int CAP_WORDS;

  private final SchwaemmType type;

  public Schwaemm(SchwaemmType type) {
    int SCHWAEMM_KEY_LEN;
    int SCHWAEMM_NONCE_LEN;
    int SCHWAEMM_TAG_LEN;
    int SPARKLE_STATE;
    int SPARKLE_RATE;
    int SPARKLE_CAPACITY;
    switch (type) {
      case S128128 -> {
        SCHWAEMM_KEY_LEN = 128;
        SCHWAEMM_NONCE_LEN = 128;
        SCHWAEMM_TAG_LEN = 128;
        SPARKLE_STATE = 256;
        SPARKLE_RATE = 128;
        SPARKLE_CAPACITY = 128;
        this.sparkleSlim = Sparkle::sparkle256Slim;
        this.sparkle = Sparkle::sparkle256;
        this.type = type;
      }
      case S192192 -> {
        SCHWAEMM_KEY_LEN = 192;
        SCHWAEMM_NONCE_LEN = 192;
        SCHWAEMM_TAG_LEN = 192;
        SPARKLE_STATE = 384;
        SPARKLE_RATE = 192;
        SPARKLE_CAPACITY = 192;
        this.sparkleSlim = Sparkle::sparkle384Slim;
        this.sparkle = Sparkle::sparkle384;
        this.type = type;
      }
      case S256128 -> {
        SCHWAEMM_KEY_LEN = 128;
        SCHWAEMM_NONCE_LEN = 256;
        SCHWAEMM_TAG_LEN = 128;
        SPARKLE_STATE = 384;
        SPARKLE_RATE = 256;
        SPARKLE_CAPACITY = 128;
        this.sparkleSlim = Sparkle::sparkle384Slim;
        this.sparkle = Sparkle::sparkle384;
        this.type = type;
      }
      case S256256 -> {
        SCHWAEMM_KEY_LEN = 256;
        SCHWAEMM_NONCE_LEN = 256;
        SCHWAEMM_TAG_LEN = 256;
        SPARKLE_STATE = 512;
        SPARKLE_RATE = 256;
        SPARKLE_CAPACITY = 256;
        this.sparkleSlim = Sparkle::sparkle512Slim;
        this.sparkle = Sparkle::sparkle512;
        this.type = type;
      }
      default -> throw new RuntimeException("Unknown schwaemm.Schwaemm configuration!");
    }
    KEY_WORDS = SCHWAEMM_KEY_LEN / 32;
    KEY_BYTES = SCHWAEMM_KEY_LEN / 8;
    NONCE_BYTES = SCHWAEMM_NONCE_LEN / 8;
    TAG_BYTES = SCHWAEMM_TAG_LEN / 8;
    STATE_WORDS = SPARKLE_STATE / 32;
    RATE_WORDS = SPARKLE_RATE / 32;
    RATE_BYTES = SPARKLE_RATE / 8;
    CAP_WORDS = SPARKLE_CAPACITY / 32;
    int CAP_BRANS = SPARKLE_CAPACITY / 64;
    CONST_A0 = (1 << CAP_BRANS) << 24;
    CONST_A1 = (1 ^ (1 << CAP_BRANS)) << 24;
    CONST_M2 = (2 ^ (1 << CAP_BRANS)) << 24;
    CONST_M3 = (3 ^ (1 << CAP_BRANS)) << 24;
  }

  public byte[] decryptAndVerify(byte[] cipher, byte[] assoData, byte[] key, byte[] nonce) {
    int[] state = new int[STATE_WORDS];
    int cipherTextLength = cipher.length - TAG_BYTES;
    initialize(state, key, nonce);
    byte[] message = new byte[cipherTextLength];
    if (assoData.length > 0) {
      associateData(state, assoData);
    }
    if (cipherTextLength > 0) {
      decrypt(state, message, cipher);
    }
    finalize(state, key);
    verifyTag(
        state,
        ConversionUtil.createIntArrayFromBytes(
            Arrays.copyOfRange(cipher, cipherTextLength, cipher.length),
            type.getVerifyTagLength()));
    return message;
  }

  void decrypt(int[] state, byte[] message, byte[] cipher) {
    int cipherLength = message.length;
    int[] cipherAsInt = ConversionUtil.createIntArrayFromBytes(
        Arrays.copyOfRange(cipher, 0, cipher.length - TAG_BYTES), (message.length - 1) / 4 + 1);
    int index = 0;
    int messageIndex = 0;
    while (cipherLength > RATE_BYTES) {
      int[] messageInt = new int[state.length / 2];
      rhoWhiDec(state, Arrays.copyOfRange(cipherAsInt, index, cipherAsInt.length), messageInt,
          messageIndex);
      sparkleSlim.accept(state);
      cipherLength -= RATE_BYTES;
      index += RATE_BYTES / 4;
      messageIndex += RATE_BYTES;
      ConversionUtil.populateByteArrayFromInts(messageInt, message, 0, TAG_BYTES, 0);
    }

    state[STATE_WORDS - 1] ^= ((cipherLength < RATE_BYTES) ? CONST_M2 : CONST_M3);

    rhoWhiDecLast(state, Arrays.copyOfRange(cipherAsInt, index, cipherAsInt.length), cipherLength,
        message, messageIndex);
    sparkle.accept(state);
  }

  private void rhoWhiDecLast(int[] state, int[] data, int length, byte[] cipher, int cipherIndex) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, 0, buffer, 0, data.length);

    if (length < RATE_BYTES) {
      ConversionUtil.copyLengthBytesFromStateToBuffer(buffer, state, length, RATE_BYTES - length);
      buffer[length / 4] ^= 128 << (8 * (length % 4));
    }

    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[i];
      int tmp2 = state[j];
      state[i] ^= state[j] ^ buffer[i] ^ state[RATE_WORDS + i];
      state[j] = tmp1 ^ buffer[j] ^ state[RATE_WORDS + capIndex(j)];
      buffer[i] ^= tmp1;
      buffer[j] ^= tmp2;
    }
    ConversionUtil.populateByteArrayFromInts(buffer, cipher, 0, length, cipherIndex);
  }

  private void rhoWhiDec(int[] state, int[] message, int[] cipher, int cipherIndex) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[i];
      int tmp2 = state[j];
      state[i] ^= state[j] ^ message[i] ^ state[RATE_WORDS + i];
      state[j] = tmp1 ^ message[j] ^ state[RATE_WORDS + capIndex(j)];
      cipher[i + cipherIndex] = message[i] ^ tmp1;
      cipher[j + cipherIndex] = message[j] ^ tmp2;
    }
  }

  void verifyTag(int[] state, int[] tag) {
    int diff = 0;
    for (int i = 0; i < (TAG_BYTES / 4); i++) {
      diff |= state[RATE_WORDS + i] ^ tag[i];
    }
    if (diff != 0) {
      throw new RuntimeException("Could not verify tag!");
    }
  }

  public void encryptAndTag(byte[] message, byte[] cipher, byte[] assoData, byte[] key,
      byte[] nonce) {
    int[] state = new int[STATE_WORDS];
    initialize(state, key, nonce);
    if (assoData.length > 0) {
      associateData(state, assoData);
    }
    if (message.length > 0) {
      encrypt(state, message, cipher);
    }
    int[] intKey = ConversionUtil.createIntArrayFromBytes(key, KEY_BYTES / 4);

    finalize(state, intKey);
    generateTag(state, cipher, message.length);
  }

  void generateTag(int[] state, byte[] cipher, int messageLength) {
    ConversionUtil.populateByteArrayFromInts(state, cipher, RATE_WORDS, TAG_BYTES, messageLength);
  }

  void finalize(int[] state, byte[] key) {
    int[] intKey = ConversionUtil.createIntArrayFromBytes(key, KEY_BYTES / 4);
    finalize(state, intKey);
  }

  void finalize(int[] state, int[] key) {
    for (int i = 0; i < KEY_WORDS; i++) {
      state[RATE_WORDS + i] ^= key[i];
    }
  }

  void encrypt(int[] state, byte[] message, byte[] cipherBytes) {
    int msgLength = message.length;
    int[] msgAsInt = ConversionUtil.createIntArrayFromBytes(message, (message.length - 1) / 4 + 1);
    int index = 0;
    int cipherIndex = 0;
    while (msgLength > RATE_BYTES) {
      int[] cipher = new int[state.length / 2];
      rhoWhiEnc(state, Arrays.copyOfRange(msgAsInt, index, msgAsInt.length), cipher, cipherIndex);
      sparkleSlim.accept(state);
      msgLength -= RATE_BYTES;
      index += RATE_BYTES / 4;
      cipherIndex += RATE_BYTES;
      ConversionUtil.populateByteArrayFromInts(cipher, cipherBytes, 0, TAG_BYTES, 0);
    }

    state[STATE_WORDS - 1] ^= msgLength < RATE_BYTES ? CONST_M2 : CONST_M3;

    rhoWhiEncLast(state, Arrays.copyOfRange(msgAsInt, index, msgAsInt.length), msgLength,
        cipherBytes, cipherIndex);
    sparkle.accept(state);
  }

  private void rhoWhiEnc(int[] state, int[] message, int[] cipher, int cipherIndex) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[i];
      int tmp2 = state[j];
      state[i] = state[j] ^ message[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp1 ^ message[j] ^ state[RATE_WORDS + capIndex(j)];
      cipher[i + cipherIndex] = message[i] ^ tmp1;
      cipher[j + cipherIndex] = message[j] ^ tmp2;
    }
  }

  private void rhoWhiEncLast(int[] state, int[] data, int length, byte[] cipher, int cipherIndex) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, 0, buffer, 0, data.length);
    if (length < RATE_BYTES) {
      buffer[length / 4] |= 128 << (8 * (length % 4));
    }

    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[i];
      int tmp2 = state[j];
      state[i] = state[j] ^ buffer[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp1 ^ buffer[j] ^ state[RATE_WORDS + capIndex(j)];
      buffer[i] ^= tmp1;
      buffer[j] ^= tmp2;
    }
    ConversionUtil.populateByteArrayFromInts(buffer, cipher, 0, length, cipherIndex);
  }

  /**
   * Absorbs the data into the state.
   *
   * @param state state
   * @param data data to be absorbed
   */
  void associateData(int[] state, byte[] data) {
    int dataSize = data.length;
    int[] dataAsInt = ConversionUtil.createIntArrayFromBytes(data, (data.length - 1) / 4 + 1);
    int index = 0;
    while (dataSize > RATE_BYTES) {
      rhoWhiAut(state, Arrays.copyOfRange(dataAsInt, index, dataAsInt.length));
      sparkleSlim.accept(state);
      dataSize -= RATE_BYTES;
      index += RATE_BYTES / 4;
    }

    // LAST BLOCK
    state[STATE_WORDS - 1] ^= dataSize < RATE_BYTES ? CONST_A0 : CONST_A1;
    rhoWhiAutLast(state, Arrays.copyOfRange(dataAsInt, index, dataAsInt.length), dataSize);
    sparkle.accept(state);
  }

  private void rhoWhiAut(int[] state, int[] data) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp = state[i];
      state[i] = state[j] ^ data[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp ^ data[j] ^ state[RATE_WORDS + capIndex(j)];
    }
  }

  private int capIndex(int i) {
    if (RATE_WORDS > CAP_WORDS) {
      return i & (CAP_WORDS - 1);
    }
    return i;
  }

  private void rhoWhiAutLast(int[] state, int[] data, int length) {
    int[] buffer = new int[RATE_WORDS];

    System.arraycopy(data, 0, buffer, 0, data.length);

    if (length < RATE_BYTES) {
      buffer[length / 4] |= 128 << (8 * (length % 4));
    }

    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp = state[i];
      state[i] = state[j] ^ buffer[i] ^ state[RATE_WORDS + i];
      state[j] ^= tmp ^ buffer[j] ^ state[RATE_WORDS + capIndex(j)];
    }
  }

  void initialize(int[] state, byte[] key, byte[] nonce) {
    int[] intNonce = ConversionUtil.createIntArrayFromBytes(nonce, NONCE_BYTES / 4);
    System.arraycopy(intNonce, 0, state, 0, intNonce.length);
    int[] intKey = ConversionUtil.createIntArrayFromBytes(key, KEY_BYTES / 4);
    System.arraycopy(intKey, 0, state, RATE_WORDS, intKey.length);
    sparkle.accept(state);
  }
}
