package com.renovo.bacnet4j.obj;

import java.util.Objects;
import java.util.TimeZone;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.enums.MaxApduLength;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.npdu.Network;
import com.renovo.bacnet4j.npdu.mstp.MasterNode;
import com.renovo.bacnet4j.npdu.mstp.MstpNetwork;
import com.renovo.bacnet4j.npdu.mstp.MstpNode;
import com.renovo.bacnet4j.obj.mixin.ActiveCovSubscriptionMixin;
import com.renovo.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.renovo.bacnet4j.obj.mixin.ObjectListMixin;
import com.renovo.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.renovo.bacnet4j.obj.mixin.TimeSynchronizationMixin;
import com.renovo.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.NoneAlgo;
import com.renovo.bacnet4j.transport.Transport;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.AddressBinding;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.DateTime;
import com.renovo.bacnet4j.type.constructed.EventTransitionBits;
import com.renovo.bacnet4j.type.constructed.ObjectTypesSupported;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.Recipient;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.ServicesSupported;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.constructed.TimeStamp;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.BackupState;
import com.renovo.bacnet4j.type.enumerated.DeviceStatus;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.enumerated.Segmentation;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Date;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.SignedInteger;
import com.renovo.bacnet4j.type.primitive.Time;
import com.renovo.bacnet4j.type.primitive.Unsigned16;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class DeviceObject extends BACnetObject {
    private static final int VENDOR_ID = 1181; //  Vendor ID for 75F

    public DeviceObject(final LocalDevice localDevice, final int instanceNumber,final String deviceName) throws BACnetServiceException {
        super(localDevice, ObjectType.device, instanceNumber, "75F-"+deviceName+"-"+ instanceNumber);

        writePropertyInternal(PropertyIdentifier.maxApduLengthAccepted,
                new UnsignedInteger(MaxApduLength.UP_TO_1476.getMaxLengthInt()));
        writePropertyInternal(PropertyIdentifier.vendorIdentifier, new UnsignedInteger(VENDOR_ID));
        writePropertyInternal(PropertyIdentifier.vendorName,
                new CharacterString("75F"));
        writePropertyInternal(PropertyIdentifier.segmentationSupported, Segmentation.noSegmentation);
        //writePropertyInternal(PropertyIdentifier.maxSegmentsAccepted, new UnsignedInteger(Integer.MAX_VALUE));
        //writePropertyInternal(PropertyIdentifier.apduSegmentTimeout, new UnsignedInteger(Transport.DEFAULT_SEG_TIMEOUT));
        writePropertyInternal(PropertyIdentifier.apduTimeout, new UnsignedInteger(Transport.DEFAULT_TIMEOUT));
        writePropertyInternal(PropertyIdentifier.numberOfApduRetries, new UnsignedInteger(Transport.DEFAULT_RETRIES));
        writePropertyInternal(PropertyIdentifier.objectList, new BACnetArray<ObjectIdentifier>());

        // Set up the supported services indicators. Remove lines as services get implemented.
        final ServicesSupported servicesSupported = new ServicesSupported();
        servicesSupported.allFalse();
        servicesSupported.setAcknowledgeAlarm(true);
        servicesSupported.setGetEventInformation(true);
        servicesSupported.setSubscribeCov(true);
        servicesSupported.setReadProperty(true);
        servicesSupported.setReadPropertyMultiple(true);
        servicesSupported.setWriteProperty(true);
        servicesSupported.setWritePropertyMultiple(true);
        servicesSupported.setDeviceCommunicationControl(true);
        servicesSupported.setReinitializeDevice(true);
        servicesSupported.setIAm(true);
        servicesSupported.setTimeSynchronization(true);
        servicesSupported.setWhoIs(true);
        servicesSupported.setReadRange(true);
        servicesSupported.setUtcTimeSynchronization(true);
        servicesSupported.setWhoHas(true);
        servicesSupported.setConfirmedCovNotification(true);
        servicesSupported.setUnconfirmedCovNotification(true);
        //servicesSupported.setGetAlarmSummary(true);
        //servicesSupported.setConfirmedEventNotification(true);
        /*servicesSupported.setConfirmedCovNotification(false);//Todo We are not going to get cov notification from server
        servicesSupported.setConfirmedEventNotification(false);//Todo We are not going to get event notification from server
        servicesSupported.setGetAlarmSummary(false);
        servicesSupported.setGetEnrollmentSummary(true);
        servicesSupported.setSubscribeCov(true);
        servicesSupported.setAtomicReadFile(false);
        servicesSupported.setAtomicWriteFile(false);
        servicesSupported.setAddListElement(false);
        servicesSupported.setRemoveListElement(true);
        servicesSupported.setCreateObject(false);
        servicesSupported.setDeleteObject(false);
        servicesSupported.setReadProperty(true);
        servicesSupported.setReadPropertyMultiple(true);
        servicesSupported.setWriteProperty(true);
        servicesSupported.setWritePropertyMultiple(true);
        servicesSupported.setDeviceCommunicationControl(true);
        servicesSupported.setConfirmedPrivateTransfer(false);
        servicesSupported.setConfirmedTextMessage(true);
        servicesSupported.setReinitializeDevice(true);
        //        servicesSupported.setVtOpen(true);
        //        servicesSupported.setVtClose(true);
        //        servicesSupported.setVtData(true);
        servicesSupported.setIAm(true);
        servicesSupported.setIHave(true);
        servicesSupported.setUnconfirmedCovNotification(false);
        servicesSupported.setUnconfirmedEventNotification(false);
        servicesSupported.setUnconfirmedPrivateTransfer(false);
        servicesSupported.setUnconfirmedTextMessage(false);
        servicesSupported.setTimeSynchronization(true);
        servicesSupported.setWhoHas(false);
        servicesSupported.setWhoIs(true);
        servicesSupported.setReadRange(true);
        servicesSupported.setUtcTimeSynchronization(true);
        servicesSupported.setLifeSafetyOperation(false);
        servicesSupported.setSubscribeCovProperty(true);
        servicesSupported.setGetEventInformation(true);
        //        servicesSupported.setWriteGroup(true);
        servicesSupported.setSubscribeCovPropertyMultiple(true);
        servicesSupported.setConfirmedCovNotificationMultiple(true);
        servicesSupported.setUnconfirmedCovNotificationMultiple(false);*/

        writePropertyInternal(PropertyIdentifier.protocolServicesSupported, servicesSupported);

        // Set up the object types supported.
        final ObjectTypesSupported objectTypesSupported = new ObjectTypesSupported();
        objectTypesSupported.set(ObjectType.analogInput, false);
        objectTypesSupported.set(ObjectType.analogOutput, false);
        objectTypesSupported.set(ObjectType.analogValue, true);
        objectTypesSupported.set(ObjectType.binaryInput, false);
        objectTypesSupported.set(ObjectType.binaryOutput, false);
        objectTypesSupported.set(ObjectType.binaryValue, true);
        objectTypesSupported.set(ObjectType.calendar, true);
        //        objectTypesSupported.set(ObjectType.command, true);
        objectTypesSupported.set(ObjectType.device, true);
        objectTypesSupported.set(ObjectType.eventEnrollment, false);
        objectTypesSupported.set(ObjectType.file, false);
        //        objectTypesSupported.set(ObjectType.group, false);
        //        objectTypesSupported.set(ObjectType.loop, true);
        objectTypesSupported.set(ObjectType.multiStateInput, false);
        objectTypesSupported.set(ObjectType.multiStateOutput, false);
        objectTypesSupported.set(ObjectType.notificationClass, true);
        //        objectTypesSupported.set(ObjectType.program, true);
        objectTypesSupported.set(ObjectType.schedule, true);
        objectTypesSupported.set(ObjectType.averaging, false);
        objectTypesSupported.set(ObjectType.multiStateValue, true);
        objectTypesSupported.set(ObjectType.trendLog, true);
        objectTypesSupported.set(ObjectType.lifeSafetyPoint, false);
        objectTypesSupported.set(ObjectType.lifeSafetyZone, false);
        objectTypesSupported.set(ObjectType.accumulator, false);
        objectTypesSupported.set(ObjectType.pulseConverter,false);
        objectTypesSupported.set(ObjectType.eventLog, false);
        //        objectTypesSupported.set(ObjectType.globalGroup, true);
        objectTypesSupported.set(ObjectType.trendLogMultiple, true);
        objectTypesSupported.set(ObjectType.group, true);
        //objectTypesSupported.set(ObjectType.bitstringValue, true);
        //objectTypesSupported.set(ObjectType.characterstringValue, true);
        //        objectTypesSupported.set(ObjectType.loadControl, true);
        //        objectTypesSupported.set(ObjectType.structuredView, true);
        //        objectTypesSupported.set(ObjectType.accessDoor, true);
        //        objectTypesSupported.set(ObjectType.timer, true);
        //        objectTypesSupported.set(ObjectType.accessCredential, true);
        //        objectTypesSupported.set(ObjectType.accessPoint, true);
        //        objectTypesSupported.set(ObjectType.accessRights, true);
        //        objectTypesSupported.set(ObjectType.accessUser, true);
        //        objectTypesSupported.set(ObjectType.accessZone, true);
        //        objectTypesSupported.set(ObjectType.credentialDataInput, true);
        //        objectTypesSupported.set(ObjectType.networkSecurity, true);
        //        objectTypesSupported.set(ObjectType.bitstringValue, true);
        //        objectTypesSupported.set(ObjectType.characterstringValue, true);
        //        objectTypesSupported.set(ObjectType.datePatternValue, true);
        //        objectTypesSupported.set(ObjectType.dateValue, true);
        //        objectTypesSupported.set(ObjectType.datetimePatternValue, true);
        //        objectTypesSupported.set(ObjectType.datetimeValue, true);
        //        objectTypesSupported.set(ObjectType.integerValue, true);
        //        objectTypesSupported.set(ObjectType.largeAnalogValue, true);
        //        objectTypesSupported.set(ObjectType.octetstringValue, true);
        //        objectTypesSupported.set(ObjectType.positiveIntegerValue, true);
        //        objectTypesSupported.set(ObjectType.timePatternValue, true);
        //        objectTypesSupported.set(ObjectType.timeValue, true);
        objectTypesSupported.set(ObjectType.notificationForwarder, false);
        objectTypesSupported.set(ObjectType.alertEnrollment, false);
        //        objectTypesSupported.set(ObjectType.channel, true);
        //        objectTypesSupported.set(ObjectType.lightingOutput, true);
        //        objectTypesSupported.set(ObjectType.binaryLightingOutput, true);
        //        objectTypesSupported.set(ObjectType.networkPort, true);
        //        objectTypesSupported.set(ObjectType.elevatorGroup, true);
        //        objectTypesSupported.set(ObjectType.escalator, true);
        //        objectTypesSupported.set(ObjectType.lift, true);

        writePropertyInternal(PropertyIdentifier.protocolObjectTypesSupported, objectTypesSupported);

        // Set some other required values to defaults
        writePropertyInternal(PropertyIdentifier.systemStatus, DeviceStatus.operational);
        writePropertyInternal(PropertyIdentifier.modelName, new CharacterString("75F-RENATUS"));
        writePropertyInternal(PropertyIdentifier.firmwareRevision, new CharacterString("4.13"));
        writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion, new CharacterString(LocalDevice.VERSION));
        writePropertyInternal(PropertyIdentifier.protocolVersion, new UnsignedInteger(1));
        writePropertyInternal(PropertyIdentifier.protocolRevision, new UnsignedInteger(19));

        UnsignedInteger databaseRevision = getLocalDevice().getPersistence()
                .loadEncodable(getPersistenceKey(PropertyIdentifier.databaseRevision), UnsignedInteger.class);
        if (databaseRevision == null)
            databaseRevision = UnsignedInteger.ZERO;
        writePropertyInternal(PropertyIdentifier.databaseRevision, databaseRevision);

        writePropertyInternal(PropertyIdentifier.timeOfDeviceRestart, new TimeStamp(new DateTime(getLocalDevice())));
        //writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        //writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        //writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        //writePropertyInternal(PropertyIdentifier.configurationFiles, new BACnetArray<>(0, null));
        //writePropertyInternal(PropertyIdentifier.lastRestoreTime, new TimeStamp(DateTime.UNSPECIFIED));
        //writePropertyInternal(PropertyIdentifier.backupFailureTimeout, new Unsigned16(60));
        //writePropertyInternal(PropertyIdentifier.backupPreparationTime, new Unsigned16(0));
        //writePropertyInternal(PropertyIdentifier.restorePreparationTime, new Unsigned16(0));
        //writePropertyInternal(PropertyIdentifier.restoreCompletionTime, new Unsigned16(0));
        //writePropertyInternal(PropertyIdentifier.backupAndRestoreState, BackupState.idle);

        //These properties are automatically overwritten when reading. They are defined here to be present when reading the PropertyList.     
        set(PropertyIdentifier.utcOffset, new SignedInteger(0));
        set(PropertyIdentifier.localTime, new Time(getLocalDevice()));
        set(PropertyIdentifier.localDate, new Date(getLocalDevice()));
        set(PropertyIdentifier.daylightSavingsStatus, Boolean.FALSE);
        
        // Mixins
        addMixin(new ActiveCovSubscriptionMixin(this));
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.activeCovSubscriptions,
                PropertyIdentifier.localTime, PropertyIdentifier.localDate, PropertyIdentifier.deviceAddressBinding));

        //Read only properties
        addMixin(new ReadOnlyPropertyMixin(this,
                PropertyIdentifier.objectIdentifier,
                PropertyIdentifier.objectName,
                PropertyIdentifier.objectType,
                PropertyIdentifier.protocolVersion,
                PropertyIdentifier.protocolRevision,
                PropertyIdentifier.protocolObjectTypesSupported,
                PropertyIdentifier.protocolServicesSupported,
                PropertyIdentifier.databaseRevision,
                PropertyIdentifier.applicationSoftwareVersion,
                PropertyIdentifier.vendorIdentifier,
                PropertyIdentifier.vendorName,
                PropertyIdentifier.modelName,
                //PropertyIdentifier.utcOffset,
                PropertyIdentifier.reliability,
                PropertyIdentifier.serialNumber,
                PropertyIdentifier.firmwareRevision,
                PropertyIdentifier.apduLength,
                PropertyIdentifier.systemStatus,
                //PropertyIdentifier.apduSegmentTimeout,
                //PropertyIdentifier.apduTimeout,
                PropertyIdentifier.maxApduLengthAccepted,
                PropertyIdentifier.numberOfApduRetries,
                PropertyIdentifier.segmentationSupported,
                PropertyIdentifier.maxSegmentsAccepted,
                PropertyIdentifier.deviceAddressBinding));
        addMixin(new ObjectListMixin(this));

        localDevice.addObject(this);
    }

    public DeviceObject supportTimeSynchronization(final SequenceOf<Recipient> timeSynchronizationRecipients,
            final SequenceOf<Recipient> utcTimeSynchronizationRecipients, final int timeSynchronizationInterval,
            final boolean alignIntervals, final int intervalOffset) {
        final TimeSynchronizationMixin m = new TimeSynchronizationMixin(this, timeSynchronizationRecipients,
                utcTimeSynchronizationRecipients, timeSynchronizationInterval, alignIntervals, intervalOffset);
        addMixin(m);
        m.update();
        return this;
    }

    public DeviceObject supportIntrinsicReporting(final int notificationClass, final EventTransitionBits eventEnable,
            final NotifyType notifyType) {
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        addMixin(new IntrinsicReportingMixin(this, new NoneAlgo(), null, null, new PropertyIdentifier[] {}));

        return this;
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) {
        //Todo Making Local Device Date & Time Writeable
        if (pid.equals(PropertyIdentifier.localTime)) {
            set(PropertyIdentifier.localTime, new Time(getLocalDevice()));
        } else if (pid.equals(PropertyIdentifier.localDate)) {
            set(PropertyIdentifier.localDate, new Date(getLocalDevice()));
        } /*else if (pid.equals(PropertyIdentifier.utcOffset)) {
            final int offsetMillis = TimeZone.getDefault().getOffset(getLocalDevice().getClock().millis());
            writePropertyInternal(PropertyIdentifier.utcOffset, new SignedInteger(offsetMillis / 1000 / 60));
        } else */ if (pid.equals(PropertyIdentifier.daylightSavingsStatus)) {
            final boolean dst = TimeZone.getDefault()
                    .inDaylightTime(new java.util.Date(getLocalDevice().getClock().millis()));
            writePropertyInternal(PropertyIdentifier.daylightSavingsStatus, Boolean.valueOf(dst));
        } else if (pid.equals(PropertyIdentifier.deviceAddressBinding)) {
            final SequenceOf<AddressBinding> bindings = new SequenceOf<>();
            for (final RemoteDevice d : getLocalDevice().getRemoteDevices()) {
                if (d != null) {
                    bindings.add(new AddressBinding(d.getObjectIdentifier(), d.getAddress()));
                }
            }
            writePropertyInternal(PropertyIdentifier.deviceAddressBinding, bindings);
        }
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (value.getPropertyIdentifier().equals(PropertyIdentifier.maxMaster)) {
            final MasterNode masterNode = getMasterNode();
            if (masterNode != null) {
                final UnsignedInteger maxMaster = value.getValue();
                if (masterNode.getThisStation() > maxMaster.intValue()) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
                }
            }
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (pid.equals(PropertyIdentifier.restartNotificationRecipients)) {
            // Persist the new list.
            getLocalDevice().getPersistence()
                    .saveEncodable(getPersistenceKey(PropertyIdentifier.restartNotificationRecipients), newValue);
        } else if (pid.equals(PropertyIdentifier.maxMaster)) {
            final MasterNode masterNode = getMasterNode();
            if (masterNode != null) {
                final UnsignedInteger maxMaster = (UnsignedInteger) newValue;
                masterNode.setMaxMaster(maxMaster.intValue());
            }
        } else if (pid.equals(PropertyIdentifier.maxInfoFrames)) {
            final MasterNode masterNode = getMasterNode();
            if (masterNode != null) {
                final UnsignedInteger maxInfoFrames = (UnsignedInteger) newValue;
                masterNode.setMaxInfoFrames(maxInfoFrames.intValue());
            }
        }
    }

    private MasterNode getMasterNode() {
        final Network network = getLocalDevice().getNetwork();
        if (network instanceof MstpNetwork) {
            final MstpNode node = ((MstpNetwork) network).getNode();
            if (node instanceof MasterNode) {
                return (MasterNode) node;
            }
        }
        return null;
    }
}
