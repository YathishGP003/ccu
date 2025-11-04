package a75f.io.usbserial

data class BacnetConfig(var serial : String, var baudRate : Int, var sourceAddress : Int, var maxMaster :  Int, var maxFrames : Int, var deviceId : Int)
