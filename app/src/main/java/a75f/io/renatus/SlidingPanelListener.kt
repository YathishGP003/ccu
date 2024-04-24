package a75f.io.renatus

import a75f.io.renatus.UtilityApplication.context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.slidingpanelayout.widget.SlidingPaneLayout


class SlidingPanelListener(private val slidingPaneLayout: SlidingPaneLayout) : SlidingPaneLayout.PanelSlideListener {

    val border_background_collapsed: Drawable? = ResourcesCompat.getDrawable(context.resources, R.drawable.border_background_collapsed, null)
    val border_background_expanded: Drawable? = ResourcesCompat.getDrawable(context.resources, R.drawable.border_background_expanded, null)

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        slidingPaneLayout.setBackgroundResource(0)
    }

    override fun onPanelOpened(panel: View) {
        slidingPaneLayout.setBackground(border_background_expanded)
    }

    override fun onPanelClosed(panel: View) {
        slidingPaneLayout.setBackground(border_background_collapsed)
    }


}