package a75f.io.device.modbus;


import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.DeviceNetwork;
import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.modbusbox.EquipsManager;

public class ModbusNetwork extends DeviceNetwork
{
    private static final int SERIAL_COMM_TIMEOUT_MS = 1000;

    @Override
    public void sendMessage() {
        
        if (!LSerial.getInstance().isModbusConnected()) {
            CcuLog.d(L.TAG_CCU_MODBUS,"ModbusNetwork: Serial device not connected");
            return;
        }
        try
        {
            for (Floor floor : HSUtil.getFloors())
            {
                for (Zone zone : HSUtil.getZones(floor.getId()))
                {
                    CcuLog.d(L.TAG_CCU_MODBUS,"SERIAL_ =======Modbus Zone: " + zone.getDisplayName() + " =================="+","+zone.getMarkers().contains("modbus"));
                    //send request for modbus modules alone
                    for (Equip equip : HSUtil.getEquips(zone.getId())) {
                        if (equip.getMarkers().contains("modbus")) {
                            EquipmentDevice modbusDevice = EquipsManager.getInstance().fetchProfileBySlaveId(Short.parseShort(equip.getGroup()));
                            
                            for(Register register: modbusDevice.getRegisters()) {
                                CcuLog.d(L.TAG_CCU_MODBUS,"SERIAL_ ======= Register "+register.getRegisterAddress()+
                                                          " "+register.getParameterDefinitionType());
                                                          //+register.getParameters().get(0).getParameterDefinitionType());
                                //TODO Need to handle sequence of registers here for now we handle one by one
                                
                                int registerNum = register.getParameterDefinitionType().equals("float") ? 2 : 1;
                                byte[] requestData = LModbus.getModbusData(Short.parseShort(equip.getGroup()),
                                                                           register.registerType,register.registerAddress,registerNum);
                                
                                LSerial.getInstance().sendSerialToModbus(requestData);
                                LModbus.getModbusCommLock().lock(register, SERIAL_COMM_TIMEOUT_MS);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void sendSystemControl() {
        //CcuLog.d(L.TAG_CCU_DEVICE, "Modbus SendSystemControl");
    }
}
