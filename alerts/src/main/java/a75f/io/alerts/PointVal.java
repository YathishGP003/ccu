package a75f.io.alerts;

/** Point value.  A point haystack ID plus its value. */
public class PointVal
{
    PointVal(String id, double val) {
        this.id = id;
        this.val = val;
    }
    String id;
    double val;
}
