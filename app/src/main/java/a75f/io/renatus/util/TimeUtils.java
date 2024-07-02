package a75f.io.renatus.util;

public class TimeUtils {

    public static String valToTime(int value) {
        StringBuilder mBuilder = new StringBuilder();
        java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);
        Object[] mArgs = new Object[2];
        mArgs[0] = value/4;
        mArgs[1] = (value%4)*(15);
        mBuilder.delete(0, mBuilder.length());
        mFmt.format("%02d:%02d", mArgs);
        return mFmt.toString();
    }

}
