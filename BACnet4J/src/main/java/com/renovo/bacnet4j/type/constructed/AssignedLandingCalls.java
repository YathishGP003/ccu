
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.enumerated.LiftCarDirection;
import com.renovo.bacnet4j.type.primitive.Unsigned8;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AssignedLandingCalls extends BaseType {
    private final SequenceOf<LandingCall> landingCalls;

    public AssignedLandingCalls(final SequenceOf<LandingCall> landingCalls) {
        this.landingCalls = landingCalls;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, landingCalls, 0);
    }

    public AssignedLandingCalls(final ByteQueue queue) throws BACnetException {
        landingCalls = readSequenceOf(queue, LandingCall.class, 0);
    }

    public SequenceOf<LandingCall> getLandingCalls() {
        return landingCalls;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (landingCalls == null ? 0 : landingCalls.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final AssignedLandingCalls other = (AssignedLandingCalls) obj;
        if (landingCalls == null) {
            if (other.landingCalls != null)
                return false;
        } else if (!landingCalls.equals(other.landingCalls))
            return false;
        return true;
    }

    public static class LandingCall extends BaseType {
        private final Unsigned8 floorNumber;
        private final LiftCarDirection direction;

        public LandingCall(final Unsigned8 floorNumber, final LiftCarDirection direction) {
            this.floorNumber = floorNumber;
            this.direction = direction;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, floorNumber, 0);
            write(queue, direction, 1);
        }

        public LandingCall(final ByteQueue queue) throws BACnetException {
            floorNumber = read(queue, Unsigned8.class, 0);
            direction = read(queue, LiftCarDirection.class, 1);
        }

        public Unsigned8 getFloorNumber() {
            return floorNumber;
        }

        public LiftCarDirection getDirection() {
            return direction;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (direction == null ? 0 : direction.hashCode());
            result = prime * result + (floorNumber == null ? 0 : floorNumber.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final LandingCall other = (LandingCall) obj;
            if (direction == null) {
                if (other.direction != null)
                    return false;
            } else if (!direction.equals(other.direction))
                return false;
            if (floorNumber == null) {
                if (other.floorNumber != null)
                    return false;
            } else if (!floorNumber.equals(other.floorNumber))
                return false;
            return true;
        }
    }

    @Override
    public String toString() {
        return "AssignedLandingCalls [landingCalls=" + landingCalls + ']';
    }   
}
