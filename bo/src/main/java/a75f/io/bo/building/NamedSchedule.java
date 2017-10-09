package a75f.io.bo.building;

import java.util.ArrayList;

/**
 * Created by Yinten on 10/4/2017.
 */

public class NamedSchedule
{
    private String              mName;
    private ArrayList<Schedule> mSchedule;
    
    
    public ArrayList<Schedule> getSchedule()
    {
        return mSchedule;
    }
    
    
    public void setSchedule(ArrayList<Schedule> schedule)
    {
        this.mSchedule = schedule;
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
