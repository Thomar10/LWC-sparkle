/***
 * http://www.crypto-uni.lu/jscoron/publications/secconvorder.pdf
 */
public class BooleanAddition {

    /***
     * Takes as input shares of x and shares of y and returns shares of z, such that z = x & y
     * @param x shares of x
     * @param y shares of y
     * @return shares of z
     */
    public static int[] secureBooleanAnd(int[] x, int[] y){
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

    /***
     * Takes as input shares of x and shares of y and returns shares of z, such that z = x + y
     * @param x shares of x
     * @param y shares of y
     * @return shares of z
     */
    public static int[] secureBooleanAdditionGoubin(int[] x, int[] y){
        int n = x.length;
        int k = 32; //bit length

        int[] w = secureBooleanAnd(x, y);
        int[] u = new int[n];
        int[] a = new int[n];

        for(int i = 0; i < n; i++){
            a[i] = x[i] ^ y[i];
        }

        for(int j = 0; j < k; j++){
            int[] ua = secureBooleanAnd(u, a);

            for(int i = 0; i < n; i++){
                u[i] = 2 * (ua[i] ^ w[i]); //Maybe wrong as ints are not unsigned
            }
        }

        int[] z = new int[n];

        for(int i = 0; i < n; i++){
            z[i] = x[i] ^ y[i] ^ u[i];
        }

        return z;
    }
}
