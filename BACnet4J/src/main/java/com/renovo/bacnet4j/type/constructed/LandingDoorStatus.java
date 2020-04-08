
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.enumerated.DoorStatus;
import com.renovo.bacnet4j.type.primitive.Unsigned8;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class LandingDoorStatus extends BaseType {
    private final SequenceOf<LandingDoor> landingDoors;

    public LandingDoorStatus(final SequenceOf<LandingDoor> landingDoors) {
        this.landingDoors = landingDoors;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, landingDoors, 0);
    }

    public LandingDoorStatus(final ByteQueue queue) throws BACnetException {
        landingDoors = readSequenceOf(queue, LandingDoor.class, 0);
    }

    public SequenceOf<LandingDoor> getLandingDoors() {
        return landingDoors;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (landingDoors == null ? 0 : landingDoors.hashCode());
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
        final LandingDoorStatus other = (LandingDoorStatus) obj;
        if (landingDoors == null) {
            if (other.landingDoors != null)
                return false;
        } else if (!landingDoors.equals(other.landingDoors))
            return false;
        return true;
    }

    public static class LandingDoor extends BaseType {
        private final Unsigned8 floorNumber;
        private final DoorStatus doorStatus;

        public LandingDoor(final Unsigned8 floorNumber, final DoorStatus doorStatus) {
            this.floorNumber = floorNumber;
            this.doorStatus = doorStatus;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, floorNumber, 0);
            write(queue, doorStatus, 1);
        }

        public LandingDoor(final ByteQueue queue) throws BACnetException {
            floorNumber = read(queue, Unsigned8.class, 0);
            doorStatus = read(queue, DoorStatus.class, 1);
        }

        public Unsigned8 getFloorNumber() {
            return floorNumber;
        }

        public DoorStatus getDoorStatus() {
            return doorStatus;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (doorStatus == null ? 0 : doorStatus.hashCode());
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
            final LandingDoor other = (LandingDoor) obj;
            if (doorStatus == null) {
                if (other.doorStatus != null)
                    return false;
            } else if (!doorStatus.equals(other.doorStatus))
                return false;
            if (floorNumber == null) {
                if (other.floorNumber != null)
                    return false;
            } else if (!floorNumber.equals(other.floorNumber))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "LandingDoor [floorNumber=" + floorNumber + ", doorStatus=" + doorStatus + ']';
        }     
    }

    @Override
    public String toString() {
        return "LandingDoorStatus [landingDoors=" + landingDoors + ']';
    }   
}
