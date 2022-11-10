import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public final class Schwaemm128128Test {

  private static final int TAG_BYTES = SchwaemmType.S128128.getTagBytes();
  private static final int STATE_WORDS = SchwaemmType.S128128.getStateSize();
  private final SchwaemmLib schwaemmC = new SchwaemmLib(SchwaemmType.S128128);
  private final Schwaemm schwaemmJava = new Schwaemm(SchwaemmType.S128128);

  @RepeatedTest(50)
  void initializeTest() {
    int[] stateC = new int[STATE_WORDS];
    int[] stateJ = new int[STATE_WORDS];
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    schwaemmC.initialize(stateC, data.key(), data.nonce());
    schwaemmJava.initialize(stateJ, data.key(), data.nonce());
    Assertions.assertThat(stateJ).isEqualTo(stateC);
  }

  @RepeatedTest(50)
  void finalizeCall() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    int[] stateC = data.stateC();
    int[] stateJ = data.stateJ();

    schwaemmC.finalize(stateC, data.key());

    schwaemmJava.finalize(stateJ, data.key());
    Assertions.assertThat(stateC).isEqualTo(stateJ);
  }

  @RepeatedTest(50)
  void stageWithFinalizeCall() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);

    int[] stateC = new int[STATE_WORDS];
    int[] stateJ = new int[STATE_WORDS];

    schwaemmC.stagesWithFinalize(stateC, data.key(), data.nonce(), data.associate(),
        data.associate().length, data.message(), data.message().length, data.cipherC());

    schwaemmJava.initialize(stateJ, data.key(), data.nonce());
    schwaemmJava.associateData(stateJ, data.associate());
    schwaemmJava.encrypt(stateJ, data.message(), data.cipherJava());
    schwaemmJava.finalize(stateJ, data.key());
    Assertions.assertThat(stateC).isEqualTo(stateJ);
    Assertions.assertThat(data.cipherC()).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(50)
  void generateTag() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    int[] cState = data.stateC();
    int[] state = data.stateJ();

    byte[] tag = new byte[data.message().length + TAG_BYTES];
    schwaemmJava.generateTag(state, tag, data.message().length);
    byte[] cTag = new byte[TAG_BYTES];
    schwaemmC.generateTag(cState, cTag);

    Assertions.assertThat(cState).isEqualTo(state);
    // the java code expects tag to have length message tag length. We need to remove message()ength after
    byte[] trueJavaTag = new byte[TAG_BYTES];
    System.arraycopy(tag, data.message().length, trueJavaTag, 0, TAG_BYTES);
    Assertions.assertThat(cTag).isEqualTo(trueJavaTag);
  }

  @RepeatedTest(50)
  void stageWithGenerateTag() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);
    int[] cState = new int[STATE_WORDS];
    byte[] cCipherWithTag = new byte[data.message().length + TAG_BYTES];
    schwaemmC.stagesWithGenerateTag(cState, data.key(), data.nonce(), data.associate(),
        data.associate().length, data.message(), data.message().length, cCipherWithTag);
    int[] state = new int[STATE_WORDS];
    schwaemmJava.initialize(state, data.key(), data.nonce());
    schwaemmJava.associateData(state, data.associate());
    schwaemmJava.encrypt(state, data.message(), data.cipherJava());
    schwaemmJava.finalize(state, data.key());
    schwaemmJava.generateTag(state, data.cipherJava(), data.message().length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cCipherWithTag).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(50)
  void checkSchwaemmEncrypt() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    schwaemmC.encryptAndTag(data.cipherC(), data.message(), data.message().length,
        data.associate(), data.associate().length, data.nonce(), data.key());
    schwaemmJava.encryptAndTag(data.message(), data.cipherJava(),
        data.associate(),
        data.key(),
        data.nonce());

    Assertions.assertThat(data.cipherC()).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(50)
  void encryptAndDecryptC() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    schwaemmC.encryptAndTag(data.cipherC(), data.message(), data.message().length,
        data.associate(), data.associate().length, data.nonce(), data.key());
    byte[] messageBack = new byte[data.message().length];
    schwaemmC.decryptAndVerify(messageBack, data.cipherC(), data.cipherC().length, data.associate(),
        data.associate().length, data.nonce(), data.key());

    schwaemmJava.encryptAndTag(data.message(), data.cipherJava(),
        data.associate(),
        data.key(),
        data.nonce());
    byte[] messageJava = schwaemmJava.decryptAndVerify(data.cipherJava(), data.associate(),
        data.key(),
        data.nonce());

    Assertions.assertThat(data.message()).isEqualTo(messageBack);
    Assertions.assertThat(messageJava).isEqualTo(messageBack);
  }

  @RepeatedTest(50)
  void encryptAndDecrypt() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    schwaemmJava.encryptAndTag(data.message(), data.cipherJava(), data.associate(), data.key(),
        data.nonce());
    byte[] messageBack = schwaemmJava.decryptAndVerify(data.cipherJava(), data.associate(),
        data.key(), data.nonce());
    Assertions.assertThat(messageBack).isEqualTo(data.message());
  }

  @RepeatedTest(50)
  void processPlaintext() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);

    int[] state = data.stateJ();
    int[] cState = data.stateC();

    schwaemmJava.encrypt(state, data.message(), data.cipherJava());
    schwaemmC.ProcessPlainText(cState, data.cipherC(), data.message(), data.message().length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(data.cipherC()).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(50)
  void stagesWithProcessPlaintext() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);

    int[] state = new int[STATE_WORDS];
    int[] cState = new int[STATE_WORDS];

    schwaemmJava.initialize(state, data.key(), data.nonce());
    schwaemmJava.associateData(state, data.associate());
    schwaemmJava.encrypt(state, data.message(), data.cipherJava());

    schwaemmC.stagesWithProcessPlainText(cState, data.key(), data.nonce(), data.associate(),
        data.associate().length, data.message(), data.message().length, data.cipherC());

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(data.cipherC()).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(20)
  void processAssociateData() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);

    int[] state = data.stateJ();
    int[] cState = data.stateC();
    schwaemmC.processAssocData(cState, data.associate(), data.associate().length);

    schwaemmJava.associateData(state, data.associate());
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(20)
  void stageWithProcessAssociateData() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);
    int[] state = new int[STATE_WORDS];
    int[] cState = new int[STATE_WORDS];
    schwaemmC.stagesWithProcessAssocData(cState, data.key(), data.nonce(), data.associate(),
        data.associate().length);

    schwaemmJava.initialize(state, data.key(), data.nonce());
    schwaemmJava.associateData(state, data.associate());
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(50)
  void verifyTagFailsOnRandomInput() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    byte[] randomCipher = new byte[TAG_BYTES];
    new Random().nextBytes(randomCipher);
    int res = schwaemmC.verifyTag(data.stateJ(), randomCipher);
    Assertions.assertThat(res).isEqualTo(-1);
    Assertions.assertThatThrownBy(() -> schwaemmJava.verifyTag(data.stateJ(),
            ConversionUtil.createIntArrayFromBytes(randomCipher,
                SchwaemmType.S128128.getStateSize() / 2)))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not verify tag!");
  }

  @RepeatedTest(50)
  void processCipherText() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);

    byte[] randomCipher = new byte[data.message().length + TAG_BYTES];
    new Random().nextBytes(randomCipher);

    schwaemmJava.decrypt(data.stateJ(), data.message(), randomCipher);

    schwaemmC.ProcessCipherText(data.stateC(), randomCipher, data.message(), data.message().length);
  }
}