package a75f.io.logic.bo.building.ccu;


import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import a75.io.algos.GenericPIController;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMP_DEAD;

//CCU As a Zone Profile
public class CazProfile extends ZoneProfile
{
    CazEquip cazEquip;

    public void addCcuAsZoneEquip(short addr, CazProfileConfig config, String floorRef, String roomRef) {
        cazEquip = new CazEquip(getProfileType(), addr);
        cazEquip.createEntities(config, floorRef, roomRef);
        cazEquip.init();
    }

    public void addCcuAsZoneEquip(short addr) {
        cazEquip = new CazEquip(getProfileType(), addr);
        cazEquip.init();
    }

    public void updateCcuAsZone(CazProfileConfig config) {
        cazEquip.updateCcuAsZoneConfig(config);
    }

    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.TEMP_INFLUENCE;
    }

    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return cazEquip.getProfileConfiguration();
    }

    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)cazEquip.nodeAddr);
        }};
    }

    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+cazEquip.nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    @Override
    public boolean isZoneDead() {

        double buildingLimitMax =  TunerUtil.readTunerValByQuery("building and limit and max", L.ccu().systemProfile.getSystemEquipRef());
        double buildingLimitMin =  TunerUtil.readTunerValByQuery("building and limit and min", L.ccu().systemProfile.getSystemEquipRef());

        double tempDeadLeeway = TunerUtil.readTunerValByQuery("temp and dead and leeway",L.ccu().systemProfile.getSystemEquipRef());

        if (cazEquip.getCurrentTemp() > (buildingLimitMax + tempDeadLeeway)
                || cazEquip.getCurrentTemp() < (buildingLimitMin - tempDeadLeeway))
        {
            return true;
        }

        return false;
    }

    @Override
    public void updateZonePoints()
    {
        if (isZoneDead()) {
            CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead: "+cazEquip.nodeAddr+" roomTemp : "+cazEquip.getCurrentTemp());
            state = TEMP_DEAD;
            String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+cazEquip.nodeAddr+"\"");
            if (!curStatus.equals("Zone Temp Dead"))
            {
                CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + cazEquip.nodeAddr + "\"", "Zone Temp Dead");

            }
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + cazEquip.nodeAddr + "\"", (double)TEMP_DEAD.ordinal());
            return;
        }

        double setTempCooling = cazEquip.getDesiredTempCooling();
        double setTempHeating = cazEquip.getDesiredTempHeating();
        double roomTemp = cazEquip.getCurrentTemp();
        //GenericPIController damperOpController = cazEquip.damperController;

        Log.d(L.TAG_CCU_ZONE, "CAZ : roomTemp" + roomTemp + " setTempCooling:  " + setTempCooling+" setTempHeating: "+setTempHeating);

        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();

        if (roomTemp > setTempCooling)
        {
            //Zone is in Cooling
            if (state != COOLING)
            {
                state = COOLING;
                //damperOpController.reset();
            }
            /*if (conditioning == SystemController.State.COOLING)
            {
                damperOpController.updateControlVariable(roomTemp, setTempCooling);
            }*/
        }
        else if (roomTemp < setTempHeating)
        {
            //Zone is in heating
            if (state != HEATING)
            {
                state = HEATING;
                //damperOpController.reset();
            }
            /*if (conditioning == SystemController.State.HEATING)
            {
                damperOpController.updateControlVariable(setTempHeating, roomTemp);
            }*/
        } else {
            if (state != DEADBAND) {
                state = DEADBAND;
                //damperOpController.reset();
            }

        }
        //damperOpController.dump();

        String zoneId = HSUtil.getZoneIdFromEquipId(cazEquip.getId());
        //Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        //boolean occupied = (occ == null ? false : occ.isOccupied());


        cazEquip.setStatus(state.ordinal(), (DabSystemController.getInstance().isEmergencyMode() || VavSystemController.getInstance().isEmergencyMode()) && (state == HEATING ? buildingLimitMinBreached()
                : state == COOLING ? buildingLimitMaxBreached() : false));
        CcuLog.d(L.TAG_CCU_ZONE, "System STATE :" + DabSystemController.getInstance().getSystemState() + " ZoneState : " + getState() );

    }

    @Override
    public void reset(){
        cazEquip.setCurrentTemp(0);
    }
}
