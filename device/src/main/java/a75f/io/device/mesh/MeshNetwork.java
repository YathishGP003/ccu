package a75f.io.device.mesh;

import static a75f.io.device.mesh.DLog.tempLogdStructAsJson;
import static a75f.io.device.mesh.MeshUtil.checkDuplicateStruct;
import static a75f.io.device.mesh.MeshUtil.sendStruct;
import static a75f.io.device.mesh.MeshUtil.sendStructToCM;
import static a75f.io.device.mesh.MeshUtil.sendStructToNodes;
import static a75f.io.logic.L.ccu;

import android.util.Log;

import java.util.HashMap;
import java.util.List;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.ControlMote;
import a75f.io.device.DeviceNetwork;
import a75f.io.device.HyperSplit;
import a75f.io.device.HyperStat;
import a75f.io.device.cm.ControlMoteMessageGeneratorKt;
import a75f.io.device.cm.ControlMoteMessageSenderKt;
import a75f.io.device.daikin.IEDeviceHandler;
import a75f.io.device.mesh.hypersplit.HyperSplitMessageGenerator;
import a75f.io.device.mesh.hypersplit.HyperSplitMessageSender;
import a75f.io.device.mesh.hyperstat.HyperStatMessageGenerator;
import a75f.io.device.mesh.hyperstat.HyperStatMessageSender;
import a75f.io.device.mesh.hyperstat.HyperStatSettingsUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSmartStatMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatSettingsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettings2Message_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettingsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu;
import a75f.io.logic.bo.building.system.vav.VavIERtu;


