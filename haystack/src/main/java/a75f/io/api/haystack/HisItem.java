package a75f.io.api.haystack;

import java.util.Date;

import a75f.io.logger.CcuLog;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;

/**
 * Created by samjithsadasivan on 9/24/18.
 */

@Entity
public class HisItem
{
    @Id
    long id;
    
    String rec;
    
    long date;
    
    Double val;
    
    boolean syncStatus;
    
    @Transient
    public Boolean initialized = true;
    
    public HisItem(){
    }
    
    public HisItem(long id, Date date, Double val) {
        this.id = id;
        this.date = date.getTime();
        this.val = val;
    }
    
    public HisItem(long id, Date date, Double val, Boolean initialized) {
        this.id = id;
        this.date = date.getTime();
        this.val = val;
        this.initialized = initialized;
    }
    
    public HisItem(String rec, Date date, Double val, Boolean initialized) {
        this.rec = rec;
        this.date = date.getTime();
        this.val = val;
        this.initialized = initialized;
    }
    
    public HisItem(String rec, Date date, Double val) {
        this.rec = rec;
        this.date = date.getTime();
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
        return new Date(date);
    }
    public void setDate(Date date)
    {
        this.date = date.getTime();
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
        CcuLog.d("CCU_HS Key:", "id: " + id + " rec: " + rec + " date: " + date + " val: " + val + " syncStatus: " + syncStatus);
    }
    public String toString() {
        return "id: " + id + " rec: " + rec + " date: " + date + " val: " + val + " syncStatus: " + syncStatus;
    }
}
