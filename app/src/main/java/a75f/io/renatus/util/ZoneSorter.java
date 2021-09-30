/*Added by Aniketh on 28/07/2021
This class has been used in TempOverrideFragment.java class for the purpose of sorting of Zones, based on their node Address. */

package a75f.io.renatus.util;

public class ZoneSorter{
    private String ZoneName;
    private int nodeAddress;

    public ZoneSorter(String zoneName, int nodeAddress) {
        this.ZoneName = zoneName;
        this.nodeAddress = nodeAddress;
    }

    public String getZoneName() {
        return ZoneName;
    }

    public void setZoneName(String zoneName) {
        ZoneName = zoneName;
    }

    public int getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(int nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    @Override
    public String toString() {
        return " ZoneName: " + this.ZoneName + ", nodeAddress:" + this.nodeAddress;
    }
}
