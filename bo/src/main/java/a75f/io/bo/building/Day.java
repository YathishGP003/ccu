package a75f.io.bo.building;

/**
 * Created by Yinten on 10/1/2017.
 */

public class Day
{
    boolean sunrise;
    boolean sunset;
    private int   day;
    private int   sthh;
    private int   stmm;
    private int   ethh;
    private int   etmm;
    private short val;
    
    
    public int getDay()
    {
        return day;
    }
    
    
    public void setDay(int day)
    {
        this.day = day;
    }
    
    
    public int getSthh()
    {
        return sthh;
    }
    
    
    public void setSthh(int sthh)
    {
        this.sthh = sthh;
    }
    
    
    public int getStmm()
    {
        return stmm;
    }
    
    
    public void setStmm(int stmm)
    {
        this.stmm = stmm;
    }
    
    
    public int getEthh()
    {
        return ethh;
    }
    
    
    public void setEthh(int ethh)
    {
        this.ethh = ethh;
    }
    
    
    public int getEtmm()
    {
        return etmm;
    }
    
    
    public void setEtmm(int etmm)
    {
        this.etmm = etmm;
    }
    
    
    @Override
    public String toString()
    {
        return "Day{" + "sunrise=" + sunrise + ", sunset=" + sunset + ", day=" + day + ", sthh=" +
               sthh + ", stmm=" + stmm + ", ethh=" + ethh + ", etmm=" + etmm + ", val=" + val + '}';
    }
    
    
    public short getVal()
    {
        return val;
    }
    
    
    public void setVal(short val)
    {
        this.val = val;
    }
}