/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class MeshNetwork extends DeviceNetwork
{
    @Override
    public void sendMessage() {
        CcuLog.d(L.TAG_CCU_DEVICE, "MeshNetwork SendNodeMessage");

        if (!LSerial.getInstance().isConnected()) {
            CcuLog.d(L.TAG_CCU_DEVICE,"Device not connected !!");
            LSerial.getInstance().setResetSeedMessage(true);
            return;
        }

        if(LSerial.getInstance().isNodesSeeding())
            return;
        if (isAnyEquipAlive(CCUHsApi.getInstance())) {
            MeshUtil.sendHeartbeat((short) 0);
        }

        MeshUtil.tSleep(1000);
        boolean sendControlMessage = (((System.currentTimeMillis() -  HyperStatSettingsUtil.Companion.getCcuControlMessageTimer())/ 1000) / 60)>20;
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
                    for(Device d : HSUtil.getDevices(zone.getId())) {

                        if (d.getMarkers().contains("modbus")) {
                          continue;
                        }
                        NodeType deviceType = NodeType.SMART_NODE;
                        if(d.getMarkers().contains("smartstat"))
                            deviceType = NodeType.SMART_STAT;
                        else if(d.getMarkers().contains("ti"))
                            deviceType = NodeType.CONTROL_MOTE;
                        else if (d.getMarkers().contains("hyperstat")) {
                            deviceType = NodeType.HYPER_STAT;
                        }
                        else if (d.getMarkers().contains("hyperstatsplit")) {
                            deviceType = NodeType.HYPERSTATSPLIT;
                        }
                        switch (deviceType) {
                            case SMART_NODE:
                                String snprofile = "dab";
                                if (d.getMarkers().contains("sse"))
                                    snprofile = "sse";
                                else if (d.getMarkers().contains("lcm"))
                                    snprofile = "lcm";
                                else if (d.getMarkers().contains("iftt"))
                                    snprofile = "iftt";
                                else if (d.getMarkers().contains("bypassDamper"))
                                    snprofile = "bypass";
                                if (bSeedMessage) {
                                    CcuLog.d("CCU_SN_MESSAGES", "=================NOW SENDING SN SEEDS=====================" + zone.getId());
                                    CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = LSmartNode.getSeedMessage(zone, Short.parseShort(d.getAddr()), d.getEquipRef(), snprofile);
                                    tempLogdStructAsJson(seedMessage);
                                    sendStructToCM(seedMessage);

                                    CcuLog.d("CCU_SN_MESSAGES", "=================NOW SENDING SN Settings2=====================");
                                    CcuToCmOverUsbSnSettings2Message_t settings2Message = LSmartNode.getSettings2Message(zone, Short.parseShort(d.getAddr()), d.getEquipRef(), snprofile);
                                    tempLogdStructAsJson(settings2Message);
                                    sendStructToCM(settings2Message);
                                } else {
                                    CcuLog.d("CCU_SN_MESSAGES", "=================NOW SENDING SN Settings=====================");
                                    CcuToCmOverUsbSnSettingsMessage_t settingsMessage = LSmartNode.getSettingsMessage(zone, Short.parseShort(d.getAddr()), d.getEquipRef(), snprofile);
                                    tempLogdStructAsJson(settingsMessage);
                                    sendStruct((short) settingsMessage.smartNodeAddress.get(), settingsMessage);
                                    CcuLog.d("CCU_SN_MESSAGES", "=================NOW SENDING SN CONTROLS=====================");
                                    CcuToCmOverUsbSnControlsMessage_t controlsMessage = LSmartNode.getControlMessage(zone, Short.parseShort(d.getAddr()), d.getEquipRef());
                                    tempLogdStructAsJson(controlsMessage);
                                    if (!checkDuplicateStruct((short) controlsMessage.smartNodeAddress.get(), controlsMessage)) {
                                        controlsMessage = LSmartNode.getCurrentTimeForControlMessage(controlsMessage);
                                        sendStructToNodes(controlsMessage);
                                    }
                                    CcuLog.d("CCU_SN_MESSAGES", "=================NOW SENDING SN Settings2=====================");
                                    CcuToCmOverUsbSnSettings2Message_t settings2Message = LSmartNode.getSettings2Message(zone, Short.parseShort(d.getAddr()), d.getEquipRef(), snprofile);
                                    tempLogdStructAsJson(settings2Message);
                                    sendStruct((short) settings2Message.smartNodeAddress.get(), settings2Message);
                                }
                                break;
                            case SMART_STAT:
                                String profile = "cpu";
                                if (d.getMarkers().contains("hpu"))
                                    profile = "hpu";
                                else if (d.getMarkers().contains("pipe2"))
                                    profile = "pipe2";
                                else if (d.getMarkers().contains("pipe4"))
                                    profile = "pipe4";
                                if (bSeedMessage) {
                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SS SEEDS=====================" + zone.getId());
                                    CcuToCmOverUsbDatabaseSeedSmartStatMessage_t seedSSMessage = LSmartStat.getSeedMessage(zone, Short.parseShort(d.getAddr()), d.getEquipRef(), profile);
                                    sendStructToCM(seedSSMessage);
                                } else {
                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SMART_STAT Settings=====================");
                                    CcuToCmOverUsbSmartStatSettingsMessage_t settingsMessage = LSmartStat.getSettingsMessage(zone, Short.parseShort(d.getAddr()), d.getEquipRef(), profile);
                                    sendStruct((short) settingsMessage.address.get(), settingsMessage);


                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING SMART_STAT CONTROLS=====================");
                                    CcuToCmOverUsbSmartStatControlsMessage_t controlsSSMessage = LSmartStat.getControlMessage(zone, Short.parseShort(d.getAddr()), d.getEquipRef());
                                    if (!checkDuplicateStruct((short) controlsSSMessage.address.get(), controlsSSMessage)) {
                                        controlsSSMessage = LSmartStat.getCurrentTimeForControlMessage(controlsSSMessage);
                                        sendStructToNodes(controlsSSMessage);
                                    }
                                }
                                break;


                            case HYPER_STAT:
                                Equip equip = new Equip.Builder()
                                        .setHashMap(CCUHsApi.getInstance()
                                                .read("equip and group ==\"" + d.getAddr() + "\"")).build();
                                if (bSeedMessage) {
                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING HyperStat " +
                                            "SEEDS ===================== " + d.getAddr());
                                    if (equip.getMarkers().contains("vrv")) {
                                        CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SEEDING HyperStat IDU Controls ===================== " + d.getAddr());
                                        HyperStatMessageSender.sendIduSeedSetting(zone.getDisplayName(), Integer.parseInt(d.getAddr()), d.getEquipRef(), false);
                                        HyperStatMessageSender.sendIduSeedControlMessage(Integer.parseInt(d.getAddr()), CCUHsApi.getInstance());
                                    } else {
                                        HyperStatMessageSender.sendSeedMessage(zone.getDisplayName(), Integer.parseInt(d.getAddr()),
                                                d.getEquipRef(), false);
                                    }
                                } else {
                                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING HyperStat Settings ===================== " + d.getAddr());
                                    HyperStatMessageSender.sendSettingsMessage(zone, Integer.parseInt(d.getAddr()), d.getEquipRef());

                                    if (!equip.getMarkers().contains("vrv")) {
                                        /** Sending the setting2 and setting3 messages */
                                        CcuLog.d(L.TAG_CCU_DEVICE, "======== NOW SENDING Additional Setting HyperStat setting Messages ===" + d.getAddr());
                                        HyperStatMessageSender.sendAdditionalSettingMessages(Integer.parseInt(d.getAddr()), d.getEquipRef());
                                    }
                                    if (equip.getMarkers().contains("vrv")) {
                                        CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING HyperStat IDU Controls ===================== " + d.getAddr());
                                        HyperStatMessageSender.sendIduControlMessage(
                                                Integer.parseInt(d.getAddr()), CCUHsApi.getInstance());
                                    } else if (!equip.getMarkers().contains("monitoring")) {
                                        CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING HyperStat Controls ===================== " + d.getAddr());
                                        if (sendControlMessage) {
                                            HyperStat.HyperStatControlsMessage_t.Builder controls =
                                                    HyperStatMessageGenerator.getControlMessage(
                                                            Integer.parseInt(d.getAddr()), d.getEquipRef());
                                            HyperStatMessageSender.writeControlMessage(controls.build(), Integer.parseInt(d.getAddr()),
                                                    MessageType.HYPERSTAT_CONTROLS_MESSAGE, false);
                                        }
                                        HyperStatMessageSender.sendControlMessage(Integer.parseInt(d.getAddr()), d.getEquipRef());
                                    }

                                }
                                break;

                            case HYPERSTATSPLIT:
                                Equip hssEquip = new Equip.Builder()
                                        .setHashMap(CCUHsApi.getInstance()
                                                .read("equip and group ==\""+d.getAddr()+ "\"")).build();

                                if (bSeedMessage) {
                                    CcuLog.d(L.TAG_CCU_SERIAL,"=================NOW SENDING HyperSplit SEEDS ===================== "+d.getAddr());
                                    HyperSplitMessageSender.sendSeedMessage(zone.getDisplayName(), Integer.parseInt(d.getAddr()),
                                            d.getEquipRef(), false);
                                    HyperSplitMessageSender.sendSettings3Message(Integer.parseInt(d.getAddr()),
                                            d.getEquipRef(), false);
                                    HyperSplitMessageSender.sendSettings4Message(Integer.parseInt(d.getAddr()),
                                            d.getEquipRef(), false);
                                } else {
                                    CcuLog.d(L.TAG_CCU_SERIAL, "=================NOW SENDING HyperSplit Settings ===================== "+d.getAddr());
                                    HyperSplitMessageSender.sendSettingsMessage(zone, Integer.parseInt(d.getAddr()), d.getEquipRef());

                                    CcuLog.d(L.TAG_CCU_SERIAL, "======== NOW SENDING Additional Setting HyperSplit setting Messages ===" + d.getAddr());
                                    HyperSplitMessageSender.sendAdditionalSettingMessages(Integer.parseInt(d.getAddr()), d.getEquipRef());

                                    CcuLog.d(L.TAG_CCU_SERIAL, "=================NOW SENDING HyperSplit Controls ===================== "+d.getAddr());

                                    if(sendControlMessage){
                                        HyperSplit.HyperSplitControlsMessage_t.Builder controls =
                                                HyperSplitMessageGenerator.getControlMessage(
                                                        Integer.parseInt(d.getAddr()), d.getEquipRef());
                                        HyperSplitMessageSender.writeControlMessage(controls.build(), Integer.parseInt(d.getAddr()),
                                                MessageType.HYPERSPLIT_CONTROLS_MESSAGE, false);
                                    }
                                    HyperSplitMessageSender.sendControlMessage(Integer.parseInt(d.getAddr()), d.getEquipRef());

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

            if (ccu().bypassDamperProfile != null)
            {
                if (bSeedMessage)
                {
                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING Bypass Damper SN SEED Message =====================");
                    CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = LSmartNode.getSeedMessage(new Zone.Builder().setDisplayName("BYPASS DAMPER").build(),
                            (short) ccu().bypassDamperProfile.getNodeAddr(), ccu().bypassDamperProfile.getEquipRef(),"bypass");
                    sendStructToCM(seedMessage);
                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING Bypass Damper SN Settings2 Message =====================");
                    CcuToCmOverUsbSnSettings2Message_t settings2Message = LSmartNode.getSettings2Message(new Zone.Builder().setDisplayName("BYPASS DAMPER").build(),
                            (short) ccu().bypassDamperProfile.getNodeAddr(), ccu().bypassDamperProfile.getEquipRef(),"bypass");
                    sendStructToCM(settings2Message);
                }
                else
                {
                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING Bypass Damper SN Settings=====================");
                    CcuToCmOverUsbSnSettingsMessage_t settingsMessage = LSmartNode.getSettingsMessage(new Zone.Builder().setDisplayName("BYPASS DAMPER").build()
                            , (short)L.ccu().bypassDamperProfile.getNodeAddr(), ccu().bypassDamperProfile.getEquipRef(),"bypass");
                    sendStruct((short) settingsMessage.smartNodeAddress.get(), settingsMessage);
                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING Bypass Damper SN CONTROLS=====================");
                    CcuToCmOverUsbSnControlsMessage_t controlsMessage = LSmartNode.getControlMessage(new Zone.Builder().setDisplayName("BYPASS DAMPER").build()
                            , (short) ccu().bypassDamperProfile.getNodeAddr(), ccu().bypassDamperProfile.getEquipRef());
                    //Check duplicated without current time and then append time to control package.
                    if (!checkDuplicateStruct((short) controlsMessage.smartNodeAddress.get(), controlsMessage))
                    {
                        controlsMessage = LSmartNode.getCurrentTimeForControlMessage(controlsMessage);
                        sendStructToNodes(controlsMessage);
                    }
                    CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING Bypass Damper SN SETTINGS2=====================");
                    CcuToCmOverUsbSnSettings2Message_t settings2Message = LSmartNode.getSettings2Message(new Zone.Builder().setDisplayName("BYPASS DAMPER").build()
                            , (short) ccu().bypassDamperProfile.getNodeAddr(), ccu().bypassDamperProfile.getEquipRef(), "bypass");
                    sendStruct((short) settings2Message.smartNodeAddress.get(), settings2Message);
                }
            }
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }finally {
            if(bSeedMessage==true) {
                CcuLog.d(L.TAG_CCU_DEVICE,"Resetting the Seed Message variable to avoid multiple seed messages");
                LSerial.getInstance().setResetSeedMessage(false);
            } else {
                CcuLog.d(L.TAG_CCU_DEVICE,"Local seed message is false. The shared variable may have been updated to true. So skipping reset to avoid Lost Update.");
            }
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

        if (ccu().systemProfile instanceof VavAdvancedAhu) {
            ControlMote.CcuToCmSettingsMessage_t cmSettingsMessage = ControlMoteMessageGeneratorKt.getCMSettingsMessage();
            CcuLog.d(L.TAG_CCU_DEVICE, "CM Proto Settings Message: " + cmSettingsMessage);
            ControlMoteMessageSenderKt.sendControlMoteMessage(MessageType.CCU_TO_CM_OVER_USB_CM_SERIAL_SETTINGS, cmSettingsMessage.toByteArray());

            ControlMote.CcuToCmOverUsbCmControlMessage_t cmControlMessage = ControlMoteMessageGeneratorKt
                                    .getCMControlsMessage();
            CcuLog.d(L.TAG_CCU_DEVICE, "CM Proto Control Message: " + cmControlMessage);
            ControlMoteMessageSenderKt.sendControlMoteMessage(MessageType.CCU_TO_CM_OVER_USB_CM_SERIAL_CONTROLS, cmControlMessage.toByteArray());
        } else {
            MeshUtil.sendStructToCM(DeviceUtil.getCMControlsMessage());
        }
        
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

    private boolean isAnyEquipAlive(CCUHsApi hayStack) {
        List<HashMap<Object, Object>> allStatusPoints = hayStack.readAllEntities("point and status and not ota and his");

        for (HashMap point : allStatusPoints) {
            int statusVal = hayStack.readHisValById(point.get("id").toString()).intValue();
            if (statusVal != ZoneState.TEMPDEAD.ordinal()) {
                return true;
            }
        }
        return false;
    }
}
