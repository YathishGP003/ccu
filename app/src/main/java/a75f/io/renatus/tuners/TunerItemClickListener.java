package a75f.io.renatus.tuners;


import java.util.HashMap;

public interface TunerItemClickListener {
    void itemClicked(HashMap item);
    void itemClicked(TunerGroupItem section);
}