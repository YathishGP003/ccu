package a75f.io.renatus;

import android.app.Activity;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class LayoutHelper
{
    public static int width;
    public static int height;
    public static Activity c;
    private static int openItems;
    private static int totalItems;
    private static ArrayList<Integer> rowHeight;
    private static  Boolean lcmdab;


    public LayoutHelper(Activity c) {
        this.c = c;
        rowHeight = new ArrayList<>();
        rowHeight.clear();
    }

    public static void getListViewSize(ListView myListView, ListAdapter adapter, int open, int total, Boolean lcm) {
        openItems = open;
        totalItems = total;
        getScreenDimen();
        ListAdapter myListAdapter = null;
        lcmdab = lcm;
        if (adapter == null) {
            myListAdapter = myListView.getAdapter();
        } else {
            myListAdapter = adapter;
        }
        if (myListAdapter == null) {
            //do nothing return null
            return;
        }
        //set listAdapter in loop for getting final size
        double totalHeight = 0;
        for (int size = 0; size < myListAdapter.getCount(); size++) {
            View listItem = myListAdapter.getView(size, null, myListView);
            listItem.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST), 0);
            if (adapter == null) {
                totalHeight += listItem.getMeasuredHeight();
                if (!rowHeight.contains(listItem.getMeasuredHeight())) {
                    rowHeight.add(new Integer(listItem.getMeasuredHeight()));
                }
            } else {
                if (totalHeight == 0) {
                    if(!lcmdab) {
                        totalHeight = ((openItems) * dpToPx(100)) + ((totalItems - openItems + 2) * dpToPx(45));
                    }else{
                        totalHeight = ((openItems+1) * dpToPx(100)) + ((totalItems - openItems + 4) * dpToPx(45));
                    }
                }
            }

        }
        //setting listview item in adapter
        ViewGroup.LayoutParams params = myListView.getLayoutParams();
        params.height = (int) (totalHeight + (myListView.getDividerHeight() * (myListAdapter.getCount() - 1)));

        if (PreferenceManager.getDefaultSharedPreferences(c).getBoolean("weather_switch", false)) {
            params.width = (int) ((0.75) * width);
        } else {
            params.width = width;
        }
        myListView.setLayoutParams(params);
        // print height of adapter on log

    }

    public static void getScreenDimen() {
        Display display = c.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
    }

    public static int dpToPx(int dp) {
        DisplayMetrics displayMetrics = c.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
}


