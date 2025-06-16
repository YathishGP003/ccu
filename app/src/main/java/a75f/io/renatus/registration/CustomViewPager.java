package a75f.io.renatus.registration;

import android.content.Context;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.lang.reflect.Field;

import a75f.io.renatus.SettingsPagerAdapter;
import a75f.io.renatus.StatusPagerAdapter;

public class CustomViewPager extends ViewPager {

    public CustomViewPager(Context context) {
        super(context);
        setMyScroller();
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMyScroller();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }

    //down one is added for smooth scrolling

    private void setMyScroller() {
        try {
            Class<?> viewpager = ViewPager.class;
            Field scroller = viewpager.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            scroller.set(this, new MyScroller(getContext()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class MyScroller extends Scroller {
        public MyScroller(Context context) {
            super(context, new DecelerateInterpolator());
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, 200);
        }
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        // Since the CustomPagerAdapters have their destroyItem method overriden to avoid deletion of fragments while
        // switching between fragments in a tab layout, if the tab layout is switched, the fragments belonging to the
        // previous tab layout will not be destroyed. This will cause the fragments to be retained in memory and cause
        // redundant state issue.
        // To avoid this, we override the setAdapter method to ensure that all views are removed when the new adapter being set is null.
        PagerAdapter currentAdapter = getAdapter();
        if(currentAdapter != null && currentAdapter != adapter) {
            if(currentAdapter instanceof StatusPagerAdapter) {
                ((StatusPagerAdapter) currentAdapter).destroyAllItems(this);
            } else if (currentAdapter instanceof SettingsPagerAdapter) {
                ((SettingsPagerAdapter) currentAdapter).destroyAllItems(this);
            }
        }
        super.setAdapter(adapter);

    }
}