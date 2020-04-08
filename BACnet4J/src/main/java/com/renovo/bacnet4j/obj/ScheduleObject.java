
package com.renovo.bacnet4j.obj;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.ResponseConsumer;
import com.renovo.bacnet4j.apdu.AckAPDU;
import com.renovo.bacnet4j.enums.DayOfWeek;
import com.renovo.bacnet4j.enums.Month;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetRuntimeException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.renovo.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.renovo.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.NoneAlgo;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.service.confirmed.WritePropertyRequest;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.DailySchedule;
import com.renovo.bacnet4j.type.constructed.DateRange;
import com.renovo.bacnet4j.type.constructed.DateTime;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.DeviceObjectReference;
import com.renovo.bacnet4j.type.constructed.EventTransitionBits;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.SpecialEvent;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.constructed.TimeValue;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.BinaryPV;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.Date;
import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.Primitive;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

import sun.rmi.runtime.Log;

/**
 * TODO
 * - use reliability to convey schedule problems.
 *
 * @author Matthew
 */
public class ScheduleObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(ScheduleObject.class);

    // CreateObject constructor
    public static ScheduleObject create(final LocalDevice localDevice, final int instanceNumber)
            throws BACnetServiceException {
        return new ScheduleObject(localDevice, instanceNumber, ObjectType.schedule.toString() + " " + instanceNumber,
                new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED),
                new BACnetArray<>(7, new DailySchedule(new SequenceOf<>())), new SequenceOf<>(), BinaryPV.inactive,
                new SequenceOf<>(), 12, false);
    }

    private ScheduledFuture<?> presentValueRefersher;

    /**
     * A proprietary mechanism to periodically write the present value to all property references in case of power
     * failures, restarts, and the like.
     */
    private ScheduledFuture<?> periodicWriter;

    public ScheduleObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final DateRange effectivePeriod, final BACnetArray<DailySchedule> weeklySchedule,
            final SequenceOf<SpecialEvent> exceptionSchedule, final Primitive scheduleDefault,
            final SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences,
            final int priorityForWriting, final boolean outOfService) throws BACnetServiceException {
        super(localDevice, ObjectType.schedule, instanceNumber, name);

        if (effectivePeriod == null)
            throw new BACnetRuntimeException("effectivePeriod cannot be null");
        if (weeklySchedule == null && exceptionSchedule == null)
            throw new BACnetRuntimeException("Both weeklySchedule and exceptionSchedule cannot be null");
        if (scheduleDefault == null)
            throw new BACnetRuntimeException("scheduleDefault cannot be null");
        if (listOfObjectPropertyReferences == null)
            throw new BACnetRuntimeException("listOfObjectPropertyReferences cannot be null");

        writePropertyInternal(PropertyIdentifier.effectivePeriod, effectivePeriod);
        writePropertyInternal(PropertyIdentifier.scheduleDefault, scheduleDefault);
        if (weeklySchedule != null) {
            if (weeklySchedule.getCount() != 7)
                throw new BACnetRuntimeException("weeklySchedule must have 7 elements");
            writeProperty(null, new PropertyValue(PropertyIdentifier.weeklySchedule, weeklySchedule));
        }
        if (exceptionSchedule != null)
            writeProperty(null, new PropertyValue(PropertyIdentifier.exceptionSchedule, exceptionSchedule));
        writePropertyInternal(PropertyIdentifier.presentValue, scheduleDefault);
        writeProperty(null,
                new PropertyValue(PropertyIdentifier.listOfObjectPropertyReferences, listOfObjectPropertyReferences));
        writePropertyInternal(PropertyIdentifier.priorityForWriting, new UnsignedInteger(priorityForWriting));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, outOfService));

        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new ScheduleMixin(this));
        addMixin(new ReadOnlyPropertyMixin(this,PropertyIdentifier.description));

        final Primitive oldValue = get(PropertyIdentifier.presentValue);
        updatePresentValue();
        final Primitive newValue = get(PropertyIdentifier.presentValue);
        // If the present value didn't change after the update, then no write would have been done. So, to ensure
        // initialization of the objects in the list, force a write.
        if (Objects.equals(oldValue, newValue))
            doWrites(newValue);

        localDevice.addObject(this);
    }

    public void supportIntrinsicReporting(final int notificationClass, final EventTransitionBits eventEnable,
            final NotifyType notifyType) {
        // Prepare the object with all of the properties that intrinsic reporting will need.
        // User-defined properties
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        // Now add the mixin.
        addMixin(new IntrinsicReportingMixin(this, new NoneAlgo(), null, null, new PropertyIdentifier[0]));
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.listOfObjectPropertyReferences.equals(value.getPropertyIdentifier())) {
            // Entries must reference properties of this type
            final Primitive scheduleDefault = get(PropertyIdentifier.scheduleDefault);
            final SequenceOf<DeviceObjectPropertyReference> refs = value.getValue();
            for (final DeviceObjectPropertyReference ref : refs) {
                final ObjectPropertyTypeDefinition def = ObjectProperties.getObjectPropertyTypeDefinition(
                        ref.getObjectIdentifier().getObjectType(), ref.getPropertyIdentifier());
                if (def != null) {
                    if (scheduleDefault.getClass() != def.getPropertyTypeDefinition().getClazz()) {
                        if(scheduleDefault.getClass() == Enumerated.class){
                            if(scheduleDefault.getClass() != def.getPropertyTypeDefinition().getClazz().getSuperclass()){
                            System.out.println("Bacnet Schedule Write"+" scheduleClass:" + scheduleDefault.getClass()+" scheduleSuperClass:"+scheduleDefault.getClass().getSuperclass() + " def:" + def.getPropertyTypeDefinition().getClazz()+" superClass:"+def.getPropertyTypeDefinition().getClazz().getSuperclass());
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                            }
                        }else{
                        System.out.println("Bacnet Schedule Write" + " scheduleClass:" + scheduleDefault.getClass()+" def:"+ def.getPropertyTypeDefinition().getClazz());
                        throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                        }
                    }
                }
            }
        } else if (PropertyIdentifier.weeklySchedule.equals(value.getPropertyIdentifier())) {
            // Time value entries must be of this type
            final Primitive scheduleDefault = get(PropertyIdentifier.scheduleDefault);
            final SequenceOf<DailySchedule> weeklySchedule = value.getValue();
            for (final DailySchedule daily : weeklySchedule) {
                int dailyCount = daily.getDaySchedule().getCount();
                /*if(dailyCount > 2)
                {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.internalError);
                }else {*/
                    for (final TimeValue timeValue : daily.getDaySchedule()) {
                        if (scheduleDefault.getClass() != timeValue.getValue().getClass()) {
                            if(scheduleDefault.getClass() == Enumerated.class){
                                if (scheduleDefault.getClass() != timeValue.getValue().getClass().getSuperclass()) {
                                    //System.out.println("Bacnet Schedule Write" + " PropertyValue:" + timeValue + " scheduleClass:" + scheduleDefault.getClass()+" scheduleSuperClass:"+scheduleDefault.getClass().getSuperclass() + " timeValueSuperClass:" + timeValue.getValue().getClass());
                                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                                }
                            }else {
                                //System.out.println("Bacnet Schedule Write" + " PropertyValue:" + timeValue + " scheduleClass:" + scheduleDefault.getClass() + " timeValueClass:" + timeValue.getValue().getClass());
                                throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                            }
                        }
                        if (!timeValue.getTime().isFullySpecified()) {
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidConfigurationData);
                        }
                    }
                //}
            }
        } else if (PropertyIdentifier.exceptionSchedule.equals(value.getPropertyIdentifier())) {
            // Time value entries must be of this type
            final Primitive scheduleDefault = get(PropertyIdentifier.scheduleDefault);
            final SequenceOf<SpecialEvent> exceptionSchedule = value.getValue();
            for (final SpecialEvent specialEvent : exceptionSchedule) {
                for (final TimeValue timeValue : specialEvent.getListOfTimeValues()) {
                    if (scheduleDefault.getClass() != timeValue.getValue().getClass()) {
                        System.out.println("Bacnet Exception-Schedule Write" + " PropertyValue:" + timeValue + " scheduleClass:" + scheduleDefault.getClass() + " timeValueClass:" + timeValue.getValue().getClass()+" scheduleDefaultSuperClass:"+scheduleDefault.getClass().getSuperclass()+" timeValueSuperClass:" + timeValue.getValue().getClass().getSuperclass());
                        if(scheduleDefault.getClass().getSuperclass() == Enumerated.class){
                            if (scheduleDefault.getClass().getSuperclass() != timeValue.getValue().getClass()) {
                                System.out.println("Bacnet Exception-Schedule Write" + " PropertyValue:" + timeValue + " scheduleClass:" + scheduleDefault.getClass() + " timeValueSuperClass:" + timeValue.getValue().getClass().getSuperclass());
                                throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                            }
                        }else {
                            System.out.println("Bacnet Exception-Schedule Write" + " PropertyValue:" + timeValue + " scheduleClass:" + scheduleDefault.getClass() + " timeValueClass:" + timeValue.getValue().getClass());
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                        }
                    }
                    if (!timeValue.getTime().isFullySpecified()) {
                        throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidConfigurationData);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Starts the internal periodic writer.
     *
     * @param delay
     *            the delay before the first execution, in milliseconds.
     * @param period
     *            the period between executions, in milliseconds.
     */
    public void startPeriodicWriter(final long delay, final long period) {
        if (delay < 0)
            throw new IllegalArgumentException("delay cannot be < 0");
        if (period < 1)
            throw new IllegalArgumentException("period cannot be < 1");

        cancelPeriodicWriter();
        periodicWriter = getLocalDevice().scheduleAtFixedRate(() -> forceWrites(), delay, period,
                TimeUnit.MILLISECONDS);
        LOG.debug("Periodic writer started");
    }

    public void stopPeriodicWriter() {
        cancelPeriodicWriter();
    }

    @Override
    protected void terminateImpl() {
        cancelRefresher();
        cancelPeriodicWriter();
    }

    synchronized public void forceWrites() {
        doWrites(get(PropertyIdentifier.presentValue));
    }

    class ScheduleMixin extends AbstractMixin {
        public ScheduleMixin(final BACnetObject bo) {
            super(bo);
        }

        @Override
        protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
                throws BACnetServiceException {
            if (PropertyIdentifier.presentValue.equals(value.getPropertyIdentifier())) {
                final Boolean outOfService = get(PropertyIdentifier.outOfService);
                if (!outOfService.booleanValue())
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }
            return false;
        }

        @Override
        protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
                final Encodable newValue) {
            if (Objects.equals(newValue, oldValue))
                return;

            if (pid.isOneOf(PropertyIdentifier.effectivePeriod, PropertyIdentifier.weeklySchedule,
                    PropertyIdentifier.exceptionSchedule, PropertyIdentifier.scheduleDefault))
                updatePresentValue();
            if (pid.equals(PropertyIdentifier.presentValue))
                doWrites(newValue);
        }
    }

    private void cancelRefresher() {
        if (presentValueRefersher != null) {
            presentValueRefersher.cancel(false);
            presentValueRefersher = null;
        }
    }

    private void cancelPeriodicWriter() {
        if (periodicWriter != null) {
            periodicWriter.cancel(false);
            periodicWriter = null;
        }
    }

   public synchronized void updatePresentValue() {
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(getLocalDevice().getClock().millis());
        updatePresentValue(new DateTime(gc));
    }

    private void updatePresentValue(final DateTime now) {
        cancelRefresher();

        Primitive newValue;
        long nextCheck;

        final Primitive scheduleDefault = get(PropertyIdentifier.scheduleDefault);
        final DateRange effectivePeriod = get(PropertyIdentifier.effectivePeriod);
        boolean checkSd = isValid(effectivePeriod.getStartDate());
        boolean checkEd = isValid(effectivePeriod.getEndDate());
        boolean isMatched = effectivePeriod.matches(now.getDate());
        if ((!isMatched)&& (!checkSd) && (!checkEd)) {
            // Not in the current effective date.
            newValue = scheduleDefault;
            nextCheck = nextDay(now);
        } else {
            SequenceOf<TimeValue> schedule = null;

            // Is there an exception schedule in effect?
            final SpecialEvent specialEvent = findExceptionSchedule(now);
            if (specialEvent != null)
                schedule = specialEvent.getListOfTimeValues();
            else {
                final DailySchedule dailySchedule = findDailySchedule(now);
                if (dailySchedule != null)
                    schedule = dailySchedule.getDaySchedule();
            }

            if (schedule == null) {
                newValue = scheduleDefault;
                nextCheck = nextDay(now);
            } else {
                // Find the schedule entry in effect at this time.
                TimeValue currentTv = null;
                int tvIndex = schedule.getCount();
                for (; tvIndex > 0; tvIndex--) {
                    final TimeValue tv = schedule.getBase1(tvIndex);

                    if (!tv.getTime().after(now.getTime())) {
                        // Found a time value entry that can be used.
                        currentTv = tv;
                        break;
                    }
                }

                // Determine the new present value.
                if (currentTv == null)
                    newValue = scheduleDefault;
                else
                    newValue = currentTv.getValue();

                // Determine the next time this method should run.
                if (tvIndex < schedule.getCount()) {
                    final TimeValue nextTv = schedule.getBase1(tvIndex + 1);
                    nextCheck = timeOf(now.getDate(), nextTv);
                } else
                    nextCheck = nextDay(now);
            }
        }

        writePropertyInternal(PropertyIdentifier.presentValue, newValue);

        final java.util.Date nextRuntime = new java.util.Date(nextCheck);
        presentValueRefersher = getLocalDevice().schedule(() -> updatePresentValue(), nextRuntime.getTime(),
                TimeUnit.MILLISECONDS);
        LOG.debug("Timer scheduled to run at {}", nextRuntime);
    }

    private static long nextDay(final DateTime now) {
        final GregorianCalendar gc = now.getGC();
        gc.add(Calendar.DATE, 1);
        gc.add(Calendar.HOUR_OF_DAY, -gc.get(Calendar.HOUR_OF_DAY));
        gc.add(Calendar.MINUTE, -gc.get(Calendar.MINUTE));
        gc.add(Calendar.SECOND, -gc.get(Calendar.SECOND));
        gc.add(Calendar.MILLISECOND, -gc.get(Calendar.MILLISECOND));
        return gc.getTimeInMillis();
    }

    private static long timeOf(final Date date, final TimeValue tv) {
        final DateTime dt = new DateTime(date, tv.getTime());
        return dt.getGC().getTimeInMillis();
    }

    private SpecialEvent findExceptionSchedule(final DateTime now) {
        final SequenceOf<SpecialEvent> exceptionSchedule = get(PropertyIdentifier.exceptionSchedule);
        if (exceptionSchedule == null)
            return null;

        SpecialEvent best = null;
        for (final SpecialEvent e : exceptionSchedule) {
            boolean active;
            if (e.isCalendarReference()) {
                final CalendarObject co = (CalendarObject) getLocalDevice().getObject(e.getCalendarReference());
                if (co != null) {
                    // Getting the property this way ensures that the calendar's present value gets is calculated.
                    try {
                        final Boolean pv = co.readProperty(PropertyIdentifier.presentValue);
                        active = pv.booleanValue();
                    } catch (final BACnetServiceException ex) {
                        LOG.warn("Error while retrieving calendar's present value", ex);
                        active = false;
                    }
                } else
                    active = false;
            } else
                active = e.getCalendarEntry().matches(now.getDate());

            if (active) {
                if (best == null || best.getEventPriority().intValue() > e.getEventPriority().intValue())
                    best = e;
            }
        }
        return best;
    }

    private DailySchedule findDailySchedule(final DateTime now) {
        final BACnetArray<DailySchedule> weeklySchedule = get(PropertyIdentifier.weeklySchedule);
        if (weeklySchedule == null)
            return null;

        DayOfWeek dow = now.getDate().getDayOfWeek();
        if (!dow.isSpecific())
            dow = DayOfWeek.forDate(now.getDate());

        return weeklySchedule.getBase1(dow.getId());
    }

    void doWrites(final Encodable value) {
        final SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = get(
                PropertyIdentifier.listOfObjectPropertyReferences);
        final UnsignedInteger priorityForWriting = get(PropertyIdentifier.priorityForWriting);

        // Send the write requests.
        for (final DeviceObjectPropertyReference dopr : listOfObjectPropertyReferences) {
            LOG.debug("Sending write request to {} in {}, value={}, priority={}", dopr.getObjectIdentifier(),
                    dopr.getDeviceIdentifier(), value, priorityForWriting);

            if (dopr.getDeviceIdentifier() == null) {
                // Local write.
                final BACnetObject that = getLocalDevice().getObject(dopr.getObjectIdentifier());
                try {
                    that.writeProperty(new ValueSource(new DeviceObjectReference(getLocalDevice().getId(), getId())),
                            new PropertyValue(dopr.getPropertyIdentifier(), dopr.getPropertyArrayIndex(), value,
                                    priorityForWriting));
                } catch (final BACnetServiceException e) {
                    LOG.warn("Schedule failed to write to local object {}", dopr.getObjectIdentifier(), e);
                }
            } else {
                final ObjectIdentifier devId = dopr.getDeviceIdentifier();
                final ObjectIdentifier oid = dopr.getObjectIdentifier();

                try {
                    final RemoteDevice d = getLocalDevice().getRemoteDevice(devId.getInstanceNumber()).get();
                    final WritePropertyRequest req = new WritePropertyRequest(oid, dopr.getPropertyIdentifier(),
                            dopr.getPropertyArrayIndex(), value, priorityForWriting);
                    getLocalDevice().send(d, req, new ResponseConsumer() {
                        @Override
                        public void success(final AcknowledgementService ack) {
                            // Whatever.
                        }

                        @Override
                        public void fail(final AckAPDU ack) {
                            LOG.warn("Schedule failed to write to {} in {}, ack={}", oid, devId, ack);
                        }

                        @Override
                        public void ex(final BACnetException e) {
                            LOG.error("Schedule failed to write to {} in {}", oid, devId, e);
                        }
                    });
                } catch (final BACnetException e) {
                    LOG.warn("Schedule failed to write to unknown remote device {}", devId, e);
                }
            }
        }
    }

    private boolean isValid(Date dateValue){
        boolean isValid = false;
        if((dateValue.getYear() == Date.UNSPECIFIED_YEAR||dateValue.getYear() == 255) && dateValue.getMonth() == Month.UNSPECIFIED && dateValue.getDay() == Date.UNSPECIFIED_DAY){
            isValid = true;
        }
        return isValid;
    }
}
