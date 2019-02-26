package a75f.io.api.haystack;

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
}
