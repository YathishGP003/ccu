package a75f.io.renatus.views;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import a75f.io.renatus.R;

public class CustomCCUSwitch extends SwitchCompat {

    public CustomCCUSwitch(Context context) {
        super(context);
        init();
    }

    public CustomCCUSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomCCUSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int thumbDrawableResourceId = R.drawable.custom_75f_thumb;
        setThumbDrawable(ContextCompat.getDrawable(getContext(), thumbDrawableResourceId));
        int trackDrawableResourceId = R.drawable.custom_75f_track;
        setTrackDrawable(ContextCompat.getDrawable(getContext(), trackDrawableResourceId));
    }
}