package a75f.io.renatus.util;

import com.google.gson.annotations.SerializedName;

public class DataMstp {
    @SerializedName("mstp_baud_rate")
    private int mstpBaudRate;
    @SerializedName("mstp_source_address")
    private int mstpSourceAddress;
    @SerializedName("mstp_max_master")
    private int mstpMaxMaster;
    @SerializedName("mstp_max_frames")
    private int mstpMaxFrames;
    @SerializedName("mstp_device_id")
    private int mstpDeviceId;
    @SerializedName("mstp_port_address")
    private String mstpPortAddress;

    public DataMstp(int mstpBaudRate, int mstpSourceAddress, int mstpMaxMaster, int mstpMaxFrames, int mstpDeviceId, String mstpPortAddress) {
        this.mstpBaudRate = mstpBaudRate;
        this.mstpSourceAddress = mstpSourceAddress;
        this.mstpMaxMaster = mstpMaxMaster;
        this.mstpMaxFrames = mstpMaxFrames;
        this.mstpDeviceId = mstpDeviceId;
        this.mstpPortAddress = mstpPortAddress;
    }

    public int getMstpBaudRate() {
        return mstpBaudRate;
    }
    public void setMstpBaudRate(int mstpBaudRate) {
        this.mstpBaudRate = mstpBaudRate;
    }
    public int getMstpSourceAddress() {
        return mstpSourceAddress;
    }
    public void setMstpSourceAddress(int mstpSourceAddress) {
        this.mstpSourceAddress = mstpSourceAddress;
    }
    public int getMstpMaxMaster() {
        return mstpMaxMaster;
    }
    public void setMstpMaxMaster(int mstpMaxMaster) {
        this.mstpMaxMaster = mstpMaxMaster;
    }
    public int getMstpMaxFrames() {
        return mstpMaxFrames;
    }
    public void setMstpMaxFrames(int mstpMaxFrames) {
        this.mstpMaxFrames = mstpMaxFrames;
    }

    public int getMstpDeviceId() {
        return mstpDeviceId;
    }

    public void setMstpDeviceId(int mstpDeviceId) {
        this.mstpDeviceId = mstpDeviceId;
    }

    public String getMstpPortAddress() {
        return mstpPortAddress;
    }

    public void setMstpPortAddress(String mstpPortAddress) {
        this.mstpPortAddress = mstpPortAddress;
    }
}
