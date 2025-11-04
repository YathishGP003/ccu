package a75f.io.usbserial

data class ModbusConfig(var serial : String , var baudRate : Int, var parity : Int, var dataBits : Int, var stopBits : Int)
