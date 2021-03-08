package a75f.io.renatus.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by mahesh on 25-07-2019.
 */
public class RangeDecorator implements DayViewDecorator {

    private HashSet<CalendarDay> dates;
    private Drawable drawable;

    public RangeDecorator(Context context, int resId, Collection<CalendarDay> dates) {
        drawable = ContextCompat.getDrawable(context, resId);
        this.dates = new HashSet<>(dates);

    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setSelectionDrawable(drawable);
    }
}
