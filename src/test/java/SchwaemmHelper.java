import java.util.Random;

public final class SchwaemmHelper {

  private record PreparedTest(byte[] key, byte[] nonce, byte[] associate, byte[] message,
                              int[] stateC, int[] stateJ) {

    private static final Random random = new Random();

    public static PreparedTest prepareTest(SchwaemmType type) {
      int randomInt = random.nextInt(32 - 1) + 1;
      int randomMsg = random.nextInt(32 - 1) + 1;
      byte[] associate = new byte[randomInt];
      byte[] message = new byte[randomMsg];
      random.nextBytes(associate);
      random.nextBytes(message);
      byte[] nonce = new byte[16];
      random.nextBytes(nonce);
      byte[] key = new byte[16];
      random.nextBytes(key);
      int[] stateC = new int[Sparkle.maxBranches * 2];
      int[] stateJ = new int[Sparkle.maxBranches * 2];
      for (int i = 0; i < stateC.length; i++) {
        int randomNumber = random.nextInt(Integer.MAX_VALUE);
        stateC[i] = randomNumber;
        stateJ[i] = randomNumber;
      }
      return new PreparedTest(key, nonce, associate, message, stateC, stateJ);
    }
  }
}
