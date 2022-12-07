package schwaemm;

import java.util.Arrays;
import java.util.function.Consumer;
import sparkle.MaskedSparkle;
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

  public SchwaemmMasked(SchwaemmType type, MaskedSparkle sparkle) {
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
        this.sparkleSlim = sparkle::sparkle256Slim;
        this.sparkle = sparkle::sparkle256;
        this.type = type;
      }
      case S192192 -> {
        SCHWAEMM_KEY_LEN = 192;
        SCHWAEMM_NONCE_LEN = 192;
        SCHWAEMM_TAG_LEN = 192;
        SPARKLE_STATE = 384;
        SPARKLE_RATE = 192;
        SPARKLE_CAPACITY = 192;
        this.sparkleSlim = sparkle::sparkle384Slim;
        this.sparkle = sparkle::sparkle384;
        this.type = type;
      }
      case S256128 -> {
        SCHWAEMM_KEY_LEN = 128;
        SCHWAEMM_NONCE_LEN = 256;
        SCHWAEMM_TAG_LEN = 128;
        SPARKLE_STATE = 384;
        SPARKLE_RATE = 256;
        SPARKLE_CAPACITY = 128;
        this.sparkleSlim = sparkle::sparkle384Slim;
        this.sparkle = sparkle::sparkle384;
        this.type = type;
      }
      case S256256 -> {
        SCHWAEMM_KEY_LEN = 256;
        SCHWAEMM_NONCE_LEN = 256;
        SCHWAEMM_TAG_LEN = 256;
        SPARKLE_STATE = 512;
        SPARKLE_RATE = 256;
        SPARKLE_CAPACITY = 256;
        this.sparkleSlim = sparkle::sparkle512Slim;
        this.sparkle = sparkle::sparkle512;
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

  static int[] recoverState(int[][] state) {
    int[] result = new int[state[0].length];
    for (int i = 0; i < state[0].length; i++) {
      int resultMask = state[0][i];
      for (int j = 1; j < state.length; j++) {
        resultMask ^= state[j][i];
      }
      result[i] = resultMask;
    }
    return result;
  }

  public static byte[] recoverByteArrays(byte[][] bytes) {
    int[][] maskedInts = new int[bytes.length][(bytes[0].length - 1) / 4 + 1];
    for (int i = 0; i < bytes.length; i++) {
      maskedInts[i] = ConversionUtil.createIntArrayFromBytes(bytes[i],
          (bytes[0].length - 1) / 4 + 1);
    }
    int[] recoveredInts = recoverState(maskedInts);
    byte[] recoveredBytes = new byte[bytes[0].length];
    ConversionUtil.populateByteArrayFromInts(recoveredInts, recoveredBytes, 0,
        recoveredBytes.length, 0);
    return recoveredBytes;
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

    int[] tags = reconstructTag(cipher, cipherTextLength);

    int diff = verifyTag(
        state[0], tags);
    for (int i = 1; i < cipher.length; i++) {
      for (int j = 0; j < (TAG_BYTES / 4); j++) {
        diff ^= state[i][RATE_WORDS + j];
      }
    }
    if (diff != 0) {
      throw new RuntimeException("Could not verify tag!");
    }
    return message;
  }

  int verifyTag(int[] state, int[] tag) {
    int diff = 0;
    for (int i = 0; i < (TAG_BYTES / 4); i++) {
      diff ^= state[RATE_WORDS + i] ^ tag[i];
    }
    return diff;
  }

  int[] reconstructTag(byte[][] cipher, int tagStart) {
    byte[] tagBytes = recoverByteArrays(cipher);
    return ConversionUtil.createIntArrayFromBytes(
        Arrays.copyOfRange(tagBytes, tagStart, cipher[0].length),
        type.getVerifyTagLength());
  }

  void decrypt(int[][] state, byte[][] message, byte[][] cipher) {
    int[][] cipherAsInt = new int[state.length][];
    for (int i = 0; i < state.length; i++) {
      cipherAsInt[i] = ConversionUtil.createIntArrayFromBytesLen(
          cipher[i],
          (message[0].length - 1) / 4 + 1,
          cipher[0].length - TAG_BYTES);
    }

    int index = 0;
    int messageIndex = 0;
    int cipherLength = message[0].length;
    boolean slimSparkle = message[0].length > RATE_BYTES;
    for (int i = 0; i < state.length; i++) {
      index = 0;
      messageIndex = 0;
      cipherLength = message[0].length;
      while (cipherLength > RATE_BYTES) {
        int[] messageInt = new int[state[0].length / 2];
        rhoWhiDec(state, cipherAsInt[i], index, messageInt,
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
    rhoWhiDecLast0(state, cipherAsInt[0], index,
        cipherLength,
        message[0], messageIndex);
    for (int i = 1; i < state.length; i++) {
      rhoWhiDecLast(state, cipherAsInt[i], index,
          cipherLength,
          message[i], messageIndex, i);
    }
    sparkle.accept(state);
  }

  private void rhoWhiDecLast0(int[][] state, int[] data, int dataIndex, int length, byte[] cipher,
      int cipherIndex) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, dataIndex, buffer, 0, data.length - dataIndex);

    if (length < RATE_BYTES) {
      ConversionUtil.copyLengthBytesFromStateToBuffer(buffer, state[0], length,
          RATE_BYTES - length);
      buffer[length / 4] ^= 128 << (8 * (length % 4));
    }

    xorStateDec(state, length, cipher, cipherIndex, 0, buffer);
  }

  private void rhoWhiDecLast(int[][] state, int[] data, int dataIndex, int length, byte[] cipher,
      int cipherIndex,
      int index) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, dataIndex, buffer, 0, data.length - dataIndex);

    if (length < RATE_BYTES) {
      ConversionUtil.copyLengthBytesFromStateToBuffer(buffer, state[index], length,
          RATE_BYTES - length);
    }

    xorStateDec(state, length, cipher, cipherIndex, index, buffer);
  }

  private void xorStateDec(int[][] state, int length, byte[] cipher, int cipherIndex, int index,
      int[] buffer) {
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

  private void rhoWhiDec(int[][] state, int[] message, int msgIndex, int[] cipher, int cipherIndex,
      int index) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp1 = state[index][i];
      int tmp2 = state[index][j];
      state[index][i] ^= state[index][j] ^ message[i + msgIndex] ^ state[index][RATE_WORDS + i];
      state[index][j] = tmp1 ^ message[j + msgIndex] ^ state[index][RATE_WORDS + capIndex(j)];
      cipher[i + cipherIndex] = message[i + msgIndex] ^ tmp1;
      cipher[j + cipherIndex] = message[j + msgIndex] ^ tmp2;
    }
  }


  public void encryptAndTag(byte[][] message, byte[][] cipher, byte[][] assoData, byte[][] key,
      byte[][] nonce) {
    int[][] state = new int[nonce.length][STATE_WORDS];
    initialize(state, key, nonce);
    if (assoData[0].length > 0) {
      associateData(state, assoData);
    }
    int messageLength = message[0].length;
    if (messageLength > 0) {
      encrypt(state, message, cipher);
    }
    int[][] intKey = new int[key.length][];
    for (int i = 0; i < key.length; i++) {
      intKey[i] = ConversionUtil.createIntArrayFromBytes(key[i], KEY_BYTES / 4);
    }

    finalize(state, intKey);
    generateTag(state, cipher, messageLength);
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
    int msgLength = message[0].length;
    int index = 0;
    int cipherIndex = 0;
    int[] cipher = new int[state[0].length / 2];
    boolean slimSparkle = message[0].length > RATE_BYTES;
    for (int i = 0; i < state.length; i++) {
      index = 0;
      cipherIndex = 0;
      msgLength = message[0].length;
      while (msgLength > RATE_BYTES) {
        rhoWhiEnc(state, msgAsInt[i], index, cipher,
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
    rhoWhiEncLast0(state, msgAsInt[0], index, msgLength, cipherBytes[0], cipherIndex);
    for (int i = 1; i < state.length; i++) {
      rhoWhiEncLast(state, msgAsInt[i], index, msgLength,
          cipherBytes[i], cipherIndex, i);
    }
    sparkle.accept(state);
  }

  private void rhoWhiEnc(int[][] state, int[] message, int msgIndex, int[] cipher, int cipherIndex,
      int index) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int iIndex = i + msgIndex;
      int jIndex = j + msgIndex;
      int tmp1 = state[index][i];
      int tmp2 = state[index][j];
      state[index][i] = state[index][j] ^ message[iIndex] ^ state[index][RATE_WORDS + i];
      state[index][j] ^= tmp1 ^ message[jIndex] ^ state[index][RATE_WORDS + capIndex(j)];
      cipher[i + cipherIndex] = message[iIndex] ^ tmp1;
      cipher[j + cipherIndex] = message[jIndex] ^ tmp2;
    }
  }

  private void xorStateEnc(int[][] state, int length, byte[] cipher, int cipherIndex, int index,
      int[] buffer) {
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


  private void rhoWhiEncLast0(int[][] state, int[] data, int dataIndex, int length, byte[] cipher,
      int cipherIndex) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, dataIndex, buffer, 0, data.length - dataIndex);
    if (length < RATE_BYTES) {
      buffer[length / 4] |= 128 << (8 * (length % 4));
    }
    xorStateEnc(state, length, cipher, cipherIndex, 0, buffer);
  }

  private void rhoWhiEncLast(int[][] state, int[] data, int dataIndex, int length, byte[] cipher,
      int cipherIndex,
      int index) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, dataIndex, buffer, 0, data.length - dataIndex);

    xorStateEnc(state, length, cipher, cipherIndex, index, buffer);
  }

  void associateData(int[][] state, byte[][] data) {
    int[][] dataAsInt = new int[data.length][];
    for (int i = 0; i < dataAsInt.length; i++) {
      dataAsInt[i] = ConversionUtil.createIntArrayFromBytes(data[i],
          (data[0].length - 1) / 4 + 1);
    }
    int dataSize = 0;
    boolean slimSparkle = data[0].length > RATE_BYTES;
    int index = 0;
    for (int i = 0; i < state.length; i++) {
      dataSize = data[0].length;
      index = 0;
      while (dataSize > RATE_BYTES) {
        rhoWhiAut(state, dataAsInt[i], index, i);
        dataSize -= RATE_BYTES;
        index += RATE_BYTES / 4;
      }
    }
    if (slimSparkle) {
      sparkleSlim.accept(state);
    }

    state[0][STATE_WORDS - 1] ^= dataSize < RATE_BYTES ? CONST_A0 : CONST_A1;
    rhoWhiAutLast0(state, dataAsInt[0], index, dataSize);
    for (int i = 1; i < state.length; i++) {
      rhoWhiAutLast(state, dataAsInt[i], index, i);
    }
    sparkle.accept(state);
  }

  private void rhoWhiAut(int[][] state, int[] data, int dataIndex, int index) {
    for (int i = 0, j = RATE_WORDS / 2; i < RATE_WORDS / 2; i++, j++) {
      int tmp = state[index][i];
      state[index][i] = state[index][j] ^ data[i + dataIndex] ^ state[index][RATE_WORDS + i];
      state[index][j] ^= tmp ^ data[j + dataIndex] ^ state[index][RATE_WORDS + capIndex(j)];
    }
  }

  private int capIndex(int i) {
    if (RATE_WORDS > CAP_WORDS) {
      return i & (CAP_WORDS - 1);
    }
    return i;
  }

  private void rhoWhiAutLast0(int[][] state, int[] data, int dataIndex, int length) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, dataIndex, buffer, 0, data.length - dataIndex);
    if (length < RATE_BYTES) {
      buffer[length / 4] |= 128 << (8 * (length % 4));
    }
    rhoWhiAut(state, buffer, 0, 0);
  }

  private void rhoWhiAutLast(int[][] state, int[] data, int dataIndex, int index) {
    int[] buffer = new int[RATE_WORDS];
    System.arraycopy(data, dataIndex, buffer, 0, data.length - dataIndex);
    rhoWhiAut(state, buffer, 0, index);
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
