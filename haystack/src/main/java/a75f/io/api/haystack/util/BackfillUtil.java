package a75f.io.api.haystack.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;

public class BackfillUtil {

    public static void setBackFillDuration(Context context) {
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        int equipCount = ccuHsApi.readAllEntities("equip and (gatewayRef or ahuRef) and not diag and not config ").size();
        boolean backFillTimeChange = false;

        Map<Integer, Double> thresholdMap = new HashMap<>();
        thresholdMap.put(40, 1.0);
        thresholdMap.put(30, 6.0);
        thresholdMap.put(20, 12.0);
        thresholdMap.put(6, 24.0);

        double currentBackFillTime = ccuHsApi.readDefaultVal("backfill and duration");
        // if the backfill value is zero
        if (currentBackFillTime == 0) {
            if (equipCount > 40) {
                currentBackFillTime = 1.0;
            } else if (equipCount > 30) {
                currentBackFillTime = 6.0;
            } else if (equipCount > 20) {
                currentBackFillTime = 12.0;
            } else if (equipCount > 6) {
                currentBackFillTime = 24.0;
            }
            updateBackfillDuration(currentBackFillTime, context);
            return;
        }

        for (Map.Entry<Integer, Double> entry : thresholdMap.entrySet()) {
            int threshold = entry.getKey();
            double thresholdValue = entry.getValue();
            if (equipCount > threshold && currentBackFillTime > thresholdValue) {
                currentBackFillTime = thresholdValue;
                backFillTimeChange = true;
            }
        }

        if (backFillTimeChange) {
            updateBackfillDuration(currentBackFillTime, context);
        }
    }
    public static void updateBackfillDuration(double currentBackFillTime, Context context) {
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        String backfillQuery = "backfill and duration";
        int[] backfillDurationArray = new int[]{0, 1, 2, 3, 6, 12, 24, 48, 72};

        ccuHsApi.writeDefaultVal(backfillQuery, currentBackFillTime);
        CcuLog.d(Tags.BACKFILL,"Backfill duration updated to: " + currentBackFillTime);
        saveBackfillConfig((int) currentBackFillTime, Arrays.binarySearch(backfillDurationArray, (int) currentBackFillTime), context);
    }
    public static void saveBackfillConfig(int backfillTimeDuration, int backfillTimeSpSelected, Context context) {
        String BACKFILL_CACHE = "backfill_cache";
        String BACKFILL_TIME_DURATION = "backFillTimeDuration";
        String BACKFILL_TIME_SP_SELECTED = "backFillTimeSpSelected";
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                BACKFILL_CACHE,
                Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(BACKFILL_TIME_DURATION, backfillTimeDuration);
        editor.putInt(BACKFILL_TIME_SP_SELECTED, backfillTimeSpSelected);
        editor.apply();
    }
}
