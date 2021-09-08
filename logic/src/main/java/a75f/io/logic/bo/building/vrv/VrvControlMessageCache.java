package a75f.io.logic.bo.building.vrv;

import java.util.HashMap;

public class VrvControlMessageCache {
    public static final int DEFAULT_RESPONSE_TIMEOUT_MINUTES = 3;
    
    private static VrvControlMessageCache instance;
    
    /**
     * Marks there is pending control message for a particular node.
     * Status messages are ignored until control gets sent and we get a response or times out.
     */
    private HashMap<Integer, Integer> pendingControls;
    
    private VrvControlMessageCache(){
        pendingControls = new HashMap<>();
    }
    
    public static VrvControlMessageCache getInstance() {
        if (instance == null) {
            instance = new VrvControlMessageCache();
        }
        return instance;
    }
    
    public void setControlsPending(int node) {
        pendingControls.put(node, DEFAULT_RESPONSE_TIMEOUT_MINUTES);
    }
    
    public void resetControlsPending(int node) {
        pendingControls.put(node, 0);
    }
    
    public boolean isControlsPendingResponse(int node) {
        return pendingControls.get(node) > 0;
    }
    
    public boolean isControlsPendingDelivery(int node) {
        return pendingControls.get(node) == DEFAULT_RESPONSE_TIMEOUT_MINUTES;
    }
    
    public int getControlsPendingTimer(int node) {
        return pendingControls.get(node);
    }
    
    public void updateControlsPending(int node) {
        int timeout = pendingControls.get(node);
        if (timeout > 0) {
            pendingControls.put(node, (timeout-1));
        }
    }
    
}
