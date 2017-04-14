package io.ourglass.bucanero.tv.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.OGUi;


public class SimpleHeaderTextFragment extends Fragment {

    private static final String ARG_TITLE = "TITLE";
    private static final String ARG_SUBTITLE = "SUBTITLE";
    private static final String ARG_BODY = "BODY";

    protected String mTitle;
    protected String mBody;
    protected String mSubtitle;
    private float mDefaultSubHeaderSize;

    public SimpleHeaderTextFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static SimpleHeaderTextFragment newInstance(String title, String subTitle, String body) {
        SimpleHeaderTextFragment fragment = new SimpleHeaderTextFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUBTITLE, subTitle);
        args.putString(ARG_BODY, body);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_TITLE);
            mSubtitle = getArguments().getString(ARG_SUBTITLE);
            mBody = getArguments().getString(ARG_BODY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simple_hdr_txt, container, false);
    }

    protected void updateTVs(){
        if ( mSubtitle.equalsIgnoreCase("")){
            ((TextView)getView().findViewById(R.id.subHeaderTV)).setTextSize(0f);
        } else {
            ((TextView)getView().findViewById(R.id.subHeaderTV)).setVisibility(View.VISIBLE);
            ((TextView)getView().findViewById(R.id.subHeaderTV)).setText(mSubtitle);
            ((TextView)getView().findViewById(R.id.subHeaderTV)).setTextSize(mDefaultSubHeaderSize);
        }
        ((TextView)getView().findViewById(R.id.headerTV)).setText(mTitle);
        ((TextView)getView().findViewById(R.id.infoTV)).setText(mBody);
    }

    public void setTitle(String title){
        mTitle = title;
        updateTVs();
    }
    public void setSubTitle(String subtitle){
        mSubtitle = subtitle;
        updateTVs();
    }

    public void setBody(String body){
        mBody = body;
        updateTVs();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onStart(){
        super.onStart();
        mDefaultSubHeaderSize = ((TextView)getView().findViewById(R.id.subHeaderTV)).getTextSize();
        ((TextView)getView().findViewById(R.id.headerTV)).setTypeface(OGUi.getBoldFont());
        ((TextView)getView().findViewById(R.id.subHeaderTV)).setTypeface(OGUi.getRegularFont());
        ((TextView)getView().findViewById(R.id.infoTV)).setTypeface(OGUi.getRegularFont());
        updateTVs();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


}
