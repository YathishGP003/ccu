package a75f.io.renatus.util.extension

import a75f.io.renatus.R
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

/**
 * Extension functions for fragments, contexts, activities...
 * @author tcase@75f.io
 * Created on 6/18/21.
 */
fun Fragment.showErrorDialog(msg: String) {
   AlertDialog.Builder(requireContext())
      .setTitle("Error")
      .setIcon(R.drawable.ic_alert)
      .setMessage(msg)
      .show()
}