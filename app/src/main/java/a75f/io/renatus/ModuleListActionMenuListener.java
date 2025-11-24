package a75f.io.renatus;

import static a75f.io.logic.bo.building.definitions.ProfileType.getProfileDescription;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.pcn.PCNUtil;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.renatus.profiles.CopyConfiguration;
import a75f.io.util.ExecutorTask;

class ModuleListActionMenuListener implements MultiChoiceModeListener
{
	
	/**
	 *
	 */
	private final FloorPlanFragment floorPlanActivity;
	ArrayList<Long> seletedModules = new ArrayList<>();
	private ActionMode aMode = null;
	CCUHsApi ccuHsApi = CCUHsApi.getInstance();
	boolean isViewEnabled = true;

	
	/**
	 * @param floorPlanFragment
	 */
	ModuleListActionMenuListener(FloorPlanFragment floorPlanFragment)
	{
		this.floorPlanActivity = floorPlanFragment;
	}
	
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu)
	{
		floorPlanActivity.mModuleListAdapter.setMultiSelectMode(true);
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.action_menu, menu);
		menu.findItem(R.id.renameSelection).setVisible(false);
		seletedModules.clear();
		aMode = mode;
		mode.setTitle("Select Modules");
		return true;
	}
	
	
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item)
	{
		if(floorPlanActivity.getContext() != null && floorPlanActivity.getUserVisibleHint()) {
			switch (item.getItemId()) {
				case R.id.deleteSelection:
					if (true) { //TODO check prefconfigured profiels
						int floorSelectedIndex = floorPlanActivity.mFloorListAdapter.getSelectedPostion();
						deleteSelectedFSV();
						seletedModules.clear();
						floorPlanActivity.refreshScreen();
						/*
						if(floorPlanActivity.floorList.size() == 0 || floorSelectedIndex == -1){
							floorPlanActivity.systemDeviceOnClick();
						}
						*/
						mode.finish(); // Action picked, so close the CAB

					} else {
						Toast.makeText(this.floorPlanActivity
								.getActivity(), "Cannot delete pre configured module", Toast.LENGTH_SHORT)
								.show();
						mode.finish();
					}
					return true;
				case R.id.copyConfiguration:
				{
					String moduleName = determineModuleName();
					CopyConfiguration.Companion.setSelectedConfiguration(seletedModules.get(0).intValue(), moduleName,floorPlanActivity);
					mode.finish();
				}
				default:
					return false;
			}
		}else{
			mode.finish();
			return false;
		}
	}
	
	
	@Override
	public void onDestroyActionMode(ActionMode mode)
	{
		floorPlanActivity.mModuleListAdapter.setMultiSelectMode(false);
		seletedModules.clear();
		aMode = null;
	}
	public void destroyActionBar() {
		if (aMode != null) {
			aMode.finish();
		}
	}
	
	
	private void deleteSelectedFSV()
	{
		for(Long selectedModule : seletedModules)
		{
			L.removeHSDeviceEntities(selectedModule, floorPlanActivity.getSelectedZone().getId());
		}

		ExecutorTask.executeBackground( () -> {
			CCUHsApi.getInstance().syncEntityTree();
			L.saveCCUState();
		});
    }
	public static String profileTypeToCamelCase(String profileType) {
		if (profileType == null || profileType.isEmpty()) {
			return "";
		}

		String[] words = profileType.toLowerCase().split("_");
		StringBuilder camelCase = new StringBuilder();

		for (String word : words) {
			camelCase.append(Character.toUpperCase(word.charAt(0)))
					.append(word.substring(1));
		}

		return camelCase.toString();
	}
	private String determineModuleName() {

		HashMap<String, Object> equip = ccuHsApi.read("zone and not equipRef and  equip and group == \"" + seletedModules.get(0) + "\"");
		HashMap<Object, Object> roomDetails = new HashMap<>();
		if (!equip.isEmpty() && equip.get("roomRef") != null) {
			String roomRef = equip.get("roomRef").toString();
			if (!roomRef.isEmpty()) {
				roomDetails = ccuHsApi.readMapById(roomRef);
			}
		}

		String roomDisplayName = roomDetails.get("dis") != null
				? roomDetails.get("dis").toString()
				: "";

		String siteName = CCUHsApi.getInstance().getSiteName();
		//for modbus ,return the model name ,for other profiles return the display name [RoomName : EquipDisplayName/modelName]
		if (equip.get("modbus") != null){
			return " " + roomDisplayName + ": " + equip.get("model").toString().replaceFirst(siteName + "-", "") + " ";
		}
		else if(equip.get("bacnet") != null){
			return " " + roomDisplayName + ": " +equip.get("modelConfig").toString().split("modelName:")[1].split(",")[0]+ " ";
		} else if(ConnectNodeUtil.Companion.isZoneContainingConnectNodeWithEquips(seletedModules.get(0).toString(), ccuHsApi)) {
			String connectNodeZoneName = ConnectNodeUtil.Companion.getZoneNameByConnectNodeAddress(
					seletedModules.get(0).toString(), ccuHsApi);
			return " " + connectNodeZoneName + ": " + "ConnectNode" + " (" + seletedModules.get(0).intValue() + ") ";
		} else if(PCNUtil.Companion.isZoneContainingPCNWithEquips(seletedModules.get(0).toString(), ccuHsApi)) {
			String pcnZoneName = PCNUtil.Companion.getZoneNameByPCNAddress(
					seletedModules.get(0).toString(), ccuHsApi);
			return " " + pcnZoneName + ": " + "PCN" + " (" + seletedModules.get(0).intValue() + ") ";
		}
		else {
			HashMap<Object,Object>device = ccuHsApi.read(" device and addr == \"" + seletedModules.get(0) + "\"");
			ProfileType profile = CCUUtils.getProfileType((String) equip.get("profile"), seletedModules.get(0).intValue());
			String profileName = getProfileDescription(profile);
			NodeType nodeType = CCUUtils.getNodeType(device);
			String nodeName = profileTypeToCamelCase(nodeType.name());
			String formattedMessage = String.format("%s: %s - %s",
					roomDisplayName, nodeName, profileName);
			return " "+ formattedMessage + " ("+ seletedModules.get(0).intValue()+ ") ";
		}
	}
	
	
	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
	{
		String nodeAddress = ConnectNodeUtil.Companion.extractNodeAddressIfConnectPaired(
				floorPlanActivity.mModuleListAdapter.getItem(position));
		Long smartNodeID = Long.parseLong(nodeAddress);
		if (checked && !seletedModules.contains(smartNodeID))
		{
			seletedModules.add(smartNodeID);
			// No sub equip if connectNode paired
			if(ConnectNodeUtil.Companion.getConnectNodeByNodeAddress(nodeAddress, CCUHsApi.getInstance()).isEmpty() &&
					PCNUtil.Companion.getPcnByNodeAddress(nodeAddress, CCUHsApi.getInstance()).isEmpty()) {
				seletedModules.addAll(HSUtil.getSubEquipPairingAddr(String.valueOf(smartNodeID)));
			}
			floorPlanActivity.mModuleListAdapter.addSelected(position, seletedModules, new ArrayList<>());
		}
		else
		{
			seletedModules.remove(smartNodeID);

			// If modbus sub equip is pending in the selection,when the User click on it ,it will remove  all the equip/sub equips from the selection
			if(!seletedModules.isEmpty()) {
				if ((seletedModules.stream().allMatch(module -> module.intValue() == seletedModules.get(0))) && seletedModules.get(0).intValue() == smartNodeID.intValue()) {
					seletedModules.clear();
					mode.finish();
				}
			}
			floorPlanActivity.mModuleListAdapter.removeSelected(position, seletedModules, new ArrayList<>());
		}
		if(seletedModules.isEmpty())
		{
			mode.finish();
		}
		floorPlanActivity.mModuleListAdapter.notifyDataSetChanged();
		final int checkedCount = seletedModules.size();
		switch (checkedCount)
		{
			case 0:
				mode.setSubtitle(null);
				break;
			case 1:
				mode.setSubtitle("One module selected");
				isViewEnabled = isCopyConfigVisibilityNeeded(smartNodeID);
				mode.getMenu().findItem(R.id.copyConfiguration).setVisible(isViewEnabled);
				mode.getMenu().findItem(R.id.divider).setVisible(isViewEnabled);
				break;
			default:
				mode.setSubtitle("" + checkedCount + " modules selected");
				isViewEnabled = isCopyConfigVisibilityNeeded(smartNodeID);
				mode.getMenu().findItem(R.id.divider).setVisible(isViewEnabled);
				mode.getMenu().findItem(R.id.copyConfiguration).setVisible(isViewEnabled);
				break;
		}
	}
	private boolean isCopyConfigVisibilityNeeded(Long selectedAddress) {
		HashMap<Object, Object> equip = ccuHsApi.read("zone and equip and group == \"" + seletedModules.get(0) + "\"");
		// single module - - visibility true
		if (seletedModules.size() == 1) {
			//Non DM profiles - visibility false
			if (equip.containsKey("smartstat") || equip.containsKey("ti") ||equip.containsKey("dualDuct") || (equip.containsKey("emr") && (equip.containsKey("smartnode") || equip.containsKey("helionode")))) {
				return false;
			}
			HashMap<Object, Object> device = ccuHsApi.readEntity("device and addr == \"" + seletedModules.get(0) + "\"");
			boolean isEmptyPCN = PCNUtil.Companion.isZoneContainingEmptyPCN(device.get(Tags.ROOMREF).toString(), ccuHsApi);
			boolean isEmptyConnectNode = ConnectNodeUtil.Companion.isEmptyConnectNodeDevice(seletedModules.get(0), ccuHsApi);
            return !(isEmptyPCN || isEmptyConnectNode);

        }
		// Non DM profiles -- visibility false
		else if (equip.containsKey("smartstat") || equip.containsKey("ti") || equip.containsKey("dualDuct") || (equip.containsKey("emr") && (equip.containsKey("smartnode") || equip.containsKey("helionode")))) {
			return false;
		}
		// For ModbusSubEquips - visibility true
		else if ((seletedModules.get(0).intValue() == selectedAddress.intValue()) ||(seletedModules.stream().allMatch(module -> module.intValue() == seletedModules.get(0)))) {
			return true;
		}
		// More than one module selected -- visibility false
		return false;
	}

}