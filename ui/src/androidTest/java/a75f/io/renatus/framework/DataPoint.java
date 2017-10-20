package a75f.io.renatus.framework;

/**
 * Created by samjithsadasivan on 10/12/17.
 */

public class DataPoint
{
    public int x;
    public float y;
    public String indexLabel;
    public DataPoint() {
        
    }
    public DataPoint(int x, float y, String i) {
        this.x = x;
        this.y = y;
        this.indexLabel =i;
    }
    public DataPoint(int x, float y) {
        this.x = x;
        this.y = y;
        this.indexLabel =null;
    }
}
