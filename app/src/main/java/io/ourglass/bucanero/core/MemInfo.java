package io.ourglass.bucanero.core;

/**
 * Created by mkahn on 10/28/17.
 */

public class MemInfo {

    public long totalMegs;
    public long availableMegs;
    public boolean lowMemory = false;

    public double getAvailablePct(){

        return (double)availableMegs / (double)totalMegs;

    }

    public String getAvailablePctString(){

        return String.format("%.3f", this.getAvailablePct() * 100.0);


    }

}
