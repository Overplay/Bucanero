package io.ourglass.bucanero.tv.WiFi;


import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.core.OGUi;
import io.ourglass.bucanero.services.ConnectivityMonitor;
import io.ourglass.bucanero.tv.Activities.MainFrameActivity;
import io.ourglass.bucanero.tv.Fragments.OverlayFragment;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WiFiPickerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WiFiPickerFragment extends OverlayFragment {

    public static final String TAG = "WiFiPickerFragment";

    TextView mHeaderTV;
    TextView mSubHeaderTV;
    TextView mCurrentSSIDTV;

    ArrayList<ScanResult> mWiFiScanResults = new ArrayList<>(); // start empty for adapter
    WiFiAdapterPro mWiFiAdapter;

    TextView mEmptyListMessage;
    TextView mPasswordPrompt;
    TextView mScanningMessage;
    EditText mPasswordEntryET;

    View mScanningHolder;
    View mPasswordHolder;
    View mListHolder;
    View mAlreadyConnectedHolder;
    View mConnectFailHolder;

    ListView mWiFiNetworkList;

    ScanResult mWorkingScanResult;

    Handler fragHandler = new Handler();

    private enum WiFiSetupMode { SEARCH, LIST, CONFIRM, CONNECTED,
        INPUT_PWD, TROUBLESHOOT, TESTING_CONNECTION, CONNECT_FAIL };
    private WiFiSetupMode mMode = WiFiSetupMode.SEARCH;

    private ConnectivityMonitor mConnMon = ConnectivityMonitor.getInstance();

    public WiFiPickerFragment() {
        // Required empty public constructor
    }

    public static WiFiPickerFragment newInstance() {
        WiFiPickerFragment fragment = new WiFiPickerFragment();
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
        return inflater.inflate(R.layout.fragment_wifi_picker, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mHeaderTV = (TextView) getView().findViewById(R.id.wifiSetupHeader);
        mSubHeaderTV = (TextView) getView().findViewById(R.id.wifiSetupSubHeader);
        mCurrentSSIDTV = (TextView) getView().findViewById(R.id.currentSSIDTV);

        mEmptyListMessage = (TextView) getView().findViewById(R.id.empty_list_message);

        mPasswordPrompt = (TextView) getView().findViewById(R.id.passwordPromptTV);

        mHeaderTV.setTypeface(OGUi.getBoldFont());
        //mErrorMsg.setTypeface(OGUi.getRegularFont());
        mSubHeaderTV.setTypeface(OGUi.getRegularFont());
        mEmptyListMessage.setTypeface(OGUi.getBoldFont());
        mPasswordPrompt.setTypeface(OGUi.getBoldFont());
        mPasswordEntryET = (EditText) getView().findViewById(R.id.editTextPassword);

        mScanningMessage = (TextView) getView().findViewById(R.id.scanningMessage) ;
        mScanningMessage.setTypeface(OGUi.getRegularFont());

        mPasswordHolder = getView().findViewById(R.id.passwordHolder);
        mScanningHolder = getView().findViewById(R.id.scanningHolder);
        mListHolder = getView().findViewById(R.id.listHolder);
        mAlreadyConnectedHolder = getView().findViewById(R.id.alreadyConnectedHolder);

        mConnectFailHolder = getView().findViewById(R.id.connectFailHolder);

        ((Button)getView().findViewById(R.id.buttonChangeWiFi)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Changing WiFi Setup requested");
                goToMode(WiFiSetupMode.SEARCH);
            }
        });

        mWiFiNetworkList = (ListView) getView().findViewById(R.id.wifiNetworkList);
        mWiFiAdapter = new WiFiAdapterPro(getContext(), mWiFiScanResults, mConnMon.wifiManager);

        mWiFiNetworkList.setAdapter(mWiFiAdapter);
        mWiFiNetworkList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Position "+position+" picked");
                ScanResult picked = mWiFiScanResults.get(position);

                boolean alreadyConfigured = mConnMon.isConfiguredNetwork(picked.BSSID);
                boolean openNetwork = ConnectivityMonitor.isOpenNetwork(picked);

                if ( alreadyConfigured || openNetwork ){
                    Log.d(TAG, "Chosen network is already configured: "+picked.SSID);
                    mConnMon.connectTo(picked.SSID);
                    goToMode(WiFiSetupMode.TESTING_CONNECTION);
                } else {
                    mWorkingScanResult = picked;
                    goToMode(WiFiSetupMode.INPUT_PWD);
                }

            }
        });

        ((Button)getView().findViewById(R.id.buttonPasswordOK)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String pwd = mPasswordEntryET.getText().toString();
                mConnMon.configureNetwork(mWorkingScanResult.SSID, pwd, false);
                mConnMon.connectTo(mWorkingScanResult.SSID);
                goToMode(WiFiSetupMode.TESTING_CONNECTION);
            }
        });


        View.OnClickListener searchListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToMode(WiFiSetupMode.SEARCH);
            }
        };

        ((Button)getView().findViewById(R.id.buttonPasswordCancel)).setOnClickListener(searchListener);

        View.OnClickListener disListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissMe();
            }
        };

        ((Button)getView().findViewById(R.id.buttonCancelWiFi)).setOnClickListener(disListener);
        ((Button)getView().findViewById(R.id.buttonQuitSetup)).setOnClickListener(disListener);
        ((Button)getView().findViewById(R.id.buttonScanAgain)).setOnClickListener(searchListener);


        // TODO this should actually be self-resetting based on activity in the fragment
        //dismissMeAfter(5*60*1000);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMode = mConnMon.isNetConnected() ?  WiFiSetupMode.CONNECTED : WiFiSetupMode.SEARCH;
        ABApplication.ottobus.register(this);
        if (!mConnMon.isNetConnected()){
            mConnMon.startWiFiScan();
        }
        updateUI();
    }

    @Override
    public void onPause() {
        ABApplication.ottobus.unregister(this);
        mConnMon.stopWiFiScan();
        super.onPause();
    }

    private void goToMode(WiFiSetupMode mode){
        mMode = mode;

        switch (mMode){
            case SEARCH:
                mConnMon.startWiFiScan();
                break;
        }
        updateUI();
    }

    private void updateUI(){

        mCurrentSSIDTV.setText(mConnMon.getCurrentWiFiSSID());

        switch (mMode){

            case CONNECTED:
                mAlreadyConnectedHolder.setVisibility(View.VISIBLE);
                mScanningHolder.setVisibility(View.INVISIBLE);
                mPasswordHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.INVISIBLE);
                mConnectFailHolder.setVisibility(View.INVISIBLE);
                ((Button)getView().findViewById(R.id.buttonChangeWiFi)).requestFocus();
                fragHandler.removeCallbacksAndMessages(null); // clear fail timer

                if (OGSystem.isFirstTimeSetup()){
                    ((Button)getView().findViewById(R.id.buttonCancelWiFi)).setText("NEXT STEP");
                    ((Button)getView().findViewById(R.id.buttonCancelWiFi)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((MainFrameActivity)getActivity()).launchVenuePairFragment();
                        }
                    });
                }

                break;

            case SEARCH:
                mAlreadyConnectedHolder.setVisibility(View.INVISIBLE);
                mScanningHolder.setVisibility(View.VISIBLE);
                mPasswordHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.INVISIBLE);
                mConnectFailHolder.setVisibility(View.INVISIBLE);

                mScanningMessage.setText("Scanning WiFi");
                break;

            case LIST:

                mAlreadyConnectedHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.VISIBLE);
                mConnectFailHolder.setVisibility(View.INVISIBLE);



                if (mWiFiAdapter.getCount()>0){
                    mEmptyListMessage.setText("");
                    mEmptyListMessage.setVisibility(View.GONE);
                } else {
                    mEmptyListMessage.setText("No WiFi networks were found.");
                    mEmptyListMessage.setVisibility(View.VISIBLE);
                }

                mScanningHolder.setVisibility(View.INVISIBLE);  // was GONE, does it matter?
                mPasswordHolder.setVisibility(View.INVISIBLE);

                break;

            case TESTING_CONNECTION:

                mAlreadyConnectedHolder.setVisibility(View.INVISIBLE);
                mScanningHolder.setVisibility(View.VISIBLE);
                mPasswordHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.INVISIBLE);
                mConnectFailHolder.setVisibility(View.INVISIBLE);
                mScanningMessage.setText("Testing Connection");

                fragHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        goToMode(WiFiSetupMode.CONNECT_FAIL);
                    }
                }, 10000);

                break;

            case INPUT_PWD:
                mAlreadyConnectedHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.INVISIBLE);
                mScanningHolder.setVisibility(View.INVISIBLE);
                mPasswordHolder.setVisibility(View.VISIBLE);
                mConnectFailHolder.setVisibility(View.INVISIBLE);
                break;

            case CONNECT_FAIL:
                mAlreadyConnectedHolder.setVisibility(View.INVISIBLE);
                mListHolder.setVisibility(View.INVISIBLE);
                mScanningHolder.setVisibility(View.INVISIBLE);
                mPasswordHolder.setVisibility(View.INVISIBLE);
                mConnectFailHolder.setVisibility(View.VISIBLE);

        }


    }

    public void setErrorMsg(String message) {
        mEmptyListMessage.setVisibility(View.VISIBLE);
        mEmptyListMessage.setText(message);
    }

    public void searchAgain(View v){
        goToMode(WiFiSetupMode.SEARCH);
    }

    public void dismiss(View v){
        dismissMe();
    }


    @Subscribe
    public void someStringChucked(NetworkChangeMessage result) {

        Log.d(TAG, "Got new string: "+result.message);
        mConnMon.updateWiFiState();
        WifiInfo info = mConnMon.currentWiFiInfo;
        SupplicantState ss = info.getSupplicantState();

        switch (ss) {
            case DISCONNECTED:
            case ASSOCIATING:
                mScanningMessage.setText("Trying to Connect");
                break;

            case COMPLETED:
            case ASSOCIATED:
                mScanningMessage.setText("Connected!");
                goToMode(WiFiSetupMode.CONNECTED);
                break;

        }

    }

    @Subscribe
    public void newScanResults(ArrayList<ScanResult> results) {
        mWiFiScanResults.clear();
        mWiFiScanResults.addAll(results);
        ((ArrayAdapter)mWiFiNetworkList.getAdapter()).notifyDataSetChanged();
        if (mMode==WiFiSetupMode.SEARCH){
            goToMode(WiFiSetupMode.LIST);
        }
    }

}
