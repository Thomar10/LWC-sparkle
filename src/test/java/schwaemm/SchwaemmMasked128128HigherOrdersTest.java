package schwaemm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import sparkle.MaskedSparkleBoolean;
import sparkle.MaskedSparkleHigherOrder;

public final class SchwaemmMasked128128HigherOrdersTest {

  private static final int TAG_BYTES = SchwaemmType.S128128.getTagBytes();
  private static final int STATE_WORDS = SchwaemmType.S128128.getStateSize();
  private final Schwaemm schwaemm = new Schwaemm(SchwaemmType.S128128);
  private final SchwaemmMasked schwaemmMasked = new SchwaemmMasked(SchwaemmType.S128128,
      new MaskedSparkleBoolean());

  @RepeatedTest(50)
  void initializeTest() {
    int[] stateJ = new int[STATE_WORDS];
    int[] stateToMask = new int[STATE_WORDS];
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    schwaemm.initialize(stateJ, data.key(), data.nonce());
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    int[][] maskedState = SchwaemmHelper.maskIntArray(stateToMask, 4);
    schwaemmMasked.initialize(maskedState, maskedData.key(), maskedData.nonce());
    Assertions.assertThat(stateJ).isEqualTo(SchwaemmHelper.recoverState(maskedState));
  }

