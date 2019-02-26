package a75f.io.logic.bo.building.system;

/**
 * Created by samjithsadasivan on 1/8/19.
 */

import java.util.ArrayList;
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
    public String getProfileName() {
        return "Default";
    }
    
    public DefaultSystem() {
      addSystemEquip();
    }
    @Override
    public void doSystemControl() {
    
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
        updateAhuRef(equipRef);
        new ControlMote(siteRef);
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
    
    public void updateAhuRef(String systemEquipId) {
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
        
        for (HashMap m : equips)
        {
            Equip q = new Equip.Builder().setHashMap(m).setAhuRef(systemEquipId).build();
            CCUHsApi.getInstance().updateEquip(q, q.getId());
        }
    }
    
    @Override
    public void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_DEFAULT.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
    }
}
