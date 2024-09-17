package a75f.io.api.haystack;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Yinten on 9/9/2017.
 */

public enum DAYS {
	MONDAY("0"), TUESDAY("1"), WEDNESDAY("2"), THURSDAY("3"), FRIDAY("4"), SATURDAY("5"), SUNDAY("6");
	String name;
	
	DAYS(String val) {
		name = val;
	}
	
	public String toString() {
		return name;
	}

	public DAYS getNextDay() {
		if(this.ordinal() == 6) return MONDAY;
		else return DAYS.values()[this.ordinal()+1];
	}

	public static List<Integer> getWeekDaysOrdinal() {
		return Arrays.asList(
				DAYS.MONDAY.ordinal(),
				DAYS.TUESDAY.ordinal(),
				DAYS.WEDNESDAY.ordinal(),
				DAYS.THURSDAY.ordinal(),
				DAYS.FRIDAY.ordinal()
		);
	}

	public static List<Integer> getWeekEndsOrdinal() {
		return Arrays.asList(
				DAYS.SATURDAY.ordinal(),
				DAYS.SUNDAY.ordinal()
		);
	}

	public static List<DAYS> getWeekDays() {
		return Arrays.asList(
				DAYS.MONDAY,
				DAYS.TUESDAY,
				DAYS.WEDNESDAY,
				DAYS.THURSDAY,
				DAYS.FRIDAY
		);
	}

	public static List<DAYS> getWeekEnds() {
		return Arrays.asList(
				DAYS.SATURDAY,
				DAYS.SUNDAY
		);
	}
	public static List<DAYS> getAllDays() {
		List<DAYS> weekDays = getWeekDays();
		weekDays.addAll(getWeekDays());
		return weekDays;
	}
}
