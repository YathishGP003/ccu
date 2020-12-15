package a75f.io.api.haystack.modbus;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
@Entity
public class Condition {
    @Id long id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("bitValues")
    @Expose
    private String bitValues;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBitValues() {
        return bitValues;
    }

    public void setBitValues(String val) {
        this.bitValues = val;
    }

}
