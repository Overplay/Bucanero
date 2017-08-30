package io.ourglass.bucanero.services.FFmpeg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

public class LazyTransferThread extends Thread {
    InputStream in;
    FileOutputStream out;
    long loopDelayMillis;

    LazyTransferThread(InputStream in,
                       FileOutputStream out,
                       long loopDelayMillis ) {
        super("LazyTransferThread");
        this.in=in;
        this.out=out;
        this.loopDelayMillis = loopDelayMillis;
    }

    public void doit() throws IOException {
        byte[] buf=new byte[4096];
        boolean looping = true;
        try {
            while(looping) {
                int len;
                sleep(loopDelayMillis);

                try {
                    while ((len=in.read(buf)) >= 0) {       // Throws IOException or NullPointerException
                        out.write(buf, 0, len);             // Throws IOException
                    }
                }
                catch (IOException e) {

                    // EPIPE (Broken pipe) gets sent when the server is dead or a connection is lost.
                    Log.e(getClass().getSimpleName(), "Exception transferring file", e);
                    looping = false;
                }

            }
        } catch (InterruptedException e) {
            Log.e(getClass().getSimpleName(), "Exception interrupted file", e);
        }
        in.close();
        out.flush();
        //out.getFD().sync();
        out.close();
        interrupt();
    }

    @Override
    public void run() {
        try {
            doit();
        }
        catch (IOException ioe) {
            Log.e(getClass().getSimpleName(), "Exception closing file", ioe);
        }
        catch (SecurityException se) {
            Log.e(getClass().getSimpleName(), "Thread interrupt", se);
        }
        catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Thread exception", e);
        }
    }
}
