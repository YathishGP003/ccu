package a75.io.algos.vav;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import a75.io.algos.SystemTrimResponse;
import a75.io.algos.TrimResponseProcessor;

/**
 * Created by samjithsadasivan on 8/13/18.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
                      @JsonSubTypes.Type(value = VavTRSystem.class, name = "VavTRSystem"),
}
)
public abstract class TRSystem
{
    
    @JsonIgnore
    public TrimResponseProcessor satTRProcessor;
    
    @JsonIgnore
    public SystemTrimResponse satTRResponse;
    
    @JsonIgnore
    public TrimResponseProcessor co2TRProcessor;
    
    @JsonIgnore
    public SystemTrimResponse    co2TRResponse;
    
    @JsonIgnore
    public TrimResponseProcessor spTRProcessor;
    
    @JsonIgnore
    public SystemTrimResponse    spTRResponse;
    
    @JsonIgnore
    public TrimResponseProcessor hwstTRProcessor;
    
    @JsonIgnore
    public SystemTrimResponse    hwstTRResponse;
    
    
    @JsonIgnore
    public void processResetResponse(){
    
    }
    
    @JsonIgnore
    public TrimResponseProcessor getSystemSATTRProcessor(){
        return satTRProcessor;
    }
    
    @JsonIgnore
    public TrimResponseProcessor getSystemCO2TRProcessor(){
        return co2TRProcessor;
    }
    
    @JsonIgnore
    public TrimResponseProcessor getSystemSpTRProcessor(){
        return spTRProcessor;
    }
    
    @JsonIgnore
    public TrimResponseProcessor getSystemHwstTRProcessor(){
        return hwstTRProcessor;
    }
}
