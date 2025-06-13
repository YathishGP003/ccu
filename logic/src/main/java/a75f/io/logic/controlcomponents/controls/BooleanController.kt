package a75f.io.logic.controlcomponents.controls

interface BooleanController {
    /**
     * Define a list of conditions that must all pass to turn ON.
     */
    fun setOnConstraints(constraints: List<Constraint>)

    /**
     * Define a list of conditions that must all pass to turn OFF.
     */
    fun setOffConstraints(constraints: List<Constraint>)

}