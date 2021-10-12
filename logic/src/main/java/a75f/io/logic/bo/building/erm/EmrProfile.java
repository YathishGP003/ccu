package a75f.io.logic.bo.building.erm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HisItem;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;

public class EmrProfile extends ZoneProfile
{
    EmrEquip emrEquip;
    
    public void addEmrEquip(short addr, String floorRef, String roomRef) {
        emrEquip = new EmrEquip(getProfileType(), addr);
        emrEquip.createEntities(floorRef, roomRef);
        emrEquip.init();
    }
    
    public void addEmrEquip(short addr) {
        emrEquip = new EmrEquip(getProfileType(), addr);
        emrEquip.init();
    }
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.EMR;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return null;
    }
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)emrEquip.nodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + emrEquip.nodeAddr + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    @Override
    public void updateZonePoints() {
        HashMap emrPoint = CCUHsApi.getInstance().read("sensor and emr and sp and equipRef == \""+emrEquip.equipRef+"\"");
        List<HisItem> hisItems = CCUHsApi.getInstance().getHisItems(emrPoint.get("id").toString(), 0 ,2);
        
        if (hisItems.size() < 2) {
            CcuLog.d(L.TAG_CCU_ZONE, "EmrProfile, Only one Reading !");
            return;
        }
        HisItem reading1 = hisItems.get(0);
        HisItem reading2 = hisItems.get(1);
        
        for (HisItem h : hisItems) {
            CcuLog.d(L.TAG_CCU_ZONE, "EmrProfile, "+h.getDate()+" "+h.getVal());
        }
        
        int timeDiffMins = (int) (reading1.getDate().getTime() - reading2.getDate().getTime())/(60*1000);
        if (timeDiffMins < 1) {
            double curRate = emrEquip.getHisVal("current and rate");
            emrEquip.setEquipStatus("Total Energy Consumed "+reading1.getVal()+" kWh "+" Current Rate "+curRate+"KW");
            return;
        }
        double readingDiff = 100 * (reading1.getVal() - reading2.getVal());
        
        double ratekWh = (60 * readingDiff) / (timeDiffMins * 1000);
        
        ratekWh = Math.round(ratekWh * 100)/100;
        
        emrEquip.setHisVal("current and rate", ratekWh);
        emrEquip.setEquipStatus("Total Energy Consumed "+reading1.getVal()+" kWh "+" Current Rate "+ratekWh+"KW");
    
        CcuLog.d(L.TAG_CCU_ZONE, "EmrProfile, Total Energy Consumed "+reading1.getVal()+" currentRate "+ratekWh);
        
    }
}
