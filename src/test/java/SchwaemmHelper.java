import java.util.Random;

public record SchwaemmHelper(byte[] key, byte[] nonce, byte[] associate, byte[] message,
                             byte[] cipherC,
                             byte[] cipherJava,
                             int[] stateC, int[] stateJ) {

  private static final Random random = new Random();

  public static SchwaemmHelper prepareTest(SchwaemmType type, int minLength) {
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

  public static SchwaemmHelper prepareTest(SchwaemmType type) {
    return prepareTest(type, 0);
  }
}

