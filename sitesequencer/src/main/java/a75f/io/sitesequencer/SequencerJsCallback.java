package a75f.io.sitesequencer;

public interface SequencerJsCallback {
    boolean triggerAlert(String blockId, String notificationMsg, String message,
                         String entityId, Object contextHelper, SiteSequencerDefinition def);
}


