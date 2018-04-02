package a75f.io.kinveybo;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

/**
 * Created by Yinten on 3/26/2018.
 */

public class AccessLog extends GenericJson {

    public AccessLog()
    {}

    @Key("accessTime")
    private long accessTime;

    @Key("macAddress")
    private String macAddress;

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
