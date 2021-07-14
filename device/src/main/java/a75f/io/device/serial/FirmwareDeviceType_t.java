package a75f.io.device.serial;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public enum FirmwareDeviceType_t
{
	REMOTE_TEMPERATURE_SENSOR_DEVICE_TYPE(	null, 			null, 		null),
	SMART_NODE_DEVICE_TYPE(					"SmartNode", 	"sn_fw/", 	"smartnode"),
	CONTROL_MOTE_DEVICE_TYPE(				null, 			null, 		null),
	ITM_DEVICE_TYPE(						"itm", 		"itm_fw/", 	"smartstat"),
	SMART_STAT_BACK_DEVICE_TYPE(			null, 			null, 		null),
	HIA_DEVICE_TYPE(						null, 			null, 		null),
	SMART_STAT_V2(						"SmartStatV2", 			"ssv2_fw/", 		"smartstatv2"),
	HYPER_STAT_DEVICE_TYPE("HyperStat","hs_fw/","hyperstat");

	private final String updateFileName;
	private final String updateUrlDirectory;
	private final String hsMarkerName;

	private FirmwareDeviceType_t(String updateFileName, String updateUrlDirectory, String hsMarkerName) {
		this.updateFileName = updateFileName;
		this.updateUrlDirectory = updateUrlDirectory;
		this.hsMarkerName = hsMarkerName;
	}

	public String getUpdateFileName() {
		return updateFileName;
	}

	public String getUpdateUrlDirectory() {
		return updateUrlDirectory;
	}

	public String getHsMarkerName() { return hsMarkerName; }
}
