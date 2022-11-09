import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public final class SchwaemmTest {

  private static final String resourceSchwaemm =
      System.getProperty("user.dir") + "/src/test/resources/schwaemm";
  private static String schwaemmCpath = SparkleTest.class.getResource("/schwaemm/schwaemmC")
      .getPath();

  static {
    String operatingSystem = System.getProperty("os.name");
    if (operatingSystem.contains("Windows")) {
      schwaemmCpath += ".exe";
    }
  }

  private final ProcessCaller caller = new ProcessCaller(resourceSchwaemm, "schwaemmState");

  @AfterAll
  public static void tearDown() throws IOException {
    Files.write(Path.of(resourceSchwaemm + "/key"), new byte[0]);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), new byte[0]);
    Files.write(Path.of(resourceSchwaemm + "/associate"), new byte[0]);
    Files.write(Path.of(resourceSchwaemm + "/message"), new byte[0]);
    Files.write(Path.of(resourceSchwaemm + "/cipher"), new byte[0]);
    Files.write(Path.of(resourceSchwaemm + "/messageBack"), new byte[0]);
    Files.write(Path.of(resourceSchwaemm + "/schwaemmState"), new byte[0]);
  }

  @RepeatedTest(50)
  void initializeTest() throws IOException, InterruptedException {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = new int[8];
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "initialize"});

    Schwaemm.initialize(state, data.key, data.nonce);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(50)
  void finalizeCall() throws IOException, InterruptedException {
    PreparedTest data = PreparedTest.prepareTest();

    int[] state = new int[8];
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "finalize"});

    Schwaemm.initialize(state, data.key, data.nonce);
    Schwaemm.associateData(state, data.associate);
    Schwaemm.encrypt(state, data.message);
    Schwaemm.finalize(state, data.key);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(50)
  void generateTag() throws IOException, InterruptedException {
    PreparedTest data = PreparedTest.prepareTest();
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "generateTag"});
    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));

    int[] state = new int[8];
    Schwaemm.initialize(state, data.key, data.nonce);
    Schwaemm.associateData(state, data.associate);
    byte[] javaCipher = Schwaemm.encrypt(state, data.message);

    Schwaemm.finalize(state, data.key);
    Schwaemm.generateTag(state, javaCipher, data.message.length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void checkSchwaemm() throws IOException, InterruptedException {
    PreparedTest data = PreparedTest.prepareTest();
    caller.callProcess(new String[]{schwaemmCpath, "fullFunction"});
    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));

    byte[] javaCipher = Schwaemm.encryptAndTag(data.message, data.associate, data.key, data.nonce);

    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void encryptAndDecryptC() throws IOException, InterruptedException {
    PreparedTest data = PreparedTest.prepareTest();

    caller.callProcess(new String[]{schwaemmCpath, "encryptAndDecrypt"});
    byte[] messageBack = Files.readAllBytes(Paths.get(resourceSchwaemm + "/messageBack"));

    byte[] javaCipher = Schwaemm.encryptAndTag(data.message, data.associate, data.key, data.nonce);
    byte[] messageJava = Schwaemm.decryptAndVerify(javaCipher, data.associate, data.key,
        data.nonce);

    Assertions.assertThat(data.message).isEqualTo(messageBack);
    Assertions.assertThat(messageJava).isEqualTo(messageBack);
  }

  @RepeatedTest(50)
  void encryptAndDecrypt() throws IOException {
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
  void processPlaintext() throws IOException, InterruptedException {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = new int[8];
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "encrypt"});

    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));

    Schwaemm.initialize(state, data.key, data.nonce);
    Schwaemm.associateData(state, data.associate);
    byte[] javaCipher = Schwaemm.encrypt(state, data.message);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void processAssociateData() throws IOException, InterruptedException {
    PreparedTest data = PreparedTest.prepareTest();
    int[] state = new int[8];
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "associate"});

    Schwaemm.initialize(state, data.key, data.nonce);
    Schwaemm.associateData(state, data.associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }


  private record PreparedTest(byte[] key, byte[] nonce, byte[] associate, byte[] message) {

    private static final Random random = new Random();

    public static PreparedTest prepareTest() throws IOException {
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
      Files.write(Path.of(resourceSchwaemm + "/key"), key);
      Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
      Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
      Files.write(Path.of(resourceSchwaemm + "/message"), message);
      return new PreparedTest(key, nonce, associate, message);
    }
  }
}
