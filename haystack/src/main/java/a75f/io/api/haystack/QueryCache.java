package a75f.io.api.haystack;

import org.projecthaystack.util.LRUCache;

public class QueryCache
{
    private static QueryCache mCacheManager;
    
    /***
     * <ID, PointCur>
     *
     * This could be adapted further to be a site cache than a point cache.
     * For now we will just have this be a point cache.
     *
     *
     */
    LRUCache<String, String> mCache;
    
    
    private QueryCache()
    {
        mCache = new LRUCache<>(10000);
    }
    
    public static QueryCache getInstance() {
        if(mCacheManager == null)
            mCacheManager = new QueryCache();
        
        return mCacheManager;
    }
    
    /***
     *
     * @param query
     * @param id
     */
    public void add(String query, String id )
    {
        mCache.put(query, id);
    }
    
    /***
     *
     * @param query
     * @return
     */
    public String get(String query)
    {
        return mCache.get(query);
    }
}
