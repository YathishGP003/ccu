package a75f.io.logic.controlcomponents.controls

class Constraint(private val condition: () -> Boolean) {
    operator fun invoke(): Boolean = condition()
}