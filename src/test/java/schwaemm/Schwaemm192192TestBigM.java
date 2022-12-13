package schwaemm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import util.ConversionUtil;

public final class Schwaemm192192TestBigM {

  private static final int TAG_BYTES = SchwaemmType.S192192.getTagBytes();
  private static final int STATE_WORDS = SchwaemmType.S192192.getStateSize();
  private final SchwaemmLib schwaemmC = new SchwaemmLib(SchwaemmType.S192192);
  private final Schwaemm schwaemmJava = new Schwaemm(SchwaemmType.S192192);

  @RepeatedTest(50)
  void initializeTest() {
    int[] stateC = new int[STATE_WORDS];
    int[] stateJ = new int[STATE_WORDS];
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);
    schwaemmC.initialize(stateC, data.key(), data.nonce());
    schwaemmJava.initialize(stateJ, data.key(), data.nonce());
    Assertions.assertThat(stateJ).isEqualTo(stateC);
  }

  @RepeatedTest(50)
  void finalizeCall() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);
    int[] stateC = data.stateC();
    int[] stateJ = data.stateJ();

    schwaemmC.finalize(stateC, data.key());

    schwaemmJava.finalize(stateJ, data.key());
    Assertions.assertThat(stateC).isEqualTo(stateJ);
  }

  @RepeatedTest(50)
  void stageWithFinalizeCall() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);

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
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);
    int[] cState = data.stateC();
    int[] state = data.stateJ();

    byte[] tag = new byte[data.message().length + TAG_BYTES];
    schwaemmJava.generateTag(state, tag, data.message().length);
    byte[] cTag = new byte[TAG_BYTES];
    schwaemmC.generateTag(cState, cTag);

    Assertions.assertThat(cState).isEqualTo(state);
    // the java code expects tag to have length message tag length. We need to remove message length after
    byte[] trueJavaTag = new byte[TAG_BYTES];
    System.arraycopy(tag, data.message().length, trueJavaTag, 0, TAG_BYTES);
    Assertions.assertThat(cTag).isEqualTo(trueJavaTag);
  }

  @RepeatedTest(50)
  void stageWithGenerateTag() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);
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
  void encryptAndDecrypt() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 64, 128);
    schwaemmJava.encryptAndTag(data.message(), data.cipherJava(), data.associate(), data.key(),
        data.nonce());
    schwaemmC.encryptAndTag(data.cipherC(), data.message(), data.message().length,
        data.associate(), data.associate().length, data.nonce(), data.key());
    byte[] messageBack = schwaemmJava.decryptAndVerify(data.cipherJava(), data.associate(),
        data.key(), data.nonce());
    Assertions.assertThat(messageBack).isEqualTo(data.message());
  }

  @RepeatedTest(50)
  void processPlaintext() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);

    int[] state = data.stateJ();
    int[] cState = data.stateC();

    schwaemmJava.encrypt(state, data.message(), data.cipherJava());
    schwaemmC.ProcessPlainText(cState, data.cipherC(), data.message(), data.message().length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(data.cipherC()).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(50)
  void stagesWithProcessPlaintext() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);

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
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);

    int[] state = data.stateJ();
    int[] cState = data.stateC();
    schwaemmC.processAssocData(cState, data.associate(), data.associate().length);

    schwaemmJava.associateData(state, data.associate());
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(20)
  void stageWithProcessAssociateData() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);
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
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);
    byte[] randomCipher = new byte[TAG_BYTES];
    new Random().nextBytes(randomCipher);
    int res = schwaemmC.verifyTag(data.stateJ(), randomCipher);
    Assertions.assertThat(res).isEqualTo(-1);
    Assertions.assertThatThrownBy(() -> schwaemmJava.verifyTag(data.stateJ(),
            ConversionUtil.createIntArrayFromBytes(randomCipher,
                SchwaemmType.S192192.getVerifyTagLength())))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not verify tag!");
  }

  @RepeatedTest(50)
  void processCipherText() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S192192, 33, 64);

    byte[] randomCipher = new byte[data.message().length + TAG_BYTES];
    new Random().nextBytes(randomCipher);

    schwaemmJava.decrypt(data.stateJ(), data.message(), randomCipher);

    schwaemmC.ProcessCipherText(data.stateC(), randomCipher, data.message(), data.message().length);
  }

  @Test
  void genkatAeadTest() throws IOException {
    BufferedReader buffer = new BufferedReader(
        new InputStreamReader(Schwaemm192192Test.class.getResourceAsStream(
            "/schwaemm/LWC_AEAD_KAT_192_192.txt")));
    byte[] key = SchwaemmHelper.initBuffer(new byte[SchwaemmType.S192192.getKeySize()]);
    byte[] nonce = SchwaemmHelper.initBuffer(new byte[SchwaemmType.S192192.getNonceSize()]);
    byte[] messageToCopy = SchwaemmHelper.initBuffer(new byte[32]);
    byte[] message2;
    byte[] associateToCopy = SchwaemmHelper.initBuffer(new byte[32]);

    int count = 1;
    int mlen;
    int mlen2;
    int adlen;
    String line;
    for (mlen = 0; mlen <= 32; mlen++) {
      byte[] cipher = new byte[mlen + SchwaemmType.S192192.getTagBytes()];
      byte[] message = Arrays.copyOfRange(messageToCopy, 0, mlen);
      for (adlen = 0; adlen <= 32; adlen++) {
        line = buffer.readLine();
        byte[] associate = Arrays.copyOfRange(associateToCopy, 0, adlen);
        String countGotten = String.format("Count = %d", count);
        Assertions.assertThat(line).isEqualTo(countGotten);

        line = buffer.readLine();
        String keyGotten = String.format("Key = %s",
            SchwaemmHelper.printBytesAsStringLength(key, SchwaemmType.S192192.getKeySize()));
        Assertions.assertThat(line).isEqualTo(keyGotten);

        line = buffer.readLine();
        String nonceGotten = String.format("Nonce = %s",
            SchwaemmHelper.printBytesAsStringLength(nonce, SchwaemmType.S192192.getNonceSize()));
        Assertions.assertThat(line).isEqualTo(nonceGotten);

        line = buffer.readLine();
        String plainGotten = String.format("PT = %s",
            SchwaemmHelper.printBytesAsStringLength(message, mlen));
        Assertions.assertThat(line).isEqualTo(plainGotten);

        line = buffer.readLine();
        String adGotten = String.format("AD = %s",
            SchwaemmHelper.printBytesAsStringLength(associate, adlen));
        Assertions.assertThat(line).isEqualTo(adGotten);

        schwaemmJava.encryptAndTag(message, cipher, associate, key, nonce);

        line = buffer.readLine();
        String cipherGotten = String.format("CT = %s",
            SchwaemmHelper.printBytesAsStringLength(cipher,
                mlen + SchwaemmType.S192192.getTagBytes()));
        Assertions.assertThat(line).isEqualTo(cipherGotten);

        // New line.
        buffer.readLine();

        message2 = schwaemmJava.decryptAndVerify(cipher, associate, key, nonce);
        mlen2 = message2.length;
        Assertions.assertThat(message).isEqualTo(message2);
        Assertions.assertThat(mlen).isEqualTo(mlen2);
        count++;
      }
    }
  }
}