package schwaemm;

import java.util.Arrays;
import java.util.function.Consumer;
import sparkle.MaskedSparkleFirstOrder;
import util.ConversionUtil;

public final class SchwaemmMasked {

  private final int TAG_BYTES;
  private final Consumer<int[][]> sparkleSlim;
  private final Consumer<int[][]> sparkle;
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

  public SchwaemmMasked(SchwaemmType type) {
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
        this.sparkleSlim = MaskedSparkleFirstOrder::sparkle256Slim;
        this.sparkle = MaskedSparkleFirstOrder::sparkle256;
        this.type = type;
      }
      case S192192 -> {
        SCHWAEMM_KEY_LEN = 192;
        SCHWAEMM_NONCE_LEN = 192;
        SCHWAEMM_TAG_LEN = 192;
        SPARKLE_STATE = 384;
        SPARKLE_RATE = 192;
        SPARKLE_CAPACITY = 192;
        this.sparkleSlim = MaskedSparkleFirstOrder::sparkle384Slim;
        this.sparkle = MaskedSparkleFirstOrder::sparkle384;
        this.type = type;
      }
      case S256128 -> {
        SCHWAEMM_KEY_LEN = 128;
        SCHWAEMM_NONCE_LEN = 256;
        SCHWAEMM_TAG_LEN = 128;
        SPARKLE_STATE = 384;
        SPARKLE_RATE = 256;
        SPARKLE_CAPACITY = 128;
        this.sparkleSlim = MaskedSparkleFirstOrder::sparkle384Slim;
        this.sparkle = MaskedSparkleFirstOrder::sparkle384;
        this.type = type;
      }
      case S256256 -> {
        SCHWAEMM_KEY_LEN = 256;
        SCHWAEMM_NONCE_LEN = 256;
        SCHWAEMM_TAG_LEN = 256;
        SPARKLE_STATE = 512;
        SPARKLE_RATE = 256;
        SPARKLE_CAPACITY = 256;
        this.sparkleSlim = MaskedSparkleFirstOrder::sparkle512Slim;
        this.sparkle = MaskedSparkleFirstOrder::sparkle512;
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

  public byte[][] decryptAndVerify(byte[][] cipher, byte[][] assoData, byte[][] key,
      byte[][] nonce) {
    int[][] state = new int[key.length][STATE_WORDS];
    int cipherTextLength = cipher[0].length - TAG_BYTES;
    initialize(state, key, nonce);
    if (assoData[0].length > 0) {
      associateData(state, assoData);
    }
    byte[][] message = new byte[cipher.length][cipherTextLength];
    if (cipherTextLength > 0) {
      decrypt(state, message, cipher);
    }
    finalize(state, key);
    for (int i = 0; i < cipher.length; i++) {
      verifyTag(
          state[i],
          ConversionUtil.createIntArrayFromBytes(
              Arrays.copyOfRange(cipher[i], cipherTextLength, cipher[0].length),
              type.getVerifyTagLength()));
    }
    return message;
  }

  void decrypt(int[][] state, byte[][] message, byte[][] cipher) {
    int[][] cipherAsInt = new int[state.length][];
    for (int i = 0; i < state.length; i++) {
      cipherAsInt[i] = ConversionUtil.createIntArrayFromBytes(
          Arrays.copyOfRange(cipher[i], 0, cipher[0].length - TAG_BYTES),
          (message[0].length - 1) / 4 + 1);
    }

    int index = 0;
    int messageIndex = 0;
    int cipherLength = message[0].length;
    int cipherAsIntLength = cipherAsInt[0].length;
    boolean slimSparkle = message[0].length > RATE_BYTES;
    for (int i = 0; i < state.length; i++) {
      index = 0;
      messageIndex = 0;
      cipherLength = message[0].length;
      while (cipherLength > RATE_BYTES) {
        int[] messageInt = new int[state[0].length / 2];
        rhoWhiDec(state, Arrays.copyOfRange(cipherAsInt[i], index, cipherAsIntLength), messageInt,
            messageIndex, i);
        cipherLength -= RATE_BYTES;
        index += RATE_BYTES / 4;
        messageIndex += RATE_BYTES;
        ConversionUtil.populateByteArrayFromInts(messageInt, message[i], 0, TAG_BYTES, 0);
      }
    }
    if (slimSparkle) {
      sparkleSlim.accept(state);
    }

    state[0][STATE_WORDS - 1] ^= ((cipherLength < RATE_BYTES) ? CONST_M2 : CONST_M3);
    rhoWhiDecLast(state, Arrays.copyOfRange(cipherAsInt[0], index, cipherAsIntLength),
        cipherLength,
        message[0], messageIndex, 0);
    for (int i = 1; i < state.length; i++) {
      rhoWhiDecLast(state, Arrays.copyOfRange(cipherAsInt[i], index, cipherAsIntLength),
          cipherLength,
          message[i], messageIndex, i);
    }
    sparkle.accept(state);
  }

  private void rhoWhiDecLast(int[][] state, int[] data, int length, byte[] cipher, int cipherIndex,
      int index) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, 0, buffer, 0, data.length);

