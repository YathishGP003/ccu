package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by Yinten on 9/29/2017.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "clazz")
public class LightProfileConfiguration extends BaseProfileConfiguration
{
    
    
}
