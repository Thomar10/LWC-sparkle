import java.util.Random;

public class BooleanAddition {

    public static void SafeBitFullAdder(int x, int y, int p, int q){

    }

    public static int[] SecAnd(int[] x, int[] y){
        Random random = new Random();
        int n = x.length;
        int[][] r = new int[n][n];

        for(int i = 0; i < n; i++){
            for(int j = i + 1; j < n; j++){
                //r[i][j] = random.nextInt(2);
                r[i][j] = (r[i][j] ^ (x[i] & y[j])) ^ (x[j] & y[i]);
            }
        }

        int[] z = new int[n];

        for(int i = 0; i < n; i++){
            z[i] = x[i] & y[i];
            for(int j = 0; j < n; j++){
                if(i != j){
                    z[i] = z[i] ^ r[i][j];
                }
            }
        }

        return z;
    }
}
