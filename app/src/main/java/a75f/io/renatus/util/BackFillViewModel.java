package a75f.io.renatus.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.ccu.renatus.BackFillDuration;
import a75f.io.renatus.R;

public class BackFillViewModel {

    public static ArrayAdapter<String> getBackFillTimeArrayAdapter(Context context) {
        int equipCount = CCUHsApi.getInstance().readAllEntities("equip and (gatewayRef or ahuRef) and not diag").size();
        String[] strings = BackFillDuration.getDisplayNames();
        ArrayList<String> backFillTimeArray = new ArrayList<String>(Arrays.asList(strings));
        return getDynamicBackFillTimeArrayAdapter(context, backFillTimeArray, equipCount);
    }

    private static ArrayAdapter<String> getDynamicBackFillTimeArrayAdapter(Context context, ArrayList<String> backFillTimeArray, int totalZones) {

        return new ArrayAdapter<String>(context, R.layout.spinner_dropdown_item, backFillTimeArray) {
            @Override
            public boolean isEnabled(int position) {
                return position < getMaxNormalRows();
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                Context mContext = getContext();
                LayoutInflater vi = LayoutInflater.from(mContext);
                View v = vi.inflate(R.layout.spinner_dropdown_item, parent, false);
                View row = super.getDropDownView(position, v, parent);
                if (position >= getMaxNormalRows()) {
                    row.setAlpha(0.5F);
                }
                return row;
            }

            private int getMaxNormalRows() {
                if (totalZones <= 6) {
                    return 10;
                } else if (totalZones <= 20) {
                    return 7;
                } else if (totalZones <= 30) {
                    return 6;
                } else if (totalZones <= 40) {
                    return 5;
                } else {
                    return 2;
                }
            }
        };

    }

}
