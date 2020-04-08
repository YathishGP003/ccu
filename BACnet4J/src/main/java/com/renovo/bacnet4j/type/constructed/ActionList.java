
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ActionList extends BaseType {
    private final SequenceOf<ActionCommand> action;

    public ActionList(final SequenceOf<ActionCommand> action) {
        this.action = action;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, action, 0);
    }

    public ActionList(final ByteQueue queue) throws BACnetException {
        action = readSequenceOf(queue, ActionCommand.class, 0);
    }

    public SequenceOf<ActionCommand> getAction() {
        return action;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (action == null ? 0 : action.hashCode());
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
        final ActionList other = (ActionList) obj;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equals(other.action))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ActionList [action=" + action + ']';
    } 
}
