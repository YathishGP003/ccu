package a75f.io.logic.controlcomponents.controls

interface StagedBooleanController  : BooleanController {

    /**
     * Evaluate ON/OFF across `totalStages`, returning a list of active-stage indices.
     * E.g. for totalStages = 3, could return [1,2] meaning stage 1 and 2 are active.
     */
    fun getActiveControls(): List<Pair<Int, Boolean>>
}