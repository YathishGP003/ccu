package a75f.io.logic;

import a75f.io.bo.SmartNode;
import a75f.io.util.prefs.EncryptionPrefs;

/**
 * Created by ryanmattison on 7/25/17.
 */

public class SmartNodeBLL {
    private static short address = 200;

    public static SmartNode generateSmartNodeFromJSON()
    {
        SmartNode sn = new SmartNode("Ryan SN");
        sn.setEncryptionKey(EncryptionPrefs.getEncryptionKey());
        sn.setMeshAddress(address);
        return sn;
    }
}
