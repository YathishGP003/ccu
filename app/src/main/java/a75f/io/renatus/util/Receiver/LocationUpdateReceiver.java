package a75f.io.renatus.util.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import a75f.io.renatus.WeatherDataDownloadService;

public class LocationUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Update weather data using new location details.
        WeatherDataDownloadService.getWeatherData();
    }
}