  @RepeatedTest(50)
  void finalizeCall() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);

    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    schwaemmMasked.finalize(maskedData.state(), maskedData.key());

    schwaemm.finalize(data.stateJ(), data.key());
    Assertions.assertThat(data.stateJ())
        .isEqualTo(SchwaemmHelper.recoverSchwaemm(maskedData).stateJ());
  }

  @RepeatedTest(50)
  void checkSchwaemmEncrypt() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    schwaemm.encryptAndTag(data.message(), data.cipherJava(),
        data.associate(),
        data.key(),
        data.nonce());
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    schwaemmMasked.encryptAndTag(maskedData.message(), maskedData.cipher(), maskedData.associate(),
        maskedData.key(),
        maskedData.nonce());
    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);

    Assertions.assertThat(recovered.cipherJava()).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(50)
  void processCipherText() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    byte[] randomCipher = new byte[data.message().length + TAG_BYTES];
    new Random().nextBytes(randomCipher);

    schwaemm.decrypt(data.stateJ(), data.message(), randomCipher);

    schwaemmMasked.decrypt(maskedData.state(), maskedData.message(),
        SchwaemmHelper.maskByteArrays(randomCipher, 4));

    Assertions.assertThat(data.message())
        .isEqualTo(SchwaemmHelper.recoverSchwaemm(maskedData).message());
  }

  @RepeatedTest(50)
  void generateTag() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);

    byte[] tag = new byte[data.message().length + TAG_BYTES];
    byte[][] maskedTag = new byte[4][data.message().length + TAG_BYTES];
    schwaemm.generateTag(data.stateJ(), tag, data.message().length);
    schwaemmMasked.generateTag(maskedData.state(), maskedTag, maskedData.message()[0].length);

    Assertions.assertThat(data.stateC()).isEqualTo(data.stateJ());
    Assertions.assertThat(SchwaemmHelper.recoverByteArrays(maskedTag)).isEqualTo(tag);
  }

  @RepeatedTest(1)
  void encryptAndDecryptMaskedAndUnmasked() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 3);

    schwaemm.encryptAndTag(data.message(), data.cipherJava(),
        data.associate(),
        data.key(),
        data.nonce());
    byte[] messageJava = schwaemm.decryptAndVerify(data.cipherJava(), data.associate(),
        data.key(),
        data.nonce());

    schwaemmMasked.encryptAndTag(maskedData.message(), maskedData.cipher(), maskedData.associate(),
        maskedData.key(), maskedData.nonce());
    byte[][] messageBack =
        schwaemmMasked.decryptAndVerify(maskedData.cipher(), maskedData.associate(),
            maskedData.key(),
            maskedData.nonce());

    Assertions.assertThat(data.message()).isEqualTo(SchwaemmHelper.recoverByteArrays(messageBack));
    Assertions.assertThat(messageJava).isEqualTo(SchwaemmHelper.recoverByteArrays(messageBack));
  }

  @RepeatedTest(50)
  void encryptAndDecrypt() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    schwaemmMasked.encryptAndTag(maskedData.message(), maskedData.cipher(), maskedData.associate(),
        maskedData.key(), maskedData.nonce());
    byte[][] messageBack =
        schwaemmMasked.decryptAndVerify(maskedData.cipher(), maskedData.associate(),
            maskedData.key(),
            maskedData.nonce());

    Assertions.assertThat(SchwaemmHelper.recoverByteArrays(messageBack)).isEqualTo(data.message());
  }

  @RepeatedTest(50)
  void processAssociateData() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    schwaemmMasked.associateData(maskedData.state(), maskedData
        .associate());
    schwaemm.associateData(data.stateJ(), data.associate());
    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);
    Assertions.assertThat(recovered.stateJ())
        .isEqualTo(data.stateJ());
  }

  @RepeatedTest(50)
  void processPlaintext() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 1);

    schwaemm.encrypt(data.stateJ(), data.message(), data.cipherJava());
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    schwaemmMasked.encrypt(maskedData.state(), maskedData.message(), maskedData.cipher());

    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);
    Assertions.assertThat(recovered.stateJ()).isEqualTo(data.stateJ());
    Assertions.assertThat(recovered.cipherJava()).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(50)
  void schwaemmHelperMaskAndRecover() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    int[][] maskedState = SchwaemmHelper.maskIntArray(data.stateJ(), 2);
    Assertions.assertThat(data.stateC()).isEqualTo(SchwaemmHelper.recoverState(maskedState));

    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);
    Assertions.assertThat(data.key()).isEqualTo(recovered.key());
    Assertions.assertThat(data.message()).isEqualTo(recovered.message());
    Assertions.assertThat(data.associate()).isEqualTo(recovered.associate());
    Assertions.assertThat(data.cipherJava()).isEqualTo(recovered.cipherJava());
  }

  @RepeatedTest(50)
  void schwaemmHelperMaskAndRecoverHigherOrder() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128);
    int[][] maskedState = SchwaemmHelper.maskIntArray(data.stateJ(), 4);
    Assertions.assertThat(data.stateC()).isEqualTo(SchwaemmHelper.recoverState(maskedState));

    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);
    Assertions.assertThat(data.key()).isEqualTo(recovered.key());
    Assertions.assertThat(data.message()).isEqualTo(recovered.message());
    Assertions.assertThat(data.associate()).isEqualTo(recovered.associate());
    Assertions.assertThat(data.cipherJava()).isEqualTo(recovered.cipherJava());
  }


  @Test
  void genkatAeadTest() throws IOException {
    BufferedReader buffer = new BufferedReader(
        new InputStreamReader(SchwaemmMasked128128HigherOrdersTest.class.getResourceAsStream(
            "/schwaemm/LWC_AEAD_KAT_128_128.txt")));
    byte[] key = SchwaemmHelper.initBuffer(new byte[SchwaemmType.S128128.getKeySize()]);
    byte[] nonce = SchwaemmHelper.initBuffer(new byte[SchwaemmType.S128128.getNonceSize()]);
    byte[] messageToCopy = SchwaemmHelper.initBuffer(new byte[32]);
    byte[][] message2;
    byte[] associateToCopy = SchwaemmHelper.initBuffer(new byte[32]);

    int count = 1;
    int mlen;
    int mlen2;
    int adlen;
    String line;
    for (mlen = 0; mlen <= 32; mlen++) {
      byte[] cipher = new byte[mlen + SchwaemmType.S128128.getTagBytes()];
      byte[] message = Arrays.copyOfRange(messageToCopy, 0, mlen);
      for (adlen = 0; adlen <= 32; adlen++) {
        line = buffer.readLine();
        byte[] associate = Arrays.copyOfRange(associateToCopy, 0, adlen);
        String countGotten = String.format("Count = %d", count);
        Assertions.assertThat(line).isEqualTo(countGotten);

        line = buffer.readLine();
        String keyGotten = String.format("Key = %s",
            SchwaemmHelper.printBytesAsStringLength(key, SchwaemmType.S128128.getKeySize()));
        Assertions.assertThat(line).isEqualTo(keyGotten);

        line = buffer.readLine();
        String nonceGotten = String.format("Nonce = %s",
            SchwaemmHelper.printBytesAsStringLength(nonce, SchwaemmType.S128128.getNonceSize()));
        Assertions.assertThat(line).isEqualTo(nonceGotten);

        line = buffer.readLine();
        String plainGotten = String.format("PT = %s",
            SchwaemmHelper.printBytesAsStringLength(message, mlen));
        Assertions.assertThat(line).isEqualTo(plainGotten);

        line = buffer.readLine();
        String adGotten = String.format("AD = %s",
            SchwaemmHelper.printBytesAsStringLength(associate, adlen));
        Assertions.assertThat(line).isEqualTo(adGotten);
        SchwaemmHelper data = new SchwaemmHelper(key, nonce, associate, message, cipher, cipher,
            new int[2],
            new int[2]);
        SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 2);
        schwaemmMasked.encryptAndTag(maskedData.message(), maskedData.cipher(),
            maskedData.associate(), maskedData.key(), maskedData.nonce());

        line = buffer.readLine();
        String cipherGotten = String.format("CT = %s",
            SchwaemmHelper.printBytesAsStringLength(
                SchwaemmHelper.recoverByteArrays(maskedData.cipher()),
                mlen + SchwaemmType.S128128.getTagBytes()));
        Assertions.assertThat(line).isEqualTo(cipherGotten);

        // New line.
        buffer.readLine();

        message2 = schwaemmMasked.decryptAndVerify(maskedData.cipher(), maskedData.associate(),
            maskedData.key(), maskedData.nonce());
        mlen2 = message2[0].length;
        Assertions.assertThat(message).isEqualTo(SchwaemmHelper.recoverByteArrays(message2));
        Assertions.assertThat(mlen).isEqualTo(mlen2);
        count++;
      }
    }
  }
}
