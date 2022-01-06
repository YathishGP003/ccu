package a75f.io.logic.ccu.restore;

public class CCU {
    private String ccuId;
    private String name;
    private String version;
    private String lastUpdated;
    private boolean isOnline;
    private String siteCode;

    public CCU(String siteCode, String ccuId, String name, String version, String lastUpdated, boolean isOnline) {
        this.siteCode = siteCode;
        this.ccuId = ccuId;
        this.name = name;
        this.version = version;
        this.lastUpdated = lastUpdated;
        this.isOnline = isOnline;
    }

    public String getCcuId() {
        return ccuId;
    }

    public String getSiteCode() {
        return siteCode;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return "CCU{" +
                "ccuId='" + ccuId + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", lastUpdated='" + lastUpdated + '\'' +
                ", isOnline=" + isOnline +
                '}';
    }
}
