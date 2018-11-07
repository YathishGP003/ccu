package a75f.io.api.haystack;

import java.util.Date;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Created by samjithsadasivan on 9/24/18.
 */

@Entity
public class HisItem
{
    @Id
    long id;
    
    String rec;
    
    Date date;
    
    Double val;
    
    boolean syncStatus;
    
    public HisItem(){
    }
    
    public HisItem(long id, Date date, Double val) {
        this.id = id;
        this.date = date;
        this.val = val;
    }
    public HisItem(String rec, Date date, Double val) {
        this.rec = rec;
        this.date = date;
        this.val = val;
    }
    public long getId()
    {
        return id;
    }
    public void setId(long id)
    {
        this.id = id;
    }
    
    public String getRec()
    {
        return rec;
    }
    public void setRec(String rec)
    {
        this.rec = rec;
    }
    public Date getDate()
    {
        return date;
    }
    public void setDate(Date date)
    {
        this.date = date;
    }
    public Double getVal()
    {
        return val;
    }
    public void setVal(Double val)
    {
        this.val = val;
    }
    
    public boolean getSyncStatus()
    {
        return syncStatus;
    }
    public void setSyncStatus(boolean syncStatus)
    {
        this.syncStatus = syncStatus;
    }
    public void dump(){
        System.out.println("id: "+id+" rec: "+rec+" date: "+date+" val: "+val+" syncStatus: "+syncStatus);
    }
}
