package a75f.io.api.haystack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 11/19/18.
 */

public class Zone extends Entity
{
    private String            displayName;
    private ArrayList<String> markers;
    private String floorRef;
    private String siteRef;

    private String scheduleRef;
    private String vacationRef;

    public boolean isSystemSchedule()
    {
        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false);
        if(systemSchedule.getId().equals(scheduleRef))
            return true;


        return false;
    }

    public boolean hasSchedule()
    {
        if(scheduleRef == null || scheduleRef.equals(""))
        {
            return false;
        }

        return true;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    private String id;
    public void setFloorRef(String floorRef)
    {
        this.floorRef = floorRef;
    }
    public void setSiteRef(String siteRef)
    {
        this.siteRef = siteRef;
    }
    public String getId()
    {
        return id;
    }
    public String toString() {
        return displayName;
    }
    public String getDisplayName()
    {
        return displayName;
    }
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getFloorRef()
    {
        return floorRef;
    }
    public String getSiteRef()
    {
        return siteRef;
    }
    public String getScheduleRef() { return scheduleRef; }
    public String getVacationRef() { return vacationRef; }

    public static class Builder {
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();;
        private String            floorRef;
        private String siteRef;
        private String id;
        private String scheduleRef;
        private String vacationRef;

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

        public Builder setFloorRef(String floorRef)
        {
            this.floorRef = floorRef;
            return this;
        }

        public Builder setScheduleRef(String scheduleID) {
            this.scheduleRef = scheduleID;
            return this;
        }
        
        public Zone build() {
            Zone z = new Zone();
            z.displayName = this.displayName;
            z.floorRef = this.floorRef;
            z.markers = this.markers;
            z.siteRef = this.siteRef;
            z.scheduleRef = this.scheduleRef;
            z.vacationRef = this.vacationRef;
            z.id = this.id;
            return z;
        }
    
        public Builder setHashMap(HashMap site)
        {
            Iterator it = site.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                if(pair.getKey().equals("id"))
                {
                    this.id = pair.getValue().toString();
                }
                else if(pair.getKey().equals("dis"))
                {
                    this.displayName = pair.getValue().toString();
                }
                else if(pair.getValue().equals("marker")/*pair.getKey().equals("marker")*/) //TODO
                {
                    this.markers.add(pair.getKey().toString()/*pair.getValue().toString()*/);
                }
                else if(pair.getKey().equals("floorRef"))
                {
                    this.floorRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("siteRef"))
                {
                    this.siteRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("scheduleRef"))
                {
                    this.scheduleRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("vacationRef"))
                {
                    this.vacationRef = pair.getValue().toString();
                }
                it.remove();
            }
        
            return this;
        }
    }
}
