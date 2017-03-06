package io.ourglass.bucanero.tv.Support;

import android.graphics.Point;

import java.io.Serializable;

/**
 * Created by mkahn on 2/13/17.
 */

public class Frame implements Serializable {
    public Point location;
    public Size size;

    public Frame(Point location, Size size){
        this.location = location;
        this.size = size;
    }

    public String toString(){
        return "("+this.location.x+","+this.location.y+"),( w:"+this.size.width+", h:"+this.size.width+")";
    }
}
