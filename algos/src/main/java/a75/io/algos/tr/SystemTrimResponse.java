package a75.io.algos.tr;

/**
 * Created by samjithsadasivan on 5/31/18.
 */

import android.util.Log;

/**
 * SP0 - Initial setpoint
 * SPmin - Minimum setpoint
 * SPmax - Maximum setpoint
 * Td - Delay timer
 * T - Time step
 * I - Number of ignored Requests
 * R - Number of Requests from zones/ systems
 * SPtrim - Trim amount
 * SPres - Respond amount (must be opposite in sign to SPtrim)
 * SPres-max - Maximum response per time interval (must be same sign as SPres)
 */
public class SystemTrimResponse
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
    public double getSP0()
    {
        return SP0;
    }
    public void setSP0(double SP0)
    {
        this.SP0 = SP0;
    }
    public double getSPmin()
    {
        return SPmin;
    }
    public void setSPmin(double SPmin)
    {
        this.SPmin = SPmin;
    }
    public double getSPmax()
    {
        return SPmax;
    }
    public void setSPmax(double SPmax)
    {
        this.SPmax = SPmax;
    }
    public int getTd()
    {
        return Td;
    }
    public void setTd(int td)
    {
        Td = td;
    }
    public int getT()
    {
        return T;
    }
    public void setT(int t)
    {
        T = t;
    }
    public int getI()
    {
        return I;
    }
    public void setI(int i)
    {
        I = i;
    }
    public int getR()
    {
        return R;
    }
    public void setR(int r)
    {
        R = r;
    }
    public double getSPtrim()
    {
        return SPtrim;
    }
    public void setSPtrim(double SPtrim)
    {
        this.SPtrim = SPtrim;
    }
    public double getSPres()
    {
        return SPres;
    }
    public void setSPres(double SPres)
    {
        this.SPres = SPres;
    }
    public double getSPresmax()
    {
        return SPresmax;
    }
    public void setSPresmax(double SPresmax)
    {
        this.SPresmax = SPresmax;
    }
    
    public SystemTrimResponse() {
    
    }
    
    public SystemTrimResponse(double SP0, double SPmin, double SPmax, int Td, int T, int I, double SPtrim, double SPres, double SPresmax) {
        this.SP0 = SP0;
        this.SPmin = SPmin;
        this.SPmax = SPmax;
        this.Td = Td;
        this.T = T;
        this.I = I;
        this.SPtrim = SPtrim;
        this.SPres = SPres;
        this.SPresmax = SPresmax;
    }
    
    public void dump() {
        Log.d("CCU_SYSTEM"," SP0: "+SP0+" SPmin: "+SPmin+" SPmax: "+SPmax+" Td: "+Td+" T: "+T+" I: "+I+" R: "+R+" SPtrim: "+SPtrim
                            +" SPres: "+SPres+" SPresmax: "+SPresmax);
    }
    
    public void resetRequest() {
        R = 0;
    }
    
    public void resetSystem() {
    
    }
    
    public void updateRequest(TrimResponseRequest r) {
        R += (r.currentRequests * r.importanceMultiplier);
    }
    
    
}
