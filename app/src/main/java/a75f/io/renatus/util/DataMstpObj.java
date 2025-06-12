package a75f.io.renatus.util;

import com.google.gson.annotations.SerializedName;

public class DataMstpObj {
    @SerializedName("mstp_data")
    private DataMstp dataMstp;

    public DataMstp getDataMstp() {
        return dataMstp;
    }

    public void setDataMstp(DataMstp dataMstp) {
        this.dataMstp = dataMstp;
    }
}