    if (length < RATE_BYTES) {
      ConversionUtil.copyLengthBytesFromStateToBuffer(buffer, state[index], length,
          RATE_BYTES - length);
      if (index == 0) {
        buffer[length / 4] ^= 128 << (8 * (length % 4));
      }
    }

    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[index][i];
      int tmp2 = state[index][j];
      state[index][i] ^= state[index][j] ^ buffer[i] ^ state[index][RATE_WORDS + i];
      state[index][j] = tmp1 ^ buffer[j] ^ state[index][RATE_WORDS + capIndex(j)];
      buffer[i] ^= tmp1;
      buffer[j] ^= tmp2;
    }
    ConversionUtil.populateByteArrayFromInts(buffer, cipher, 0, length, cipherIndex);
  }

  private void rhoWhiDec(int[][] state, int[] message, int[] cipher, int cipherIndex, int index) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[index][i];
      int tmp2 = state[index][j];
      state[index][i] ^= state[index][j] ^ message[i] ^ state[index][RATE_WORDS + i];
      state[index][j] = tmp1 ^ message[j] ^ state[index][RATE_WORDS + capIndex(j)];
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

  public void encryptAndTag(byte[][] message, byte[][] cipher, byte[][] assoData, byte[][] key,
      byte[][] nonce) {
    int[][] state = new int[nonce.length][STATE_WORDS];
    initialize(state, key, nonce);
    if (assoData[0].length > 0) {
      associateData(state, assoData);
    }
    if (message[0].length > 0) {
      encrypt(state, message, cipher);
    }
    int[][] intKey = new int[key.length][];
    for (int i = 0; i < key.length; i++) {
      intKey[i] = ConversionUtil.createIntArrayFromBytes(key[i], KEY_BYTES / 4);
    }

    finalize(state, intKey);
    generateTag(state, cipher, message[0].length);
  }

  void generateTag(int[][] state, byte[][] cipher, int messageLength) {
    for (int i = 0; i < state.length; i++) {
      ConversionUtil.populateByteArrayFromInts(state[i], cipher[i], RATE_WORDS, TAG_BYTES,
          messageLength);
    }
  }

  void finalize(int[][] state, byte[][] key) {
    int[][] keyInt = new int[state.length][];
    for (int i = 0; i < keyInt.length; i++) {
      keyInt[i] = ConversionUtil.createIntArrayFromBytes(key[i], KEY_BYTES / 4);
    }
    finalize(state, keyInt);
  }

  void finalize(int[][] state, int[][] key) {
    for (int i = 0; i < state.length; i++) {
      for (int j = 0; j < KEY_WORDS; j++) {
        state[i][RATE_WORDS + j] ^= key[i][j];
      }
    }
  }

  void encrypt(int[][] state, byte[][] message, byte[][] cipherBytes) {
    int[][] msgAsInt = new int[message.length][];
    for (int i = 0; i < message.length; i++) {
      msgAsInt[i] = ConversionUtil.createIntArrayFromBytes(message[i],
          (message[0].length - 1) / 4 + 1);
    }
    int msgAsIntLength = msgAsInt[0].length;
    int msgLength = message[0].length;
    int index = 0;
    int cipherIndex = 0;
    boolean slimSparkle = message[0].length > RATE_BYTES;
    for (int i = 0; i < state.length; i++) {
      index = 0;
      cipherIndex = 0;
      msgLength = message[0].length;
      while (msgLength > RATE_BYTES) {
        int[] cipher = new int[state[0].length / 2];
        rhoWhiEnc(state, Arrays.copyOfRange(msgAsInt[i], index, msgAsIntLength), cipher,
            cipherIndex, i);
        msgLength -= RATE_BYTES;
        index += RATE_BYTES / 4;
        cipherIndex += RATE_BYTES;
        ConversionUtil.populateByteArrayFromInts(cipher, cipherBytes[i], 0, TAG_BYTES, 0);
      }
    }
    if (slimSparkle) {
      sparkleSlim.accept(state);
    }
    state[0][STATE_WORDS - 1] ^= msgLength < RATE_BYTES ? CONST_M2 : CONST_M3;
    rhoWhiEncLast(state, Arrays.copyOfRange(msgAsInt[0], index, msgAsIntLength), msgLength,
        cipherBytes[0], cipherIndex, 0);
    for (int i = 1; i < state.length; i++) {
      rhoWhiEncLast(state, Arrays.copyOfRange(msgAsInt[i], index, msgAsIntLength), msgLength,
          cipherBytes[i], cipherIndex, i);
    }
    sparkle.accept(state);
  }

  private void rhoWhiEnc(int[][] state, int[] message, int[] cipher, int cipherIndex, int index) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[index][i];
      int tmp2 = state[index][j];
      state[index][i] = state[index][j] ^ message[i] ^ state[index][RATE_WORDS + i];
      state[index][j] ^= tmp1 ^ message[j] ^ state[index][RATE_WORDS + capIndex(j)];
      cipher[i + cipherIndex] = message[i] ^ tmp1;
      cipher[j + cipherIndex] = message[j] ^ tmp2;
    }
  }

  private void rhoWhiEncLast(int[][] state, int[] data, int length, byte[] cipher, int cipherIndex,
      int index) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, 0, buffer, 0, data.length);
    if (length < RATE_BYTES) {
      if (index == 0) {
        buffer[length / 4] |= 128 << (8 * (length % 4));
      }
    }

    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[index][i];
      int tmp2 = state[index][j];
      state[index][i] = state[index][j] ^ buffer[i] ^ state[index][RATE_WORDS + i];
      state[index][j] ^= tmp1 ^ buffer[j] ^ state[index][RATE_WORDS + capIndex(j)];
      buffer[i] ^= tmp1;
      buffer[j] ^= tmp2;
    }
    ConversionUtil.populateByteArrayFromInts(buffer, cipher, 0, length, cipherIndex);
  }

  void associateData(int[][] state, byte[][] data) {
    int[][] dataAsInt = new int[data.length][];
    for (int i = 0; i < dataAsInt.length; i++) {
      dataAsInt[i] = ConversionUtil.createIntArrayFromBytes(data[i],
          (data[0].length - 1) / 4 + 1);
    }
    int dataSize = 0;
    boolean slimSparkle = data[0].length > RATE_BYTES;
    int dataLength = 0;
    int index = 0;
    for (int i = 0; i < state.length; i++) {
      dataSize = data[0].length;
      dataLength = dataAsInt[0].length;
      index = 0;
      while (dataSize > RATE_BYTES) {
        rhoWhiAut(state, Arrays.copyOfRange(dataAsInt[i], index, dataLength), i);
        dataSize -= RATE_BYTES;
        index += RATE_BYTES / 4;
      }
    }
    if (slimSparkle) {
      sparkleSlim.accept(state);
    }

    state[0][STATE_WORDS - 1] ^= dataSize < RATE_BYTES ? CONST_A0 : CONST_A1;
    rhoWhiAutLast(state, Arrays.copyOfRange(dataAsInt[0], index, dataLength), dataSize, 0);
    for (int i = 1; i < state.length; i++) {
      rhoWhiAutLast(state, Arrays.copyOfRange(dataAsInt[i], index, dataLength), dataSize, i);
    }
    sparkle.accept(state);
  }

  private void rhoWhiAut(int[][] state, int[] data, int index) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp = state[index][i];
      state[index][i] = state[index][j] ^ data[i] ^ state[index][RATE_WORDS + i];
      state[index][j] ^= tmp ^ data[j] ^ state[index][RATE_WORDS + capIndex(j)];
    }
  }

  private int capIndex(int i) {
    if (RATE_WORDS > CAP_WORDS) {
      return i & (CAP_WORDS - 1);
    }
    return i;
  }

  private void rhoWhiAutLast(int[][] state, int[] data, int length, int index) {
    int[] buffer = new int[RATE_WORDS];

    System.arraycopy(data, 0, buffer, 0, data.length);

    if (length < RATE_BYTES) {
      if (index == 0) {
        buffer[length / 4] |= 128 << (8 * (length % 4));
      }
    }

    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp = state[index][i];
      state[index][i] = state[index][j] ^ buffer[i] ^ state[index][RATE_WORDS + i];
      state[index][j] ^= tmp ^ buffer[j] ^ state[index][RATE_WORDS + capIndex(j)];
    }
  }

  void initialize(int[][] state, byte[][] key, byte[][] nonce) {
    for (int i = 0; i < state.length; i++) {
      int[] intNonce = ConversionUtil.createIntArrayFromBytes(nonce[i], NONCE_BYTES / 4);
      System.arraycopy(intNonce, 0, state[i], 0, intNonce.length);
      int[] intKey = ConversionUtil.createIntArrayFromBytes(key[i], KEY_BYTES / 4);
      System.arraycopy(intKey, 0, state[i], RATE_WORDS, intKey.length);
    }
    sparkle.accept(state);
  }
}