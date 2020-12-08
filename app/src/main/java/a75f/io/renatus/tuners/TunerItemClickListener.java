package a75f.io.renatus.tuners;


import java.util.HashMap;

public interface TunerItemClickListener {
    void itemClicked(HashMap item, int position);
    void itemClicked(TunerGroupItem section);
}