package a75f.io.renatus;

public class HyperStatSenseModel {

    String tempOffset;
    boolean th1toggle;
    boolean th2toggle;
    boolean anlg1toggle;
    boolean anlg2toggle;
    int th1Sp;
    int th2Sp;
    int anlg1Sp;
    int anlg2Sp;

    public HyperStatSenseModel() {
        tempOffset = "0";
        th1toggle = false;
        th2toggle = false;
        anlg1toggle = false;
        anlg2toggle = false;
        th1Sp = -1;
        th2Sp = -1;
        anlg1Sp = -1;
        anlg2Sp = -1;
    }

    public boolean getth1toggle() {
        return th1toggle;
    }

    public void setth1toggle(boolean val) {
        th1toggle = val;
    }

    public boolean getth2toggle() {
        return th2toggle;
    }

    public void setth2toggle(boolean val) {
        th2toggle = val;
    }

    public boolean getanlg1toggle() {
        return anlg1toggle;
    }

    public void setanlg1toggle(boolean val) {
        anlg1toggle = val;
    }

    public int getth1SpVal() {
        return th1Sp;
    }

    public void setth1SpVal(int val) {
        th1Sp = val;
    }

    public int getth2SpVal() {
        return th2Sp;
    }

    public void setth2SpVal(int val) {
        th1Sp = val;
    }

    public int getanlg1SpVal() {
        return anlg1Sp;
    }

    public void setanlg1SpVal(int val) {
        th1Sp = val;
    }

    public int getanlg2SpVal() {
        return anlg2Sp;
    }

    public void setanlg2SpVal(int val) {
        th1Sp = val;
    }

    public void setDefault() {


    }


}
