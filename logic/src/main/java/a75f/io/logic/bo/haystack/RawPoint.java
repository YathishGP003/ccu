package a75f.io.logic.bo.haystack;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 9/6/18.
 */

public class RawPoint
{
    private String            displayName;
    private ArrayList<String> markers;
    private String deviceRef;
    private String pointRef;
    private String port;
    private String type;
    public void setPointRef(String pointRef)
    {
        this.pointRef = pointRef;
    }
    
    public String getDisplayName()
    {
        return displayName;
    }
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getDeviceRef()
    {
        return deviceRef;
    }
    public String getPointRef()
    {
        return pointRef;
    }
    public String getPort()
    {
        return port;
    }
    public String getType()
    {
        return type;
    }
    public static class Builder{
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String deviceRef;
        private String pointRef;
        private String port;
        private String type;
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
        public Builder setDeviceRef(String deviceRef)
        {
            this.deviceRef = deviceRef;
            return this;
        }
        public Builder setPointRef(String pointRef)
        {
            this.pointRef = pointRef;
            return this;
        }
        public Builder setPort(String port)
        {
            this.port = port;
            return this;
        }
        public Builder setType(String type)
        {
            this.type = type;
            return this;
        }
        
        public RawPoint build(){
            RawPoint p = new RawPoint();
            p.displayName = this.displayName;
            p.markers = this.markers;
            p.deviceRef = this.deviceRef;
            p.pointRef = this.pointRef;
            p.port = this.port;
            p.type = this.type;
            //CCUHsApi.getInstance().addRawPoint(p);
            return p;
        }
    }
    
    
}
