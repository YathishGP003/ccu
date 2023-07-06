package a75f.io.logic.bo.building.sse;

public class SSERelayAssociationUtil {
    enum DesiredTempDisplayMode
    {
        NOT_ENABLED, HEATING, COOLING
    }
    public static boolean isRelayAssociatedToHeating(int relayState) {
        return relayState == DesiredTempDisplayMode.HEATING.ordinal();
    }
    public static boolean isRelayAssociatedToCooling(int relayState) {
        return relayState == DesiredTempDisplayMode.COOLING.ordinal();
    }
}