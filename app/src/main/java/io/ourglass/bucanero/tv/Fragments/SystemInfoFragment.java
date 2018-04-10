package io.ourglass.bucanero.tv.Fragments;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

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
        mSubtitle = "";
        mBody = OGSystem.getSystemInfoString();
        dismissMeAfter(60000);
        super.onStart();

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentType("Menu")
                .putContentName("System Info Menu"));
    }


}
