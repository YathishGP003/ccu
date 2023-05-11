package a75f.io.logic.bo.building;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.ccu.renatus.BackFillDuration;
import a75f.io.logic.tuners.TunerConstants;

public class BackFillUtil {

    public static void addBackFillDurationPointIfNotExists(CCUHsApi ccuHsApi) {

            BackfillPref backfillPref = new BackfillPref();
            HashMap<Object, Object> siteMap = ccuHsApi.readEntity(Tags.SITE);
            HashMap<Object, Object> equipMap = ccuHsApi.readEntity("equip and system");
            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            String equipref = equip.getId();
            String siteRef = Objects.requireNonNull(siteMap.get(Tags.ID)).toString();
            String tz = siteMap.get("tz").toString();
            String equipDis = siteMap.get("dis").toString() + "-SystemEquip";

        if(!verifyBackFillPointAvailability(equipref)) {

            Point backFillDurationPoint = new Point.Builder().setDisplayName(equipDis + "-" + "backFillDuration")
                    .setSiteRef(siteRef).setEquipRef(equipref).addMarker("sp").addMarker("system")
                    .addMarker("backfill").addMarker("writable").addMarker("config").addMarker("duration")
                    .addMarker("ventilation").setEnums(Arrays.toString(BackFillDuration.toIntArray()))
                    .setTz(tz).setUnit("hrs")
                    .build();

            int defaultBackFillDurationSelected = backfillPref.getBackFillTimeDuration();
            String backFillDurationPointId = CCUHsApi.getInstance().addPoint(backFillDurationPoint);
            CCUHsApi.getInstance().writePointForCcuUser(backFillDurationPointId, TunerConstants.UI_DEFAULT_VAL_LEVEL, (double) defaultBackFillDurationSelected,0);
            backfillPref.saveBackfillConfig(defaultBackFillDurationSelected, Arrays.binarySearch(BackFillDuration.toIntArray(), defaultBackFillDurationSelected));
        }
    }

    private static boolean verifyBackFillPointAvailability(String equipRef){
        ArrayList<HashMap<Object, Object>> backFillDuration = CCUHsApi.getInstance().readAllEntities("point and system and backfill and duration and equipRef == \"" + equipRef + "\"");
        return !backFillDuration.isEmpty();
    }

    public static void setBackFillDuration() {
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        int equipCount = ccuHsApi.readAllEntities("equip and (gatewayRef or ahuRef) and not diag").size();
        boolean backFillTimeChange = false;

        Map<Integer, Double> thresholdMap = new HashMap<>();
        thresholdMap.put(40, 1.0);
        thresholdMap.put(30, 6.0);
        thresholdMap.put(20, 12.0);
        thresholdMap.put(6, 24.0);

        double currentBackFillTime = ccuHsApi.readDefaultVal("backfill and duration");

        for (Map.Entry<Integer, Double> entry : thresholdMap.entrySet()) {
            int threshold = entry.getKey();
            double thresholdValue = entry.getValue();
            if (equipCount > threshold && currentBackFillTime > thresholdValue) {
                currentBackFillTime = thresholdValue;
                backFillTimeChange = true;
            }
        }

        if (backFillTimeChange) {
            ccuHsApi.writeDefaultVal("backfill and duration", currentBackFillTime);
            updateBackfillDuration(currentBackFillTime);
        }
    }
    public static void updateBackfillDuration(double currentBackFillTime) {
        BackfillPref backfillPref = new BackfillPref();
        backfillPref.saveBackfillConfig((int) currentBackFillTime, Arrays.binarySearch(BackFillDuration.toIntArray(), (int) currentBackFillTime));
    }


}

