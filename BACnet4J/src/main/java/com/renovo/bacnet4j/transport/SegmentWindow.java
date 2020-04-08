
package com.renovo.bacnet4j.transport;

import com.renovo.bacnet4j.apdu.Segmentable;

public class SegmentWindow {
    private int firstSequenceId;
    private final Segmentable[] segments;

    public SegmentWindow(final int windowSize, final int firstSequenceId) {
        this.firstSequenceId = firstSequenceId;
        segments = new Segmentable[windowSize];
    }

    public int getFirstSequenceId() {
        return firstSequenceId;
    }

    public Segmentable getSegment(final int sequenceId) {
        return segments[sequenceId - firstSequenceId];
    }

    public void setSegment(final Segmentable segment) {
        segments[segment.getSequenceNumber() - firstSequenceId] = segment;
    }

    public boolean fitsInWindow(final Segmentable segment) {
        final int index = segment.getSequenceNumber() - firstSequenceId;
        if (index < 0 || index >= segments.length)
            return false;
        return true;
    }

    public boolean isEmpty() {
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] != null)
                return false;
        }
        return true;
    }

    /**
     * The segment window is full if all slots are non-null.
     *
     * @return
     */
    public boolean isFull() {
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == null)
                return false;
        }
        return true;
    }

    /**
     * The message is complete if the last full slot is marked as not more follows, and all slots before it are filled.
     * (Full slots following it are ignored.)
     *
     * @return
     */
    public boolean isMessageComplete() {
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == null)
                return false;
            if (!segments[i].isMoreFollows())
                return true;
        }
        return false;
    }

    public void clear(final int firstSequenceId) {
        this.firstSequenceId = firstSequenceId;
        for (int i = 0; i < segments.length; i++)
            segments[i] = null;
    }

    public boolean isLastSegment(final int sequenceId) {
        return sequenceId == segments.length + firstSequenceId - 1;
    }

    public Segmentable[] getSegments() {
        return segments;
    }

    public int getLatestSequenceId() {
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i] != null)
                return segments[i].getSequenceNumber();
        }
        return -1;
    }

    public int getWindowSize() {
        return segments.length;
    }
}
