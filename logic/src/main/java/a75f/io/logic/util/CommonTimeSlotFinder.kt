package a75f.io.logic.util


import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.Schedule
import a75f.io.api.haystack.util.TimeUtil.getEndTimeHr
import a75f.io.api.haystack.util.TimeUtil.getEndTimeMin
import a75f.io.logger.CcuLog
import a75f.io.logic.DefaultSchedules
import a75f.io.logic.L
import a75f.io.logic.bo.building.schedules.ScheduleUtil
import a75f.io.logic.schedule.ScheduleGroup
import org.joda.time.Interval

class CommonTimeSlotFinder {

    data class TimeSlot(val startHour: Int, val startMinute: Int, val endHour: Int, val endMinute: Int) {
        private fun overlaps(other: TimeSlot): Boolean {
            val thisStart = startHour * 60 + startMinute
            val thisEnd = endHour * 60 + endMinute
            val otherStart = other.startHour * 60 + other.startMinute
            val otherEnd = other.endHour * 60 + other.endMinute
            return thisStart < otherEnd && thisEnd > otherStart
        }
        fun toMinutes(): Pair<Int, Int> {
            val start = startHour * 60 + startMinute
            val end = endHour * 60 + endMinute
            return Pair(start, end)
        }
        fun intersection(other: TimeSlot): TimeSlot? {
            if (!overlaps(other)) return null
            val start = maxOf(startHour * 60 + startMinute, other.startHour * 60 + other.startMinute)
            val end = minOf(endHour * 60 + endMinute, other.endHour * 60 + other.endMinute)
            return TimeSlot(start / 60, start % 60, end / 60, end % 60)
        }

        override fun toString(): String {
            return "%02d:%02d - %02d:%02d".format(startHour, startMinute, endHour, endMinute)
        }
        fun isValidInterval(): Boolean {
            val startMinutes = startHour * 60 + startMinute
            val endMinutes = endHour * 60 + endMinute
            return startMinutes <= endMinutes
        }
    }

    private fun findSpills(buildingScheduleDays: List<TimeSlot>, zoneScheduleDays: List<TimeSlot>): List<TimeSlot> {
        return getUncommonIntervals(buildingScheduleDays, zoneScheduleDays)
    }

    private fun getUncommonIntervals(buildingIntervals: List<TimeSlot>, zoneIntervals: List<TimeSlot>): List<TimeSlot> {
        val uncommonIntervals = mutableListOf<TimeSlot>()
        val sortedBuildingIntervals = buildingIntervals.sortedWith(compareBy({ it.startHour }, { it.startMinute }))
        val sortedZoneIntervals = zoneIntervals.sortedWith(compareBy({ it.startHour }, { it.startMinute }))

        for (zone in sortedZoneIntervals) {
            val (zoneStartMinutes, zoneEndMinutes) = zone.toMinutes()
            var currentStartMinutes = zoneStartMinutes
            var hasOverlap = false

            for (building in sortedBuildingIntervals) {
                val (buildingStartMinutes, buildingEndMinutes) = building.toMinutes()

                if (currentStartMinutes < buildingEndMinutes) {
                    if (zoneEndMinutes <= buildingStartMinutes) {
                        // No overlap at all
                        uncommonIntervals.add(TimeSlot(
                            currentStartMinutes / 60, currentStartMinutes % 60,
                            zoneEndMinutes / 60, zoneEndMinutes % 60
                        ))
                        hasOverlap = true
                        break
                    } else {
                        // Overlap found
                        if (currentStartMinutes < buildingStartMinutes) {
                            uncommonIntervals.add(TimeSlot(
                                currentStartMinutes / 60, currentStartMinutes % 60,
                                buildingStartMinutes / 60, buildingStartMinutes % 60
                            ))
                        }
                        currentStartMinutes = maxOf(currentStartMinutes, buildingEndMinutes)
                    }
                }
            }

            if (!hasOverlap && currentStartMinutes < zoneEndMinutes) {
                uncommonIntervals.add(TimeSlot(
                    currentStartMinutes / 60, currentStartMinutes % 60,
                    zoneEndMinutes / 60, zoneEndMinutes % 60
                ))
            }
        }

        return uncommonIntervals.filter { (it.startHour * 60 + it.startMinute) != (it.endHour * 60 + it.endMinute) }.distinct()
    }
    private fun getCommonIntervals(schedule: Map<String, List<TimeSlot>>): List<TimeSlot> {
        if (schedule.isEmpty()) return emptyList()

        var commonIntervals = schedule.values.first().toMutableList()

        for (dayIntervals in schedule.values.drop(1)) {
            val newCommonIntervals = mutableListOf<TimeSlot>()
            for (commonInterval in commonIntervals) {
                for (dayInterval in dayIntervals) {
                    commonInterval.intersection(dayInterval)?.let { newCommonIntervals.add(it) }
                }
            }
            commonIntervals = newCommonIntervals
        }

        return commonIntervals
    }
    private fun removeOverlappingTimeSlots(schedules: List<TimeSlot>, intervalToRemove: TimeSlot): List<TimeSlot> {
        val filteredSchedules = mutableListOf<TimeSlot>()

        for (schedule in schedules) {
            val scheduleStart = schedule.startHour * 60 + schedule.startMinute
            val scheduleEnd = schedule.endHour * 60 + schedule.endMinute
            val removeStart = intervalToRemove.startHour * 60 + intervalToRemove.startMinute
            val removeEnd = intervalToRemove.endHour * 60 + intervalToRemove.endMinute

            if (scheduleEnd <= removeStart || scheduleStart >= removeEnd) {
                filteredSchedules.add(schedule)
            } else {
                if (scheduleStart < removeStart && scheduleEnd > removeEnd) {
                    filteredSchedules.add(TimeSlot(schedule.startHour, schedule.startMinute, intervalToRemove.startHour, intervalToRemove.startMinute))
                    filteredSchedules.add(TimeSlot(intervalToRemove.endHour, intervalToRemove.endMinute, schedule.endHour, schedule.endMinute))
                } else if (removeStart in (scheduleStart + 1) until scheduleEnd) {
                    filteredSchedules.add(TimeSlot(schedule.startHour, schedule.startMinute, intervalToRemove.startHour, intervalToRemove.startMinute))
                } else if (removeEnd in (scheduleStart + 1) until scheduleEnd) {
                    filteredSchedules.add(TimeSlot(intervalToRemove.endHour, intervalToRemove.endMinute, schedule.endHour, schedule.endMinute))
                }
            }
        }

        return filteredSchedules
    }

