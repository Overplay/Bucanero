package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import org.json.JSONObject;

import io.ourglass.bucanero.core.OGSettings;
import io.ourglass.bucanero.messages.SystemCommandMessage;
import io.ourglass.bucanero.services.SocketIO.SIOMessages.SettingResponseMessage;

import static io.ourglass.bucanero.services.SocketIO.SocketIOManager.sailsSIODeviceMessage;

/**
 * Created by mkahn on 8/16/17.
 */

public class SystemSettingAction extends SIOAction {

    public SystemSettingAction(){
        super("system_setting");
    }

    @Override
    public void process(JSONObject inboundObject) {
        Log.d(TAG, "Socket system_setting command received");

        String setting = inboundObject.optString("setting", null);

        if (setting==null){
            Log.d(TAG, "No setting field, ignoring.");
            return;
        }

        switch (setting){
            case "verboseMode":

                boolean mode = inboundObject.optBoolean("value");
                OGSettings.setVerboseMode(mode);
                sailsSIODeviceMessage(new SettingResponseMessage("verboseMode", OGSettings.getVerboseMode()?"true":"false").toJson(), null);
                break;

            case "uploadLogcat":

                boolean upload = inboundObject.optBoolean("value");
                OGSettings.setLogcatUploadMode(upload);
                sailsSIODeviceMessage(new SettingResponseMessage("uploadLogcat", OGSettings.getLogcatUploadMode()?"true":"false").toJson(), null);
                break;

            case "relaunch":
                //FIXME this is not going to work until we become a system app
                sailsSIODeviceMessage(new SettingResponseMessage("relaunch", "Relaunching 5 seconds. Except this won't since we don't have privs yet.").toJson(), null);
                (new SystemCommandMessage(SystemCommandMessage.SystemCommand.REBOOT)).post();
                break;

            case "dmServer":
                //Not implemented yet, will need a lot of network teardown and restart!
            default:
                sailsSIODeviceMessage(new SettingResponseMessage("wtf?", "Dunno what you mean, sparky,").toJson(), null);

        }

    }
}
