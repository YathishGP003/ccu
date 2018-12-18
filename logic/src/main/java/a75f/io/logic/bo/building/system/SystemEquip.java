package a75f.io.logic.bo.building.system;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.ControlMote;

/**
 * Created by samjithsadasivan on 11/19/18.
 */

public class SystemEquip
{
    private String equipRef;
    private String equipDis;
    private String siteRef;
    CCUHsApi hayStack;
    String tz;
    
    private static SystemEquip instance = new SystemEquip();//TODO- revisit
    private SystemEquip() {
        hayStack = CCUHsApi.getInstance();
        initSystemEquip();
    }
    
    public static SystemEquip getInstance() {
        return instance;
    }
    
    
    public void initSystemEquip() {
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            equipRef = equip.get("id").toString();
            equipDis = equip.get("dis").toString();
            siteRef = equip.get("siteRef").toString();
            HashMap siteMap = hayStack.read(Tags.SITE);
            tz = siteMap.get("tz").toString();
            return;
        }
        System.out.println("System Equip does not exist. Create Now");
        HashMap siteMap = hayStack.read(Tags.SITE);
        siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        Equip systemEquip= new Equip.Builder()
                                  .setSiteRef(siteRef)
                                  .setDisplayName(siteDis+"-SystemEquip")
                                  .setProfile(ProfileType.SYSTEM_VAV_ANALOG_RTU.name())
                                  .addMarker("equip")
                                  .addMarker("system")
                                  .setTz(siteMap.get("tz").toString())
                                  .build();
        equipRef = hayStack.addEquip(systemEquip);
        equipDis = siteDis+"-SystemEquip";
        tz = siteMap.get("tz").toString();
        addSystemPoints();
        new ControlMote(siteRef);
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    public String getEquipRef() {
        return equipRef;
    }
    
    public void addSystemPoints(){
        Point sat = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"SAT")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tr").addMarker("sat").addMarker("his").addMarker("system").addMarker("equipHis")
                                   .setUnit("\u00B0F")
                                   .setTz(tz)
                                   .build();
        hayStack.addPoint(sat);
    
        Point co2 = new Point.Builder()
                            .setDisplayName(equipDis+"-"+"CO2")
                            .setSiteRef(siteRef)
                            .setEquipRef(equipRef)
                            .addMarker("tr").addMarker("co2").addMarker("his").addMarker("system").addMarker("equipHis")
                            .setUnit("\u00B0ppm")
                            .setTz(tz)
                            .build();
        hayStack.addPoint(co2);
    
        Point sp = new Point.Builder()
                            .setDisplayName(equipDis+"-"+"SP")
                            .setSiteRef(siteRef)
                            .setEquipRef(equipRef)
                            .addMarker("tr").addMarker("sp").addMarker("his").addMarker("system").addMarker("equipHis")
                            .setUnit("\u00B0in")
                            .setTz(tz)
                            .build();
        hayStack.addPoint(sp);
    
        Point hwst = new Point.Builder()
                           .setDisplayName(equipDis+"-"+"HWST")
                           .setSiteRef(siteRef)
                           .setEquipRef(equipRef)
                           .addMarker("tr").addMarker("hwst").addMarker("his").addMarker("system").addMarker("equipHis")
                           .setUnit("\u00B0F")
                           .setTz(tz)
                           .build();
        hayStack.addPoint(hwst);
        
        addRelaySelectionPoint("relay1");
        addRelaySelectionPoint("relay2");
        addRelaySelectionPoint("relay3");
        addRelaySelectionPoint("relay4");
        addRelaySelectionPoint("relay5");
        addRelaySelectionPoint("relay6");
        addRelaySelectionPoint("relay7");
        addRelaySelectionPoint("relay8");
        
        setRelaySelection("relay1",0);
        setRelaySelection("relay2",0);
        setRelaySelection("relay3",0);
        setRelaySelection("relay4",0);
        setRelaySelection("relay5",0);
        setRelaySelection("relay6",0);
        setRelaySelection("relay7",0);
        setRelaySelection("relay8",0);
        
        addAnalogOutSelectionPoint("analog1");
        addAnalogOutSelectionPoint("analog2");
        addAnalogOutSelectionPoint("analog3");
        addAnalogOutSelectionPoint("analog4");
        
        setAnalogOutSelection("analog1", 0);
        setAnalogOutSelection("analog2", 0);
        setAnalogOutSelection("analog3", 0);
        setAnalogOutSelection("analog4", 0);
        
