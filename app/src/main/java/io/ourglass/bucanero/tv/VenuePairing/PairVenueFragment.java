package io.ourglass.bucanero.tv.VenuePairing;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.json.JSONObject;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.api.JSONCallback;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.core.OGUi;
import io.ourglass.bucanero.messages.MainThreadBus;
import io.ourglass.bucanero.messages.VenuePairCompleteMessage;
import io.ourglass.bucanero.objects.NetworkException;
import io.ourglass.bucanero.tv.Fragments.OverlayFragment;

import static io.ourglass.bucanero.tv.VenuePairing.PairVenueFragment.PairMode.CODE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PairVenueFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PairVenueFragment extends OverlayFragment {

    public static final String TAG = "PairVenueFragment";

    private static int COUNTDOWN_TIME = 5 * 60 * 1000;
    private static int CD_INTERVAL = COUNTDOWN_TIME / 100;

    TextView mTitle;
    TextView mCodeTV;
    TextView mVenueNameTV; // not used for now

    ProgressBar mCountdownPB;

    View mAlreadyPairedHolder;
    View mCodeHolder;
    View mPairDoneHolder;

    Button mReRegButton;
    Button mExitButton;

    private MainThreadBus bus = ABApplication.ottobus;

    public enum PairMode { PAIRED, CODE, CONFIRM, COMPLETE };
    private PairMode mMode = CODE;


    public PairVenueFragment() {
        // Required empty public constructor
    }


    public static PairVenueFragment newInstance() {
        PairVenueFragment fragment = new PairVenueFragment();
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
        return inflater.inflate(R.layout.fragment_venue_pair, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mTitle = (TextView) getView().findViewById(R.id.venue_pair_title);
        mTitle.setTypeface(OGUi.getBoldFont());

        mExitButton = (Button)getView().findViewById(R.id.buttonExit);
        mExitButton.setTypeface(OGUi.getRegularFont());
        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissMe();
            }
        });

        mReRegButton = (Button)getView().findViewById(R.id.buttonReReg);
        mReRegButton.setTypeface(OGUi.getRegularFont());
        mReRegButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToCodeMode();
            }
        });

        mCountdownPB = (ProgressBar) getView().findViewById(R.id.progressBar);
        mCodeTV = (TextView) getView().findViewById(R.id.codeTV);
        mCodeTV.setTypeface(OGUi.getBoldFont());

        ((TextView)getView().findViewById(R.id.instructionsTV)).setTypeface(OGUi.getRegularFont());
        ((TextView)getView().findViewById(R.id.alreadyPairedTV)).setTypeface(OGUi.getRegularFont());
        ((TextView)getView().findViewById(R.id.pairDoneTV)).setTypeface(OGUi.getRegularFont());

        mCodeHolder = getView().findViewById(R.id.codeHolder);
        mAlreadyPairedHolder = getView().findViewById(R.id.alreadyPairedHolder);
        mPairDoneHolder = getView().findViewById(R.id.pairDoneHolder);

        // TODO this should actually be self-resetting based on activity in the fragment
        //dismissMeAfter(5*60*1000);
    }

    private void goToCodeMode(){
        getCode();
        mMode = CODE;
        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();

        bus.register(this);
        // If hard-paired, just show a message
        mMode = ( OGSystem.getVenueId().isEmpty() || OGConstants.FORCE_VENUE_PAIR )  ? CODE : PairMode.PAIRED;
        if (mMode== CODE){
            goToCodeMode();
        }
        updateUI();
    }

    @Override
    public void onPause() {
        bus.unregister(this);
        mCountdownPB.removeCallbacks(null);
        super.onPause();
    }

    private void startCountdown(){

        mCountdownPB.postDelayed(new Runnable() {
            @Override
            public void run() {
                int prog = mCountdownPB.getProgress()-1;
                mCountdownPB.setProgress(prog);
                // TODO make bar change color as it decreases
                if (prog <= 0) {
                    dismissMe();
                } else {
                    mCountdownPB.postDelayed(this, CD_INTERVAL);
                }
            }
        }, CD_INTERVAL);
    }

    private void updateCodeText(final String code){

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCountdownPB.setProgress(100);
                mCodeTV.setText(code);
                startCountdown();
            }
        });
    }

    private void getCode(){
        BelliniDMAPI.getRegCode(new JSONCallback() {
            @Override
            public void jsonCallback(JSONObject jsonData) {
                String code = jsonData.optString("code", "ERROR");
                updateCodeText(code);
            }

            @Override
            public void error(NetworkException e) {
                updateCodeText("NETWORK PROBLEM");
            }
        });
    }

    private void updateUI(){

        switch (mMode){

            case CODE:
                mCodeHolder.setVisibility(View.VISIBLE);
                mAlreadyPairedHolder.setVisibility(View.INVISIBLE);
                mPairDoneHolder.setVisibility(View.INVISIBLE);
                break;

            case COMPLETE:
                mCodeHolder.setVisibility(View.INVISIBLE);
                mAlreadyPairedHolder.setVisibility(View.INVISIBLE);
                mPairDoneHolder.setVisibility(View.VISIBLE);
                break;

            case PAIRED:
                mCodeHolder.setVisibility(View.INVISIBLE);
                mAlreadyPairedHolder.setVisibility(View.VISIBLE);
                mPairDoneHolder.setVisibility(View.INVISIBLE);
                break;

        }

    }



    @Subscribe
    public void venuePairComplete(VenuePairCompleteMessage msg) {
        Log.d(TAG, "Got venue pair complete msg, dimissing!");
        OGSystem.setFirstTimeSetup(false); // if you get this message, all else is cool.
        dismissMe();
    }



}
