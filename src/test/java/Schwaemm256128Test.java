import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public final class Schwaemm256128Test {

  private static final int TAG_BYTES = SchwaemmType.S256128.getTagBytes();

  private static final int STATE_WORDS = SchwaemmType.S256128.getStateSize();
  private final SchwaemmLib schwaemmC = new SchwaemmLib(SchwaemmType.S256128);
  private final Schwaemm schwaemmJava = new Schwaemm(SchwaemmType.S256128);

  @RepeatedTest(50)
  void initializeTest() {
    int[] stateC = new int[STATE_WORDS];
    int[] stateJ = new int[STATE_WORDS];
    PreparedTest data = PreparedTest.prepareTest();
    schwaemmC.initialize(stateC, data.key, data.nonce);
    schwaemmJava.initialize(stateJ, data.key, data.nonce);
    Assertions.assertThat(stateJ).isEqualTo(stateC);
  }

  @RepeatedTest(50)
  void finalizeCall() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] stateC = data.stateC;
    int[] stateJ = data.stateJ;

    schwaemmC.finalize(stateC, data.key);

    schwaemmJava.finalize(stateJ, data.key);
    Assertions.assertThat(stateC).isEqualTo(stateJ);
  }

  @RepeatedTest(50)
  void stageWithFinalizeCall() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] stateC = new int[STATE_WORDS];
    int[] stateJ = new int[STATE_WORDS];
    byte[] cipher = new byte[data.message.length + TAG_BYTES];
    schwaemmC.stagesWithFinalize(stateC, data.key, data.nonce, data.associate,
        data.associate.length, data.message, data.message.length, cipher);

    schwaemmJava.initialize(stateJ, data.key, data.nonce);
    schwaemmJava.associateData(stateJ, data.associate);
    schwaemmJava.encrypt(stateJ, data.message);
    schwaemmJava.finalize(stateJ, data.key);
    Assertions.assertThat(stateC).isEqualTo(stateJ);
  }

  @RepeatedTest(50)
  void generateTag() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] cState = data.stateC;
    int[] state = data.stateJ;

    byte[] tag = new byte[data.message.length + TAG_BYTES];
    schwaemmJava.generateTag(state, tag, data.message.length);
    byte[] cTag = new byte[TAG_BYTES];
    schwaemmC.generateTag(cState, cTag);

    Assertions.assertThat(cState).isEqualTo(state);
    // the java code expects tag to have length message + tag length. We need to remove message length after
    byte[] trueJavaTag = new byte[TAG_BYTES];
    System.arraycopy(tag, data.message.length, trueJavaTag, 0, TAG_BYTES);
    Assertions.assertThat(cTag).isEqualTo(trueJavaTag);
  }

  @RepeatedTest(50)
  void stageWithGenerateTag() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] cState = new int[STATE_WORDS];
    byte[] cCipherWithTag = new byte[data.message.length + TAG_BYTES];
    schwaemmC.stagesWithGenerateTag(cState, data.key, data.nonce, data.associate,
        data.associate.length, data.message, data.message.length, cCipherWithTag);
    int[] state = new int[STATE_WORDS];
    schwaemmJava.initialize(state, data.key, data.nonce);
    schwaemmJava.associateData(state, data.associate);
    byte[] javaCipher = schwaemmJava.encrypt(state, data.message);
    schwaemmJava.finalize(state, data.key);
    schwaemmJava.generateTag(state, javaCipher, data.message.length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cCipherWithTag).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void checkSchwaemmEncrypt() {
    PreparedTest data = PreparedTest.prepareTest();
    byte[] cipher = new byte[data.message.length + TAG_BYTES];
    schwaemmC.encryptAndTag(cipher, data.message, data.message.length,
        data.associate, data.associate.length, data.nonce, data.key);
    byte[] javaCipher = schwaemmJava.encryptAndTag(data.message, data.associate, data.key,
        data.nonce);

    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void encryptAndDecryptC() {
    PreparedTest data = PreparedTest.prepareTest();

    byte[] cipher = new byte[data.message.length + TAG_BYTES];
    schwaemmC.encryptAndTag(cipher, data.message, data.message.length,
        data.associate, data.associate.length, data.nonce, data.key);
    byte[] messageBack = new byte[data.message.length];
    schwaemmC.decryptAndVerify(messageBack, cipher, cipher.length, data.associate,
        data.associate.length, data.nonce, data.key);

    byte[] javaCipher = schwaemmJava.encryptAndTag(data.message, data.associate, data.key,
        data.nonce);
    byte[] messageJava = schwaemmJava.decryptAndVerify(javaCipher, data.associate, data.key,
        data.nonce);

    Assertions.assertThat(data.message).isEqualTo(messageBack);
    Assertions.assertThat(messageJava).isEqualTo(messageBack);
  }

  @RepeatedTest(50)
  void encryptAndDecrypt() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = new int[STATE_WORDS];
    schwaemmJava.initialize(state, data.key, data.nonce);
    schwaemmJava.associateData(state, data.associate);
    byte[] cipher = schwaemmJava.encrypt(state, data.message);
    schwaemmJava.finalize(state, data.key);
    byte[] javaCipher = schwaemmJava.generateTag(state, cipher, data.message.length);

    int[] decryptState = new int[STATE_WORDS];
    schwaemmJava.initialize(decryptState, data.key, data.nonce);
    schwaemmJava.associateData(decryptState, data.associate);
    byte[] messageBack = new byte[data.message.length];
    schwaemmJava.decrypt(decryptState, messageBack, javaCipher);
    Assertions.assertThat(messageBack).isEqualTo(data.message);
  }

  @RepeatedTest(50)
  void processPlaintext() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = data.stateJ;
    int[] cState = data.stateC;

    byte[] javaCipher = schwaemmJava.encrypt(state, data.message);
    byte[] cipher = new byte[data.message.length + TAG_BYTES];
    schwaemmC.ProcessPlainText(cState, cipher, data.message, data.message.length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void stagesWithProcessPlaintext() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = new int[STATE_WORDS];
    int[] cState = new int[STATE_WORDS];

    schwaemmJava.initialize(state, data.key, data.nonce);
    schwaemmJava.associateData(state, data.associate);
    byte[] javaCipher = schwaemmJava.encrypt(state, data.message);
    byte[] cipher = new byte[data.message.length + TAG_BYTES];
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

    schwaemmJava.associateData(state, data.associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(20)
  void stageWithProcessAssociateData() {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = new int[STATE_WORDS];
    int[] cState = new int[STATE_WORDS];
    schwaemmC.stagesWithProcessAssocData(cState, data.key, data.nonce, data.associate,
        data.associate.length);

    schwaemmJava.initialize(state, data.key, data.nonce);
    schwaemmJava.associateData(state, data.associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }


  @RepeatedTest(50)
  void verifyTagFailsOnRandomInput() {
    PreparedTest data = PreparedTest.prepareTest();
    byte[] randomCipher = new byte[TAG_BYTES];
    new Random().nextBytes(randomCipher);
    int res = schwaemmC.verifyTag(data.stateJ, randomCipher);
    Assertions.assertThat(res).isEqualTo(-1);
    // TODO REFACTOR ALL HARDCODED STATE LENGTHS IN TESTS
    // 4 = 8 / 2 (by the state)
    Assertions.assertThatThrownBy(() -> schwaemmJava.verifyTag(data.stateJ,
            ConversionUtil.createIntArrayFromBytes(randomCipher, 4))).isInstanceOf(RuntimeException.class)
        .hasMessage("Could not verify tag!");
  }

  @RepeatedTest(50)
  void processCipherText() {
    PreparedTest data = PreparedTest.prepareTest();
    byte[] randomCipher = new byte[data.message.length + TAG_BYTES];
    new Random().nextBytes(randomCipher);

    schwaemmJava.decrypt(data.stateJ, data.message, randomCipher);

    schwaemmC.ProcessCipherText(data.stateC, randomCipher, data.message, data.message.length);
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
      byte[] nonce = new byte[32];
      random.nextBytes(nonce);
      byte[] key = new byte[16];
      random.nextBytes(key);
      int[] stateC = new int[STATE_WORDS];
      int[] stateJ = new int[STATE_WORDS];
      for (int i = 0; i < stateC.length; i++) {
        int randomNumber = random.nextInt(Integer.MAX_VALUE);
        stateC[i] = randomNumber;
        stateJ[i] = randomNumber;
      }
      return new PreparedTest(key, nonce, associate, message, stateC, stateJ);
    }
  }
}
