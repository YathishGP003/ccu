package a75f.io.dal;

import com.google.api.client.util.Key;

/**
 * Created by Yinten on 10/14/2017.
 */

public class PasswordReset
{
    @Key
    private String status;
    
    @Key
    private String lastStateChangeAt;
    
    
    public String getStatus()
    {
        return status;
    }
    
    
    public void setStatus(String status)
    {
        this.status = status;
    }
    
    
    public String getLastStateChangeAt()
    {
        return lastStateChangeAt;
    }
    
    
    public void setLastStateChangeAt(String lastStateChangeAt)
    {
        this.lastStateChangeAt = lastStateChangeAt;
    }
}
