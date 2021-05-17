package a75f.io.alerts.model

import org.joda.time.DateTime
import java.lang.IllegalStateException

/**
 * Representation of progress of an alert def which tests positive, either raised as an alert, resolved, or
 * partially on the way to being raised with number of consecutive occurrences.
 *
 * @author tcase@75f.io
 * Created on 4/22/21.
 */

// A sealed class is like an enum where all its subclasses (here, Partial and Raised) are the values.  But
//  they are also full baked classes, often with state and behavior.
sealed class AlertDefProgress {
   data class Partial(val occurrenceCount: Int = 1)  : AlertDefProgress() {
      fun inc() = Partial(this.occurrenceCount+1)
   }
   data class Raised(
      val timeRaised: DateTime = DateTime.now(),
      val timeFixed: DateTime? = null
   ) : AlertDefProgress()

   /** Convert this Raised alert to resolved, with fixed time as right now.
    *
    * @throws IllegalStateException if this is only a partially progressed alert def.
    * */
   fun toFixed() = if (this is Raised) copy(timeFixed = DateTime.now())
   else throw IllegalStateException("Cannot fix a non-raised alert")

   /** Convert this to a raised Alert, with now as the alert time. */
   fun toRaised() = Raised(timeRaised = DateTime.now())

   fun isActive(): Boolean = (this is Raised) && (this.timeFixed == null)
   fun isFixed(): Boolean = (this is Raised) && (this.timeFixed != null)
}

