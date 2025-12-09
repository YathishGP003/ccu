package a75f.io.renatus.ui.zonescreen.tempprofiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Schedule
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.tuners.BuildingTunerCache
import a75f.io.logic.util.isOfflineMode
import a75f.io.renatus.RenatusLandingActivity
import a75f.io.renatus.schedules.ScheduleGroupFragment
import a75f.io.renatus.ui.zonescreen.EDIT_SCHEDULE
import a75f.io.renatus.ui.zonescreen.EDIT_SPECIAL_SCHEDULE
import a75f.io.renatus.ui.zonescreen.EDIT_VACATION_SCHEDULE
import a75f.io.renatus.ui.zonescreen.SCHEDULE
import a75f.io.renatus.ui.zonescreen.VIEW_SCHEDULE
import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import a75f.io.renatus.ui.zonescreen.tempprofiles.viewmodel.TempProfileViewModel
import a75f.io.renatus.util.CCUUiUtil
import a75f.io.renatus.util.SeekArc
import androidx.fragment.app.FragmentManager
import org.apache.commons.lang3.StringUtils
import java.util.Objects


fun getScheduleList(): List<String> {
    val scheduleArray = ArrayList<String>()
    scheduleArray.add("Zone Schedule")
    scheduleArray.add("Shared Schedule")

    val hasImage = ArrayList<Boolean>()
    hasImage.add(false)
    hasImage.add(false)

    val namedSchedules = CCUHsApi.getInstance().allNamedSchedules
    if (namedSchedules.isNotEmpty()) {
        for (nameSched in namedSchedules) {
            val namedScheduleDis = Objects.requireNonNull(nameSched["dis"]).toString()
            if (nameSched["default"] != null) {
                scheduleArray.add(
                    CCUUtils.getTruncatedString(
                        "Default - " + CCUHsApi.getInstance().siteName,
                        25,
                        0,
                        25
                    )
                )
                hasImage.add(true)
            } else if (namedScheduleDis.length > 25) {
                scheduleArray.add(
                    CCUUtils.getTruncatedString(
                        Objects.requireNonNull(
                            nameSched["dis"].toString()
                        ), 25, 0, 25
                    )
                )
                hasImage.add(true)
            } else {
                scheduleArray.add(Objects.requireNonNull(nameSched["dis"]).toString())
                hasImage.add(true)
            }
        }
    } else {
        scheduleArray.add("No Shared Schedule available")
        hasImage.add(false)
    }

    return scheduleArray
}

fun getNamedSchedulePosition(namedSchedules: List<HashMap<Any, Any>>, equipId: String): Int {
    var position = 0
    for (a in namedSchedules) {
        if ((Objects.requireNonNull<Any>(Objects.requireNonNull<Any>(a.get("id"))).toString()
                .substring(1)) == Schedule.getScheduleByEquipId(equipId).id
        ) {
            position = namedSchedules.indexOf(a) + 2
        }
    }

    return position
}

fun refreshSchedules(
    tempProfileViewModels: List<TempProfileViewModel>
) {
    if (tempProfileViewModels.isNotEmpty()) {
        tempProfileViewModels[0].pointValueChangeListener?.refreshSchedules()
    }
}

interface OnScheduleChangeListener {
    fun onScheduleSelected(position: Int)
}

interface OnScheduleEditClickListener {
    fun onScheduleEditClick(position: Int)
}

interface OnScheduleViewClickListener {
    fun onScheduleViewClick(position: Int)
}

interface OnSpecialScheduleEditClickListener {
    fun onSpecialScheduleEditClick(position: Int)
}

interface OnVacationScheduleEditClickListener {
    fun onVacationScheduleEditClick(position: Int)
}

fun editSchedule(
    mSchedule: Schedule, zoneId: String, equipId: Array<String>,
    childFragmentManager2: FragmentManager, isSpecial: Boolean, viewModels: ArrayList<TempProfileViewModel>
) {
    val schedule = CCUHsApi.getInstance().getScheduleById(mSchedule.id)

    if (isOfflineMode() && schedule.isNamedSchedule && !isSpecial) {
        val selectedTab = RenatusLandingActivity.mTabLayout.getTabAt(3)
        selectedTab!!.select()
    } else {
        val scheduleGroupFragment = if (isSpecial) {
            ScheduleGroupFragment().showSpecialScheduleLayout(zoneId, mSchedule)
        } else {
            ScheduleGroupFragment(schedule, schedule.scheduleGroup)
        }

        scheduleGroupFragment.setTempProfileViewModels(viewModels)

        childFragmentManager2.beginTransaction()
        scheduleGroupFragment.show(childFragmentManager2, "dialog")
    }
}


fun doCallBack(
    point: Any,
    position: Int,
    scheduleChangeListener: OnScheduleChangeListener,
    scheduleViewClickListener: OnScheduleViewClickListener,
    scheduleEditClickListener: OnScheduleEditClickListener,
    specialScheduleEditClickListener: OnSpecialScheduleEditClickListener,
    vacationScheduleEditClickListener: OnVacationScheduleEditClickListener,
) {
    if (point is HeaderViewItem) {
        when (point.disName) {
            SCHEDULE -> {
                scheduleChangeListener.onScheduleSelected(position)
            }

            VIEW_SCHEDULE -> {
                scheduleViewClickListener.onScheduleViewClick(position)
            }

            EDIT_SCHEDULE -> {
                scheduleEditClickListener.onScheduleEditClick(position)
            }

            EDIT_SPECIAL_SCHEDULE -> {
                specialScheduleEditClickListener.onSpecialScheduleEditClick(position)
            }

            EDIT_VACATION_SCHEDULE -> {
                vacationScheduleEditClickListener.onVacationScheduleEditClick(position)
            }
        }
    } else {
        CcuLog.d("scheduleUtil", "Not a HeaderViewItem")
    }
}

