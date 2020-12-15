package a75f.io.api.haystack.modbus;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
@Entity
public class UserIntentPointTags {
    @Id long id;
    @SerializedName("tagName")
    @Expose
    private String tagName;
    @SerializedName("value")
    @Expose
    private String tagValue;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }
}
