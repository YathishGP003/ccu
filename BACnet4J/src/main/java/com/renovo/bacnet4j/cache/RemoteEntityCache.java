
package com.renovo.bacnet4j.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.LocalDevice;

public class RemoteEntityCache<K, T> {
    static final Logger LOG = LoggerFactory.getLogger(RemoteEntityCache.class);

    /**
     * The local device, as usual.
     */
    private final LocalDevice localDevice;

    /**
     * The cache of entities, wrapped with their policy state.
     */
    private final Map<K, CachedRemoteEntity<T>> cache = new HashMap<>();

    public RemoteEntityCache(final LocalDevice localDevice) {
        this.localDevice = localDevice;
    }

    public T getCachedEntity(final K key) {
        // Check for a cached instance
        synchronized (cache) {
            final CachedRemoteEntity<T> cre = cache.get(key);
            if (cre != null && !cre.hasExpired(localDevice)) {
                LOG.debug("Returning cached entity: {}", key);
                return cre.getEntity();
            }

            if (cre != null) {
                // The value has expired. Remove it from the cache.
                cache.remove(key);
            }

            return null;
        }
    }

    public T getCachedEntity(final Predicate<T> predicate) {
        synchronized (cache) {
            final Iterator<CachedRemoteEntity<T>> iter = cache.values().iterator();
            while (iter.hasNext()) {
                final CachedRemoteEntity<T> cre = iter.next();
                if (cre.hasExpired(localDevice)) {
                    iter.remove();
                } else if (predicate.test(cre.getEntity())) {
                    return cre.getEntity();
                }
            }
            return null;
        }
    }

    public void putEntity(final K key, final T value, final RemoteEntityCachePolicy policy) {
        synchronized (cache) {
            cache.put(key, new CachedRemoteEntity<>(localDevice, value, policy));
        }
    }

    public T removeEntity(final K key) {
        synchronized (cache) {
            final CachedRemoteEntity<T> cp = cache.remove(key);
            if (cp == null)
                return null;
            return cp.getEntity();
        }
    }

    public List<T> getEntities() {
        synchronized (cache) {
            return cache.values().stream().map((e) -> e.getEntity()).collect(Collectors.toList());
        }
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }
}
