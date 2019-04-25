package a75f.io.api.haystack;

public class PointArrayVal
{
    
    /*public HRef   id;
    public int    level;
    public String who;
    public HVal   val;
    public HNum   dur;*/
    
    public String   id;
    public int    level;
    public String who;
    public double   val;
    public long   dur;
    
    public PointArrayVal(String id, int level, String who, double val, long dur) {
        this.id = id;
        this.level = level;
        this.who = who;
        this.val = val;
        this.dur = dur;
    }
}
