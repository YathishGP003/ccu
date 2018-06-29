package a75.io.algos;

/**
 * Created by samjithsadasivan on 5/31/18.
 */

public class SystemTrimResponseBuilder
{
    private double SP0;
    private double SPmin;
    private double SPmax;
    private int Td;
    private int T;
    private int I;
    private int R;
    private double SPtrim;
    private double SPres;
    private double SPresmax;
    
    public SystemTrimResponseBuilder setSP0(double SP0)
    {
        this.SP0 = SP0;
        return this;
    }
    public SystemTrimResponseBuilder setSPmin(double SPmin)
    {
        this.SPmin = SPmin;
        return this;
    }
    public SystemTrimResponseBuilder setSPmax(double SPmax)
    {
        this.SPmax = SPmax;
        return this;
    }
    public SystemTrimResponseBuilder setTd(int td)
    {
        this.Td = td;
        return this;
    }
    public SystemTrimResponseBuilder setT(int t)
    {
        this.T = t;
        return this;
    }
    public SystemTrimResponseBuilder setI(int i)
    {
        this.I = i;
        return this;
    }
    public SystemTrimResponseBuilder setR(int r)
    {
        this.R = r;
        return this;
    }
    public SystemTrimResponseBuilder setSPtrim(double SPtrim)
    {
        this.SPtrim = SPtrim;
        return this;
    }
    public SystemTrimResponseBuilder setSPres(double SPres)
    {
        this.SPres = SPres;
        return this;
    }
    public SystemTrimResponseBuilder setSPresmax(double SPresmax)
    {
        this.SPresmax = SPresmax;
        return this;
    }
    
    public SystemTrimResponse buildTRSystem(){
        return new SystemTrimResponse(SP0, SPmin, SPmax, Td, T, I, SPtrim, SPres, SPresmax);
    }
}
