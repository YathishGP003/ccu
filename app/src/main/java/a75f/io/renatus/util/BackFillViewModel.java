package a75f.io.renatus.util;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.Globals;
import a75f.io.logic.bo.building.BackfillPref;
import a75f.io.logic.bo.building.BackfillUtil;
import a75f.io.renatus.R;

public class BackFillViewModel {

    static BackfillPref backfillPref = new BackfillPref();

    private static final int BACKFIELD_DEFAULT_DURATION_INDEX = 6;
    private static final int BACKFIELD_DEFAULT_DURATION = 24;

    public static ArrayAdapter<String> getBackFillTimeArrayAdapter(Context context) {
        String[] strings = BackFillDuration.getDisplayNames();
        ArrayList<String> backFillTimeArray = new ArrayList<>(Arrays.asList(strings));
        return new ArrayAdapter<>(context, R.layout.spinner_dropdown_item, backFillTimeArray);
    }

    public static int backfieldTimeSelectedValue() {

        String backfillQuery = "backfill and duration";
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        if (!ccuHsApi.readEntity(backfillQuery).isEmpty()) {
            Double value = ccuHsApi.readDefaultVal(backfillQuery);
            return BackFillDuration.getIndex(BackFillDuration.toIntArray(),value.intValue(), BACKFIELD_DEFAULT_DURATION);
        } else {
            return BACKFIELD_DEFAULT_DURATION_INDEX;
        }
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
            BackfillUtil.updateBackfillDuration(currentBackFillTime);
        }
    }

    public static int getBackFillDuration() {
        return backfillPref.getBackFillTimeDuration();
    }

    public static void generateToastMessage(View toastLayout) {
        Toast toast = new Toast(Globals.getInstance().getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 50, 50);
        toast.setView(toastLayout);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    public enum BackFillDuration {

        NONE("None"),
        ONE_HOUR("1 Hr"),
        TWO_HOURS("2 Hrs"),
        THREE_HOURS("3 Hrs"),
        SIX_HOURS("6 Hrs"),
        TWELVE_HOURS("12 Hrs"),
        TWENTY_FOUR_HOURS("24 Hrs"),
        FORTY_EIGHT_HOURS("48 Hrs"),
        SEVENTY_TWO_HOURS("72 Hrs");

        final String displayName;

        BackFillDuration(String str) {
            displayName = str;
        }

        public static String[] getDisplayNames() {
            BackFillDuration[] values = BackFillDuration.values();
            String[] displayNames = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                displayNames[i] = values[i].displayName;
            }
            return displayNames;
        }

        public static int[] toIntArray() {
            int[] intValues = new int[values().length];
            for (int i = 0; i < values().length; i++) {
                String stringValue = values()[i].displayName;
                if (stringValue.equals("None")) {
                    intValues[i] = 0;
                } else {
                    intValues[i] = Integer.parseInt(stringValue.split(" ")[0]);
                }
            }
            return intValues;
        }

        public static int getIndex(int[] array,int selectedValue, int defaultVal) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == selectedValue) {
                    return i;
                }
            }
            return defaultVal;
        }
    }
}