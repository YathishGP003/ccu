package a75f.io.logic.schedule

enum class ScheduleGroup(val group: String) {
    SEVEN_DAY("7 day"), WEEKDAY_SATURDAY_SUNDAY("Weekday+Saturday+Sunday"),
    WEEKDAY_WEEKEND("Weekday+Weekend"), EVERYDAY("Everyday");
}

enum class PossibleScheduleImpactTable(val group: String) {
    SEVEN_DAY("7 day"), WEEKDAY_SATURDAY_SUNDAY("Weekday+Saturday+Sunday"),
    WEEKDAY_WEEKEND("Weekday+Weekend"), EVERYDAY("Everyday"), NAMED_SEVEN_DAY("7 day"),
    NAMED_WEEKDAY_SATURDAY_SUNDAY("Weekday+Saturday+Sunday"), NAMED_WEEKDAY_WEEKEND("Weekday+Weekend"),
    NAMED_EVERYDAY("Everyday")
}