package a75f.io.renatus;

import android.view.View;

public class ZoneEquip {
    public View textViewModule;
    public Boolean isTemperatureProfile;
    public ZoneEquip(View textViewModule, Boolean isTemperatureProfile) {
        this.textViewModule = textViewModule;
        this.isTemperatureProfile = isTemperatureProfile;
    }
    public View getTextViewModule() {
        return textViewModule;
    }

    public Boolean getIsTemperatureProfile() {
        return isTemperatureProfile;
    }
}

