package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 10/12/17.
 */

public class DataPoint
{
    public int x;
    public int y;
    public String indexLabel;
    public DataPoint() {
        
    }
    public DataPoint(int x, int y, String i) {
        this.x = x;
        this.y = y;
        this.indexLabel =i;
    }
    public DataPoint(int x, int y) {
        this.x = x;
        this.y = y;
        this.indexLabel =null;
    }
}