    private fun complainedTimeslots(schedules: List<TimeSlot>, intervalToRemove: TimeSlot?) : List<TimeSlot> {
        return removeOverlappingTimeSlots(schedules, intervalToRemove!!)
    }

    private fun removeOverlappingTimeSlot(schedule: TimeSlot, intervalToRemove: TimeSlot): List<TimeSlot> {
        val scheduleStart = schedule.startHour * 60 + schedule.startMinute
        val scheduleEnd = schedule.endHour * 60 + schedule.endMinute
        val removeStart = intervalToRemove.startHour * 60 + intervalToRemove.startMinute
        val removeEnd = intervalToRemove.endHour * 60 + intervalToRemove.endMinute

        val resultingSlots = mutableListOf<TimeSlot>()

        if (scheduleEnd <= removeStart || scheduleStart >= removeEnd) {
            resultingSlots.add(schedule)
        } else {
            if (scheduleStart < removeStart) {
                // Add the part before the removal interval
                resultingSlots.add(TimeSlot(
                    schedule.startHour,
                    schedule.startMinute,
                    removeStart / 60,
                    removeStart % 60
                ))
            }

            if (scheduleEnd > removeEnd) {
                // Add the part after the removal interval
                resultingSlots.add(TimeSlot(
                    removeEnd / 60,
                    removeEnd % 60,
                    schedule.endHour,
                    schedule.endMinute
                ))
            }
        }

        return resultingSlots
    }
    private fun getComplainedTimeslot(schedules: TimeSlot, intervalsToRemove: List<TimeSlot>): List<TimeSlot> {
        var remainingTimeSlots = listOf(schedules)

        for (interval in intervalsToRemove) {
            remainingTimeSlots = remainingTimeSlots.flatMap { timeSlot ->
                removeOverlappingTimeSlot(timeSlot, interval)
            }
        }
        return remainingTimeSlots
    }
    fun getCommonTimeSlot(
        scheduleGroup: Int, buildingDays: List<Schedule.Days>, zoneDays: List<Schedule.Days>, isScheduleGroupChanged : Boolean,
    ) : List<MutableList<TimeSlot>> {

        val mondayDays = mutableListOf<Schedule.Days>()
        val tuesdayDays = mutableListOf<Schedule.Days>()
        val wednesdayDays = mutableListOf<Schedule.Days>()
        val thursdayDays = mutableListOf<Schedule.Days>()
        val fridayDays = mutableListOf<Schedule.Days>()
        val saturdayDays = mutableListOf<Schedule.Days>()
        val sundayDays = mutableListOf<Schedule.Days>()

        val zoneMondayDays = mutableListOf<Schedule.Days>()
        val zoneTuesdayDays = mutableListOf<Schedule.Days>()
        val zoneWednesdayDays = mutableListOf<Schedule.Days>()
        val zoneThursdayDays = mutableListOf<Schedule.Days>()
        val zoneFridayDays = mutableListOf<Schedule.Days>()
        val zoneSaturdayDays = mutableListOf<Schedule.Days>()
        val zoneSundayDays = mutableListOf<Schedule.Days>()

        val dayMapping = listOf(
            mondayDays to zoneMondayDays,
            tuesdayDays to zoneTuesdayDays,
            wednesdayDays to zoneWednesdayDays,
            thursdayDays to zoneThursdayDays,
            fridayDays to zoneFridayDays,
            saturdayDays to zoneSaturdayDays,
            sundayDays to zoneSundayDays
        )

        fun addDaysToAppropriateLists(days: List<Schedule.Days>, targetLists: List<MutableList<Schedule.Days>>) {
            for (day in days) {
                targetLists[day.day].add(day)
            }
        }

        addDaysToAppropriateLists(zoneDays, dayMapping.map { it.second })
        addDaysToAppropriateLists(buildingDays, dayMapping.map { it.first })

        fun createDayScheduleWithBoundaries(timeSlots: List<TimeSlot>,
                                            boundaries: List<TimeSlot>
        ): List<TimeSlot> {
            val result = mutableListOf<TimeSlot>()

            for (boundary in boundaries) {
                for (timeSlot in timeSlots) {
                    // Check if the time slot overlaps with the boundary
                    if ((timeSlot.startHour < boundary.endHour || (timeSlot.startHour == boundary.endHour && timeSlot.startMinute < boundary.endMinute)) &&
                        (timeSlot.endHour > boundary.startHour || (timeSlot.endHour == boundary.startHour && timeSlot.endMinute > boundary.startMinute))) {

                        val startHour = maxOf(boundary.startHour, timeSlot.startHour)
                        val startMinute = if (startHour == boundary.startHour) maxOf(boundary.startMinute, timeSlot.startMinute) else timeSlot.startMinute

                        val endHour = minOf(boundary.endHour, timeSlot.endHour)
                        val endMinute = if(endHour < timeSlots.maxByOrNull { it.endHour}!!.endHour) boundary.endMinute
                        else if (endHour == boundary.endHour) minOf(boundary.endMinute, timeSlot.endMinute)
                        else timeSlot.endMinute

                        if(endHour < timeSlots.maxByOrNull { it.endHour}!!.endHour) maxOf(boundary.endMinute, timeSlot.endMinute)
                        // Only add valid time slots within the boundary limits
                        if (startHour < endHour || (startHour == endHour && startMinute <= endMinute)) {
                            result.add(TimeSlot(startHour, startMinute, endHour, endMinute))
                        }
                    }
                }
            }
            return result
        }
        fun generateWeekendSchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.SATURDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, saturdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.SUNDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, sundayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }

