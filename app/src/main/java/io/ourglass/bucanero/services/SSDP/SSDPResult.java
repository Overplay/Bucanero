package io.ourglass.bucanero.services.SSDP;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by mkahn on 4/10/17.
 */

public class SSDPResult {

    public HashMap<String, String> devices;
    public HashSet<String> addresses;
    public Boolean filtered = true; // this isn't used right now
    public Boolean errorThrown = false;
    public String errorMessage;


}
