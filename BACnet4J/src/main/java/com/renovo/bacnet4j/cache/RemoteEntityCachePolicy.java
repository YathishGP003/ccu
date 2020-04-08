
package com.renovo.bacnet4j.cache;

import org.threeten.bp.Duration;

import com.renovo.bacnet4j.LocalDevice;

public interface RemoteEntityCachePolicy {
    Object prepareState(LocalDevice localDevice);

    boolean hasExpired(LocalDevice localDevice, Object state);

    public static final RemoteEntityCachePolicy NEVER_CACHE = new NeverCache();
    public static final RemoteEntityCachePolicy EXPIRE_5_SECONDS = new TimedExpiry(Duration.ofSeconds(5));
    public static final RemoteEntityCachePolicy EXPIRE_1_MINUTE = new TimedExpiry(Duration.ofMinutes(1));
    public static final RemoteEntityCachePolicy EXPIRE_15_MINUTES = new TimedExpiry(Duration.ofMinutes(15));
    public static final RemoteEntityCachePolicy EXPIRE_1_HOUR = new TimedExpiry(Duration.ofHours(1));
    public static final RemoteEntityCachePolicy EXPIRE_4_HOURS = new TimedExpiry(Duration.ofHours(4));
    public static final RemoteEntityCachePolicy EXPIRE_1_DAY = new TimedExpiry(Duration.ofDays(1));
    public static final RemoteEntityCachePolicy NEVER_EXPIRE = new NeverExpire();

    public static class TimedExpiry implements RemoteEntityCachePolicy {
        private final long duration;

        public TimedExpiry(final Duration duration) {
            this.duration = duration.toMillis();
        }

        @Override
        public Object prepareState(final LocalDevice localDevice) {
            return localDevice.getClock().millis() + duration;
        }

        @Override
        public boolean hasExpired(final LocalDevice localDevice, final Object state) {
            return localDevice.getClock().millis() > (Long) state;
        }

        @Override
        public String toString() {
            return "TimedExpiry [duration=" + duration + "]";
        }
    }

    public static class NeverExpire implements RemoteEntityCachePolicy {
        @Override
        public Object prepareState(final LocalDevice localDevice) {
            return null;
        }

        @Override
        public boolean hasExpired(final LocalDevice localDevice, final Object state) {
            return false;
        }
    }

    public static class NeverCache implements RemoteEntityCachePolicy {
        @Override
        public Object prepareState(final LocalDevice localDevice) {
            return null;
        }

        @Override
        public boolean hasExpired(final LocalDevice localDevice, final Object state) {
            return true;
        }
    }
}
