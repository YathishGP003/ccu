package a75.io.bo.haystack;

import org.junit.Test;

/**
 * Created by samjithsadasivan on 9/4/18.
 */

public class HayStackApiTest
{
    
    
    
    @Test
    public void testSite()
    {
        Site s = new Site.Builder()
                         .setDisplayName("75F")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setArea(1000).build();
        
        //HashMap data = L.ccu().tagsMap;
        
        System.out.println();
    }
                    
}
