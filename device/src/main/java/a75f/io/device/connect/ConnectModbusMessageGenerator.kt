package a75f.io.device.connect

import a75f.io.device.cm.SERIAL_COMM_SCALE
import a75f.io.domain.devices.ConnectDevice
import a75f.io.domain.equips.ConnectModuleEquip
import java.nio.ByteBuffer

fun getUniversalInputMappingConfig(connectEquip: ConnectModuleEquip) : ByteArray {
    val byteBuffer = ByteBuffer.allocate(UNIN_MAPPING_CONFIG_REG_COUNT * 2)
    byteBuffer.putShort(connectEquip.universalIn1Association.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.universalIn2Association.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.universalIn3Association.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.universalIn4Association.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.universalIn5Association.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.universalIn6Association.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.universalIn7Association.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.universalIn8Association.readDefaultVal().toInt().toShort())
    return byteBuffer.array()
}

fun getRelayMappingConfig(connectEquip: ConnectModuleEquip) : ByteArray {
    val byteBuffer = ByteBuffer.allocate(RELAY_MAPPING_CONFIG_REG_COUNT * 2)
    byteBuffer.putShort(connectEquip.relay1OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.relay2OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.relay3OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.relay4OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.relay5OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.relay6OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.relay7OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.relay8OutputAssociation.readDefaultVal().toInt().toShort())
    return byteBuffer.array()
}

fun getAnalogOutMappingConfig(connectEquip: ConnectModuleEquip) : ByteArray {
    val byteBuffer = ByteBuffer.allocate(AOUT_MAPPING_CONFIG_REG_COUNT * 2)
    byteBuffer.putShort(connectEquip.analog1OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.analog2OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.analog3OutputAssociation.readDefaultVal().toInt().toShort())
    byteBuffer.putShort(connectEquip.analog4OutputAssociation.readDefaultVal().toInt().toShort())
    return byteBuffer.array()
}

fun getRelayOutputMappedValues(device : ConnectDevice) : ByteArray {
    val byteBuffer = ByteBuffer.allocate(RELAY_MAPPED_VAL_REG_COUNT * 2)
    byteBuffer.putShort(device.relay1.readHisVal().toInt().toShort())
    byteBuffer.putShort(device.relay2.readHisVal().toInt().toShort())
    byteBuffer.putShort(device.relay3.readHisVal().toInt().toShort())
    byteBuffer.putShort(device.relay4.readHisVal().toInt().toShort())
    byteBuffer.putShort(device.relay5.readHisVal().toInt().toShort())
    byteBuffer.putShort(device.relay6.readHisVal().toInt().toShort())
    byteBuffer.putShort(device.relay7.readHisVal().toInt().toShort())
    byteBuffer.putShort(device.relay8.readHisVal().toInt().toShort())
    return byteBuffer.array()
}

fun getAnalogOutputMappedValues(device : ConnectDevice) : ByteArray {
    val byteBuffer = ByteBuffer.allocate(AOUT_MAPPED_VAL_REG_COUNT * 2)
    byteBuffer.putFloat((device.analog1Out.readHisVal()/SERIAL_COMM_SCALE).toFloat())
    byteBuffer.putFloat((device.analog2Out.readHisVal()/SERIAL_COMM_SCALE).toFloat())
    byteBuffer.putFloat((device.analog3Out.readHisVal()/SERIAL_COMM_SCALE).toFloat())
    byteBuffer.putFloat((device.analog4Out.readHisVal()/SERIAL_COMM_SCALE).toFloat())
    return byteBuffer.array()
}