        fun generateWeekdaySchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.MONDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, mondayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.TUESDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, tuesdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.WEDNESDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, wednesdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.THURSDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, thursdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.FRIDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, fridayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }

        fun generateEverydaySchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.MONDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, mondayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.TUESDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, tuesdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.WEDNESDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, wednesdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.THURSDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, thursdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.FRIDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, fridayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.SATURDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, saturdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                ),
                DAYS.SUNDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, sundayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }

        fun generateMonDaySchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.MONDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, mondayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }

        fun generateTueDaySchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.TUESDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, tuesdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }

        fun generateWedDaySchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.WEDNESDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, wednesdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }

        fun generateThuDaySchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.THURSDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, thursdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }

        fun generateFriDaySchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.FRIDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, fridayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }

        fun generateSunDaySchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.SUNDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, sundayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }

        fun generateSaturdaySchedule1(boundaryDays: MutableList<Schedule.Days>): Map<String, List<TimeSlot>> {
            return mapOf(
                DAYS.SATURDAY.name to createDayScheduleWithBoundaries(
                    boundaryDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }, saturdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                )
            )
        }


        when (scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                val everydayCommonIntervalsFinal = mutableListOf<TimeSlot>()

                val everydayCommonIntervals = getCommonIntervals(generateEverydaySchedule1(
                    if(isScheduleGroupChanged) zoneMondayDays else {mondayDays})
                ).toMutableList()
                everydayCommonIntervalsFinal.addAll(everydayCommonIntervals)

                return listOf(everydayCommonIntervalsFinal)
            }

            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                val weekdayIntervalsFinal = mutableListOf<TimeSlot>()
                val weekdayCommonIntervals = getCommonIntervals(generateWeekdaySchedule1(
                    if(isScheduleGroupChanged) zoneMondayDays else {mondayDays})
                ).toMutableList()
                weekdayIntervalsFinal.addAll(weekdayCommonIntervals)

                val weekEndIntervalsFinal = mutableListOf<TimeSlot>()
                val saturdayCommonIntervals = getCommonIntervals(
                    generateWeekendSchedule1(
                        if(isScheduleGroupChanged) zoneSaturdayDays else {saturdayDays}
                    )).toMutableList()
                weekEndIntervalsFinal.addAll(saturdayCommonIntervals)
                return listOf(weekdayIntervalsFinal, weekEndIntervalsFinal)
            }

            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                val weekdayIntervalsFinal = mutableListOf<TimeSlot>()
                val weekdayCommonIntervals = getCommonIntervals(generateWeekdaySchedule1(if(isScheduleGroupChanged) zoneMondayDays else {mondayDays})).toMutableList()
                weekdayIntervalsFinal.addAll(weekdayCommonIntervals)

                val saturdayFinalIntervals = mutableListOf<TimeSlot>()
                val saturdayCommonIntervals = getCommonIntervals(generateSaturdaySchedule1(if(isScheduleGroupChanged) zoneSaturdayDays else {saturdayDays})).toMutableList()
                saturdayFinalIntervals.addAll(saturdayCommonIntervals)

                val sundayFinalIntervals = mutableListOf<TimeSlot>()
                val sundayCommonIntervals = getCommonIntervals(generateSunDaySchedule1(if(isScheduleGroupChanged) zoneSundayDays else {sundayDays})).toMutableList()
                sundayFinalIntervals.addAll(sundayCommonIntervals)

                return listOf(
                    weekdayIntervalsFinal,
                    saturdayFinalIntervals,
                    sundayFinalIntervals
                )
            }

            else -> {
                val mondayCommonIntervalsFinal = mutableListOf<TimeSlot>()
                val mondayCommonIntervals = getCommonIntervals(
                    generateMonDaySchedule1(
                        if(isScheduleGroupChanged) zoneMondayDays else {mondayDays})
                ).toMutableList()
                mondayCommonIntervalsFinal.addAll(mondayCommonIntervals)

                val tuesdayCommonIntervalsFinal = mutableListOf<TimeSlot>()
                val tuesdayCommonIntervals = getCommonIntervals(
                    generateTueDaySchedule1(
                        if(isScheduleGroupChanged) zoneTuesdayDays else {tuesdayDays})
                ).toMutableList()
                tuesdayCommonIntervalsFinal.addAll(tuesdayCommonIntervals)

                val wednesdayCommonIntervalsFinal = mutableListOf<TimeSlot>()
                val wednesdayCommonIntervals = getCommonIntervals(
                    generateWedDaySchedule1(
                        if(isScheduleGroupChanged) zoneWednesdayDays else {wednesdayDays})
                ).toMutableList()
                wednesdayCommonIntervalsFinal.addAll(wednesdayCommonIntervals)

                val thursdayCommonIntervalsFinal = mutableListOf<TimeSlot>()
                val thursdayCommonIntervals = getCommonIntervals(
                    generateThuDaySchedule1(
                        if(isScheduleGroupChanged) zoneThursdayDays else {thursdayDays})
                ).toMutableList()
                thursdayCommonIntervalsFinal.addAll(thursdayCommonIntervals)

                val fridayCommonIntervalsFinal = mutableListOf<TimeSlot>()
                val fridayCommonIntervals = getCommonIntervals(
                    generateFriDaySchedule1(
                        if(isScheduleGroupChanged) zoneFridayDays else {fridayDays})
                ).toMutableList()
                fridayCommonIntervalsFinal.addAll(fridayCommonIntervals)

                val saturdayCommonIntervalsFinal = mutableListOf<TimeSlot>()
                val saturdayCommonIntervals = getCommonIntervals(
                    generateSaturdaySchedule1(
                        if(isScheduleGroupChanged) zoneSaturdayDays else {saturdayDays})
                ).toMutableList()
                saturdayCommonIntervalsFinal.addAll(saturdayCommonIntervals)

                val sundayCommonIntervalsFinal = mutableListOf<TimeSlot>()
                val sundayCommonIntervals = getCommonIntervals(
                    generateSunDaySchedule1(
                        if(isScheduleGroupChanged) zoneSundayDays else {sundayDays})
                ).toMutableList()
                sundayCommonIntervalsFinal.addAll(sundayCommonIntervals)

                return listOf(
                    mondayCommonIntervalsFinal, tuesdayCommonIntervalsFinal, wednesdayCommonIntervalsFinal,
                    thursdayCommonIntervalsFinal, fridayCommonIntervalsFinal, saturdayCommonIntervalsFinal,
                    sundayCommonIntervalsFinal)
            }
        }
    }
    fun getUnCommonTimeSlot(
        scheduleGroup: Int,
        commonIntervals: List<List<TimeSlot>>,
        zoneDays: ArrayList<Schedule.Days>
    ) : List<List<TimeSlot>> {
        val mondayZoneDays = mutableListOf<Schedule.Days>()
        val tuesdayDays = mutableListOf<Schedule.Days>()
        val wednesdayDays = mutableListOf<Schedule.Days>()
        val thursdayDays = mutableListOf<Schedule.Days>()
        val fridayDays = mutableListOf<Schedule.Days>()
        val saturdayDays = mutableListOf<Schedule.Days>()
        val sundayDays = mutableListOf<Schedule.Days>()


        fun addDaysToAppropriateLists(
            days: List<Schedule.Days>,
            targetLists: List<MutableList<Schedule.Days>>
        ) {
            for (day in days) {
                targetLists[day.day].add(day)
            }
        }

        addDaysToAppropriateLists(
            zoneDays, listOf(
                mondayZoneDays,
                tuesdayDays,
                wednesdayDays,
                thursdayDays,
                fridayDays,
                saturdayDays,
                sundayDays
            )
        )
        when (scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                val unCommonIntervals = mutableListOf<List<TimeSlot>>()

                val weekdayBuildingCommonIntervals = commonIntervals.first()
                val weekdaysFinal = mutableListOf<TimeSlot>()
                val weekdayZoneCommonIntervals =
                    mondayZoneDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val weekdaySpills =
                    findSpills(weekdayBuildingCommonIntervals, weekdayZoneCommonIntervals)
                weekdaysFinal.addAll(weekdaySpills)
                unCommonIntervals.addAll(listOf(weekdaysFinal))
                return unCommonIntervals
            }

            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                val unCommonIntervals = mutableListOf<List<TimeSlot>>()

                val weekdayBuildingCommonIntervals = commonIntervals.first()
                val weekEndBuildingCommonIntervals = commonIntervals.last()

                val weekdaysFinal = mutableListOf<TimeSlot>()
                val weekdayZoneCommonIntervals =
                    mondayZoneDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val weekdaySpills =
                    findSpills(weekdayBuildingCommonIntervals, weekdayZoneCommonIntervals)
                weekdaysFinal.addAll(weekdaySpills)
                unCommonIntervals.addAll(listOf(weekdaysFinal))

                val weekendZoneCommonIntervals =
                    saturdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val weekendSpills =
                    findSpills(weekEndBuildingCommonIntervals, weekendZoneCommonIntervals)
                val weekendFinalIntervals = mutableListOf<TimeSlot>()
                weekendFinalIntervals.addAll(weekendSpills)
                unCommonIntervals.addAll(listOf(weekendFinalIntervals))

                return unCommonIntervals
            }

            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                val unCommonIntervals = mutableListOf<List<TimeSlot>>()

                val weekdayBuildingCommonIntervals = commonIntervals.first()
                val saturdayBuildingCommonIntervals = commonIntervals[1]
                val sundayBuildingCommonIntervals = commonIntervals[2]

                val weekdaysFinal = mutableListOf<TimeSlot>()
                val weekdayZoneCommonIntervals =
                    mondayZoneDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val weekdaySpills =
                    findSpills(weekdayBuildingCommonIntervals, weekdayZoneCommonIntervals)
                weekdaysFinal.addAll(weekdaySpills)
                unCommonIntervals.addAll(listOf(weekdaysFinal))

                val saturdayZoneCommonIntervals =
                    saturdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val saturdaySpills =
                    findSpills(saturdayBuildingCommonIntervals, saturdayZoneCommonIntervals)
                val saturdayFinalIntervals = mutableListOf<TimeSlot>()
                saturdayFinalIntervals.addAll(saturdaySpills)
                unCommonIntervals.addAll(listOf(saturdayFinalIntervals))

                val sundayZoneCommonIntervals =
                    sundayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val sundaySpills =
                    findSpills(sundayBuildingCommonIntervals, sundayZoneCommonIntervals)
                val sundayFinalIntervals = mutableListOf<TimeSlot>()
                sundayFinalIntervals.addAll(sundaySpills)
                unCommonIntervals.addAll(listOf(sundayFinalIntervals))

                return unCommonIntervals
            }

            else -> {
                val mondayCommonIntervals = commonIntervals.first()
                val tuesdayCommonIntervals = commonIntervals[1]
                val wednesdayCommonIntervals = commonIntervals[2]
                val thursdayCommonIntervals = commonIntervals[3]
                val fridayCommonIntervals = commonIntervals[4]
                val saturdayCommonIntervals = commonIntervals[5]
                val sundayCommonIntervals = commonIntervals[6]
                val unCommonIntervals = mutableListOf<List<TimeSlot>>()

                val weekdaysFinal = mutableListOf<TimeSlot>()
                val weekdayZoneCommonIntervals =
                    mondayZoneDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val weekdaySpills = findSpills(mondayCommonIntervals, weekdayZoneCommonIntervals)
                weekdaysFinal.addAll(weekdaySpills)
                unCommonIntervals.addAll(listOf(weekdaysFinal))


                val tuesdayFinal = mutableListOf<TimeSlot>()
                val tuesdayZoneCommonIntervals =
                    tuesdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val tuesdaySpills = findSpills(tuesdayCommonIntervals, tuesdayZoneCommonIntervals)
                tuesdayFinal.addAll(tuesdaySpills)
                unCommonIntervals.addAll(listOf(tuesdayFinal))

                val wednesdayFinal = mutableListOf<TimeSlot>()
                val wednesdayZoneCommonIntervals =
                    wednesdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val wednesdaySpills = findSpills(wednesdayCommonIntervals, wednesdayZoneCommonIntervals)
                wednesdayFinal.addAll(wednesdaySpills)
                unCommonIntervals.addAll(listOf(wednesdayFinal))

                val thursdayFinal = mutableListOf<TimeSlot>()
                val thursdayZoneCommonIntervals =
                    thursdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val thursdaySpills = findSpills(thursdayCommonIntervals, thursdayZoneCommonIntervals)
                thursdayFinal.addAll(thursdaySpills)
                unCommonIntervals.addAll(listOf(thursdayFinal))

                val fridayFinal = mutableListOf<TimeSlot>()
                val fridayZoneCommonIntervals =
                    fridayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val fridaySpills = findSpills(fridayCommonIntervals, fridayZoneCommonIntervals)
                fridayFinal.addAll(fridaySpills)
                unCommonIntervals.addAll(listOf(fridayFinal))

                val saturdayFinal = mutableListOf<TimeSlot>()
                val saturdayZoneCommonIntervals =
                    saturdayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val saturdaySpills = findSpills(saturdayCommonIntervals, saturdayZoneCommonIntervals)
                saturdayFinal.addAll(saturdaySpills)
                unCommonIntervals.addAll(listOf(saturdayFinal))

                val sundayFinal = mutableListOf<TimeSlot>()
                val sundayZoneCommonIntervals =
                    sundayDays.map { TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm) }
                val sundaySpills = findSpills(sundayCommonIntervals, sundayZoneCommonIntervals)
                sundayFinal.addAll(sundaySpills)
                unCommonIntervals.addAll(listOf(sundayFinal))



                return unCommonIntervals

            }
        }
    }

    fun getSpilledZones(
        mSchedule: Schedule,
        uncommonIntervals: List<List<TimeSlot>>
    ): List<String> {
        val result = when (mSchedule.scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                listOfNotNull(
                    "All Days ${
                        uncommonIntervals[0].takeIf { it.isNotEmpty() }?.joinToString(", ") { "(${it})" } ?: ""
                    }"
                )
            }

            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                listOfNotNull(
                    uncommonIntervals[0].takeIf { it.isNotEmpty() }
                        ?.let { "Weekday ${it.joinToString(", ") { "(${it})" }}" },
                    uncommonIntervals[1].takeIf { it.isNotEmpty() }
                        ?.let { "Weekend ${it.joinToString(", ") { "(${it})" }}" }
                )
            }

            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                listOfNotNull(
                    uncommonIntervals[0].takeIf { it.isNotEmpty() }
                        ?.let { "Weekday ${it.joinToString(", ") { "(${it})" }}" },
                    uncommonIntervals[1].takeIf { it.isNotEmpty() }
                        ?.let { "Saturday ${it.joinToString(", ") { "(${it})" }}" },
                    uncommonIntervals[2].takeIf { it.isNotEmpty() }
                        ?.let { "Sunday ${it.joinToString(", ") { "(${it})" }}" }
                )
            }

            else -> {
                listOfNotNull(
                    uncommonIntervals[DAYS.MONDAY.ordinal].takeIf { it.isNotEmpty() }
                        ?.let { "Monday ${it.joinToString(", ") { "(${it})" }}" },
                    uncommonIntervals[DAYS.TUESDAY.ordinal].takeIf { it.isNotEmpty() }
                        ?.let { "Tuesday ${it.joinToString(", ") { "(${it})" }}" },
                    uncommonIntervals[DAYS.WEDNESDAY.ordinal].takeIf { it.isNotEmpty() }
                        ?.let { "Wednesday ${it.joinToString(", ") { "(${it})" }}" },
                    uncommonIntervals[DAYS.THURSDAY.ordinal].takeIf { it.isNotEmpty() }
                        ?.let { "Thursday ${it.joinToString(", ") { "(${it})" }}" },
                    uncommonIntervals[DAYS.FRIDAY.ordinal].takeIf { it.isNotEmpty() }
                        ?.let { "Friday ${it.joinToString(", ") { "(${it})" }}" },
                    uncommonIntervals[DAYS.SATURDAY.ordinal].takeIf { it.isNotEmpty() }
                        ?.let { "Saturday ${it.joinToString(", ") { "(${it})" }}" },
                    uncommonIntervals[DAYS.SUNDAY.ordinal].takeIf { it.isNotEmpty() }
                        ?.let { "Sunday ${it.joinToString(", ") { "(${it})" }}" }
                )
            }
        }

        return result
    }
    fun getNonSevenDaySpilledZonesList(
        mSchedule: Schedule,
        uncommonIntervals: List<List<TimeSlot>>
    ): List<String> {
        val spillZonesMap = mutableMapOf<String, MutableList<String>>()

        fun addIntervalsToMap(day: String, intervals: List<TimeSlot>) {
            if (intervals.isNotEmpty()) {
                val intervalsString = intervals.joinToString(", ") { "(${it})" }
                if (spillZonesMap.containsKey(day)) {
                    spillZonesMap[day]?.add(intervalsString)
                } else {
                    spillZonesMap[day] = mutableListOf(intervalsString)
                }
            }
        }

        when (mSchedule.scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                addIntervalsToMap("All Days", uncommonIntervals[0])
            }

            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                addIntervalsToMap("Weekday", uncommonIntervals[0])
                addIntervalsToMap("Weekend", uncommonIntervals[1])
            }

            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                addIntervalsToMap("Weekday", uncommonIntervals[0])
                addIntervalsToMap("Saturday", uncommonIntervals[1])
                addIntervalsToMap("Sunday", uncommonIntervals[2])
            }
        }

        val resultList = spillZonesMap.map { (day, intervals) ->
            "$day ${intervals.joinToString(", ")}"
        }

        return resultList
    }

    fun getSpilledZonesForSevenDayGroup(spillsMap: Map<String, List<Interval>>): List<String> {
        val spillZonesMessages = mutableMapOf<String, MutableList<String>>()

        for ((_, intervals) in spillsMap) {
            for (interval in intervals) {
                if(isIntervalNotSpilled(interval)) continue
                if(isOverNightSchedule(interval)){
                    if(interval.start.hourOfDay == 23 && interval.start.minuteOfHour == 59){ continue }
                    val dayString = getDayString(interval.start.dayOfWeek)
                    val spillMessage = "(${interval.start.hourOfDay().get()}:" +
                            "${ if (interval.start.minuteOfHour().get() == 0) "00"
                            else interval.start.minuteOfHour().get()} -" +
                            " 24:" + "00)"
                    spillZonesMessages.getOrPut(dayString) { mutableListOf() }.add(spillMessage)
                }

                val dayString = if(isOverNightSchedule(interval)){getDayString(interval.start.dayOfWeek + 1)}
                else getDayString(interval.start.dayOfWeek)
                val spillMessage = "(${ if(isOverNightSchedule(interval)) "00"
                else interval.start.hourOfDay().get()}:" +
                        "${ if (interval.start.minuteOfHour().get() == 0 ||
                            isOverNightSchedule(interval)) "00"
                        else interval.start.minuteOfHour().get()} -" +
                        " ${getEndTimeHr(interval.end.hourOfDay().get(),
                            interval.end.minuteOfHour().get())}:" +
                        "${if (getEndTimeMin(interval.end.hourOfDay().get(),
                                interval.end.minuteOfHour().get()) == 0) "00"
                        else getEndTimeMin(interval.end.hourOfDay().get(),
                            interval.end.minuteOfHour().get())})"

                spillZonesMessages.getOrPut(dayString) { mutableListOf() }.add(spillMessage)
            }
        }

        val resultList = spillZonesMessages.map { (day, intervals) ->
            "$day ${intervals.joinToString(", ")}"
        }

        return resultList
    }
    private fun isOverNightSchedule(interval: Interval): Boolean {
        return interval.start.hourOfDay > interval.end.hourOfDay ||
                (interval.start.hourOfDay == interval.end.hourOfDay &&
                        interval.start.minuteOfHour > interval.end.minuteOfHour)
    }

    private fun isIntervalNotSpilled(interval: Interval): Boolean {
        return interval.start.hourOfDay == 23 && interval.start.minuteOfHour == 59 &&
                (interval.end.hourOfDay == 0 && interval.end.minuteOfHour == 0 ||
                        interval.end.hourOfDay == 24)
    }

    fun trimScheduleTowardCommonTimeSlotAndSync(schedule:Schedule, commonIntervals: List<List<TimeSlot>>,
                                                ccuHsApi: CCUHsApi) {
        schedule.days.clear()
        when (schedule.scheduleGroup) {
            ScheduleGroup.EVERYDAY.ordinal -> {
                for (i in commonIntervals[0]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.MONDAY.ordinal,
                            DAYS.SUNDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
            }
            ScheduleGroup.WEEKDAY_WEEKEND.ordinal -> {
                for (i in commonIntervals[0]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.MONDAY.ordinal,
                            DAYS.FRIDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
                for (i in commonIntervals[1]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.SATURDAY.ordinal,
                            DAYS.SUNDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )

                }
            }
            ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal -> {
                for (i in commonIntervals[0]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.MONDAY.ordinal,
                            DAYS.FRIDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
                for (i in commonIntervals[1]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.SATURDAY.ordinal,
                            DAYS.SATURDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
                for (i in commonIntervals[2]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.SUNDAY.ordinal,
                            DAYS.SUNDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }

            }
            else -> {
                for (i in commonIntervals[0]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.MONDAY.ordinal,
                            DAYS.MONDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
                for (i in commonIntervals[1]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.TUESDAY.ordinal,
                            DAYS.TUESDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
                for (i in commonIntervals[2]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.WEDNESDAY.ordinal,
                            DAYS.WEDNESDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
                for (i in commonIntervals[3]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.THURSDAY.ordinal,
                            DAYS.THURSDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
                for (i in commonIntervals[4]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.FRIDAY.ordinal,
                            DAYS.FRIDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
                for (i in commonIntervals[5]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.SATURDAY.ordinal,
                            DAYS.SATURDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
                for (i in commonIntervals[6]) {
                    schedule.days.addAll(
                        DefaultSchedules.getDefaultDays(
                            schedule.roomRef,
                            DAYS.SUNDAY.ordinal,
                            DAYS.SUNDAY.ordinal,
                            i.startHour,
                            i.startMinute,
                            i.endHour,
                            i.endMinute
                        )
                    )
                }
            }
        }

        RxjavaUtil.executeBackground {
            if (schedule.isZoneSchedule) {
                ccuHsApi.updateZoneSchedule(schedule, schedule.roomRef)
            } else {
                ccuHsApi.updateSchedule(schedule)
            }
            ccuHsApi.syncEntityTree()
        }
    }

    fun forceTrimWeekdayWeekEndSchedule(
        mSchedule: Schedule,
        daysToBeAdded: ArrayList<DAYS>,
        uncommonIntervals: List<List<TimeSlot>>,
        daysArrayList: ArrayList<Schedule.Days>
    ) {
        val weekdayArrayList = getDays(mSchedule.days, if(daysArrayList.any{it.day == 0}) 0 else {-1})
        val weekendArrayList = getDays(mSchedule.days, if(daysArrayList.any{it.day == 5}) 5 else {-1})

        val weekdayListOfTimeSlot: List<TimeSlot> = weekdayArrayList.map {
            TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm)
        }
        val weekendListOfTimeSlot:List<TimeSlot> = weekendArrayList.map {
            TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm)
        }

        val itemsToRemove = mutableListOf<Int>()
        val itemsToAdd = mutableListOf<Schedule.Days>()
        for (i in mSchedule.days) {
            if (daysToBeAdded.contains(DAYS.values()[i.day])) {
                if(i.day in 0..4) {
                    if (weekdayListOfTimeSlot.isNotEmpty()) {
                        val d: Schedule.Days = i
                        val matchingTimeSlot = weekdayListOfTimeSlot.find { it.startHour == d.sthh && it.startMinute == d.stmm }
                        if(matchingTimeSlot == null) {
                            continue
                        }
                        val complainedTimeslots = getComplainedTimeslot(matchingTimeSlot,
                            uncommonIntervals[0]
                        )
                        complainedTimeslots.forEach { complainedTimeslot ->
                            val dayBO = Schedule.Days().apply {
                                ethh = complainedTimeslot.endHour
                                sthh = complainedTimeslot.startHour
                                etmm = complainedTimeslot.endMinute
                                stmm = complainedTimeslot.startMinute
                                heatingVal = d.heatingVal
                                coolingVal = d.coolingVal
                                isSunset = false
                                isSunrise = false
                                day = d.day
                                heatingUserLimitMin = d.heatingUserLimitMin
                                heatingUserLimitMax = d.heatingUserLimitMax
                                coolingUserLimitMin = d.coolingUserLimitMin
                                coolingUserLimitMax = d.coolingUserLimitMax
                                heatingDeadBand = d.heatingDeadBand
                                coolingDeadBand = d.coolingDeadBand
                            }
                            itemsToAdd.add(dayBO)
                        }
                        itemsToRemove.add(i.day)

                    } else {
                        if (uncommonIntervals[0].isNotEmpty()) {
                            itemsToRemove.add(i.day)
                        }
                    }
                } else {
                    if (weekendListOfTimeSlot.isNotEmpty()) {
                        val d: Schedule.Days = i
                        val matchingTimeSlot = weekendListOfTimeSlot.find { it.startHour == d.sthh && it.startMinute == d.stmm }
                        if(matchingTimeSlot == null) {
                            continue
                        }
                        val complainedTimeslots = getComplainedTimeslot(
                            matchingTimeSlot,
                            uncommonIntervals[1]
                        )
                        complainedTimeslots.forEach { complainedTimeslot ->
                            val dayBO = Schedule.Days().apply {
                                ethh = complainedTimeslot.endHour
                                sthh = complainedTimeslot.startHour
                                etmm = complainedTimeslot.endMinute
                                stmm = complainedTimeslot.startMinute
                                heatingVal = d.heatingVal
                                coolingVal = d.coolingVal
                                isSunset = false
                                isSunrise = false
                                day = d.day
                                heatingUserLimitMin = d.heatingUserLimitMin
                                heatingUserLimitMax = d.heatingUserLimitMax
                                coolingUserLimitMin = d.coolingUserLimitMin
                                coolingUserLimitMax = d.coolingUserLimitMax
                                heatingDeadBand = d.heatingDeadBand
                                coolingDeadBand = d.coolingDeadBand
                            }
                            itemsToAdd.add(dayBO)
                        }
                        itemsToRemove.add(i.day)
                    } else {
                        if (uncommonIntervals[1].isNotEmpty()) {
                            itemsToRemove.add(i.day)
                        }
                    }
                }
            }
        }
        for(i in itemsToRemove) {
            mSchedule.days.removeAll { it.day == i }
        }
        mSchedule.days.addAll(itemsToAdd)
    }

    fun forceTrimEveryDaySchedule(
        mSchedule: Schedule,
        daysToBeAdded: ArrayList<DAYS>,
        uncommonIntervals: List<List<TimeSlot>>,
        daysArrayList: ArrayList<Schedule.Days>
    ) {
        val weekdayArrayList = if (daysArrayList.any { it.day == 0 }) {
            getDays(mSchedule.days, 0)
        } else {
            emptyList()
        }

        val everydayListOfTimeSlot: List<TimeSlot> = weekdayArrayList.map {
            TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm)
        }

        val everydayFinal = uncommonIntervals.getOrNull(0)?.getOrNull(0)?.let {
            complainedTimeslots(everydayListOfTimeSlot, it)
        } ?: emptyList()

        val itemsToRemove = mutableListOf<Int>()
        val itemsToAdd = mutableListOf<Schedule.Days>()

        for (i in mSchedule.days) {
            if (daysToBeAdded.contains(DAYS.values()[i.day])) {
                if (everydayFinal.isNotEmpty()) {
                    val d: Schedule.Days = i
                    val matchingTimeSlot =
                        everydayFinal.find { it.startHour == d.sthh && it.startMinute == d.stmm }
                    if(matchingTimeSlot == null) {
                        continue
                    }
                    val complainedTimeslots = getComplainedTimeslot(
                        matchingTimeSlot,
                        uncommonIntervals[0]
                    )
                    complainedTimeslots.forEach { complainedTimeslot ->
                        val dayBO = Schedule.Days().apply {
                            ethh = complainedTimeslot.endHour
                            sthh = complainedTimeslot.startHour
                            etmm = complainedTimeslot.endMinute
                            stmm = complainedTimeslot.startMinute
                            heatingVal = d.heatingVal
                            coolingVal = d.coolingVal
                            isSunset = false
                            isSunrise = false
                            day = d.day
                            heatingUserLimitMin = d.heatingUserLimitMin
                            heatingUserLimitMax = d.heatingUserLimitMax
                            coolingUserLimitMin = d.coolingUserLimitMin
                            coolingUserLimitMax = d.coolingUserLimitMax
                            heatingDeadBand = d.heatingDeadBand
                            coolingDeadBand = d.coolingDeadBand
                        }
                        itemsToAdd.add(dayBO)
                    }
                    itemsToRemove.add(i.day)

                } else {
                    if (uncommonIntervals[0].isNotEmpty()) {
                        itemsToRemove.add(i.day)
                    }
                }
            }

        }

        for (i in itemsToRemove) {
            mSchedule.days.removeAll { it.day == i }
        }
        mSchedule.days.addAll(itemsToAdd)
    }


    fun forceTrimWeekdaySaturdayAndSundaySchedule(
        mSchedule: Schedule,
        daysToBeAdded: ArrayList<DAYS>,
        uncommonIntervals: List<List<TimeSlot>>,
        daysArrayList: ArrayList<Schedule.Days>
    ) {
        val weekdayArrayList = getDays(mSchedule.days, if(daysArrayList.any{it.day == 0}) 0 else {-1})
        val saturdayArrayList = getDays(mSchedule.days, if(daysArrayList.any{it.day == 5}) 5 else {-1})
        val sundayArrayList = getDays(mSchedule.days, if(daysArrayList.any{it.day == 6}) 6 else {-1})

        val weekdayListOfTimeSlot: List<TimeSlot> = weekdayArrayList.map {
            TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm)
        }
        val saturdayListOfTimeSlot:List<TimeSlot> = saturdayArrayList.map {
            TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm)
        }
        val sundayListOfTimeSlot:List<TimeSlot> = sundayArrayList.map {
            TimeSlot(it.sthh, it.stmm, it.ethh, it.etmm)
        }

        val weekdayFinal = uncommonIntervals.getOrNull(0)?.getOrNull(0)?.let {
            complainedTimeslots(weekdayListOfTimeSlot, it)
        } ?: emptyList()

        val saturdayFinal = uncommonIntervals.getOrNull(1)?.getOrNull(0)?.let {
            complainedTimeslots(saturdayListOfTimeSlot, it)
        } ?: emptyList()
        val sundayFinal = uncommonIntervals.getOrNull(2)?.getOrNull(0)?.let {
            complainedTimeslots(sundayListOfTimeSlot, it)
        } ?: emptyList()


        val itemsToRemove = mutableListOf<Int>()
        val itemsToAdd = mutableListOf<Schedule.Days>()
        for (i in mSchedule.days) {
            if (daysToBeAdded.contains(DAYS.values()[i.day])) {
                if(i.day in 0..4) {
                    if (weekdayFinal.isNotEmpty()) {
                        val d: Schedule.Days = i
                        val matchingTimeSlot = weekdayListOfTimeSlot.find { it.startHour == d.sthh && it.startMinute == d.stmm }
                        if(matchingTimeSlot == null) {
                            continue
                        }
                        val complainedTimeslots = getComplainedTimeslot(
                            matchingTimeSlot,
                            uncommonIntervals[0]
                        )
                        complainedTimeslots.forEach { complainedTimeslot ->
                            val dayBO = Schedule.Days().apply {
                                ethh = complainedTimeslot.endHour
                                sthh = complainedTimeslot.startHour
                                etmm = complainedTimeslot.endMinute
                                stmm = complainedTimeslot.startMinute
                                heatingVal = d.heatingVal
                                coolingVal = d.coolingVal
                                isSunset = false
                                isSunrise = false
                                day = d.day
                                heatingUserLimitMin = d.heatingUserLimitMin
                                heatingUserLimitMax = d.heatingUserLimitMax
                                coolingUserLimitMin = d.coolingUserLimitMin
                                coolingUserLimitMax = d.coolingUserLimitMax
                                heatingDeadBand = d.heatingDeadBand
                                coolingDeadBand = d.coolingDeadBand
                            }
                            itemsToAdd.add(dayBO)
                        }
                        itemsToRemove.add(i.day)

                    } else {
                        if(uncommonIntervals[0].isNotEmpty()) {
                            itemsToRemove.add(i.day)
                        }
                    }
                } else if(i.day == 5) {
                    if (saturdayFinal.isNotEmpty()) {
                        val d: Schedule.Days = i
                        val matchingTimeSlot = saturdayListOfTimeSlot.find { it.startHour == d.sthh && it.startMinute == d.stmm }
                        if (matchingTimeSlot == null) {
                            continue
                        }
                        val complainedTimeslots = getComplainedTimeslot(matchingTimeSlot,
                            uncommonIntervals[1]
                        )

                        complainedTimeslots.forEach { complainedTimeslot ->
                            val dayBO = Schedule.Days().apply {
                                ethh = complainedTimeslot.endHour
                                sthh = complainedTimeslot.startHour
                                etmm = complainedTimeslot.endMinute
                                stmm = complainedTimeslot.startMinute
                                heatingVal = d.heatingVal
                                coolingVal = d.coolingVal
                                isSunset = false
                                isSunrise = false
                                day = d.day
                                heatingUserLimitMin = d.heatingUserLimitMin
                                heatingUserLimitMax = d.heatingUserLimitMax
                                coolingUserLimitMin = d.coolingUserLimitMin
                                coolingUserLimitMax = d.coolingUserLimitMax
                                heatingDeadBand = d.heatingDeadBand
                                coolingDeadBand = d.coolingDeadBand
                            }
                            itemsToAdd.add(dayBO)
                        }
                        itemsToRemove.add(i.day)
                    } else {
                        if (uncommonIntervals[1].isNotEmpty()) {
                            itemsToRemove.add(i.day)
                        }
                    }
                } else {
                    if (sundayFinal.isNotEmpty()) {
                        val d: Schedule.Days = i
                        val matchingTimeSlot =
                            sundayListOfTimeSlot.find { it.startHour == d.sthh && it.startMinute == d.stmm }
                        if (matchingTimeSlot == null) {
                            continue
                        }
                        val complainedTimeslots = getComplainedTimeslot(
                            matchingTimeSlot,
                            uncommonIntervals[2]
                        )
                        complainedTimeslots.forEach { complainedTimeslot ->
                            val dayBO = Schedule.Days().apply {
                                ethh = complainedTimeslot.endHour
                                sthh = complainedTimeslot.startHour
                                etmm = complainedTimeslot.endMinute
                                stmm = complainedTimeslot.startMinute
                                heatingVal = d.heatingVal
                                coolingVal = d.coolingVal
                                isSunset = false
                                isSunrise = false
                                day = d.day
                                heatingUserLimitMin = d.heatingUserLimitMin
                                heatingUserLimitMax = d.heatingUserLimitMax
                                coolingUserLimitMin = d.coolingUserLimitMin
                                coolingUserLimitMax = d.coolingUserLimitMax
                                heatingDeadBand = d.heatingDeadBand
                                coolingDeadBand = d.coolingDeadBand
                            }
                            itemsToAdd.add(dayBO)
                        }
                        itemsToRemove.add(i.day)
                    } else {
                        if(uncommonIntervals[2].isNotEmpty()) {
                            itemsToRemove.add(i.day)
                        }
                    }
                }
            }
        }
        for(i in itemsToRemove) {
            mSchedule.days.removeAll { it.day == i }
        }
        mSchedule.days.addAll(itemsToAdd)
    }
    fun getDays(days: List<Schedule.Days>, day: Int): List<Schedule.Days> {
        return if (day == -1) {
            emptyList()
        } else {
            days.filter { it.day == day }
        }
    }

    fun forceTrimSevenDaySchedule(
        schedule: Schedule, spillsMap: HashMap<String, ArrayList<Interval>>
    ) {
        ScheduleUtil.trimZoneSchedule(
            schedule, spillsMap
        )
    }
    fun isUncommonIntervalsHasAnySpills(uncommonIntervals: List<List<TimeSlot>>): Boolean {
        return uncommonIntervals.any { intervalList ->
            intervalList.isNotEmpty() && intervalList.all { it.isValidInterval() }
        }
    }

    fun forceTrimScheduleTowardsCommonTimeslot(ccuHsApi: CCUHsApi) {
        val buildingOccupancy = ccuHsApi.getSystemSchedule(false)[0]
        val activeZoneScheduleList = ArrayList<Schedule>()
        CcuLog.d(
            L.TAG_CCU_SCHEDULE,
            "Trimming schedule for building occupancy " + buildingOccupancy.days
        )
        val zones = ccuHsApi.readAllEntities("room")
        for(zone in zones) {
            if (zone.containsKey("scheduleRef")) {
                var zoneSchedule =
                    CCUHsApi.getInstance().getScheduleById(zone["scheduleRef"].toString())
                if (zoneSchedule == null) {
                    // Handling Crash here.
                    zoneSchedule =
                        CCUHsApi.getInstance().getRemoteSchedule(zone["scheduleRef"].toString())
                    CcuLog.d("CCU_MESSAGING", "Fetching the schedule from remote $zoneSchedule")
                    if (zoneSchedule == null) {
                        CcuLog.d(
                            "CCU_MESSAGING",
                            "The schedule retrieved from the remote source is also null."
                        )
                        continue
                    }
                }
                activeZoneScheduleList.add(zoneSchedule)
            }
        }
        for (zoneSchedule in activeZoneScheduleList) {
            if(zoneSchedule.scheduleGroup == ScheduleGroup.SEVEN_DAY.ordinal) {
                val spillsMap : HashMap<String, ArrayList<Interval>> = ScheduleUtil.getScheduleSpills(zoneSchedule.days, zoneSchedule)
                forceTrimSevenDaySchedule(
                    zoneSchedule,
                    spillsMap!!
                )
                ccuHsApi.updateScheduleNoSync(zoneSchedule, zoneSchedule.roomRef)
            } else {
                val commonTimeslot: List<List<TimeSlot>> = getCommonTimeSlot(
                    zoneSchedule.scheduleGroup, buildingOccupancy.getDays(),
                    zoneSchedule.days, true
                )
                trimScheduleTowardCommonTimeSlotAndSync(
                    zoneSchedule,
                    commonTimeslot,
                    ccuHsApi
                )
            }
            CcuLog.d(
                L.TAG_CCU_SCHEDULE,
                "Trimming schedule for zone : " + zoneSchedule.dis +
                        "Schedule Days :" + zoneSchedule.days +"Schedule Group : " +
                        zoneSchedule.scheduleGroup
            )
        }
        ccuHsApi.syncEntityTree()
    }
    fun getDayString(day: Int): String {
        when (day) {
            1 -> return "Monday"
            2 -> return "Tuesday"
            3 -> return "Wednesday"
            4 -> return "Thursday"
            5 -> return "Friday"
            6 -> return "Saturday"
            7 -> return "Sunday"
            /*Overnight schedule*/
            8 -> return "Monday"
        }
        return ""
    }
}