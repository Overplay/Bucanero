package io.ourglass.bucanero.tv.SettingsAndSetup;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.OGSettings;
import io.ourglass.bucanero.core.OGUi;
import io.ourglass.bucanero.services.Connectivity.ConnectivityCenter;
import io.ourglass.bucanero.tv.Fragments.OverlayFragment;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DeveloperSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeveloperSettingsFragment extends OverlayFragment {

    public static final String TAG = "DevSettingsFragment";

    TextView mHeaderTV;
    TextView mSubHeaderTV;

    Switch switchDevBellini;
    Switch switchVerbose;

    ViewGroup mButtonHolder;

    private String initialBelliniServer = OGSettings.getBelliniDMAddress();

    public DeveloperSettingsFragment() {
        // Required empty public constructor
    }

    public static DeveloperSettingsFragment newInstance() {
        DeveloperSettingsFragment fragment = new DeveloperSettingsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_devsettings, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mHeaderTV = (TextView) getView().findViewById(R.id.fragmentHeader);
        mSubHeaderTV = (TextView) getView().findViewById(R.id.fragmentSubHeader);

        mHeaderTV.setTypeface(OGUi.getBoldFont());
        //mErrorMsg.setTypeface(OGUi.getRegularFont());
        mSubHeaderTV.setTypeface(OGUi.getRegularFont());

        switchDevBellini = (Switch) getView().findViewById(R.id.switchDevBellini);
        switchDevBellini.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                OGSettings.setBelliniDMMode(isChecked ? "dev" : "production");

            }
        });

        switchDevBellini.setChecked(OGSettings.getBelliniDMMode().equalsIgnoreCase("dev"));

        switchVerbose = (Switch) getView().findViewById(R.id.switchVerboseMode);
        switchVerbose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                OGSettings.setVerboseMode( isChecked );

            }
        });

        switchVerbose.setChecked(OGSettings.getVerboseMode());

        // TODO this should actually be self-resetting based on activity in the fragment
        //dismissMeAfter(5*60*1000);
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {

        if (!initialBelliniServer.equalsIgnoreCase(OGSettings.getBelliniDMAddress())){
            Log.d(TAG, "Bellini server was changed, restarting network");
            ConnectivityCenter.getInstance().initializeCloudComms();
        }
        super.onPause();
    }


    public void dismiss(View v) {
        dismissMe();
    }


}
