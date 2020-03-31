package a75f.io.logic.bo.building.system;

/**
 * Created by samjithsadasivan on 1/8/19.
 */

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.ControlMote;

/**
 * Default System Profile does nothing.
 *
 */
public class DefaultSystem extends SystemProfile
{
    @Override
    public String getProfileName() {
        return "Default";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_DEFAULT;
    }
    
    public DefaultSystem() {
      addSystemEquip();
    }
    @Override
    public void doSystemControl() {
        updateOutsideWeatherParams();
        //update default points for apps and portals to consume
        String systemStatus = getStatusMessage();
        String scheduleStatus =  "No Central equipment is connected.";
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and status and message").equals(systemStatus))
        {
            CCUHsApi.getInstance().writeDefaultVal("system and status and message", systemStatus);
        }
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and scheduleStatus").equals(scheduleStatus))
        {
            CCUHsApi.getInstance().writeDefaultVal("system and scheduleStatus", scheduleStatus);
        }
    }
    
    @Override
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_DEFAULT.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
            } else {
                return;
            }
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"Add Default System Equip");
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        Equip systemEquip= new Equip.Builder()
                                   .setSiteRef(siteRef)
                                   .setDisplayName(siteDis+"-SystemEquip")
                                   .setProfile(ProfileType.SYSTEM_DEFAULT.name())
                                   .addMarker("equip").addMarker("system").addMarker("default")
                                   .addMarker("equipHis")
                                   .setTz(siteMap.get("tz").toString())
                                   .build();
        String equipRef = hayStack.addEquip(systemEquip);
        addSystemTuners();
        addDefaultSystemPoints(siteRef, equipRef, siteDis+"-SystemEquip", siteMap.get("tz").toString());
        updateGatewayRef(equipRef);

        new ControlMote(equipRef);
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return false;
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return false;
    }
    
    @Override
    public boolean isCoolingActive(){
        return false;
    }
    
    @Override
    public boolean isHeatingActive(){
        return false;
    }
    
    @Override
    public void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_DEFAULT.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
    }
    
    @Override
    public String getStatusMessage()
    {
        return "System is in gateway mode.";
    }
}
