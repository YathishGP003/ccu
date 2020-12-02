package a75f.io.renatus.tuners;


public class TunerGroupItem {

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
