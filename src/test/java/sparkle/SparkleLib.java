package sparkle;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Utility class for loading in C library of sparkle and exposing desired functions.
 */
public final class SparkleLib {

  private static final SparkleC sparkleC;

  static {
    String library;
    if (System.getProperty("os.name").contains("Windows")) {
      library = "sparkle/libsparkle.dll";
    } else {
      library = "sparkle/libsparkle.so";
    }
    sparkleC = (SparkleC) Native.synchronizedLibrary(Native.load(library, SparkleC.class));
  }

  public static void sparkleC(int[] state, int branch, int steps) {
    sparkleC.sparkle_opt(state, branch, steps);
  }

  interface SparkleC extends Library {

    void sparkle_opt(int[] state, int branch, int step);
  }
}
