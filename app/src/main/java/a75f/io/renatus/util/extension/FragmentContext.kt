package a75f.io.renatus.util.extension

import a75f.io.api.haystack.util.validateMigration
import a75f.io.renatus.R
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import java.util.*

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

 fun showMigrationErrorDialog(context: Context) {
   AlertDialog.Builder(context)
      .setTitle("Data Migration Error")
      .setIcon(R.drawable.ic_alert)
      .setMessage("Data Migration is in progress, please try after sometime")
      .setNegativeButton("Cancel") { dialog, _ ->
         dialog.dismiss()
      }.show()
}
