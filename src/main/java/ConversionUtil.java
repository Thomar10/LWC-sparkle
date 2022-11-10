/** Utility class for number conversion. */
public final class ConversionUtil {
  private static final int BYTE_MASK = (1 << 8) - 1;
  public static void intToBytes(int value, byte[] writeBuffer, int offset) {
    writeBuffer[offset] = (byte) value;
    writeBuffer[1 + offset] = (byte) (value >>> 8);
    writeBuffer[2 + offset] = (byte) (value >>> 16);
    writeBuffer[3 + offset] = (byte) (value >>> 24);
  }

  public static int getIntFromBytes(int length, int bufferElement, byte[] bytes) {
    if (length == 0) {
      return bytesToIntSafe(bytes, 0);
    }
    if (length == 1) {
      return bufferElement
          | ((((int) bytes[1]) & BYTE_MASK) << 8)
          | ((((int) bytes[2]) & BYTE_MASK) << 16)
          | ((((int) bytes[3]) & BYTE_MASK) << 24);
    }
    if (length == 2) {
      return bufferElement
          | ((((int) bytes[2]) & BYTE_MASK) << 16)
          | ((((int) bytes[3]) & BYTE_MASK) << 24);
    }
    return bufferElement | ((((int) bytes[3]) & BYTE_MASK) << 24);
  }

  /**
   * Creates an integer from the remaining from offset to offset + 4. Note that the conversion is
   * little endian.
   *
   * @param bytes byte to convert to integer
   * @param offset offset for where to select bytes
   * @return integer consisting of the bytes.
   */
  public static int bytesToIntSafe(byte[] bytes, int offset) {
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

  public static int[] createIntArrayFromBytes(byte[] bytes, int length) {
    int[] result = new int[length];
    for (int i = 0; i < result.length; i++) {
      result[i] = bytesToIntSafe(bytes, i * 4);
    }
    return result;
  }

  public static int intToBytesSafe(int value, byte[] buffer, int offset, int remainingBytes) {
    if (remainingBytes >= 4) {
      intToBytes(value, buffer, offset);
      return remainingBytes - 4;
    }
    if (remainingBytes == 3) {
      buffer[offset] = (byte) value;
      buffer[1 + offset] = (byte) (value >>> 8);
      buffer[2 + offset] = (byte) (value >>> 16);
    }
    if (remainingBytes == 2) {
      buffer[offset] = (byte) value;
      buffer[1 + offset] = (byte) (value >>> 8);
    }
    if (remainingBytes == 1) {
      buffer[offset] = (byte) value;
    }
    return 0;
  }

  public static void copyLengthBytesFromStateToBuffer(
      int[] buffer, int[] state, int length, int rest) {
    byte[] bytesCreated = new byte[4];
    int index = length / 4;
    intToBytes(state[index], bytesCreated, 0);
    // First copy uneven bytes into buffer[index]. If length % 4 = 0 this is the same as
    // copying buffer[index] = state[index] for all indexes.
    buffer[index] = getIntFromBytes(length % 4, buffer[index], bytesCreated);
    for (; rest > 4; rest -= 4, index++) {
      buffer[index + 1] = state[index + 1];
    }
  }

  public static void populateByteArrayFromInts(
      int[] ints, byte[] buffer, int startIndex, int elements, int bufferStartIndex) {
    for (int i = startIndex, j = 0; i < ints.length && elements > 0; i++, j++) {
      elements = intToBytesSafe(ints[i], buffer, j * 4 + bufferStartIndex, elements);
    }
  }
}
