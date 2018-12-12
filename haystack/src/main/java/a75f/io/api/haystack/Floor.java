package a75f.io.api.haystack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 11/19/18.
 */

public class Floor
{
    private String            displayName;
    private ArrayList<String> markers;
    private String            siteRef;
    private String id;
    public String getId()
    {
        return id;
    }
    public String getDisplayName()
    {
        return displayName;
    }
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getSiteRef()
    {
        return siteRef;
    }
    public String toString() {
        return displayName;
    }
    public static class Builder{
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();;
        private String            siteRef;
        private String id;
        public Builder setDisplayName(String displayName)
        {
            this.displayName = displayName;
            return this;
        }
        public Builder setMarkers(ArrayList<String> markers)
        {
            this.markers = markers;
            return this;
        }
        public Builder setSiteRef(String siteRef)
        {
            this.siteRef = siteRef;
            return this;
        }
        
        public Floor build(){
            Floor f = new Floor();
            f.displayName = this.displayName;
            f.siteRef = this.siteRef;
            f.markers = this.markers;
            f.id = this.id;
            return f;
        }
    
        public Builder setHashMap(HashMap site)
        {
            Iterator it = site.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                System.out.println(pair.getKey() + " = " + pair.getValue());
                if(pair.getKey().equals("id"))
                {
                    this.id = pair.getValue().toString();
                }
                else if(pair.getKey().equals("dis"))
                {
                    this.displayName = pair.getValue().toString();
                }
                else if(pair.getKey().equals("marker"))
                {
                    this.markers.add(pair.getValue().toString());
                }
                else if(pair.getKey().equals("siteRef"))
                {
                    this.siteRef = pair.getValue().toString();
                }
                it.remove();
            }
        
            return this;
        }
        
    }
}
