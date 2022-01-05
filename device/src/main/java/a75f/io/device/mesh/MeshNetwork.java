package a75f.io.device.mesh;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.DeviceNetwork;
import a75f.io.device.daikin.IEDeviceHandler;
import a75f.io.device.mesh.hyperstat.HyperStatMessageSender;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSmartStatMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatSettingsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettingsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.diag.DiagEquip;

import static a75f.io.device.mesh.MeshUtil.checkDuplicateStruct;
import static a75f.io.device.mesh.MeshUtil.sendStruct;
import static a75f.io.device.mesh.MeshUtil.sendStructToCM;
import static a75f.io.device.mesh.MeshUtil.sendStructToNodes;
import static a75f.io.logic.L.ccu;

import android.util.Log;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class MeshNetwork extends DeviceNetwork
{
    @Override
    public void sendMessage() {
        CcuLog.d(L.TAG_CCU_DEVICE, "MeshNetwork SendNodeMessage");
        DiagEquip.getInstance().setDiagHisVal("serial and connection", LSerial.getInstance().isConnected() ? 1.0 :0);
        if (!LSerial.getInstance().isConnected()) {
            CcuLog.d(L.TAG_CCU_DEVICE,"Device not connected !!");
            LSerial.getInstance().setResetSeedMessage(true);
            return;
        }
        if(LSerial.getInstance().isNodesSeeding())
            return;
        MeshUtil.sendHeartbeat((short)0);
        
        MeshUtil.tSleep(1000);
        boolean bSeedMessage = LSerial.getInstance().isReseedMessage();
        Log.i(L.TAG_CCU_DEVICE, "bSeedMessage: "+bSeedMessage);

        try
        {
            for (Floor floor : HSUtil.getFloors())
            {
                for (Zone zone : HSUtil.getZones(floor.getId()))
                {
                    if(LSerial.getInstance().isNodesSeeding())break;
                    CcuLog.d(L.TAG_CCU_DEVICE,"=============Zone: " + zone.getDisplayName() + " =================="+bSeedMessage);
                    for(Device d : HSUtil.getDevices(zone.getId())) { //TODO Will this work? kumar
                        NodeType deviceType = NodeType.SMART_NODE;
                        if(d.getMarkers().contains("smartstat"))
                            deviceType = NodeType.SMART_STAT;
                        else if(d.getMarkers().contains("ti"))
                            deviceType = NodeType.CONTROL_MOTE;
                        else if (d.getMarkers().contains("hyperstat")) {
                            deviceType = NodeType.HYPER_STAT;
                        }
                        switch (deviceType) {
                            case SMART_NODE:
                                String snprofile = "dab";
                                if(d.getMarkers().contains("sse"))
                                    snprofile = "sse";
                                else if(d.getMarkers().contains("lcm"))
                                    snprofile = "lcm";
                                else if(d.getMarkers().contains("iftt"))
                                    snprofile = "iftt";
                                if(bSeedMessage) {
                                    CcuLog.d(L.TAG_CCU_DEVICE,"=================NOW SENDING SN SEEDS====================="+zone.getId());
                                    CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = LSmartNode.getSeedMessage(zone, Short.parseShort(d.getAddr()),d.getEquipRef(),snprofile);
                                    if (sendStructToCM(/*(short) seedMessage.smartNodeAddress.get(),*/ seedMessage)) {
                                        //Log.w(DLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                                    }
                                }else {
                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SN Settings=====================");
                                    CcuToCmOverUsbSnSettingsMessage_t settingsMessage = LSmartNode.getSettingsMessage(zone,Short.parseShort(d.getAddr()),d.getEquipRef(),snprofile);
                                    if (sendStruct((short) settingsMessage.smartNodeAddress.get(), settingsMessage)) {
                                        //Log.w(DLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                                    }
                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SN CONTROLS=====================");
                                    CcuToCmOverUsbSnControlsMessage_t controlsMessage = LSmartNode.getControlMessage(zone,Short.parseShort(d.getAddr()),d.getEquipRef());
                                    //Check duplicated without current time and then append time to control package.
                                    if(!checkDuplicateStruct((short)controlsMessage.smartNodeAddress.get(),controlsMessage)){
                                        controlsMessage = LSmartNode.getCurrentTimeForControlMessage(controlsMessage);
                                        sendStructToNodes(controlsMessage);
                                    }
                                }
                                break;
                            case SMART_STAT:
                                String profile = "cpu";
                                if(d.getMarkers().contains("hpu"))
                                    profile = "hpu";
                                else if(d.getMarkers().contains("pipe2"))
                                    profile = "pipe2";
                                else if(d.getMarkers().contains("pipe4"))
                                    profile = "pipe4";
                                if(bSeedMessage) {
                                    CcuLog.d(L.TAG_CCU_DEVICE,"=================NOW SENDING SS SEEDS====================="+zone.getId());
                                        CcuToCmOverUsbDatabaseSeedSmartStatMessage_t seedSSMessage = LSmartStat.getSeedMessage(zone,Short.parseShort(d.getAddr()),d.getEquipRef(),profile);
                                        if (sendStructToCM( seedSSMessage)) {
                                            //Log.w(DLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                                        }
                                }else {
                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SMART_STAT Settings=====================");
                                    CcuToCmOverUsbSmartStatSettingsMessage_t settingsMessage = LSmartStat.getSettingsMessage(zone,Short.parseShort(d.getAddr()),d.getEquipRef(),profile);
                                    if (sendStruct((short) settingsMessage.address.get(), settingsMessage)) {
                                        //Log.w(DLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                                    }
                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SMART_STAT CONTROLS=====================");
                                    CcuToCmOverUsbSmartStatControlsMessage_t controlsSSMessage = LSmartStat.getControlMessage(zone,Short.parseShort(d.getAddr()),d.getEquipRef());
                                    if(!checkDuplicateStruct((short)controlsSSMessage.address.get(),controlsSSMessage)){
                                        controlsSSMessage = LSmartStat.getCurrentTimeForControlMessage(controlsSSMessage);
                                        sendStructToNodes(controlsSSMessage);
                                    }
                                }
                                break;
                                
                                
                            case HYPER_STAT:
                                String hyperStatProfile = "sense"; //TODO
                                Equip equip = new Equip.Builder()
                                                  .setHashMap(CCUHsApi.getInstance()
                                                                      .read("equip and group ==\""+d.getAddr()+ "\"")).build();


                                if (bSeedMessage) {
                                    CcuLog.d(L.TAG_CCU_DEVICE,"=================NOW SENDING HyperStat " +
                                                              "SEEDS ===================== "+d.getAddr());
                                    HyperStatMessageSender.sendSeedMessage(zone.getDisplayName(), Integer.parseInt(d.getAddr()),
                                                                           d.getEquipRef(), hyperStatProfile, false);
                                } else{
                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING HyperStat Settings ===================== "+d.getAddr());
                                    HyperStatMessageSender.sendSettingsMessage(zone.getDisplayName(), Integer.parseInt(d.getAddr()), d.getEquipRef());

                                    /** Sending the setting2 and setting3 messages */
                                    CcuLog.d(L.TAG_CCU_DEVICE, "======== NOW SENDING Additional Setting HyperStat setting Messages ==="+d.getAddr());
                                    HyperStatMessageSender.sendAdditionalSettingMessages(Integer.parseInt(d.getAddr()), d.getEquipRef());

                                    if (equip.getMarkers().contains("vrv") ){
                                        CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING HyperStat IDU Controls ===================== "+d.getAddr());
                                        HyperStatMessageSender.sendIduControlMessage(
                                                Integer.parseInt(d.getAddr()), CCUHsApi.getInstance());
                                    }
                                    else if (!equip.getMarkers().contains("sense")){
                                        CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING HyperStat Controls ===================== "+d.getAddr());
                                        HyperStatMessageSender.sendControlMessage(Integer.parseInt(d.getAddr()), d.getEquipRef());
                                    }


                                }
                        }
                    }
                }
            }
    
            if (ccu().oaoProfile != null)
            {
                if (bSeedMessage)
                {
                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING OAO SN SEED Message =====================");
                    CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = LSmartNode.getSeedMessage(new Zone.Builder().setDisplayName("OAO").build(),
                            (short)L.ccu().oaoProfile.getNodeAddress(), ccu().oaoProfile.getEquipRef(),"oao");
                    sendStructToCM(seedMessage);
                }
                else
                {
                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING OAO SN Settings=====================");
                    CcuToCmOverUsbSnSettingsMessage_t settingsMessage = LSmartNode.getSettingsMessage(new Zone.Builder().setDisplayName("OAO").build()
                                                                                , (short)L.ccu().oaoProfile.getNodeAddress(), ccu().oaoProfile.getEquipRef(),"oao");
                    sendStruct((short) settingsMessage.smartNodeAddress.get(), settingsMessage);
                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SN CONTROLS=====================");
                    CcuToCmOverUsbSnControlsMessage_t controlsMessage = LSmartNode.getControlMessage(new Zone.Builder().setDisplayName("OAO").build()
                                                                                , (short)L.ccu().oaoProfile.getNodeAddress(), ccu().oaoProfile.getEquipRef());
                    //Check duplicated without current time and then append time to control package.
                    if (!checkDuplicateStruct((short) controlsMessage.smartNodeAddress.get(), controlsMessage))
                    {
                        controlsMessage = LSmartNode.getCurrentTimeForControlMessage(controlsMessage);
                        sendStructToNodes(controlsMessage);
                    }
                }
            }
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }finally {
            LSerial.getInstance().setResetSeedMessage(false);
        }
    }
    
    public void sendSystemControl() {
        CcuLog.i(L.TAG_CCU_DEVICE, "MeshNetwork SendSystemControl");
        
        if (ccu().systemProfile == null) {
            CcuLog.d(L.TAG_CCU_DEVICE, "MeshNetwork SendSystemControl : Abort , No system profile");
            return;
        }
        Pulse.checkForDeviceDead();

        if (ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_IE_RTU) {
            //DaikinIE.sendControl();
            VavIERtu systemProfile = (VavIERtu) L.ccu().systemProfile;
            IEDeviceHandler.getInstance().sendControl(systemProfile, CCUHsApi.getInstance());
        }
    
        if (!LSerial.getInstance().isConnected()) {
            CcuLog.d(L.TAG_CCU_DEVICE,"Device not connected !!");
            LSerial.getInstance().setResetSeedMessage(true);
            Pulse.setCMDeadTimerIncrement(false);
            return;
        } else {
            AlertManager.getInstance().fixCMDead();
        }
        
        if (ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_IE_RTU
                && DLog.isLoggingEnabled()) {
            sendIETestMessage((VavIERtu) L.ccu().systemProfile);
            return;
        }
    
        MeshUtil.sendStructToCM(DeviceUtil.getCMControlsMessage());
        
    }
    
    private void sendIETestMessage(VavIERtu systemProfile) {
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
        msg.analog0.set((short) systemProfile.getCmdSignal("dat and setpoint"));
        msg.analog1.set((short) systemProfile.getCmdSignal("fan"));
        msg.analog2.set((short) systemProfile.getSystemController().getAverageSystemHumidity());
        msg.analog3.set((short) (systemProfile.getCmdSignal("staticPressure") * 10));
        MeshUtil.sendStructToCM(msg);
    }
}
