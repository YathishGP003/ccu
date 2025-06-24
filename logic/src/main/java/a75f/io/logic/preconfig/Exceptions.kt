package a75f.io.logic.preconfig

class InvalidPreconfigurationDataException(message: String) : RuntimeException(message)
class UnsupportedTimeZoneException(message: String) : RuntimeException(message)
class UnsupportedPreconfigurationException(message: String) : RuntimeException(message)
class InvalidStagesException(message: String) : RuntimeException(message)
class ModbusEquipCreationException(message: String) : RuntimeException(message)
class LowCodeDownloadException(message: String) : RuntimeException(message)