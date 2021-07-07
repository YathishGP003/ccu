package a75f.io.renatus

import a75f.io.renatus.BASE.BaseDialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * @author tcase@75f.io
 * Created on 6/14/21.
 */
class HyperstatCpuFragment: BaseDialogFragment() {

   companion object {
      const val ID = "HyperStatCpuFragment"
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
      return inflater.inflate(R.layout.fragment_hyperstat_cpu, container, false)
   }

   override fun onStart() {
      super.onStart()
      // todo: Found this code in other profile Fragments.  Wow!  There are so many bad things about this.  Plan -- for starters, convert to Dips, move up in class heirarchy.
      val dialog = dialog
      if (dialog != null) {
         val width = 1165 //ViewGroup.LayoutParams.WRAP_CONTENT;
         val height = 720 //ViewGroup.LayoutParams.WRAP_CONTENT;
         dialog.window?.setLayout(width, height)
      }
      // setTitle() -- todo: may need to add set title logic from FragmentCPUConfiguration to remove native title divider
   }


   override fun getIdString(): String {
      return ID
   }
}