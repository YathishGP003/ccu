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
    public int alertCount;
    
    //Backend guid of the alert
    public String alertId;
    
    public String ref;
    
    public String deviceRef;
    public String siteRef;
    
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
    public int getAlertCount()
    {
        return alertCount;
    }
    public void setAlertCount(int alertCount)
    {
        this.alertCount = alertCount;
    }
    public String getAlertId()
    {
        return alertId;
    }
    public void setAlertId(String alertId)
    {
        this.alertId = alertId;
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(" {").append(mTitle).append(",").append(mMessage).append(", ").append(alertId).append(",")
         .append(ref).append(", ").append(startTime).append(", ").append(endTime).append(", ").append(isFixed).append(", ").append(deviceRef).append(", ").append(siteRef).append("}");
        return b.toString();
    }
    public Alert(){
    }
    
    public enum AlertSeverity
    {
        FATAL,
        ERROR,
        WARN,
        INFO,
        INTERNAL_FATAL,
        INTERNAL_ERROR,
        INTERNAL_WARN,
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

