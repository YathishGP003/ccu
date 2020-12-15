package a75f.io.api.haystack.modbus;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.relation.ToMany;

import java.util.List;
@Entity
public class Register {
    @SerializedName("registerNumber")
    @Expose
    public String registerNumber;
    @Id long id;
    @SerializedName("registerAddress")
    @Expose
    public int registerAddress;
    @SerializedName("registerType")
    @Expose
    public String registerType;
    @Expose
    @SerializedName("parameterDefinitionType")
    public String parameterDefinitionType;

    @SerializedName("parameters")
    @Expose
    @Convert(converter = EncounterParameterConverter.class, dbType = String.class)
    private List<Parameter> parameters = null;

    public int getRegisterAddress() {
        return registerAddress;
    }

    public void setRegisterAddress(int registerAddress) {
        this.registerAddress = registerAddress;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(String registerType) {
        this.registerType = registerType;
    }

    public String getParameterDefinitionType() {
        return parameterDefinitionType;
    }

    public void setParameterDefinitionType(String parameterDefinitionType) {
        this.parameterDefinitionType = parameterDefinitionType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public static class EncounterParameterConverter implements PropertyConverter<List<Parameter>, String> {

        @Override
        public List<Parameter> convertToEntityProperty(String databaseValue) {
            if (databaseValue == null) {
                return null;
            }

            return new Gson().fromJson(databaseValue, new TypeToken<List<Parameter>>() {
            }.getType());
        }

        @Override
        public String convertToDatabaseValue(List<Parameter> parameterList) {
            if (parameterList == null) {
                return null;
            }

            return new Gson().toJson(parameterList);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(registerNumber+":"+registerAddress+":"+registerType+":"+parameterDefinitionType+" ");
        
        for(Parameter p : parameters) {
            sb.append(p.toString());
        }
        return sb.toString();
    }

}