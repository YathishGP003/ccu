package a75f.io.api.haystack;

import javax.annotation.Nullable;

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
    public long id;     // a unique id from object box
    public String mTitle;           // alertDef DTO
    public String mMessage;           // alertDef DTO
    public String mNotificationMsg;          // alertDef DTO

    @Convert(converter = SeverityConverter.class, dbType = Integer.class)
    public AlertSeverity mSeverity;          // alertDef DTO
    public boolean       mEnabled;          // alertDef DTO
    public String mAlertType;           // alertDef DTO

    public long startTime;
    public long endTime;
    public boolean  isFixed;

    // new for service upgrade, 04-15-21
    public String alertDefId;
    public String siteIdNoAt;
    public String ccuIdNoAt;
    public String siteName;
    public String ccuName;
    public @Nullable String equipId;
    public String equipName;

    public @Nullable String floorId;
    public String floorName;
    public @Nullable String zoneId;
    public String zoneName;

    //Backend guid of the alert
    public String _id;

    public @Nullable String ref;

    public String deviceRef;

    // true if the alert has been synced (created or updated) with server
    public boolean syncStatus;

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
    public void setmNotificationMsg(String mNotificationMsg) { this.mNotificationMsg = mNotificationMsg; }
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
    public boolean isActive() { return !isFixed; }
    public String getGuid()
    {
        return _id;
    }
    public void setGuid(String alertId) { this._id = alertId; }
    public String getSafeEquipId() { return equipId == null ? "" : equipId; }
    public void setEquipId(@Nullable String equipId) { this.equipId = equipId; }


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
        INTERNAL_INFO,
        // the remaining four were added in v2
        ERROR,
        WARN,
        INTERNAL_ERROR,
        INTERNAL_WARN
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

    @Override
    public String toString() {
        return "Alert{" +
                "id=" + id +
                ", mTitle='" + mTitle + '\'' +
                ", equipId='" + equipId + '\'' +
                ", syncStatus=" + syncStatus +
                ", isFixed=" + isFixed +
                ", _id='" + _id + '\'' +
                ", mMessage='" + mMessage + '\'' +
                ", mNotificationMsg='" + mNotificationMsg + '\'' +
                ", mSeverity=" + mSeverity +
                ", mEnabled=" + mEnabled +
                ", mAlertType='" + mAlertType + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", alertDefId='" + alertDefId + '\'' +
                ", siteIdNoAt='" + siteIdNoAt + '\'' +
                ", ccuIdNoAt='" + ccuIdNoAt + '\'' +
                ", siteName='" + siteName + '\'' +
                ", ccuName='" + ccuName + '\'' +
                ", equipName='" + equipName + '\'' +
                ", floorId='" + floorId + '\'' +
                ", floorName='" + floorName + '\'' +
                ", zoneId='" + zoneId + '\'' +
                ", zoneName='" + zoneName + '\'' +
                ", ref='" + ref + '\'' +
                ", deviceRef='" + deviceRef + '\'' +
                '}';
    }
}