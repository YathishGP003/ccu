package a75f.io.renatus.tuners;


import java.io.Serializable;

public class TunerGroupItem implements Serializable {

    private final String tunerGroupName;

    public boolean isExpanded;

    public TunerGroupItem(String name) {
        this.tunerGroupName = name;
        isExpanded = false;
    }

    public String getName() {
        return tunerGroupName;
    }
}
