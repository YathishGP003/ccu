package a75f.io.bo.building.system;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.javolution.io.Struct;

import a75.io.algos.vav.TRSystem;
import a75f.io.bo.building.Schedule;

/**
 * Created by Yinten isOn 8/15/2017.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
                      @JsonSubTypes.Type(value = AHU.class, name = "AHU"),
                      @JsonSubTypes.Type(value = RtuIE.class, name = "RtuIE"),
                      @JsonSubTypes.Type(value = StagedRTU.class, name = "StagedRTU"),
                      @JsonSubTypes.Type(value = DefaultSystem.class, name = "DefaultSystem"),
}
)
public class SystemProfile
{
    
    @JsonIgnore
    public Schedule schedule = new Schedule();
    
    @JsonIgnore
    public TRSystem trSystem;
    
    public void doSystemControl() {
        if (trSystem != null) {
            trSystem.processResetResponse();
        }
    }
    
    @JsonIgnore
    public Struct getSystemControlMsg() {
        return null;
    }
    
}
