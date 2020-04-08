
package com.renovo.bacnet4j.cache;

import com.renovo.bacnet4j.LocalDevice;

public class CachedRemoteEntity<T> {
    private final T entity;
    private final RemoteEntityCachePolicy policy;
    private final Object cacheState;

    public CachedRemoteEntity(final LocalDevice localDevice, final T entity, final RemoteEntityCachePolicy policy) {
        this.entity = entity;
        this.policy = policy;
        this.cacheState = policy.prepareState(localDevice);
    }

    public boolean hasExpired(final LocalDevice localDevice) {
        return policy.hasExpired(localDevice, cacheState);
    }

    public T getEntity() {
        return entity;
    }
}
