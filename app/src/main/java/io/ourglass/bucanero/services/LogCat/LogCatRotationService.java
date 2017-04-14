package io.ourglass.bucanero.services.LogCat;

import android.Manifest;
import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class LogCatRotationService extends IntentService {
    private static final String TAG = "LogCatRotationService";
    private static Handler wakeUpAndWorkHandler;
    private static File currentLTFFile;

    private final static String LOG_UPLOAD_ENDPOINT = OGConstants.ASAHI_MEDIA_ENDPOINT;

    private static File appDirectory;
    private static File logDirectory;

    @Override
    public void onCreate(){

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public LogCatRotationService(){
        super("LogCatRotationService");

    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* set up the logging file structure */
        //set up the directories
        appDirectory = new File( Environment.getExternalStorageDirectory() + "/ABLogs" );
        logDirectory = new File( appDirectory + "/log" );

        // create app folder
        if ( !appDirectory.exists() ) {
            appDirectory.mkdir();
        }

        // create log folder
        if ( !logDirectory.exists() ) {
            logDirectory.mkdir();
        }

        /* set up the repeater */
        wakeUpAndWorkHandler = new Handler();

        Runnable repeater = new Runnable() {
            @Override
            public void run() {
                //generate a new LTFFile
                File newLTFFile = generateNewLTFFileName();

                if(newLTFFile != null){
                    //redirect logcat to new file
                    boolean redirectSuccess = startLoggingToNewFile(newLTFFile);
                    if(redirectSuccess){
                        /*
                        doesn't appear that there is any need to close file as the logcat command handles all of this
                        leaving this in in case there is a problem with this in the future, however at present there is no evident problem
                         */

                        //set the current file reference to the new file
                        currentLTFFile = newLTFFile;

                        attemptUploadOfOldestLogFile();
                    }
                    else {
                        Log.e(TAG, "Could not redirect logcat to the new file");
                    }
                }
                else {
                    Log.e(TAG, "Could not create a new logging file");
                }

                attemptUploadOfOldestLogFile();

                //run this every hour
                wakeUpAndWorkHandler.postDelayed(this, 1000 * 60 * 60);
            }
        };

        boolean repeating = wakeUpAndWorkHandler.postDelayed(repeater, 1000);

        return START_NOT_STICKY;
    }

    /**
     * finds the oldest log file and attempts to upload it to Backend
     * if upload succeeds, will delete the file
     */
    private void attemptUploadOfOldestLogFile() {
        //get the current log files
        File[] files = logDirectory.listFiles();

        //make sure we only upload/remove a logfile if it isn't the only one (and likely the current)
        if(files.length > 1){
            File oldest = getOldestLogFile(files);

            if(oldest.getName().equals(currentLTFFile.getName())){
                Log.e(TAG, "Receiving current file as oldest, likely the other log files do not have a valid file name");
                return;
            }

            postOldLogFileToBackend(oldest);
        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    /**
     * creates a new ltf file in the sdcard/ABLogs/logs directory.
     * File name will take the form of logcat-[uuid]-[mmddyy]-[hh:mm:ss].txt based on the current time
     * @return created file
     */
    public static File generateNewLTFFileName(){

        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMddyy-HH:mm:ss");
        String timeStr = sdf.format(now.getTime());

        String fileName = "logcat-" + OGSystem.getUDID() + "-" + timeStr + ".txt";

        File logFile = new File( logDirectory, fileName );

        return logFile;

    }

    /**
     * points logcat to the supplied file
     * @param newLogFile file to Log to
     * @return true if logcat system command succeeds, false otherwise
     */
    public static boolean startLoggingToNewFile(File newLogFile){
        boolean redirectSuccessful = false;

        // clear the previous logcat and then write the new one to the file
        try {
            Process process = Runtime.getRuntime().exec( "logcat -c");
            process = Runtime.getRuntime().exec( "logcat -f " + newLogFile + "");

            redirectSuccessful = true;

        } catch ( IOException e ) {
            e.printStackTrace();
        }

        return redirectSuccessful;
    }

    /**
     * given an array of logfiles, returns the oldest in the array (based on file name date info)
     * @param logFiles array of logfiles
     * @return the oldest file in the array
     */
    public static File getOldestLogFile(File[] logFiles){
        File oldestFile;
        Date oldestFilesDate = null;

        int i = 0;
        do {
            oldestFile = logFiles[i];
            oldestFilesDate = extractDateFromFileName(oldestFile);
        } while(oldestFilesDate == null && ++i < logFiles.length);

        for(; i < logFiles.length; i++){
            Date logFilesDate = extractDateFromFileName(logFiles[i]);
            if(logFilesDate != null) {
                if (logFilesDate.compareTo(oldestFilesDate) < 0) {
                    oldestFile = logFiles[i];
                    oldestFilesDate = logFilesDate;
                }
            }
        }

        return oldestFile;
    }

    /**
     * Extracts the date information from the supplied file's name and creates a date object from it
     * @param logFile the log file with date information in the form "logcat-[uuid]-[mmddyy]-[hh:mm:ss].txt"
     * @return date object corresponding to file's name, null if not formatted correctly
     */
    public static Date extractDateFromFileName(File logFile){
        String fileName = logFile.getName();

        int month = 0, day = 0, year = 0, hours = 0, minutes = 0, seconds = 0;

        //remove the prefix
        int prefixIdx = fileName.indexOf("logcat-");
        if(prefixIdx == -1){
            Log.e(TAG, "there is a file (" + logFile.getName() + ") in the log directory, with an incorrectly formatted name, please remove this");
            return null;
        }
        fileName = fileName.substring(prefixIdx + "logcat-".length());

        //remove uuid
        fileName = fileName.substring(fileName.indexOf("-") + 1);

        //get mmddyy string
        String dateStr = fileName.substring(0,fileName.indexOf("-"));
        if(dateStr.length() != 6){
            Log.e(TAG, "apparent illegal datestring in fileName (" + logFile.getName() + ") (could also be caused by missing uuid");
            return null;
        }
        String monthString = dateStr.substring(0,2);
        String dayString = dateStr.substring(2,4);
        String yearString = "20" + dateStr.substring(4);

        //extract values from mmddyy
        try{
            month = Integer.parseInt(monthString);
            day = Integer.parseInt(dayString);
            year = Integer.parseInt(yearString);
        } catch (NumberFormatException e){
            Log.e(TAG, "apparent illegal datestring in fileName (" + logFile.getName() + ") (could also be caused by missing uuid");
            return null;
        }
        fileName = fileName.substring(fileName.indexOf("-") + 1);

        //extract hours, minutes, and seconds
        int firstColon = fileName.indexOf(":");
        int secondColon = fileName.indexOf(":", firstColon + 1);

        String hoursString = fileName.substring(0, firstColon);
        String minutesString = fileName.substring(firstColon + 1, secondColon);
        String secondsString = fileName.substring(secondColon + 1, fileName.indexOf(".txt"));

        if(hoursString.length() != 2|| minutesString.length() != 2 || secondsString.length() != 2){
            Log.e(TAG, "apparent illegal minutes or seconds value in fileName (" + logFile.getName() + ")");
            return null;
        }
        try {
            hours = Integer.parseInt(hoursString);
            minutes = Integer.parseInt(minutesString);
            seconds = Integer.parseInt(secondsString);
        } catch (NumberFormatException e){
            Log.e(TAG, "apparent illegal minutes or seconds value in fileName (" + logFile.getName() + ")");
            return null;
        }

        //create and return date from values
        Calendar c = Calendar.getInstance();
        c.set(year, month, day, hours, minutes, seconds);

        return c.getTime();
    }

    /**
     * posts the contents of the supplied file to /media/upload with the associated body parameters
     * NOTE: does not delete the file
     * @param oldLogFile oldest log file to upload contents of
     */
    public static void postOldLogFileToBackend(File oldLogFile){

        PostFileTask postFile = new PostFileTask();
        postFile.execute(oldLogFile);
    }

    private static class PostFileTask extends AsyncTask<File, Void, Boolean> {

        private Exception exception;

        protected Boolean doInBackground(File... files) {
            OkHttpClient client = ABApplication.okclient;

            try {
                File oldLogFile = files[0];
                Date date = extractDateFromFileName(oldLogFile);

                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                SimpleDateFormat sdf = new SimpleDateFormat("MMddyy");
                String dateStr = sdf.format(cal.getTime());

                JSONObject logCatObj = new JSONObject();
                logCatObj.put("uuid", OGSystem.getUDID());
                logCatObj.put("date", dateStr);

                JSONObject metadataObj = new JSONObject();
                metadataObj.put("logcat", logCatObj);
                metadataObj.put("src", oldLogFile.getName());

                RequestBody reqBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("metadata", metadataObj.toString())
                        .addFormDataPart("file", oldLogFile.getName(),
                                RequestBody.create(MediaType.parse("text/plain"), oldLogFile))
                        .build();

                Request request = new Request.Builder()
                        .url(OGConstants.BELLINI_ADDRESS + LOG_UPLOAD_ENDPOINT)
                        .post(reqBody)
                        .build();


                Response res = client.newCall(request).execute();
                if(!res.isSuccessful()) {
                    Log.e(TAG, "Posting old log file to backend failed with code " + res);

                    res.close();
                    //don't remove
                    return false;
                }

                Log.v(TAG, "Successfully uploaded old logfile: " + oldLogFile.getName() + " will now be deleted");

                //remove the old file
                boolean deleteSuccess = oldLogFile.delete();
                if(!deleteSuccess){
                    Log.e(TAG, "Could not delete old log file: " + oldLogFile.getParent());

                    res.close();
                    return false;
                }
                res.close();
                return true;
            } catch (Exception e) {
                this.exception = e;
                return false;
            }
        }

    }}
