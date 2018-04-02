package a75f.io.kinveybo;

import com.google.api.client.util.Key;

import java.util.ArrayList;

/**
 * Created by Yinten on 3/26/2018.
 */

public class InternalSchedule {


    //[0, 1, 2, 3, 4]
    @Key
    public ArrayList<Integer> days;

    @Key
    boolean sunrise;

    @Key
    boolean sunset;

    @Key
    private int   day;

    @Key
    private int   sthh;

    @Key
    private int   stmm;

    @Key
    private int   ethh;

    @Key
    private int   etmm;

    @Key
    private short val;


    public ArrayList<Integer> getDays() {
        return days;
    }

    public void setDays(ArrayList<Integer> days) {
        this.days = days;
    }

    public boolean isSunrise() {
        return sunrise;
    }

    public void setSunrise(boolean sunrise) {
        this.sunrise = sunrise;
    }

    public boolean isSunset() {
        return sunset;
    }

    public void setSunset(boolean sunset) {
        this.sunset = sunset;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getSthh() {
        return sthh;
    }

    public void setSthh(int sthh) {
        this.sthh = sthh;
    }

    public int getStmm() {
        return stmm;
    }

    public void setStmm(int stmm) {
        this.stmm = stmm;
    }

    public int getEthh() {
        return ethh;
    }

    public void setEthh(int ethh) {
        this.ethh = ethh;
    }

    public int getEtmm() {
        return etmm;
    }

    public void setEtmm(int etmm) {
        this.etmm = etmm;
    }

    public short getVal() {
        return val;
    }

    public void setVal(short val) {
        this.val = val;
    }
}
