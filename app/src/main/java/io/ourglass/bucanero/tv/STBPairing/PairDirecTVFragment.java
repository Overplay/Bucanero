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
import android.widget.ListView;
import android.widget.TextView;

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
import io.ourglass.bucanero.services.SSDP.SSDPBroadcastReceiver;
import io.ourglass.bucanero.services.SSDP.SSDPResult;
import io.ourglass.bucanero.services.SSDP.SSDPService;
import io.ourglass.bucanero.services.STB.DirecTV.DirecTVSetTopBox;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PairDirecTVFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PairDirecTVFragment extends Fragment {

    public static final String TAG = "PairDirecTVFragment";

    static final int REQUEST_CODE = 43;

    TextView mTitle;
    //TextView mErrorMsg;
    TextView mCurrentPair;
    ListView mDirectvDevicesList;
    TextView mEmptyListMessage;
    View mScanningHolder;
    View mConfirmationHolder;
    View mListHolder;

    ArrayList<SetTopBox> mFoundBoxes = new ArrayList<>();
    SetTopBoxAdapter mSTBArrayAdapter;

    SSDPBroadcastReceiver mSsdpBR;

    public String lastIpAddressClicked;
    public SetTopBox lastSTBClicked;

    private MainThreadBus bus = ABApplication.ottobus;

    private enum PairMode { SEARCH, LIST, CONFIRM };
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

        mTitle.setTypeface(OGUi.getBoldFont());
        //mErrorMsg.setTypeface(OGUi.getRegularFont());
        mCurrentPair.setTypeface(OGUi.getRegularFont());
        mEmptyListMessage.setTypeface(OGUi.getBoldFont());

        mConfirmationHolder = getView().findViewById(R.id.confirmationHolder);
        mScanningHolder = getView().findViewById(R.id.scanningHolder);
        mListHolder = getView().findViewById(R.id.listHolder);

        mSTBArrayAdapter = new SetTopBoxAdapter(getContext(), mFoundBoxes);
        mDirectvDevicesList.setAdapter(mSTBArrayAdapter);

        mDirectvDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDirectvDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        Log.d(TAG, "User selected STB @ position "+ position);
                        SetTopBox selectedBox = mFoundBoxes.get(position);
                        lastSTBClicked = selectedBox;
                        final String ip = selectedBox.ipAddress;
                        final String name = selectedBox.modelName;
                        Log.d(TAG, "User selected STB @ IP Address "+ ip);

                    }
                });

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        bus.register(this);
        mMode = PairMode.SEARCH;
        Intent ssdpi = new Intent(getContext(), SSDPService.class);
        ssdpi.putExtra("deviceFilter", "DIRECTV");
        getActivity().startService(ssdpi);
        updateCurrentPairText();
        updateUI();
    }

    @Override
    public void onPause() {
        bus.unregister(this);
        super.onPause();
    }

    private void updateUI(){

        switch (mMode){

            case SEARCH:
                mEmptyListMessage.setVisibility(View.INVISIBLE);
                mScanningHolder.setVisibility(View.VISIBLE);
                mConfirmationHolder.setVisibility(View.INVISIBLE);
                break;

            case LIST:

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
                        uiListRefresh();
                    }
                });

                break;

            case CONFIRM:
                mEmptyListMessage.setVisibility(View.INVISIBLE);
                mScanningHolder.setVisibility(View.INVISIBLE);
                mConfirmationHolder.setVisibility(View.VISIBLE);

                break;
        }

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
