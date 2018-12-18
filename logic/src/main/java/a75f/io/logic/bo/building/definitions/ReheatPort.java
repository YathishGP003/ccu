package a75f.io.logic.bo.building.definitions;

/**
 * Created by samjithsadasivan on 12/14/18.
 */

public enum ReheatPort
{
    ANALOG2("Analog 2 Out"), RELAY1("Relay 1"), RELAY1_2("Relay 1 and 2");
    
    public String displayName;
    
    ReheatPort(String str) {
        displayName = str;
    }
}
