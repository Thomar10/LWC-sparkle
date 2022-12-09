package schwaemm;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import sparkle.MaskedSparkleFirstOrder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

public final class SchwaemmMasked128128TestBigM {

  private static final int TAG_BYTES = SchwaemmType.S128128.getTagBytes();
  private static final int STATE_WORDS = SchwaemmType.S128128.getStateSize();
  private final Schwaemm schwaemm = new Schwaemm(SchwaemmType.S128128);
  private final SchwaemmMasked schwaemmMasked = new SchwaemmMasked(SchwaemmType.S128128, new MaskedSparkleFirstOrder());

  @RepeatedTest(50)
  void initializeTest() {
    int[] stateJ = new int[STATE_WORDS];
    int[] stateToMask = new int[STATE_WORDS];
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);
    schwaemm.initialize(stateJ, data.key(), data.nonce());
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 2);
    int[][] maskedState = SchwaemmHelper.maskIntArray(stateToMask, 2);
    schwaemmMasked.initialize(maskedState, maskedData.key(), maskedData.nonce());
    Assertions.assertThat(stateJ).isEqualTo(SchwaemmHelper.recoverState(maskedState));
  }

  @RepeatedTest(50)
  void finalizeCall() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);

    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataFirstOrder(data);
    schwaemmMasked.finalize(maskedData.state(), maskedData.key());

    schwaemm.finalize(data.stateJ(), data.key());
    Assertions.assertThat(data.stateJ())
        .isEqualTo(SchwaemmHelper.recoverSchwaemm(maskedData).stateJ());
  }

  @RepeatedTest(50)
  void checkSchwaemmEncrypt() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);
    schwaemm.encryptAndTag(data.message(), data.cipherJava(),
        data.associate(),
        data.key(),
        data.nonce());
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataFirstOrder(data);
    schwaemmMasked.encryptAndTag(maskedData.message(), maskedData.cipher(), maskedData.associate(),
        maskedData.key(),
        maskedData.nonce());
    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);

    Assertions.assertThat(recovered.cipherJava()).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(50)
  void processCipherText() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataFirstOrder(data);
    byte[] randomCipher = new byte[data.message().length + TAG_BYTES];
    new Random().nextBytes(randomCipher);

    schwaemm.decrypt(data.stateJ(), data.message(), randomCipher);

    schwaemmMasked.decrypt(maskedData.state(), maskedData.message(),
        SchwaemmHelper.maskByteFirstOrder(randomCipher));

    Assertions.assertThat(data.message())
        .isEqualTo(SchwaemmHelper.recoverSchwaemm(maskedData).message());
  }

  @RepeatedTest(50)
  void generateTag() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataFirstOrder(data);

    byte[] tag = new byte[data.message().length + TAG_BYTES];
    byte[][] maskedTag = new byte[2][data.message().length + TAG_BYTES];
    schwaemm.generateTag(data.stateJ(), tag, data.message().length);
    schwaemmMasked.generateTag(maskedData.state(), maskedTag, maskedData.message()[0].length);

    Assertions.assertThat(data.stateC()).isEqualTo(data.stateJ());
    Assertions.assertThat(SchwaemmHelper.recoverByteArrays(maskedTag)).isEqualTo(tag);
  }

  @RepeatedTest(50)
  void encryptAndDecryptMaskedAndUnmasked() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataFirstOrder(data);

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
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataFirstOrder(data);
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
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataFirstOrder(data);

    schwaemmMasked.associateData(maskedData.state(), maskedData.associate());
    schwaemm.associateData(data.stateJ(), data.associate());

    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);
    Assertions.assertThat(recovered.stateJ())
        .isEqualTo(data.stateJ());
  }

  @RepeatedTest(50)
  void processPlaintext() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);

    schwaemm.encrypt(data.stateJ(), data.message(), data.cipherJava());
    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataFirstOrder(data);
    schwaemmMasked.encrypt(maskedData.state(), maskedData.message(), maskedData.cipher());

    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);
    Assertions.assertThat(recovered.stateJ()).isEqualTo(data.stateJ());
    Assertions.assertThat(recovered.cipherJava()).isEqualTo(data.cipherJava());
  }

  @RepeatedTest(50)
  void schwaemmHelperMaskAndRecover() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);
    int[][] maskedState = SchwaemmHelper.maskIntArray(data.stateJ(), 2);
    Assertions.assertThat(data.stateC()).isEqualTo(SchwaemmHelper.recoverState(maskedState));

    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataFirstOrder(data);
    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);
    Assertions.assertThat(data.key()).isEqualTo(recovered.key());
    Assertions.assertThat(data.message()).isEqualTo(recovered.message());
    Assertions.assertThat(data.associate()).isEqualTo(recovered.associate());
    Assertions.assertThat(data.cipherJava()).isEqualTo(recovered.cipherJava());
  }

  @RepeatedTest(50)
  void schwaemmHelperMaskAndRecoverHigherOrder() {
    SchwaemmHelper data = SchwaemmHelper.prepareTest(SchwaemmType.S128128, 33, 64);
    int[][] maskedState = SchwaemmHelper.maskIntArray(data.stateJ(), 4);
    Assertions.assertThat(data.stateC()).isEqualTo(SchwaemmHelper.recoverState(maskedState));

    SchwaemmHelper.MaskedData maskedData = SchwaemmHelper.convertDataToMasked(data, 4);
    SchwaemmHelper recovered = SchwaemmHelper.recoverSchwaemm(maskedData);
    Assertions.assertThat(data.key()).isEqualTo(recovered.key());
    Assertions.assertThat(data.message()).isEqualTo(recovered.message());
    Assertions.assertThat(data.associate()).isEqualTo(recovered.associate());
    Assertions.assertThat(data.cipherJava()).isEqualTo(recovered.cipherJava());
  }

}
