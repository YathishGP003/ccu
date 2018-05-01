package a75f.io.alerts;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by samjithsadasivan on 4/23/18.
 */

public class Alert
{
    private String mAlertType;
    private String mTitle; //Short message
    private String mMessage; //Details
    private String mNotificationMsg; //Tooltip info message
    
    private AlertSeverity mSeverity;
    private boolean       mEnabled;
    
    public List<String> emails;
    public List<String> sms;
    public List<String> pushNotifications;
    
    public Calendar startTime;
    public Calendar endTime;
    public boolean  isFixed;
    public UUID     alertID;
    public int alertCount;
    
    public String getAlertType()
    {
        return mAlertType;
    }
    public void setAlertType(String mAlertType)
    {
        this.mAlertType = mAlertType;
    }
    public String getTitle()
    {
        return mTitle;
    }
    public void setTitle(String mTitle)
    {
        this.mTitle = mTitle;
    }
    public String getMessage()
    {
        return mMessage;
    }
    public void setMessage(String mMessage)
    {
        this.mMessage = mMessage;
    }
    public String getNotificationMsg()
    {
        return mNotificationMsg;
    }
    public void setNotificationMsg(String mNotificationMsg)
    {
        this.mNotificationMsg = mNotificationMsg;
    }
    public AlertSeverity getSeverity()
    {
        return mSeverity;
    }
    public void setSeverity(AlertSeverity mSeverity)
    {
        this.mSeverity = mSeverity;
    }
    public boolean isEnabled()
    {
        return mEnabled;
    }
    public void setEnabled(boolean mEnabled)
    {
        this.mEnabled = mEnabled;
    }
    public List<String> getEmails()
    {
        return emails;
    }
    public void setEmails(List<String> emails)
    {
        this.emails = emails;
    }
    public List<String> getSms()
    {
        return sms;
    }
    public void setSms(List<String> sms)
    {
        this.sms = sms;
    }
    public List<String> getPushNotifications()
    {
        return pushNotifications;
    }
    public void setPushNotifications(List<String> pushNotifications)
    {
        this.pushNotifications = pushNotifications;
    }
    public String getStartTime()
    {
        SimpleDateFormat formatMMDDHHMMSS = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);
        return formatMMDDHHMMSS.format(startTime.getTime());
    }
    
    public void setStartTime(Calendar startTime)
    {
        this.startTime = startTime;
    }
    public String getEndTime()
    {
        if (endTime == null)
            return "";
        SimpleDateFormat formatMMDDHHMMSS = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);
        return formatMMDDHHMMSS.format(endTime.getTime());
    }
    public void setEndTime(Calendar endTime)
    {
        this.endTime = endTime;
    }
    
    
}

