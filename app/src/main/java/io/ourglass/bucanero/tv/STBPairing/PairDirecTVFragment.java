package io.ourglass.bucanero.tv.STBPairing;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.core.OGUi;
import io.ourglass.bucanero.messages.MainThreadBus;
import io.ourglass.bucanero.objects.SetTopBox;
import io.ourglass.bucanero.services.SSDP.SSDPResult;
import io.ourglass.bucanero.services.SSDP.SSDPService;
import io.ourglass.bucanero.services.STB.DirecTV.DirecTVSetTopBox;
import io.ourglass.bucanero.tv.Fragments.OverlayFragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PairDirecTVFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PairDirecTVFragment extends OverlayFragment {

    public static final String TAG = "PairDirecTVFragment";

    TextView mTitle;
    TextView mCurrentPair;
    ListView mDirectvDevicesList;
    TextView mEmptyListMessage;
    TextView mConfirmationPrompt;

    View mScanningHolder;
    View mConfirmationHolder;
    View mListHolder;
    View mHPHolder;

    ArrayList<SetTopBox> mFoundBoxes = new ArrayList<>();
    SetTopBoxAdapter mSTBArrayAdapter;

    public SetTopBox lastSTBClicked;

    private MainThreadBus bus = ABApplication.ottobus;

    private enum PairMode { SEARCH, LIST, CONFIRM, HARD_PAIRED };
    private PairMode mMode = PairMode.SEARCH;


    public PairDirecTVFragment() {
        // Required empty public constructor
    }


    public static PairDirecTVFragment newInstance() {
        PairDirecTVFragment fragment = new PairDirecTVFragment();
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
        return inflater.inflate(R.layout.fragment_stbpair_directv, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        mTitle = (TextView) getView().findViewById(R.id.directv_pair_title);
        //mErrorMsg = (TextView) getView().findViewById(R.id.error_msg);
        mCurrentPair = (TextView) getView().findViewById(R.id.current_pair);
        mDirectvDevicesList = (ListView) getView().findViewById(R.id.directv_devices_list);
        mEmptyListMessage = (TextView) getView().findViewById(R.id.empty_list_message);
        mConfirmationPrompt = (TextView) getView().findViewById(R.id.confirmationPromptTV);

        mTitle.setTypeface(OGUi.getBoldFont());
        //mErrorMsg.setTypeface(OGUi.getRegularFont());
        mCurrentPair.setTypeface(OGUi.getRegularFont());
        mEmptyListMessage.setTypeface(OGUi.getBoldFont());
        mConfirmationPrompt.setTypeface(OGUi.getBoldFont());

        mConfirmationHolder = getView().findViewById(R.id.confirmationHolder);
        mScanningHolder = getView().findViewById(R.id.scanningHolder);
        mListHolder = getView().findViewById(R.id.listHolder);
        mHPHolder = getView().findViewById(R.id.hardPairHolder);

        mSTBArrayAdapter = new SetTopBoxAdapter(getContext(), mFoundBoxes);
        mDirectvDevicesList.setAdapter(mSTBArrayAdapter);

        mDirectvDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDirectvDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        Log.d(TAG, "User selected STB @ position "+ position);
                        lastSTBClicked = mFoundBoxes.get(position);
                        confirmSTBChoice();

                    }
                });

            }
        });

        ((Button)getView().findViewById(R.id.buttonOK)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OGSystem.setPairedSTB(lastSTBClicked);
                Toast.makeText(getContext(), "Set Top Box Paired", Toast.LENGTH_LONG).show();
                mMode = PairMode.LIST;
                updateUI();
            }
        });

        ((Button)getView().findViewById(R.id.buttonCancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Set Top Box Pairing Canceled", Toast.LENGTH_LONG).show();
                mMode = PairMode.LIST;
                updateUI();
            }
        });

        // TODO this should actually be self-resetting based on activity in the fragment
        dismissMeAfter(5*60*1000);
    }

    @Override
    public void onResume() {
        super.onResume();

        bus.register(this);

        // If hard-paired, just show a message
        mMode = OGSystem.isHardPaired() ? PairMode.HARD_PAIRED : PairMode.SEARCH;

        // Only kick off search if not hard-paired
        if (mMode==PairMode.SEARCH){
            Intent ssdpi = new Intent(getContext(), SSDPService.class);
            ssdpi.putExtra("deviceFilter", "DIRECTV");
            getActivity().startService(ssdpi);
        }

        updateUI();
    }

    @Override
    public void onPause() {
        bus.unregister(this);
        super.onPause();
    }

    private void confirmSTBChoice(){

        Log.d(TAG, "User selected STB @ IP Address "+ lastSTBClicked.ipAddress);
        mMode = PairMode.CONFIRM;
        mConfirmationPrompt.setText("Are you sure you want to pair to the Set Top Box at IP Address: "
                + lastSTBClicked.ipAddress+"?");
        updateUI();

    }

    private void updateUI(){

        switch (mMode){

            case HARD_PAIRED:
                mHPHolder.setVisibility(View.VISIBLE);
                mScanningHolder.setVisibility(View.INVISIBLE);
                mConfirmationHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.INVISIBLE);

                break;

            case SEARCH:
                mHPHolder.setVisibility(View.INVISIBLE);
                mScanningHolder.setVisibility(View.VISIBLE);
                mConfirmationHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.INVISIBLE);

                break;

            case LIST:

                mHPHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.VISIBLE);


                if (mSTBArrayAdapter.getCount()>0){
                    mEmptyListMessage.setText("");
                    mEmptyListMessage.setVisibility(View.GONE);
                } else {
                    mEmptyListMessage.setText("No set top boxes were found.");
                    mEmptyListMessage.setVisibility(View.VISIBLE);
                }

                mScanningHolder.setVisibility(View.INVISIBLE);  // was GONE, does it matter?
                mConfirmationHolder.setVisibility(View.INVISIBLE);

                mSTBArrayAdapter.refreshDevices(new SetTopBoxAdapter.UpdateListener() {
                    @Override
                    public void done() {
                        try {
                            uiListRefresh();
                        } catch (Exception e){
                            Log.d(TAG, "Exception on refresh upon STB list background refresh. Probably dead frag.");
                        }
                    }
                });

                break;

            case CONFIRM:
                mHPHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.INVISIBLE);
                mScanningHolder.setVisibility(View.INVISIBLE);
                mConfirmationHolder.setVisibility(View.VISIBLE);

                break;
        }

        updateCurrentPairText();

    }

    public void setErrorMsg(String message) {
        mEmptyListMessage.setVisibility(View.VISIBLE);
        mEmptyListMessage.setText(message);
    }


    private void updateCurrentPairText(){
        if (OGSystem.isPairedToSTB()) {
            mCurrentPair.setText(Html.fromHtml("<b>Paired to:</b> ")+OGSystem.getPairedSTBIpAddress());
        } else {
            mCurrentPair.setText("Not Paired");
        }
    }


    public void uiListRefresh() {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSTBArrayAdapter.notifyDataSetChanged();
            }
        });

    }

    public void updateDTVArray(HashMap<String, String> devices) {

        mSTBArrayAdapter.clear();

        for (Map.Entry<String, String> device : devices.entrySet()) {

            DirecTVSetTopBox newSTB = new DirecTVSetTopBox(null, device.getKey(), SetTopBox.STBConnectionType.IPGENERIC, device.getValue());
            //newSTB.ssdpResponse = device.getValue(); // Save it for getting the model name
            //newSTB.updateWhatsOn();
            mSTBArrayAdapter.add(newSTB);

        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMode = PairMode.LIST;
                updateUI();
            }
        });


    }

    @Subscribe
    public void ssdpResultsAreIn(SSDPResult result) {

        Log.d(TAG, "Got new SSDP results");
        HashMap<String, String> devices = result.devices;
        if (devices != null) {
            Log.d(TAG, "Got some DirecTV boxes, updating ArrayList");
            updateDTVArray(devices);
        }

    }


}
