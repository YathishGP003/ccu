package a75f.io.alerts

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

/**
 * @author tcase@75f.io
 * Created on 3/9/21.
 */

/** creates a nicely formatted string out of this alert defs' evaluated conditionals */
fun AlertDefinition.conditionEvaluationText(): SpannableStringBuilder {
   val sb = SpannableStringBuilder()

   for (cond in conditionals) {
      sb.append(cond.evaluationText())
   }
   return sb
}

private fun Conditional.evaluationText(): SpannableStringBuilder {
   val orange = Color.parseColor("#E24301")

   val sb = SpannableStringBuilder()
   sb.append(" ")

   var startIndex = sb.length
   if (operator != null) {
      // print the operation (i.e. "|| or &&)
      sb.append(operator)
      sb.setSpan(ForegroundColorSpan(Color.BLACK), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
      sb.setSpan(StyleSpan(Typeface.BOLD), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
      sb.append(" ")
   } else {
      // ccu internal

      if (!error.isNullOrBlank()) {
         sb.append(error);
         sb.setSpan(ForegroundColorSpan(Color.RED), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
         sb.setSpan(StyleSpan(Typeface.ITALIC), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
      }
      else if (key.isNullOrBlank() || condition.isNullOrBlank() || value.isNullOrBlank()) {
         sb.append("[ccu internal logic]");
         sb.setSpan(ForegroundColorSpan(Color.GRAY), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
         sb.setSpan(StyleSpan(Typeface.ITALIC), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
      } else {
         // print key and the res value.
         sb.append(key)
         sb.setSpan(ForegroundColorSpan(Color.DKGRAY), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
         sb.append(" ")
         startIndex = sb.length
         sb.append("(").append(resVal.toString()).append(")")
         sb.setSpan(ForegroundColorSpan(orange), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
         sb.append(" ")
         startIndex = sb.length

         sb.append(condition)
         sb.setSpan(ForegroundColorSpan(Color.BLACK), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
         sb.append(" ")
         startIndex = sb.length
         sb.append(value)
         sb.setSpan(ForegroundColorSpan(Color.DKGRAY), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

         if (! `val`.isNullOrBlank() ) {
            sb.append(" ")
            startIndex = sb.length
            sb.append("(").append(`val`).append(")")
            sb.setSpan(ForegroundColorSpan(orange), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            sb.append(" ")
            startIndex = sb.length
         }
      }
      sb.append(" ")
      startIndex = sb.length
      sb.append("(")
      if (grpOperation != null) sb.append(grpOperation)
      sb.append(")")
      sb.setSpan(ForegroundColorSpan(Color.BLACK), startIndex, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

      if (status) {
         sb.setSpan(StyleSpan(Typeface.BOLD), 1, sb.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
      }
   }
   return sb
}

