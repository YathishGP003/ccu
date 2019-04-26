package a75f.io.api.haystack.sync;

import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 12/31/18.
 */

public class EquipSyncAdapter extends EntitySyncAdapter
{
    @Override
    public boolean onSync() {
        CcuLog.i("CCU", "doSyncEquips ->");
        HashMap site = CCUHsApi.getInstance().read("site");
        String siteLUID = site.get("id").toString();
        
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and siteRef == \"" + siteLUID + "\"");
        ArrayList<String> equipLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        
        //Sync system equip first as ahuRef is to be used in other equips.
        if (syncSystemEquip(siteLUID) == false) {
            return false;
        }
        
        for (Map m: equips)
        {
            CcuLog.i("CCU", m.toString());
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                equipLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                if (m.get("floorRef") != null && !m.get("floorRef").toString().equals("SYSTEM"))
                {
                    String guid = CCUHsApi.getInstance().getGUID(m.get("floorRef").toString());
                    if(guid != null)
                        m.put("floorRef", HRef.copy(guid));
                }
                if (m.get("roomRef") != null && !m.get("roomRef").toString().equals("SYSTEM") )
                {
                    String guid = CCUHsApi.getInstance().getGUID(m.get("roomRef").toString());
                    if(guid != null)
                        m.put("roomRef", HRef.copy(guid));
                }
                if (m.get("ahuRef") != null && CCUHsApi.getInstance().getGUID(m.get("ahuRef").toString()) != null)
                {
                    String guid = CCUHsApi.getInstance().getGUID(m.get("ahuRef").toString());
                    if(guid != null)
                        m.put("ahuRef", HRef.copy(guid));
                }
                entities.add(HSUtil.mapToHDict(m));
            }
        }
        if (equipLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response: \n" + response);
            if (response == null)
            {
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            String equipGUID = "";
            int index = 0;
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                equipGUID = row.get("id").toString();
                if (equipGUID != null && equipGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(equipLUIDList.get(index++), equipGUID);
                } else {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean syncSystemEquip(String siteRef) {
        CcuLog.i("CCU", "doSyncSystemEquip ->");
        HashMap systemEquip = CCUHsApi.getInstance().read("equip and system");
        if (systemEquip == null || systemEquip.size() == 0) {
            CcuLog.d("CCU","Abort System, System Equip does not exist");
            return false;
        }
        String equipLUID = systemEquip.remove("id").toString();
        if (CCUHsApi.getInstance().getGUID(equipLUID) != null) {
            return true;
        }
        
        ArrayList<HDict> entities = new ArrayList<>();
        systemEquip.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteRef)));
        entities.add(HSUtil.mapToHDict(systemEquip));
    
        HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
        String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
        CcuLog.i("CCU", "Response: \n" + response);
        if (response == null)
        {
            return false;
        }
        HZincReader zReader = new HZincReader(response);
        Iterator it = zReader.readGrid().iterator();
        while (it.hasNext())
        {
            HRow row = (HRow) it.next();
            String equipGUID = row.get("id").toString();
            if (equipGUID != null && equipGUID != "")
            {
                CCUHsApi.getInstance().putUIDMap(equipLUID, equipGUID);
            } else {
                return false;
            }
        }
        
        return true;
    }
}
