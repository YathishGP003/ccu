package a75f.io.device.bacnet;

import android.util.Log;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Vacations {
    String vacationID;
    Date   startDate;
    Date   endDate;

    public Vacations (String vacationID,Date startDate,Date endDate){
        this.vacationID = vacationID;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    public String getVacationID() {
        return vacationID;
    }

    public void setVacationID(String vacationID) {
        this.vacationID = vacationID;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String toString(){
        return "ID:"+this.vacationID + " st:"+this.startDate + " et:"+this.endDate;
    }

    /*@Override
    public int hashCode(){
        return Objects.hash(startDate,endDate);
    }*/

    @Override
    public boolean equals(final Object obj) {
        /*if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vacations vacItem = (Vacations) o;
        return startDate == vacItem.startDate && Objects.equals(startDate, vacItem.startDate) && endDate == vacItem.endDate && Objects.equals(endDate, vacItem.endDate);*/
        if (this.startDate.equals(((Vacations)obj).startDate) && this.endDate.equals(((Vacations)obj).endDate) ) {
            //Log.i("BACnetUtil","Contains St:"+this.startDate+" compared:"+((Vacations)obj).startDate);
            //Log.i("BACnetUtil","Contains Et:"+this.endDate+" compared:"+((Vacations)obj).endDate);
            return true;
        }
        return false;
    }

}
