package a75f.io.renatus.util;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class DataFdObj {
    @SerializedName("register_fd")
    private DataFd dataFd;

    public DataFd getDataFd() {
        return dataFd;
    }

    public void setDataFd(DataFd dataFd) {
        this.dataFd = dataFd;
    }
}
