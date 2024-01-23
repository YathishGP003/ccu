package a75f.io.renatus.util;

import com.google.gson.annotations.SerializedName;

public class DataFd {
    @SerializedName("bbmd_addr")
    private String bbmdIp;
    @SerializedName("bbmd_port")
    private int bbmdPort;
    @SerializedName("bbmd_time_to_live")
    private int bbmdMask;

    public DataFd(String bbmdIp, int bbmdPort, int bbmdMask) {
        this.bbmdIp = bbmdIp;
        this.bbmdPort = bbmdPort;
        this.bbmdMask = bbmdMask;
    }

    public String getBbmdIp() {
        return bbmdIp;
    }

    public void setBbmdIp(String bbmdIp) {
        this.bbmdIp = bbmdIp;
    }

    public int getBbmdPort() {
        return bbmdPort;
    }

    public void setBbmdPort(int bbmdPort) {
        this.bbmdPort = bbmdPort;
    }

    public int getBbmdMask() {
        return bbmdMask;
    }

    public void setBbmdMask(int bbmdMask) {
        this.bbmdMask = bbmdMask;
    }
}
