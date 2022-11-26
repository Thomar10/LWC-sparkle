package schwaemm;

import java.util.Random;
import util.ConversionUtil;

public record SchwaemmHelper(byte[] key, byte[] nonce, byte[] associate, byte[] message,
                             byte[] cipherC, byte[] cipherJava, int[] stateC, int[] stateJ) {

  static byte[] initBuffer(byte[] buffer) {
    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = (byte) i;
    }
    return buffer;
  }

  static String printBytesAsStringLength(byte[] bytes, int length) {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < length; i++) {
      s.append(String.format("%02X", bytes[i]));
    }
    return s.toString();
  }

  private static final Random random = new Random();

  public static SchwaemmHelper prepareTest(SchwaemmType type, int minLength, Random random) {
    // Tests in C only goes up to 32 bits.
    int randomInt = random.nextInt(32 - minLength) + minLength;
    int randomMsg = random.nextInt(32 - minLength) + minLength;
    byte[] associate = new byte[randomInt];
    byte[] message = new byte[randomMsg];
    random.nextBytes(associate);
    random.nextBytes(message);
    byte[] nonce = new byte[type.getNonceSize()];
    random.nextBytes(nonce);
    byte[] key = new byte[type.getKeySize()];
    random.nextBytes(key);
    byte[] cipherC = new byte[message.length + type.getTagBytes()];
    byte[] cipherJava = new byte[message.length + type.getTagBytes()];
    int[] stateC = new int[type.getStateSize()];
    int[] stateJ = new int[type.getStateSize()];
    for (int i = 0; i < stateC.length; i++) {
      int randomNumber = random.nextInt(Integer.MAX_VALUE);
      stateC[i] = randomNumber;
      stateJ[i] = randomNumber;
    }
    return new SchwaemmHelper(key, nonce, associate, message, cipherC, cipherJava, stateC, stateJ);
  }

  public static MaskedData prepareBenchmarkMasked(SchwaemmType type, Random random, int order) {
    return convertDataToMasked(prepareTest(type, 0, random), order, random);
  }

  public static SchwaemmHelper prepareTest(SchwaemmType type, int length) {
    return prepareTest(type, 0, random);
  }

  public static SchwaemmHelper prepareTest(SchwaemmType type) {
    return prepareTest(type, 0, random);
  }

  public static MaskedData convertDataToMasked(SchwaemmHelper data, int order) {
    return convertDataToMasked(data, order, random);
  }

  public static MaskedData convertDataToMasked(SchwaemmHelper data, int order, Random random) {
    int[][] maskedState = maskIntArray(data.stateC, order);

    return new MaskedData(maskByteArrays(data.key, order), maskByteArrays(data.nonce, order),
        maskByteArrays(data.associate, order), maskByteArrays(data.message, order),
        maskByteArrays(data.cipherC, order), maskedState);
  }

  public static MaskedData convertDataFirstOrder(SchwaemmHelper data) {
    int[][] maskedState = maskIntArray(data.stateC, 2);

    return new MaskedData(maskByteFirstOrder(data.key), maskByteFirstOrder(data.nonce),
        maskByteFirstOrder(data.associate), maskByteFirstOrder(data.message),
        maskByteFirstOrder(data.cipherC), maskedState);
  }

  public static int[][] maskIntArray(int[] ints, int order) {
    int[][] maskedState = new int[order][ints.length];
    for (int i = 1; i < order; i++) {
      for (int j = 0; j < ints.length; j++) {
        int number = random.nextInt(Integer.MAX_VALUE);
        maskedState[i][j] = number;
      }
    }
    for (int j = 0; j < ints.length; j++) {
      int resultMask = ints[j];
      for (int i = 1; i < order; i++) {
        resultMask ^= maskedState[i][j];
      }
      maskedState[0][j] = resultMask;
    }

    return maskedState;
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

  public static SchwaemmHelper recoverSchwaemm(MaskedData data) {

    return new SchwaemmHelper(recoverByteArrays(data.key), recoverByteArrays(data.nonce),
        recoverByteArrays(data.associate), recoverByteArrays(data.message), null,
        recoverByteArrays(data.cipher), null, recoverState(data.state));
  }

  public static byte[][] maskByteFirstOrder(byte[] bytes) {
    return maskByteArrays(bytes, 2);
  }

  public static byte[][] maskByteArrays(byte[] bytes, int order) {
    byte[][] maskedBytes = new byte[order][];
    int[] intAsBytes = ConversionUtil.createIntArrayFromBytes(bytes, (bytes.length + 4 - 1) / 4);
    int[][] maskedInts = maskIntArray(intAsBytes, order);
    for (int i = 0; i < order; i++) {
      byte[] buffer = new byte[bytes.length];
      ConversionUtil.populateByteArrayFromInts(maskedInts[i], buffer, 0, buffer.length, 0);
      maskedBytes[i] = buffer;
    }
    return maskedBytes;
  }

  public static byte[] recoverByteArrays(byte[][] bytes) {
    // TODO FIX if length of bytes[i] == 0 better
    if (bytes[0].length == 0) {
      return new byte[0];
    }
    int[][] maskedInts = new int[bytes.length][(bytes[0].length - 1) / 4 + 1];
    for (int i = 0; i < bytes.length; i++) {
      maskedInts[i] = ConversionUtil.createIntArrayFromBytes(bytes[i], (bytes[0].length - 1) / 4 + 1);
    }
    int[] recoveredInts = recoverState(maskedInts);
    byte[] recoveredBytes = new byte[bytes[0].length];
    ConversionUtil.populateByteArrayFromInts(recoveredInts, recoveredBytes, 0,
        recoveredBytes.length, 0);
    return recoveredBytes;
  }

  public record MaskedData(byte[][] key, byte[][] nonce, byte[][] associate, byte[][] message,
                           byte[][] cipher, int[][] state) {

  }
}