fun updateDesiredCurrentTemp(openEquip: Equip, seekArc: SeekArc) {
    val coolingUpperLimitVal: Float
    val heatUpperLimitVal: Float
    val coolingLowerLimitVal: Float
    val heatLowerLimitVal: Float
    var heatingDesired: Float
    var coolingDesired: Float
    var heatingDeadBand: Float
    var coolingDeadBand: Float
    var currentTemp = 0.0
    var avgTemp = 0f

    val roomRefZone = StringUtils.prependIfMissing(openEquip.roomRef, "@")
    val buildingLimitMin = BuildingTunerCache.getInstance().buildingLimitMin.toFloat()
    val buildingLimitMax = BuildingTunerCache.getInstance().buildingLimitMax.toFloat()
    heatUpperLimitVal = CCUHsApi.getInstance()
        .readPointPriorityValByQuery("schedulable and point and limit and user and max and heating and roomRef == \"$roomRefZone\"")
        .toFloat()
    heatLowerLimitVal = CCUHsApi.getInstance()
        .readPointPriorityValByQuery("schedulable and point and limit and user and min and heating and roomRef == \"$roomRefZone\"")
        .toFloat()
    coolingLowerLimitVal = CCUHsApi.getInstance()
        .readPointPriorityValByQuery("schedulable and point and limit and user and min and cooling and roomRef == \"$roomRefZone\"")
        .toFloat()
    coolingUpperLimitVal = CCUHsApi.getInstance()
        .readPointPriorityValByQuery("schedulable and point and limit and user and max and cooling and roomRef == \"$roomRefZone\"")
        .toFloat()
    val isScheduleSlotsAvailable =
        CCUHsApi.getInstance().isScheduleSlotExitsForRoom(openEquip.getId())

    if (isScheduleSlotsAvailable) {
        val unOccupiedSetback = CCUHsApi.getInstance().getUnoccupiedSetback(openEquip.getId())
        coolingDesired = CCUUtils.DEFAULT_COOLING_DESIRED + unOccupiedSetback.toFloat()
        heatingDesired = CCUUtils.DEFAULT_HEATING_DESIRED - unOccupiedSetback.toFloat()
    } else {
        if (CCUUiUtil.isDomainEquip(openEquip.getId(), "equip")) {
            coolingDesired =
                CCUUiUtil.readPriorityValByRoomRef(DomainName.desiredTempCooling, roomRefZone)
                    .toFloat()
            heatingDesired =
                CCUUiUtil.readPriorityValByRoomRef(DomainName.desiredTempHeating, roomRefZone)
                    .toFloat()
        } else {
            coolingDesired = CCUHsApi.getInstance()
                .readPointPriorityValByQuery("point and temp and desired and cooling and roomRef == \"$roomRefZone\"")
                .toFloat()
            heatingDesired = CCUHsApi.getInstance()
                .readPointPriorityValByQuery("point and temp and desired and heating and roomRef == \"$roomRefZone\"")
                .toFloat()
        }
    }
    heatingDeadBand = CCUHsApi.getInstance()
        .readPointPriorityValByQuery("heating and deadband and zone and not multiplier and roomRef == \"$roomRefZone\"")
        .toFloat()
    coolingDeadBand = CCUHsApi.getInstance()
        .readPointPriorityValByQuery("cooling and deadband and zone and not multiplier and roomRef == \"$roomRefZone\"")
        .toFloat()


    val modeType =
        CCUHsApi.getInstance()
            .readHisValByQuery("zone and hvacMode and roomRef == \"${roomRefZone}\"")
            .toInt()


    var noTempSensor = 0;
    val equips = CCUHsApi.getInstance()
        .readAllEntities("equip and zone and roomRef ==\"${roomRefZone}\"")
    for (equip in equips) {
        val updatedEquip = Equip.Builder().setHashMap(equip).build()
        val statusVal =
            CCUUiUtil.readHisValByEquipRef(DomainName.equipStatus, updatedEquip.id).toInt()
        if (statusVal != ZoneState.TEMPDEAD.ordinal && statusVal != ZoneState.RFDEAD.ordinal) {
            currentTemp += CCUHsApi.getInstance().readHisValByQuery(
                "domainName == \"" + DomainName.currentTemp +
                        "\" and equipRef == \"" + updatedEquip.id + "\""
            ).toInt()
        } else {
            noTempSensor++
        }
    }

    if(equips.isNotEmpty()) {
            avgTemp = (currentTemp / (equips.size - noTempSensor)).toFloat()
    }

    if (heatingDesired.toDouble() != 0.0 && coolingDesired.toDouble() != 0.0)
        seekArc.setData(
            seekArc.isDetailedView,
            buildingLimitMin,
            buildingLimitMax,
            heatLowerLimitVal,
            heatUpperLimitVal,
            coolingLowerLimitVal,
            coolingUpperLimitVal,
            heatingDesired,
            coolingDesired,
            (Math.round(avgTemp * 10.0) / 10.0).toFloat(),
            heatingDeadBand,
            coolingDeadBand,
            modeType
        );
}