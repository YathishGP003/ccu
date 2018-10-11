package a75f.io.logic.bo.building.lights;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

/**
 * Created by Yinten on 9/29/2017.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "clazz")
public class LightProfileConfiguration extends BaseProfileConfiguration
{
    
    
}
