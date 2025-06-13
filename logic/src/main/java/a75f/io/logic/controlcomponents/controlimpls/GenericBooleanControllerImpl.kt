package a75f.io.logic.controlcomponents.controlimpls

import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.GenericBooleanController

class GenericBooleanControllerImpl : GenericBooleanController {
    private var onConstraints: ArrayList<Constraint> = arrayListOf()
    private var offConstraints: ArrayList<Constraint> = arrayListOf()
    private var currentState: Boolean = false

    override fun getActiveControl(): Boolean {

        val isOn = onConstraints.any { it() }  // true if any on constraint is true
        val isOff = offConstraints.any { it() }  // true if any off constraint is true
        if (isOn) {
            currentState = true
            return true
        }
        if (isOff) {
            currentState = false
            return false
        }
        return currentState
    }

    override fun setOnConstraints(constraints: List<Constraint>) {
        onConstraints.addAll(constraints)
    }

    override fun setOffConstraints(constraints: List<Constraint>) {
        offConstraints.addAll(constraints)
    }

    fun addOnConstraint(constraint: Constraint) {
        onConstraints.add(constraint)
    }

    fun addOffConstraints(constraint: Constraint) {
        offConstraints.add(constraint)
    }
}