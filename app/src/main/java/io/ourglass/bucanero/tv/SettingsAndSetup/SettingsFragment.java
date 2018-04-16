package io.ourglass.bucanero.tv.SettingsAndSetup;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

//import com.crashlytics.android.answers.Answers;
//import com.crashlytics.android.answers.ContentViewEvent;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.OGUi;
import io.ourglass.bucanero.tv.Activities.MainFrameActivity;
import io.ourglass.bucanero.tv.Fragments.OverlayFragment;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends OverlayFragment {

    public static final String TAG = "SettingsFragment";

    TextView mHeaderTV;
    TextView mSubHeaderTV;

    ViewGroup mButtonHolder;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
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
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mHeaderTV = (TextView) getView().findViewById(R.id.fragmentHeader);
        mSubHeaderTV = (TextView) getView().findViewById(R.id.fragmentSubHeader);

        mHeaderTV.setTypeface(OGUi.getBoldFont());
        //mErrorMsg.setTypeface(OGUi.getRegularFont());
        mSubHeaderTV.setTypeface(OGUi.getRegularFont());

        View.OnClickListener clickL = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processTag((String)v.getTag());
            }
        };

        mButtonHolder = (ViewGroup)getView().findViewById(R.id.buttonHolder);

        for (int i=0; i<mButtonHolder.getChildCount(); i++){
            View v = mButtonHolder.getChildAt(i);
            if ( v instanceof Button){
                v.setOnClickListener(clickL);
            }
        }

//        Answers.getInstance().logContentView(new ContentViewEvent()
//                .putContentType("Menu")
//                .putContentName("Settings Menu"));


        // TODO this should actually be self-resetting based on activity in the fragment
        //dismissMeAfter(5*60*1000);
    }

    public void processTag(String tag){
        Log.d(TAG, "Clicked "+ tag);

        switch (tag){
            case "sysinfo":
                ((MainFrameActivity)getActivity()).launchSysInfoFragment();
                break;

            case "stb_pair":
                ((MainFrameActivity)getActivity()).launchSTBPairFragment();
                break;

            case "wifi":
                MainFrameActivity mf = ((MainFrameActivity)getActivity());
                Intent launchIntent = mf.getPackageManager().getLaunchIntentForPackage("io.ourglass.wort");
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    mf.finish();
                }
                break;

            case "venue":
                ((MainFrameActivity)getActivity()).launchVenuePairFragment();
                break;

            case "adv":
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                break;

            case "cancel":
                dismissMe();
                break;

        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    public void dismiss(View v){
        dismissMe();
    }




}
