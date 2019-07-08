package a75f.io.device.serial;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public enum FirmwareDeviceType_t
{
	REMOTE_TEMPERATURE_SENSOR_DEVICE_TYPE(null, null),
	SMART_NODE_DEVICE_TYPE("SmartNode", "sn_fw/"),
	CONTROL_MOTE_DEVICE_TYPE(null, null),
	ITM_DEVICE_TYPE("itm", "itm_fw/"),
	SMART_STAT_BACK_DEVICE_TYPE(null, null),
	HIA_DEVICE_TYPE(null, null);

	private final String updateFileName;
	private final String updateUrlDirectory;

	private FirmwareDeviceType_t(String updateFileName, String updateUrlDirectory) {
		this.updateFileName = updateFileName;
		this.updateUrlDirectory = updateUrlDirectory;
	}

	public String getUpdateFileName() {
		return updateFileName;
	}

	public String getUpdateUrlDirectory() {
		return updateUrlDirectory;
	}
}
