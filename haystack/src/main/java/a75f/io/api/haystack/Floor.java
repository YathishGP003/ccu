package a75f.io.api.haystack;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 11/19/18.
 */

public class Floor
{
    private String            displayName;
    private ArrayList<String> markers;
    private String            siteRef;
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
    public static class Builder{
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();;
        private String            siteRef;
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
            return f;
        }
        
    }
}
