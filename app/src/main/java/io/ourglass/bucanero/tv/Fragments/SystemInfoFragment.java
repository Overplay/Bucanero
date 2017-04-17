package io.ourglass.bucanero.tv.Fragments;

import io.ourglass.bucanero.core.OGSystem;

public class SystemInfoFragment extends SimpleHeaderTextFragment {


    public SystemInfoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static SystemInfoFragment newInstance() {
        SystemInfoFragment fragment = new SystemInfoFragment();
        return fragment;
    }

    @Override
    public void onStart(){
        mTitle = "System Information";
        mSubtitle = "More than you really need to know about this OG!";
        mBody = OGSystem.getSystemInfoString();
        dismissMeAfter(60000);
        super.onStart();
    }


}
