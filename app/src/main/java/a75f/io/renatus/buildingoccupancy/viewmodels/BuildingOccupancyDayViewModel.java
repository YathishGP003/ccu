package a75f.io.renatus.buildingoccupancy.viewmodels;


import a75f.io.api.haystack.schedule.BuildingOccupancy;
import a75f.io.renatus.schedules.ScheduleUtil;

public class BuildingOccupancyDayViewModel {
    private static final String ADD_BUILDING_OCCUPANCY_TITLE = "ADD BUILDING OCCUPANCY";
    private static final String EDIT = "EDIT";
    private static final String SPACE = " ";
    private static final String OB = "(";
    private static final String CB = ")";
    private static final String TO = "TO";
    private static final String COLON = ":";
    private static final String ZERO = "0";

    public String constructBuildingOccupancyTitle(BuildingOccupancy.Days mDay) {
        String title = ADD_BUILDING_OCCUPANCY_TITLE;
        if (mDay != null) {
            StringBuffer stringBuffer = new StringBuffer(EDIT);
            stringBuffer.append(SPACE);
            stringBuffer.append(ScheduleUtil.getDayString(mDay.getDay() + 1).toUpperCase());
            stringBuffer.append(SPACE);
            stringBuffer.append(OB);
            stringBuffer.append(constructTime(mDay));
            stringBuffer.append(CB);
            return stringBuffer.toString();
        }
        return title;
    }

    private String constructTime(BuildingOccupancy.Days mDay) {

        StringBuffer timeString = new StringBuffer();
        if (mDay.getSthh() < 10) {
            timeString.append(ZERO);
        }
        timeString.append(mDay.getSthh());
        timeString.append(COLON);
        if (mDay.getStmm() < 10) {
            timeString.append(ZERO);
        }
        timeString.append(mDay.getStmm());

        timeString.append(SPACE);
        timeString.append(TO);
        timeString.append(SPACE);
        if (mDay.getEthh() < 10) {
            timeString.append(ZERO);
        }
        timeString.append(mDay.getEthh());
        timeString.append(COLON);
        if (mDay.getEtmm() < 10) {
            timeString.append(ZERO);
        }
        timeString.append(mDay.getEtmm());

        return timeString.toString();
    }
}

