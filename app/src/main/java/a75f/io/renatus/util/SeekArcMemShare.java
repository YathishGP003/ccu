package a75f.io.renatus.util;

import android.util.Log;
import android.view.View;

public class SeekArcMemShare
{
    public static SeekArc.OnTemperatureChangeListener onTemperatureChangeListener = new SeekArc.OnTemperatureChangeListener()
    {
        @Override
        public void onTemperatureChange(SeekArc seekArc, float coolingDesiredTemp, float heatingDesiredTemp, boolean syncToHaystack)
        {
            //String seekArcId = (String) seekArc.getTag();
            //Log.i("SEEKARC", "ID: " + seekArcId + " coolingDesired: " + coolingDesiredTemp + " heatingDesired: " + heatingDesiredTemp);
        }
    };


    public static View.OnClickListener mSeekArcOnClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            String tag = (String) v.getTag();

            Log.i("SEEKARC", "OnClick ID: " + tag);
        }
    };
}


