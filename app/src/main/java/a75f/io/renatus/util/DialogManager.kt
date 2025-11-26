package a75f.io.renatus.util
import android.app.Dialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * Author: Manjunath Kundaragi
 * Created on: 24-11-2025
 */

object DialogManager {

    // List to track all Dialogs (AlertDialog, custom Dialog)
    private val dialogs = mutableListOf<Dialog>()
    private val dialogsFragments = mutableListOf<DialogFragment>()

    fun register(dialog: Dialog) {
        dialogs.add(dialog)
    }
    fun register(dialog: DialogFragment) {
        dialogsFragments.add(dialog)
    }


    private fun dismissAll() {
        dialogs.forEach { dialog ->
            if (dialog.isShowing) {
                try {
                    dialog.dismiss()
                } catch (ignored: Exception) {}
            }
        }

        dialogsFragments.forEach { dialogFragment ->
            try {
                dialogFragment.dismissAllowingStateLoss()
            } catch (ignored: Exception) {}
        }

        dialogs.clear()
    }

    private fun dismissAllDialogFragments(fragmentManager: FragmentManager) {
        fragmentManager.fragments.forEach { fragment ->
            if (fragment is DialogFragment) {
                try {
                    fragment.dismissAllowingStateLoss()
                } catch (ignored: Exception) {}
            }
        }
    }

    fun dismissAll(fragmentManager: FragmentManager) {
        dismissAll()
        dismissAllDialogFragments(fragmentManager)
    }
}
