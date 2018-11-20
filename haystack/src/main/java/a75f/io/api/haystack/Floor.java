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
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }
    public void setMarkers(ArrayList<String> markers)
    {
        this.markers = markers;
    }
    public void setSiteRef(String siteRef)
    {
        this.siteRef = siteRef;
    }
    
    public static class Builder{
        private String            displayName;
        private ArrayList<String> markers;
        private String            siteRef;
        public void setDisplayName(String displayName)
        {
            this.displayName = displayName;
        }
        public void setMarkers(ArrayList<String> markers)
        {
            this.markers = markers;
        }
        public void setSiteRef(String siteRef)
        {
            this.siteRef = siteRef;
        }
        
        public Floor Builder(){
            Floor f = new Floor();
            f.displayName = this.displayName;
            f.siteRef = this.siteRef;
            f.markers = this.markers;
            return f;
        }
        
    }
}
