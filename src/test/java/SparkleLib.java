import com.sun.jna.Library;
import com.sun.jna.Native;

/** Utility class for loading in C library of sparkle and exposing desired functions. */
public final class SparkleLib {

  private static final SparkleC sparkleC =
      (SparkleC) Native.synchronizedLibrary(Native.load("sparkle/libsparkle.so", SparkleC.class));

  public static void sparkleC(int[] state, int branch, int steps) {
    sparkleC.sparkle_opt(state, branch, steps);
  }

  interface SparkleC extends Library {

    void sparkle_opt(int[] state, int branch, int step);
  }
}
