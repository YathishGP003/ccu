package a75f.io.renatus.util;

import com.google.gson.annotations.SerializedName;

public class DataBbmd {
    @SerializedName("ip_addr")
    private String bbmdIp;
    @SerializedName("port")
    private int bbmdPort;
    @SerializedName("broadcast_mask")
    private int bbmdMask;

    public DataBbmd(String bbmdIp, int bbmdPort, int bbmdMask) {
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
