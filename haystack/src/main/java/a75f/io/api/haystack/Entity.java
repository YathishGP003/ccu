package a75f.io.api.haystack;

/**
 * Created by samjithsadasivan on 12/21/18.
 */

import org.projecthaystack.HDateTime;

/**
 * Base class of Haystack entities
 */
public class Entity {

    private HDateTime createdDateTime;
    private HDateTime lastModifiedDateTime;
    private String lastModifiedBy;

    private String domainName;

    public HDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(HDateTime createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public HDateTime getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public void setLastModifiedDateTime(HDateTime lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getDomainName() {
        return domainName;
    }
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
