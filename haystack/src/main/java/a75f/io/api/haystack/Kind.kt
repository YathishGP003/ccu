package a75f.io.api.haystack

/**
 * This enum is used to set the Kind on a Point when creating a Point using Point.Builder.
 * Currently, only Str and Number are used so I haven't added the other types.
 *
 * @author Tony Case
 * Created on 1/15/21.
 */
enum class Kind(val value: String) {
   STRING("Str"),
   NUMBER("Number");
   // there are other, unused Kinds in Haystack which I am not yet adding

   companion object {
      @JvmStatic
      fun parse(value: String) = values().firstOrNull { it.value == value } ?: NUMBER
   }
}
