import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public final class EschKatTestHelper {
    ArrayList<String> msgs;
    ArrayList<String> mds;

    int count = 0;


    public EschKatTestHelper(File file) throws FileNotFoundException { //let someone else deal with a missing file
        msgs = new ArrayList<>();
        mds = new ArrayList<>();

        //Load data from file into arrays
        Scanner reader = new Scanner(file);
        while (reader.hasNextLine()) {
            String data = reader.nextLine();
            //System.out.println(data);

            if (data.contains("Msg")) {
                msgs.add(data.substring(6));


            } else if (data.contains("MD")) {
                mds.add(data.substring(5));
            }
        }
    }

    public boolean hasNext(){
        return count < msgs.size() && count < mds.size();
    }

    public String[] getNextTest(){
        String[] test = new String[2];
        test[0] = msgs.get(count);
        test[1] = mds.get(count);
        count++;
        return test;
    }


}
