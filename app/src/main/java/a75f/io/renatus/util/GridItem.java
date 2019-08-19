package a75f.io.renatus.util;

import java.util.ArrayList;
import java.util.HashMap;

public class GridItem {

    String GridItem;
    int GridID;
    short nodeAddress;
    ArrayList<HashMap> zoneEquips;
    ArrayList<Short> zoneNodes;

    public String getGridItem() {
        return GridItem;
    }

    public void setGridItem(String gridItem) {
        GridItem = gridItem;
    }

    public int getGridID() {
        return GridID;
    }

    public void setGridID(int gridID) {
        GridID = gridID;
    }

    public short getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(short nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public ArrayList<HashMap> getZoneEquips() {
        return zoneEquips;
    }
    public ArrayList<Short> getZoneNodes() { return zoneNodes; }

    public void setZoneEquips(ArrayList<HashMap> zoneEquips) {
        this.zoneEquips = zoneEquips;
    }
    public void setZoneNodes(ArrayList<Short> zoneNodes) { this.zoneNodes = zoneNodes;}


}
