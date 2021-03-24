package a75f.io.api.haystack;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.converter.PropertyConverter;

/**
 * Created by samjithsadasivan on 4/23/18.
 */
//TODO- Object box entities require to be in the same android module.
//TODO- Have a data module to have all entities
@Entity
public class Alert
{
    @Id
    public long id;
    public String mTitle; //Short message
    public String mMessage; //Details
    public String mNotificationMsg; //Tooltip info message
    
    @Convert(converter = SeverityConverter.class, dbType = Integer.class)
    public AlertSeverity mSeverity;
    public boolean       mEnabled;
    
    public long startTime;
    public long endTime;
    public boolean  isFixed;
    
    //Backend guid of the alert
    public String _id;
    
    public String ref;
    
    public String deviceRef;
    public String siteRef;
    
    public boolean syncStatus;
    public String mAlertType;
    
    public String getmAlertType()
    {
        return mAlertType;
    }
    public void setmAlertType(String mAlertType)
    {
        this.mAlertType = mAlertType;
    }
    public boolean getSyncStatus()
    {
        return syncStatus;
    }
    public void setSyncStatus(boolean syncStatus)
    {
        this.syncStatus = syncStatus;
    }
    
    public String getRef()
    {
        return ref;
    }
    public void setRef(String ref)
    {
        this.ref = ref;
    }
    public String getDeviceRef()
    {
        return deviceRef;
    }
    public void setDeviceRef(String deviceRef)
    {
        this.deviceRef = deviceRef;
    }
    public String getSiteRef()
    {
        return siteRef;
    }
    public void setSiteRef(String siteRef)
    {
        this.siteRef = siteRef;
    }
    public long getId()
    {
        return id;
    }
    public void setId(long id)
    {
        this.id = id;
    }
    
    public String getmTitle()
    {
        return mTitle;
    }
    public void setmTitle(String mTitle)
    {
        this.mTitle = mTitle;
    }
    public String getmMessage()
    {
        return mMessage;
    }
    public void setmMessage(String mMessage)
    {
        this.mMessage = mMessage;
    }
    public String getmNotificationMsg()
    {
        return mNotificationMsg;
    }
    public void setmNotificationMsg(String mNotificationMsg)
    {
        this.mNotificationMsg = mNotificationMsg;
    }
    public AlertSeverity getmSeverity()
    {
        return mSeverity;
    }
    public void setmSeverity(AlertSeverity mSeverity)
    {
        this.mSeverity = mSeverity;
    }
    public boolean ismEnabled()
    {
        return mEnabled;
    }
    public void setmEnabled(boolean mEnabled)
    {
        this.mEnabled = mEnabled;
    }
    public long getStartTime()
    {
        return startTime;
    }
    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }
    public long getEndTime()
    {
        return endTime;
    }
    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }
    public boolean isFixed()
    {
        return isFixed;
    }
    public void setFixed(boolean fixed)
    {
        isFixed = fixed;
    }
    public String getGuid()
    {
        return _id;
    }
    public void setGuid(String alertId)
    {
        this._id = alertId;
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(" {").append(id).append(", ").append(mTitle).append(",").append(mMessage).append(", ").append(",")
         .append(ref).append(", ").append(startTime).append(", ").append(endTime).append(", ").append(isFixed).append(", ")
         .append(deviceRef).append(", ").append(siteRef).append(", ").append(syncStatus).append(", ").append(_id).append("}");
        return b.toString();
    }
    public Alert(){
    }
    
    public enum AlertSeverity
    {
        SEVERE,
        MODERATE,
        LOW,
        INFO,
        INTERNAL_SEVERE,
        INTERNAL_MODERATE,
        INTERNAL_LOW,
        INTERNAL_INFO
    }
    
    public static class SeverityConverter implements PropertyConverter<AlertSeverity, Integer> {
        @Override
        public AlertSeverity convertToEntityProperty(Integer databaseValue) {
            if (databaseValue == null) {
                return null;
            }
            return AlertSeverity.values()[databaseValue];
        }
        
        @Override
        public Integer convertToDatabaseValue(AlertSeverity entityProperty) {
            return entityProperty.ordinal();
        }
    }
}

