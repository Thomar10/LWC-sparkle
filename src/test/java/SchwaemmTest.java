import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public final class SchwaemmTest {

  private final SchwaemmLib schwaemmC = new SchwaemmLib("128128");

  @RepeatedTest(50)
  void initializeTest() {
    int[] stateC = new int[8];
    int[] stateJ = new int[8];
    PreparedTest data = PreparedTest.prepareTest();
    schwaemmC.initialize(stateC, data.key, data.nonce);
    Schwaemm.initialize(stateJ, data.key, data.nonce);
    Assertions.assertThat(stateJ).isEqualTo(stateC);
  }

  @RepeatedTest(50)
  void finalizeCall() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] stateC = data.stateC;
    int[] stateJ = data.stateJ;

    schwaemmC.finalize(stateC, data.key);

    Schwaemm.finalize(stateJ, data.key);
    Assertions.assertThat(stateC).isEqualTo(stateJ);
  }

  @RepeatedTest(50)
  void stageWithFinalizeCall() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] stateC = new int[8];
    int[] stateJ = new int[8];
    byte[] cipher = new byte[data.message.length + Schwaemm.TAG_BYTES];
    schwaemmC.stagesWithFinalize(stateC, data.key, data.nonce, data.associate,
        data.associate.length, data.message, data.message.length, cipher);

    Schwaemm.initialize(stateJ, data.key, data.nonce);
    Schwaemm.associateData(stateJ, data.associate);
    Schwaemm.encrypt(stateJ, data.message);
    Schwaemm.finalize(stateJ, data.key);
    Assertions.assertThat(stateC).isEqualTo(stateJ);
  }

  @RepeatedTest(50)
  void generateTag() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] cState = data.stateC;
    int[] state = data.stateJ;

    byte[] tag = new byte[data.message.length + Schwaemm.TAG_BYTES];
    Schwaemm.generateTag(state, tag, data.message.length);
    byte[] cTag = new byte[Schwaemm.TAG_BYTES];
    schwaemmC.generateTag(cState, cTag);

    Assertions.assertThat(cState).isEqualTo(state);
    // the java code expects tag to have length message + tag length. We need to remove message length after
    byte[] trueJavaTag = new byte[16];
    System.arraycopy(tag, data.message.length, trueJavaTag, 0, 16);
    Assertions.assertThat(cTag).isEqualTo(trueJavaTag);
  }

  @RepeatedTest(50)
  void stageWithGenerateTag() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] cState = new int[8];
    byte[] cCipherWithTag = new byte[data.message.length + Schwaemm.TAG_BYTES];
    schwaemmC.stagesWithGenerateTag(cState, data.key, data.nonce, data.associate,
        data.associate.length, data.message, data.message.length, cCipherWithTag);
    int[] state = new int[8];
    Schwaemm.initialize(state, data.key, data.nonce);
    Schwaemm.associateData(state, data.associate);
    byte[] javaCipher = Schwaemm.encrypt(state, data.message);
    Schwaemm.finalize(state, data.key);
    Schwaemm.generateTag(state, javaCipher, data.message.length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cCipherWithTag).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void checkSchwaemmEncrypt() {
    PreparedTest data = PreparedTest.prepareTest();
    byte[] cipher = new byte[data.message.length + Schwaemm.TAG_BYTES];
    schwaemmC.encryptAndTag(cipher, data.message, data.message.length,
        data.associate, data.associate.length, data.nonce, data.key);
    byte[] javaCipher = Schwaemm.encryptAndTag(data.message, data.associate, data.key, data.nonce);

    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void encryptAndDecryptC() {
    PreparedTest data = PreparedTest.prepareTest();

    byte[] cipher = new byte[data.message.length + Schwaemm.TAG_BYTES];
    schwaemmC.encryptAndTag(cipher, data.message, data.message.length,
        data.associate, data.associate.length, data.nonce, data.key);
    byte[] messageBack = new byte[data.message.length];
    schwaemmC.decryptAndVerify(messageBack, cipher, cipher.length, data.associate,
        data.associate.length, data.nonce, data.key);

    byte[] javaCipher = Schwaemm.encryptAndTag(data.message, data.associate, data.key, data.nonce);
    byte[] messageJava = Schwaemm.decryptAndVerify(javaCipher, data.associate, data.key,
        data.nonce);

    Assertions.assertThat(data.message).isEqualTo(messageBack);
    Assertions.assertThat(messageJava).isEqualTo(messageBack);
  }

  @RepeatedTest(50)
  void encryptAndDecrypt() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = new int[8];
    Schwaemm.initialize(state, data.key, data.nonce);
    Schwaemm.associateData(state, data.associate);
    byte[] cipher = Schwaemm.encrypt(state, data.message);
    Schwaemm.finalize(state, data.key);
    byte[] javaCipher = Schwaemm.generateTag(state, cipher, data.message.length);

    int[] decryptState = new int[8];
    Schwaemm.initialize(decryptState, data.key, data.nonce);
    Schwaemm.associateData(decryptState, data.associate);
    byte[] messageBack = new byte[data.message.length];
    Schwaemm.decrypt(decryptState, messageBack, javaCipher);
    Assertions.assertThat(messageBack).isEqualTo(data.message);
  }

  @RepeatedTest(50)
  void processPlaintext() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = data.stateJ;
    int[] cState = data.stateC;

    byte[] javaCipher = Schwaemm.encrypt(state, data.message);
    byte[] cipher = new byte[data.message.length + Schwaemm.TAG_BYTES];
    schwaemmC.ProcessPlainText(cState, cipher, data.message, data.message.length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void stagesWithProcessPlaintext() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = new int[8];
    int[] cState = new int[8];

    Schwaemm.initialize(state, data.key, data.nonce);
    Schwaemm.associateData(state, data.associate);
    byte[] javaCipher = Schwaemm.encrypt(state, data.message);
    byte[] cipher = new byte[data.message.length + Schwaemm.TAG_BYTES];
    schwaemmC.stagesWithProcessPlainText(cState, data.key, data.nonce, data.associate,
        data.associate.length, data.message, data.message.length, cipher);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }


  @RepeatedTest(20)
  void processAssociateData() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = data.stateJ;
    int[] cState = data.stateC;
    schwaemmC.processAssocData(cState, data.associate, data.associate.length);

    Schwaemm.associateData(state, data.associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(20)
  void stageWithProcessAssociateData() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = new int[8];
    int[] cState = new int[8];
    schwaemmC.stagesWithProcessAssocData(cState, data.key, data.nonce, data.associate,
        data.associate.length);

    Schwaemm.initialize(state, data.key, data.nonce);
    Schwaemm.associateData(state, data.associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  private record PreparedTest(byte[] key, byte[] nonce, byte[] associate, byte[] message,
                              int[] stateC, int[] stateJ) {

    private static final Random random = new Random();

    public static PreparedTest prepareTest() {
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
