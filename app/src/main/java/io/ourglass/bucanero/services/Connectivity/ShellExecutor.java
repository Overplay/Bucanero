package io.ourglass.bucanero.services.Connectivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by mkahn on 1/26/17.
 */

public class ShellExecutor {

    private ShellExecutorListener mListener;

    public ShellExecutor(ShellExecutorListener listener) {
        mListener = listener;
    }

    public interface ShellExecutorListener {
        public void results(ArrayList<String> results);
    }

    public void exec(final String command) {

        Runnable seRun = new Runnable() {
            @Override
            public void run() {

                ArrayList<String> output = new ArrayList<>();

                Process p;
                try {
                    p = Runtime.getRuntime().exec(command);
                    p.waitFor();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        //output.append(line + "n");
                        output.add(line);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (mListener != null) {
                    mListener.results(output);
                }
            }
        };

        Thread seThread = new Thread(seRun);
        seThread.start();
    }
}
