package a75f.io.device.serial;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public enum FirmwareComponentType_t
{
	REMOTE_TEMPERATURE_SENSOR_DEVICE_TYPE(	null, 			null, 		null),
	SMART_NODE_DEVICE_TYPE(					"SmartNode", 	"sn_fw/", 	"smartnode"),
	CONTROL_MOTE_DEVICE_TYPE(				"CM4", 			"cm4_fw/", 		"cm"),
	ITM_DEVICE_TYPE(						"ITM", 		"itm_fw/", 	"smartstat"),
	SMART_STAT_BACK_DEVICE_TYPE(			null, 			null, 		null),
	HIA_DEVICE_TYPE(						null, 			null, 		null),
	HYPER_STAT_DEVICE_TYPE("HyperStat","hs_fw/","hyperstat"),
	//Firm ware has limitation that helionode device type(8) to be same as firmware component type(7)
	DUMMY_DEVICE_TYPE(null,null,null),
	HELIO_NODE_DEVICE_TYPE("HelioNode", "hn_fw/", 	"helionode"),
	HYPERSTAT_SPLIT_DEVICE_TYPE("HyperStat", "hs_fw/", "hyperstatsplit"),
	CONNECT_MODULE_DEVICE_TYPE("ConnectModule", "connect_fw/", "hyperstatsplit"),
	Reserve1(null,null,null),
	MY_STAT_DEVICE_TYPE("MyStat", "ms_fw/", "mystat");

	private final String updateFileName;
	private final String updateUrlDirectory;
	private final String hsMarkerName;

	FirmwareComponentType_t(String updateFileName, String updateUrlDirectory, String hsMarkerName) {
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
