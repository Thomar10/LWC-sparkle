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

  private final Random random = new Random();
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
    byte[] nonce = new byte[16];
    random.nextBytes(nonce);
    byte[] key = new byte[16];
    random.nextBytes(key);
    int[] state = new int[8];

    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "initialize"});

    Schwaemm.initialize(state, key, nonce);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(50)
  void finalizeCall() throws IOException, InterruptedException {
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

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "finalize"});

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Schwaemm.encrypt(state, message);
    Schwaemm.finalize(state, key);
    Assertions.assertThat(cState).isEqualTo(state);
  }

  @RepeatedTest(50)
  void generateTag() throws IOException, InterruptedException {
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

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "generateTag"});
    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    byte[] javaCipher = Schwaemm.encrypt(state, message);

    Schwaemm.finalize(state, key);
    Schwaemm.generateTag(state, javaCipher, message.length);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void checkSchwaemm() throws IOException, InterruptedException {
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
    caller.callProcess(new String[]{schwaemmCpath, "fullFunction"});
    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));

    byte[] javaCipher = Schwaemm.encryptAndTag(message, associate, key, nonce);

    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void encryptAndDecryptC() throws IOException, InterruptedException {
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
    caller.callProcess(new String[]{schwaemmCpath, "encryptAndDecrypt"});
    byte[] messageBack = Files.readAllBytes(Paths.get(resourceSchwaemm + "/messageBack"));

    byte[] javaCipher = Schwaemm.encryptAndTag(message, associate, key, nonce);
    byte[] messageJava = Schwaemm.decryptAndVerify(javaCipher, associate, key, nonce);

    Assertions.assertThat(message).isEqualTo(messageBack);
    Assertions.assertThat(messageJava).isEqualTo(messageBack);
  }

  @RepeatedTest(50)
  void encryptAndDecrypt() {
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

    int[] state = new int[8];
    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    byte[] cipher = Schwaemm.encrypt(state, message);
    Schwaemm.finalize(state, key);
    byte[] javaCipher = Schwaemm.generateTag(state, cipher, message.length);

    int[] decryptState = new int[8];
    Schwaemm.initialize(decryptState, key, nonce);
    Schwaemm.associateData(decryptState, associate);
    byte[] messageBack = new byte[message.length];
    Schwaemm.decrypt(decryptState, messageBack, javaCipher);
    Assertions.assertThat(messageBack).isEqualTo(message);
  }

  @RepeatedTest(50)
  void processPlaintext() throws IOException, InterruptedException {
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

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    Files.write(Path.of(resourceSchwaemm + "/message"), message);
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "encrypt"});

    byte[] cipher = Files.readAllBytes(Paths.get(resourceSchwaemm + "/cipher"));

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    byte[] javaCipher = Schwaemm.encrypt(state, message);

    Assertions.assertThat(cState).isEqualTo(state);
    Assertions.assertThat(cipher).isEqualTo(javaCipher);
  }

  @RepeatedTest(50)
  void processAssociateData() throws IOException, InterruptedException {
    int randomInt = random.nextInt(32 - 1) + 1;
    byte[] associate = new byte[randomInt];
    random.nextBytes(associate);

    byte[] nonce = new byte[16];
    random.nextBytes(nonce);

    byte[] key = new byte[16];
    random.nextBytes(key);

    int[] state = new int[8];
    Files.write(Path.of(resourceSchwaemm + "/key"), key);
    Files.write(Path.of(resourceSchwaemm + "/nonce"), nonce);
    Files.write(Path.of(resourceSchwaemm + "/associate"), associate);
    int[] cState = caller.callProcess(new String[]{schwaemmCpath, "associate"});

    Schwaemm.initialize(state, key, nonce);
    Schwaemm.associateData(state, associate);
    Assertions.assertThat(cState).isEqualTo(state);
  }
}
