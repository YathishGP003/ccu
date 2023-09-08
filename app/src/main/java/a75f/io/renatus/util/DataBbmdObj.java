package a75f.io.renatus.util;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class DataBbmdObj {
    @SerializedName("write_bdt")
    private ArrayList<DataBbmd> listOfDataBbmd;

    public DataBbmdObj() {
        this.listOfDataBbmd = new ArrayList<>();
    }

    public void addItem(DataBbmd item) {
        listOfDataBbmd.add(item);
    }
}
