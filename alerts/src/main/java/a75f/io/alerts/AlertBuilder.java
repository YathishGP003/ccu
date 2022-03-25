package a75f.io.alerts;

import java.util.GregorianCalendar;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;

/**
 * Created by samjithsadasivan on 4/25/18.
 */

public class AlertBuilder
{
    public static Alert build(AlertDefinition def, String message, CCUHsApi haystack) {
        return build(def, message, haystack, null, null);
    }
    
    public static Alert build(AlertDefinition def, String message, CCUHsApi haystack, String equipRef, String pointId) {
        Alert alert = new Alert();
        alert.setGuid("");
        alert.setStartTime(GregorianCalendar.getInstance().getTimeInMillis());
        alert.setmTitle(def.alert.mTitle);
        alert.setmMessage(message);
        alert.setmNotificationMsg(message);
        alert.setmSeverity(def.alert.mSeverity);
        alert.setmAlertType(def.alert.mAlertType);
        alert.setRef(pointId);
        alert.setDeviceRef(CCUHsApi.getInstance().getCcuRef().toString());
        alert.setmEnabled(true);
        alert.setSyncStatus(false);
        alert.alertDefId = def._id;
        alert.siteIdNoAt = haystack.getSiteIdRef().toVal();
        alert.siteName = haystack.getSiteName();
        alert.ccuIdNoAt = haystack.getCcuRef().toVal();
        alert.ccuName = haystack.getCcuName();
        if (equipRef != null) {
            alert.equipId = equipRef.replaceFirst("@","");

            Equip equip = HSUtil.getEquipInfo(equipRef);
            String zoneRef = equip.getRoomRef();
            String floorRef = equip.getFloorRef();
            alert.equipName = equip.getDisplayName();
            alert.zoneId = zoneRef.replaceFirst("@","");
            alert.zoneName = HSUtil.getDis(zoneRef);
            alert.floorId = floorRef.replaceFirst("@","");
            alert.floorName = HSUtil.getDis(floorRef);
        }
        return alert;
    }
}
