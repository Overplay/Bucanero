package io.ourglass.bucanero.tv.SettingsAndSetup;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.OGUi;
import io.ourglass.bucanero.tv.Activities.MainFrameActivity;
import io.ourglass.bucanero.tv.Fragments.OverlayFragment;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WelcomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WelcomeFragment extends OverlayFragment {

    public static final String TAG = "WelcomeFragment";

    TextView mHeaderTV;
    TextView mSubHeaderTV;

    Button nextStepButton;
    Button cancelButton;

    public WelcomeFragment() {
        // Required empty public constructor
    }

    public static WelcomeFragment newInstance() {
        WelcomeFragment fragment = new WelcomeFragment();
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
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mHeaderTV = (TextView) getView().findViewById(R.id.fragmentHeader);
        mSubHeaderTV = (TextView) getView().findViewById(R.id.fragmentSubHeader);

        mHeaderTV.setTypeface(OGUi.getBoldFont());
        //mErrorMsg.setTypeface(OGUi.getRegularFont());
        mSubHeaderTV.setTypeface(OGUi.getRegularFont());

        nextStepButton = (Button)getView().findViewById(R.id.buttonNextStep);
        nextStepButton.setTypeface(OGUi.getRegularFont());
        nextStepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainFrameActivity)getActivity()).launchVenuePairFragment();
            }
        });


        cancelButton = (Button)getView().findViewById(R.id.buttonCancel);
        cancelButton.setTypeface(OGUi.getRegularFont());
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainFrameActivity)getActivity()).mFirstTimeSetupSkipped = true;
                dismissMe();
            }
        });


        Answers.getInstance().logContentView(new ContentViewEvent()
            .putContentType("Menu")
            .putContentName("Welcome/Setup Menu"));

        // TODO this should actually be self-resetting based on activity in the fragment
        dismissMeAfter(5*60*1000);
    }


//    @Override
//    public void onResume() {
//        super.onResume();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//    }


    public void dismiss(View v){
        dismissMe();
    }




}
