package a75f.io.renatus.util;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class DataFdObj {
    @SerializedName("register_fd")
    private ArrayList<DataFd> listOfDataFd;

    public DataFdObj() {
        this.listOfDataFd = new ArrayList<>();
    }

    public void addItem(DataFd item) {
        listOfDataFd.add(item);
    }
}
