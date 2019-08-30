package a75f.io.api.haystack;

import org.projecthaystack.HDict;
import org.projecthaystack.util.LRUCache;

public class HisItemCache
{

	private static HisItemCache mCacheManager;

	/***
	 * <ID, PointCur>
	 * 
	 * This could be adapted further to be a site cache than a point cache. 
	 * For now we will just have this be a point cache.  
	 * 
	 * 
	 */
	LRUCache<String, HisItem> mCache;
	
	
	private HisItemCache()
	{
		mCache = new LRUCache<>(1000000);
	}
	
	public static HisItemCache getInstance() {
		if(mCacheManager == null)
			mCacheManager = new HisItemCache();

		return mCacheManager;
	}
	

	/***
	 * 
	 * @param dict 
	 * @return if this dict is a point. 
	 */
	public boolean checkPoint(HDict dict)
	{
		return dict.has("point") && dict.has("cur"); 
	}


	/***
	 *
	 * @param hisItem
	 * @param id
	 */
	public void add(String id, HisItem hisItem )
	{
		mCache.put(id, hisItem);
	}
	
	/***
	 *
	 * @param id
	 * @return
	 */
	public HisItem get(String id)
	{
		return mCache.get(id);
	}
}
