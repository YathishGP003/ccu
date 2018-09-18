package a75f.io.haystack.api;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;

/**
 * Created by samjithsadasivan on 9/17/18.
 */

public class TestHS
{
    
    
    @Test
    public void testSerialization(){
        CCUHsApi api = new CCUHsApi();
        api.tagsDb.tagsMap = new HashMap<>();
        Site s = new Site.Builder()
                         .setDisplayName("Name")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setTz("Chicago")
                         .setArea(1000).build();
        CCUHsApi.getInstance().addSite(s);
    
        Site s1 = new Site.Builder()
                         .setDisplayName("aaaa")
                         .addMarker("bbbb")
                         .setGeoCity("cccc")
                         .setGeoState("dddd")
                         .setTz("eeee")
                         .setArea(1000).build();
        CCUHsApi.getInstance().addSite(s1);
        
        Map m1 = api.tagsDb.tagsMap;
        System.out.println(m1);
        api.tagsDb.saveString();
        System.out.println(api.tagsDb.tagsString);
        api.tagsDb.init();
        System.out.println(api.tagsDb.tagsMap);
        /*Map m2 = api.tagsDb.tagsMap;
        Iterator i1 = m1.values().iterator();
        Iterator i2 = m2.keySet().iterator();
        
    
        while (i1.hasNext() )
        {
            System.out.println(i1.next());
        }
        
        System.out.println("########################");
    
        HashMap newTagsMap = new HashMap();
        while (i2.hasNext() )
        {
            Object mapKey = i2.next();
            Map h = (Map) m2.get(mapKey);
            System.out.println(h);
    
            Map newMap = new HashMap();
            
            for (Iterator i = h.keySet().iterator(); i.hasNext();) {
                String key = (String)i.next();
                if (key.equals("map"))
                {
                    Map dmap = (Map) h.get(key);
                    for (Iterator g = dmap.keySet().iterator();g.hasNext();) {
                        String k = (String) g.next();
                        newMap.put(k,((Map)dmap.get(k)).get("val"));
                    }
                }
            }
    
            //System.out.println(newMap);
            
            newTagsMap.put(mapKey,newMap);
            
            //System.out.println(b.toDict());
        }
        System.out.println(newTagsMap);*/
    
        HashMap site = CCUHsApi.getInstance().read("site");
        System.out.print(site);
        
    }
    
    
}
