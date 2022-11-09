import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Scanner;

public final class ProcessCaller {

  private final String resourcePath;
  private final String stateString;

  public ProcessCaller(String resourcePath, String stateString) {
    this.resourcePath = resourcePath;
    this.stateString = stateString;
  }


  public int[] callProcess(String[] arguments) throws InterruptedException, IOException {
    Process process =
        new ProcessBuilder(Arrays.asList(arguments)).directory(new File(resourcePath)).start();
    int result = process.waitFor();
    if (result != 0) {
      printError(process);
    }
    File myObj = new File(resourcePath + "/" + stateString);
    Scanner scanner = new Scanner(myObj);
    int[] cState = new int[Sparkle.maxBranches];
    int i = 0;
    while (scanner.hasNextInt()) {
      cState[i] = scanner.nextInt();
      i++;
    }
    return cState;
  }

  private void printError(Process process) throws IOException {
    BufferedReader errinput = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String line = errinput.readLine();
    while (line != null) {
      System.out.println(line);
      line = errinput.readLine();
    }
  }
}
