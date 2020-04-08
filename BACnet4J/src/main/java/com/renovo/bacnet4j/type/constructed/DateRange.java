
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.enums.DayOfWeek;
import com.renovo.bacnet4j.enums.Month;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetRuntimeException;
import com.renovo.bacnet4j.type.DateMatchable;
import com.renovo.bacnet4j.type.primitive.Date;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class DateRange extends BaseType implements DateMatchable {
    private final Date startDate;
    private final Date endDate;

    public DateRange(final Date startDate, final Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        validate();
    }

    public DateRange(final ByteQueue queue) throws BACnetException {
        startDate = read(queue, Date.class);
        endDate = read(queue, Date.class);
        validate();
    }

    private void validate() {
        /*if (startDate.getYear() != Date.UNSPECIFIED_YEAR && endDate.getYear() == Date.UNSPECIFIED_YEAR
                || startDate.getYear() == Date.UNSPECIFIED_YEAR && endDate.getYear() != Date.UNSPECIFIED_YEAR*//* ||
            startDate.getYear() != 255 && endDate.getYear() == 255
                || startDate.getYear() == 255 && endDate.getYear() != 255*//*)
            throw new BACnetRuntimeException("start and end years must both be specific or unspecific");
        *//*if (startDate.getMonth() == Month.EVEN_MONTHS || startDate.getMonth() == Month.ODD_MONTHS)
            throw new BACnetRuntimeException("even/odd months are not supported in date ranges");
        if (endDate.getMonth() == Month.EVEN_MONTHS || endDate.getMonth() == Month.ODD_MONTHS)
            throw new BACnetRuntimeException("even/odd months are not supported in date ranges");*//*
        if (startDate.getMonth() != Month.UNSPECIFIED && endDate.getMonth() == Month.UNSPECIFIED
                || startDate.getMonth() == Month.UNSPECIFIED && endDate.getMonth() != Month.UNSPECIFIED)
            throw new BACnetRuntimeException("start and end months must both be specific or unspecific");
        if (startDate.getDay() != Date.UNSPECIFIED_DAY && endDate.getDay() == Date.UNSPECIFIED_DAY
                || startDate.getDay() == Date.UNSPECIFIED_DAY && endDate.getDay() != Date.UNSPECIFIED_DAY)
            throw new BACnetRuntimeException("start and end day must both be specific or unspecific");       
        if ((startDate.getDay() == Date.UNSPECIFIED_DAY && startDate.getDayOfWeek() != DayOfWeek.UNSPECIFIED) 
                || (endDate.getDay() == Date.UNSPECIFIED_DAY && endDate.getDayOfWeek() != DayOfWeek.UNSPECIFIED))
            throw new BACnetRuntimeException("day of week ranges are not supported");*/
        if(!isSpecific(startDate) && !isSpecific(endDate)){
            if(!isValid(startDate) && !isValid(endDate)){
                throw new BACnetRuntimeException("start and end years must both be specific or unspecific");
            }if(!isValid(startDate) && isValid(endDate)){
                throw new BACnetRuntimeException("start and end years must both be specific or unspecific");
            }if(isValid(startDate) && !isValid(endDate)){
                throw new BACnetRuntimeException("start and end years must both be specific or unspecific");
            }
        }else if(isSpecific(startDate) && !isSpecific(endDate)){
            if(!isValid(endDate)){
                throw new BACnetRuntimeException("start and end years must both be specific or unspecific");
            }
        }else if(!isSpecific(startDate) && isSpecific(endDate)){
            if(!isValid(startDate)){
                throw new BACnetRuntimeException("start and end years must both be specific or unspecific");
            }
        }
    }

    private boolean isSpecific(Date dateValue){
        boolean isSpecific = false;
        if(dateValue.getYear() != Date.UNSPECIFIED_YEAR && dateValue.getMonth() != Month.UNSPECIFIED && dateValue.getDay() != Date.UNSPECIFIED_DAY){
            if(dateValue.getMonth().isEven() && dateValue.getDay() <=30) {
                isSpecific = true;
            }if(dateValue.getMonth().isOdd() && dateValue.getDay() <=31) {
                isSpecific = true;
            }
        }
        return isSpecific;
    }

    private boolean isValid(Date dateValue){
        boolean isValid = false;
        if((dateValue.getYear() == Date.UNSPECIFIED_YEAR||dateValue.getYear() == 255) && dateValue.getMonth() == Month.UNSPECIFIED && dateValue.getDay() == Date.UNSPECIFIED_DAY){
            isValid = true;
        }
        return isValid;
    }
    @Override
    public void write(final ByteQueue queue) {
        write(queue, startDate);
        write(queue, endDate);
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (endDate == null ? 0 : endDate.hashCode());
        result = PRIME * result + (startDate == null ? 0 : startDate.hashCode());
        return result;
    }

    @Override
    public boolean matches(final Date date) {
        if (!date.isSpecific())
            throw new BACnetRuntimeException("Dates for matching must be completely specified: " + date);

        final Date leastBefore = startDate.calculateLeastMatchOnOrBefore(date);
        final Date greatestBefore = endDate.calculateGreatestMatchOnOrBefore(date);

        if (greatestBefore == null)
            return leastBefore != null;
        if (greatestBefore.before(leastBefore))
            return true;
        if (date.sameAs(greatestBefore))
            return true;
        return false;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DateRange other = (DateRange) obj;
        if (endDate == null) {
            if (other.endDate != null)
                return false;
        } else if (!endDate.equals(other.endDate))
            return false;
        if (startDate == null) {
            if (other.startDate != null)
                return false;
        } else if (!startDate.equals(other.startDate))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DateRange [startDate=" + startDate + ", endDate=" + endDate + "]";
    }
}
