package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import a75.io.algos.SystemTrimResponse;
import a75.io.algos.TrimResponseProcessor;

/**
 * Created by Yinten isOn 8/15/2017.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
              @JsonSubTypes.Type(value = VAVSystemProfile.class, name = "VAVSystemProfile"),
              }
)
public class SystemProfile
{
    
    @JsonIgnore
    public Schedule schedule = new Schedule();
    
    @JsonIgnore
    public TrimResponseProcessor satTRProcessor;
    
    @JsonIgnore
    public SystemTrimResponse    satTRResponse;
    
    @JsonIgnore
    public void doSystemControl(){
    
    }
    
    @JsonIgnore
    public TrimResponseProcessor getSystemTRProcessor(){
        return null;
    }
}
