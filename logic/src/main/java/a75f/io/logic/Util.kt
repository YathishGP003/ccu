package a75f.io.logic

import a75f.io.logger.CcuLog
import a75f.io.logic.L.TAG_CCU

/**
 * Miscellaneous utility functions.
 *
 * @author Tony Case
 * Created on 1/13/21.
 */

/**
 * For the case where you'd like a value to be non-null, but rather than just using !! (in Kotlin),
 * which would crash the app in the case of null, you'd like to give it a default while reporting
 * the case to our crash reporter.
 *
 * Credit to now unknown article on medium.com
 *
 * This function can be called from Java -> usage would be
 *
 *    String valueNotNull = UtilKt.reportNull(value, default, message);
 *
 * In Kotlin,
 *
 *    val valueNotNull = value.reportNull(default, message)
 *
 * The "inline" is present so that the crash report stackTrace will show the calling location of this function
 * rather than the line inside the function where we create the NullPointerException.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> T?.reportNull(
   default: T,
   message: String = ""
): T {
   if (this == null) {
      reportToCrashlytics(NullPointerException(message))
      return default
   }
   return this
}

fun reportToCrashlytics(e: Throwable) {
   // We do not currently have Crashylitics set up -- that is a ccu modernization todue.
   CcuLog.e(TAG_CCU, e.message, e)
}