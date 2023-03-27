package a75f.io.renatus;

public enum BackFillDuration {

    NONE("None"),
    ONE_HOUR("1 Hr"),
    TWO_HOURS("2 Hrs"),
    THREE_HOURS("3 Hr"),
    SIX_HOURS("6 Hrs"),
    TWELVE_HOURS("12 Hr"),
    TWENTY_FOUR_HOURS("24 Hrs"),
    FORTY_EIGHT_HOURS("48 Hrs"),
    SEVENTY_TWO_HOURS("72 Hrs");

    String displayName;

    BackFillDuration(String str) {
        displayName = str;
    }

    public static String[] getDisplayNames() {
        BackFillDuration[] values = BackFillDuration.values();
        String[] displayNames = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            displayNames[i] = values[i].displayName;
        }
        return displayNames;
    }

}
