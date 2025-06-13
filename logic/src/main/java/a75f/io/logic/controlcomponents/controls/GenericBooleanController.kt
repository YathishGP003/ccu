package a75f.io.logic.controlcomponents.controls

interface GenericBooleanController  : BooleanController {

    /**
     * Evaluate a single stage: `true` if active, `false` otherwise.
     */
    fun getActiveControl(): Boolean
}