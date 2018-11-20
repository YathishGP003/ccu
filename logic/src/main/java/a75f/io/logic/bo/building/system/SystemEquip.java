package a75f.io.logic.bo.building.system;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;

/**
 * Created by samjithsadasivan on 11/19/18.
 */

public class SystemEquip
{
    private String equipRef;
    private String equipDis;
    private String siteRef;
    CCUHsApi hayStack;
    
    private static SystemEquip instance = new SystemEquip();//TODO- revisit
    private SystemEquip() {
        hayStack = CCUHsApi.getInstance();
        initSystemEquip();
    }
    
    public static SystemEquip getInstance() {
        return instance;
    }
    
    
    public void initSystemEquip() {
        HashMap tuner = hayStack.read("equip and system");
        if (tuner != null && tuner.size() > 0) {
            return;
        }
        HashMap siteMap = hayStack.read(Tags.SITE);
        siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        Equip tunerEquip= new Equip.Builder()
                                  .setSiteRef(siteRef)
                                  .setDisplayName(siteDis+"-SystemEquip")
                                  .addMarker("equip")
                                  .addMarker("system")
                                  .build();
        equipRef = hayStack.addEquip(tunerEquip);
        equipDis = siteDis+"-SystemEquip";
        
        addSystemPoints();
    }
    
    public void addSystemPoints(){
        String tz = "Chicago";
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
    
        Point sat = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"SAT")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tr").addMarker("sat").addMarker("his").addMarker("system")
                                   .setUnit("\u00B0F")
                                   .setTz(tz)
                                   .build();
        hayStack.addPoint(sat);
    
        Point co2 = new Point.Builder()
                            .setDisplayName(equipDis+"-"+"CO2")
                            .setSiteRef(siteRef)
                            .setEquipRef(equipRef)
                            .addMarker("tr").addMarker("co2").addMarker("his").addMarker("system")
                            .setUnit("\u00B0ppm")
                            .setTz(tz)
                            .build();
        hayStack.addPoint(co2);
    
        Point sp = new Point.Builder()
                            .setDisplayName(equipDis+"-"+"SP")
                            .setSiteRef(siteRef)
                            .setEquipRef(equipRef)
                            .addMarker("tr").addMarker("sp").addMarker("his").addMarker("system")
                            .setUnit("\u00B0in")
                            .setTz(tz)
                            .build();
        hayStack.addPoint(sp);
    
        Point hwst = new Point.Builder()
                           .setDisplayName(equipDis+"-"+"HWST")
                           .setSiteRef(siteRef)
                           .setEquipRef(equipRef)
                           .addMarker("tr").addMarker("hwst").addMarker("his").addMarker("system")
                           .setUnit("\u00B0F")
                           .setTz(tz)
                           .build();
        hayStack.addPoint(hwst);
    
        Point analog1Out = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog1Out")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("analog1").addMarker("his").addMarker("system").addMarker("out")
                                   .setUnit("\u00B0V")
                                   .setTz(tz)
                                   .build();
        hayStack.addPoint(analog1Out);
    
        Point analog2Out = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog2Out")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("analog2").addMarker("his").addMarker("system").addMarker("out")
                                   .setUnit("\u00B0V")
                                   .setTz(tz)
                                   .build();
        hayStack.addPoint(analog2Out);
    
        Point analog3Out = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog3Out")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("analog3").addMarker("his").addMarker("system").addMarker("out")
                                   .setUnit("\u00B0V")
                                   .setTz(tz)
                                   .build();
        hayStack.addPoint(analog3Out);
    
        Point analog4Out = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog4Out")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("analog4").addMarker("his").addMarker("system").addMarker("out")
                                   .setUnit("\u00B0V")
                                   .setTz(tz)
                                   .build();
        hayStack.addPoint(analog4Out);
        putSat(0);
        putCo2(0);
        putSp(0);
        putHwst(0);
        putAnalogOut("analog1",0);
        putAnalogOut("analog2",0);
        putAnalogOut("analog3",0);
        putAnalogOut("analog4",0);
    }
    
    public double getSat()
    {
        return hayStack.readHisValByQuery("point and tr and sat and his and system");
    }
    public void putSat(double supplyAirTemp)
    {
        hayStack.writeHisValByQuery("point and tr and sat and his and system", supplyAirTemp);
    }
    
    public double getCo2()
    {
        return hayStack.readHisValByQuery("point and tr and co2 and his and system");
    }
    public void putCo2(double co2)
    {
        hayStack.writeHisValByQuery("point and tr and co2 and his and system", co2);
    }
    
    public double getSp()
    {
        return hayStack.readHisValByQuery("point and tr and sp and his and system");
    }
    public void putSp(double sp)
    {
        hayStack.writeHisValByQuery("point and tr and sp and his and system", sp);
    }
    
    public double getHwst()
    {
        return hayStack.readHisValByQuery("point and tr and hwst and his and system");
    }
    public void putHwst(double hwst)
    {
        hayStack.writeHisValByQuery("point and tr and hwst and his and system", hwst);
    }
    
    public double getAnalogOut(String analog)
    {
        return hayStack.readHisValByQuery("point and his and system and out and "+analog);
    }
    public void putAnalogOut(String analog, double val)
    {
        hayStack.writeHisValByQuery("point and his and system and out and "+analog, val);
    }
    
    public void dump() {
        System.out.println(getSat());
        System.out.println(getCo2());
        System.out.println(getSp());
        System.out.println(getHwst());
        System.out.println(getAnalogOut("analog1"));
        System.out.println(getAnalogOut("analog2"));
        System.out.println(getAnalogOut("analog3"));
        System.out.println(getAnalogOut("analog4"));
    }
    
}
