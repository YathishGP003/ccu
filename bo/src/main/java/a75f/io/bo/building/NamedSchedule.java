package a75f.io.bo.building;

/**
 * Created by Yinten on 10/4/2017.
 */

public class NamedSchedule
{
    private String mName;
    private Schedule mSchedule;
    private boolean mIsLCMSchedule;
    
    
    public Schedule getSchedule()
    {
        return mSchedule;
    }
    
    
    public void setSchedule(Schedule schedule)
    {
        this.mSchedule = schedule;
    }
    
    
    public boolean isLCMSchedule()
    {
        return mIsLCMSchedule;
    }
    
    
    public void setLCMSchedule(boolean LCMSchedule)
    {
        mIsLCMSchedule = LCMSchedule;
    }
    
    
    public String getName()
    {
        return mName;
    }
    
    
    public void setName(String name)
    {
        this.mName = name;
    }
}
