package io.ourglass.bucanero.tv.STBPairing;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.core.OGUi;
import io.ourglass.bucanero.objects.SetTopBox;


/**
 * Created by mkahn on 11/14/16. BAsed on Ethan's original.
 */

public class SetTopBoxAdapter extends ArrayAdapter<SetTopBox> {

    private Typeface font;
    private Typeface indexFont;

    public interface UpdateListener {
        public void done();
    }

    public SetTopBoxAdapter(Context context, ArrayList<SetTopBox> boxes){
        super(context, 0, boxes);

        this.font = OGUi.getRegularFont();
        this.indexFont = OGUi.getBoldFont();
    }

    // TODO JEA: You should be coloring the currently selected STB in here by checking the ip addr
    // of the entry view you are building, vs the currently paired STB ip addr. If they match, invert
    // the colors of the text and backgrounds. This is much simpler than the way you were doing it,
    // and cleaner.

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        SetTopBox box = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.dtv_list_elem_layout, parent, false);
        }

        TextView friendlyName = (TextView) convertView.findViewById(R.id.dtv_list_elem_friendlyName);
        TextView curPlaying = (TextView) convertView.findViewById(R.id.dtv_list_elem_curPlaying);
        TextView ipAddr = (TextView) convertView.findViewById(R.id.dtv_list_elem_ipAddr);
        TextView idx = (TextView) convertView.findViewById(R.id.dtv_list_elem_idx_num);

        if(font != null) {
            friendlyName.setTypeface(font);
            curPlaying.setTypeface(font);
            ipAddr.setTypeface(font);
        }

        if(indexFont != null){
            idx.setTypeface(indexFont);
        }
        //SpannableString friendlyNameUnderlined = new SpannableString(box.friendlyName);
        //friendlyNameUnderlined.setSpan(new UnderlineSpan(), 0, friendlyNameUnderlined.length(), 0);

        if (box.modelName==null || box.modelName.equalsIgnoreCase("")){
            friendlyName.setText(Html.fromHtml("<b>UNRECOGNIZED MODEL</b>"));
        } else {
            friendlyName.setText(Html.fromHtml("<b>"+box.modelName+"</b>"));

        }
        curPlaying.setText(Html.fromHtml("<b>Showing:</b> " +
                (box.nowPlaying == null ? "---" : box.nowPlaying.title+" on " + box.nowPlaying.networkName)));

        String ip = box.ipAddress;
        ipAddr.setText(ip.replace("http://", "").replace("https://", "")+" (ID: "+box.receiverId+")");

        //format to contain leading 0 for better aesthetics
        idx.setText(String.format("%02d", position + 1));

        // TODO JEA: here's where you can modify the colors...
        if (ip.equalsIgnoreCase(OGSystem.getPairedSTBIpAddress())){
            // This box is the one paired, change the color
        }

        return convertView;
    }

    // This grabs all the info from the STBs in the background
    public void refreshDevices(final UpdateListener listener){

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                int boxCount = getCount();
                for (int i=0; i<boxCount; i++){
                    SetTopBox box = getItem(i);
                    box.updateAllSync();
                }

                listener.done();

            }
        });

        t.start();

    }

}