        setSat(0);
        setCo2(0);
        setSp(0);
        setHwst(0);
    }
    
    public void updateSystemProfile(ProfileType profile) {
        Log.d("CCU", " Update Profile type for " + SystemEquip.getInstance().getEquipRef() + " :" + profile.name());
        Equip q = new Equip.Builder().setHashMap(CCUHsApi.getInstance().readMapById(equipRef)).setProfile(profile.name()).build();
        CCUHsApi.getInstance().updateEquip(q, equipRef);
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    private void addAnalogOutSelectionPoint(String analog) {
        Point p = new Point.Builder()
                          .setDisplayName(equipDis+"-"+analog+"Out")
                          .setSiteRef(siteRef)
                          .setEquipRef(equipRef)
                          .addMarker(analog).addMarker("writable").addMarker("system").addMarker("selection").addMarker("out")
                          .setUnit("\u00B0V")
                          .setTz(tz)
                          .build();
        hayStack.addPoint(p);
    }
    
    public double getAnalogOutSelection(String analog)
    {
        return hayStack.readDefaultVal("point and system and selection and "+analog);
    }
    public void setAnalogOutSelection(String analog, double val)
    {
        hayStack.writeDefaultVal("point and system and selection and "+analog, val);
    }
    
    private void addRelaySelectionPoint(String relay){
        Point p = new Point.Builder()
                            .setDisplayName(equipDis+"-"+relay+"Selection")
                            .setSiteRef(siteRef)
                            .setEquipRef(equipRef)
                            .setUnit("\u00B0")
                            .addMarker(relay).addMarker("writable").addMarker("system").addMarker("selection")
                            .setTz(tz)
                            .build();
        hayStack.addPoint(p);
    }
    
    public double getRelaySelection(String relay)
    {
        return hayStack.readDefaultVal("point and system and selection and "+relay);
    }
    public void setRelaySelection(String relay, double val)
    {
        hayStack.writeDefaultVal("point and system and selection and "+relay, val);
    }
    
    private void addRelayStatePoint(String relay){
        Point p = new Point.Builder()
                          .setDisplayName(equipDis+"-"+relay+"Selection")
                          .setSiteRef(siteRef)
                          .setEquipRef(equipRef)
                          .setUnit("\u00B0V")
                          .addMarker(relay).addMarker("writable").addMarker("system").addMarker("state")
                          .setTz(tz)
                          .build();
        hayStack.addPoint(p);
    }
    
    public double getRelayState(String relay)
    {
        return hayStack.readDefaultVal("point and system and state and "+relay);
    }
    public void setRelayState(String relay, double val)
    {
        hayStack.writeDefaultVal("point and system and state and "+relay, val);
    }
    
    
    public double getSat()
    {
        return hayStack.readHisValByQuery("point and tr and sat and his and system");
    }
    public void setSat(double supplyAirTemp)
    {
        hayStack.writeHisValByQuery("point and tr and sat and his and system", supplyAirTemp);
    }
    
    public double getCo2()
    {
        return hayStack.readHisValByQuery("point and tr and co2 and his and system");
    }
    public void setCo2(double co2)
    {
        hayStack.writeHisValByQuery("point and tr and co2 and his and system", co2);
    }
    
    public double getSp()
    {
        return hayStack.readHisValByQuery("point and tr and sp and his and system");
    }
    public void setSp(double sp)
    {
        hayStack.writeHisValByQuery("point and tr and sp and his and system", sp);
    }
    
    public double getHwst()
    {
        return hayStack.readHisValByQuery("point and tr and hwst and his and system");
    }
    public void setHwst(double hwst)
    {
        hayStack.writeHisValByQuery("point and tr and hwst and his and system", hwst);
    }
    
    /*public double getAnalogOut(String analog)
    {
        return hayStack.readHisValByQuery("point and his and system and out and "+analog);
    }
    public void setAnalogOut(String analog, double val)
    {
        hayStack.writeHisValByQuery("point and his and system and out and "+analog, val);
    }*/
    
    public void dump() {
        System.out.println(getSat());
        System.out.println(getCo2());
        System.out.println(getSp());
        System.out.println(getHwst());
        /*System.out.println(getAnalogOut("analog1"));
        System.out.println(getAnalogOut("analog2"));
        System.out.println(getAnalogOut("analog3"));
        System.out.println(getAnalogOut("analog4"));*/
    }
    
}
