package io.ourglass.bucanero.messages;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * USED TO MOVE A WebView regardless of app to an explicit slot
 * normally used for channel change moves
 */

public class BestPositionMessage extends OttobusMainThreadMessage {

    public JSONObject exclusionJson = new JSONObject();
    public boolean[] availableWidgetSlots = { true, true, true, true };
    public boolean[] availableCrawlerSlots = { true, true };

    public BestPositionMessage(JSONObject bestPosition) {
        this.exclusionJson = bestPosition;

        try {
            JSONObject cExclusions = bestPosition.getJSONObject("crawlerLocation");
            availableCrawlerSlots[0] = !cExclusions.optBoolean("top", false);
            availableCrawlerSlots[1] = !cExclusions.optBoolean("bottom", false);

            JSONObject wExclusions = bestPosition.getJSONObject("widgetLocation");
            availableWidgetSlots[0] = !wExclusions.optBoolean("leftUpper", false);
            availableWidgetSlots[1] = !wExclusions.optBoolean("rightUpper", false);
            availableWidgetSlots[2] = !wExclusions.optBoolean("rightLower", false);
            availableWidgetSlots[3] = !wExclusions.optBoolean("rightUpper", false);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.wtf("BPMessage", "Fucked up JSON in best position!");
        }
    }

    /**
     * Pass in current slot so we don't move if the current slot is OK
     * @param currentSlot
     * @return
     */
    public int getPreferredWidgetSlot(int currentSlot){

        // If the current slot is OK, just return it
        if (availableWidgetSlots[currentSlot]){
            return currentSlot;
        }

        int slot = 0;

        while (availableWidgetSlots[slot]!=true){
            slot++;
        }

        return (slot<4) ? slot : currentSlot; // if all slots are bad, just punt
    }

    /**
     * Pass in current slot so we don't move if the current slot is OK
     * @param currentSlot
     * @return
     */
    public int getPreferredCrawlerSlot(int currentSlot){

        if (availableCrawlerSlots[currentSlot]){
            return currentSlot;
        }

        int otherSlot = (currentSlot == 0 ) ? 1 : 0;

        // if the other slot is cool, return that, otherwise keep current
        return availableCrawlerSlots[otherSlot] ? otherSlot : currentSlot;

    }

}
