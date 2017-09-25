package io.ourglass.bucanero.services.FFmpeg;

import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class to lazily transfer from the read to write side of a pipe.  Most transfers exit when the
 * read size is empty (returns 0), but in this case that is perfectly acceptable.  Designed to
 * run infinitely provided the receiver does not go away.
 *
 */

public class LazyTransferThread extends Thread {
    InputStream      mInStream;
    FileOutputStream mOutStream;
    long             mLoopDelayMillis;
    int              mBufferSize;

    /**
     *
     * @param in
     * @param out
     * @param bufferSize
     * @param loopDelayMillis
     */
    LazyTransferThread(InputStream      in,
                       FileOutputStream out,
                       int              bufferSize,
                       long             loopDelayMillis ) {
        super("LazyTransferThread");
        this.mInStream        = in;
        this.mOutStream       = out;
        this.mBufferSize      = bufferSize;
        this.mLoopDelayMillis = loopDelayMillis;
    }

    public void doit() throws IOException {
        byte[] buf=new byte[mBufferSize];
        boolean looping = true;
        try {
            while(looping) {
                int len;
                sleep(mLoopDelayMillis);

                try {
                    while ((len=mInStream.read(buf)) >= 0) {       // Throws IOException or NullPointerException
                        mOutStream.write(buf, 0, len);             // Throws IOException
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
        mInStream.close();
        mOutStream.flush();
        //mOutStream.getFD().sync();
        mOutStream.close();
        interrupt();
    }

    @Override
    public void run() {
        Log.i(getClass().getSimpleName(), "SJM Running transfer thread");
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
