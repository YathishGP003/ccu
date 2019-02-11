package a75f.io.api.haystack;

import android.util.Log;

import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Schedule;

public class VAVScheduler {


    private static final String TAG = "VAVScheduler";
    boolean occupied; // determined by schedule
    private int heatingDeadBand = 0;
    private int coolingDeadBand = 0;



    public static void processEquip(Equip equip, Schedule equipSchedule) {

        Log.i(TAG, "Equip: " + equip);
        Log.i(TAG, "Equip Schedule: " + equipSchedule);
        Occupied cooling = equipSchedule.getCurrentValueForMarker("cooling");
        Occupied heating = equipSchedule.getCurrentValueForMarker("heating");


        Double desiredTemperatureOutput = null;
        Double desiredTemperatureCooling = null;
        Double desiredTemperatureHeating = null;

        if(cooling == null)
        {
            desiredTemp = heating;
        }
        else if(heating == null)
        {
            desiredTemp = cooling;
        }

        if(cooling !=null) setDesiredTemp(equip, cooling);
        if(heating != null) setDesiredTemp(equip, heating);

    }

    private static void setDesiredTemp(Equip equip, Double cooling) {




    }
}
