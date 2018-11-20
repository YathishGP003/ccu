package a75f.io.logic.bo.building.system;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import a75.io.algos.tr.TRSystem;
import a75f.io.logic.bo.building.Schedule;

/**
 * Created by Yinten isOn 8/15/2017.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
                      @JsonSubTypes.Type(value = AHU_RP1455.class, name = "AHU_RP1455"),
                      @JsonSubTypes.Type(value = VavIERtu.class, name = "VavIERtu"),
                      @JsonSubTypes.Type(value = VavStagedRtu.class, name = "VavStagedRtu"),
                      @JsonSubTypes.Type(value = VavAnalogRtu.class, name = "VavAnalogRtu"),
                      @JsonSubTypes.Type(value = VavBacnetRtu.class, name = "VavBacnetRtu"),
                      @JsonSubTypes.Type(value = DabStagedRtu.class, name = "DabStagedRtu"),
}
)
public class SystemProfile
{
    @JsonIgnore
    public Schedule schedule = new Schedule();
    
    @JsonIgnore
    public TRSystem trSystem;
    
    public void doSystemControl() {
    }
    @JsonIgnore
    public  int getSystemSAT() {
        return 0;
    }
    @JsonIgnore
    public  int getSystemCO2() {
        return 0;
    }
    @JsonIgnore
    public  int getSystemOADamper() {
        return 0;
    }
    @JsonIgnore
    public int getStaticPressure() {
        return 0;
    }
    @JsonIgnore
    public String getProfileName() {
        return "";
    }
    
    @JsonIgnore
    public int getAnalog1Out() {
        return 0;
    }
    
    @JsonIgnore
    public int getAnalog2Out() {
        return 0;
    }
    
    @JsonIgnore
    public int getAnalog3Out() {
        return 0;
    }
    
    @JsonIgnore
    public int getAnalog4Out() {
        return 0;
    }
    
    /*@JsonIgnore
    public Struct getSystemControlMsg() {
        return null;
    }*/
}
