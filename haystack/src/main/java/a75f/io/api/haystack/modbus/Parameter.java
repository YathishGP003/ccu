package a75f.io.api.haystack.modbus;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.util.List;


import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.relation.ToMany;

@Entity
public class Parameter {
    @Id long id;
    @SerializedName("parameterId")
    @Expose
    public String parameterId;
    @SerializedName("name")
    @Expose
    public String name;

    @SerializedName("startBit")
    @Expose
    public Integer startBit;

    @SerializedName("endBit")
    @Expose
    public Integer endBit;

    @SerializedName("bitParamRange")
    @Expose
    public String bitParamRange;

    @SerializedName("bitParam")
    @Expose
    public Integer bitParam;

    @SerializedName("commands")
    @Expose
    @Convert(converter = EncounterCommandConverter.class, dbType = String.class)
    public List<Command> commands;

    @SerializedName("logicalPointTags")
    @Expose
    @Convert(converter = EncounterLogicalPointTagsConverter.class, dbType = String.class)
    public List<LogicalPointTags> logicalPointTags;

    @SerializedName("userIntentPointTags")
    @Expose
    @Convert(converter = EncounterUserIntentPointTagsConverter.class, dbType = String.class)
    public List<UserIntentPointTags> userIntentPointTags;

    @SerializedName("conditions")
    @Expose
    @Convert(converter = EncounterConditionConverter.class, dbType = String.class)
    public List<Condition> conditions;

    public boolean isDisplayInUiDefault() {
        return displayInUiDefault;
    }

    public void setDisplayInUiDefault(boolean displayInUiDefault) {
        this.displayInUiDefault = displayInUiDefault;
    }

    @SerializedName("displayInUiDefault")
    @Expose
    private boolean displayInUiDefault = false;
    private boolean displayInUI = false;
    private int registerAddress;
    private String registerNumber;
    private String registerType;
    private String parameterDefinitionType;
    public String getParameterId() {
        return parameterId;
    }

    public void setParameterId(String id) {
        this.parameterId = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStartBit() {
        return startBit;
    }

    public void setStartBit(Integer startBit) {
        this.startBit = startBit;
    }

    public Integer getEndBit() {
        return endBit;
    }

    public void setEndBit(Integer endBit) {
        this.endBit = endBit;
    }

    public String getBitParamRange() {
        return bitParamRange;
    }

    public void setBitParamRange(String bitParamRange) {
        this.bitParamRange = bitParamRange;
    }

    public Integer getBitParam() {
        return bitParam;
    }

    public void setBitParam(Integer bitParam) {
        this.bitParam = bitParam;
    }

    public List<LogicalPointTags> getLogicalPointTags() {
        return logicalPointTags;
    }

    public void setLogicalPointTags(List<LogicalPointTags> logicalPointTags) {
        this.logicalPointTags = logicalPointTags;
    }

    public List<UserIntentPointTags> getUserIntentPointTags() {
        return userIntentPointTags;
    }

    public void setUserIntentPointTags(List<UserIntentPointTags> userintentPointTags) {
        this.userIntentPointTags = userintentPointTags;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }
    public boolean getDisplayInUiDefault() {
        return displayInUiDefault;
    }
    public boolean isDisplayInUI() {
        return displayInUI;
    }

    public void setDisplayInUI(boolean displayInUI) {
        this.displayInUI = displayInUI;
    }

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

    public static class EncounterCommandConverter implements PropertyConverter<List<Command>, String> {

        @Override
        public List<Command> convertToEntityProperty(String databaseValue) {
            if (databaseValue == null) {
                return null;
            }

            return new Gson().fromJson(databaseValue, new TypeToken<List<Command>>() {
            }.getType());
        }

        @Override
        public String convertToDatabaseValue(List<Command> commandList) {
            if (commandList == null) {
                return null;
            }

            return new Gson().toJson(commandList);
        }
    }

    public static class EncounterLogicalPointTagsConverter implements PropertyConverter<List<LogicalPointTags>, String> {

        @Override
        public List<LogicalPointTags> convertToEntityProperty(String databaseValue) {
            if (databaseValue == null) {
                return null;
            }

            return new Gson().fromJson(databaseValue, new TypeToken<List<LogicalPointTags>>() {
            }.getType());
        }

        @Override
        public String convertToDatabaseValue(List<LogicalPointTags> logicalPointTagsList) {

            if (logicalPointTagsList == null) {
                return null;
            }

            return new Gson().toJson(logicalPointTagsList);
        }
    }

    public static class EncounterUserIntentPointTagsConverter implements PropertyConverter<List<UserIntentPointTags>, String> {

        @Override
        public List<UserIntentPointTags> convertToEntityProperty(String databaseValue) {
            if (databaseValue == null) {
                return null;
            }

            return new Gson().fromJson(databaseValue, new TypeToken<List<UserIntentPointTags>>() {
            }.getType());
        }

        @Override
        public String convertToDatabaseValue(List<UserIntentPointTags> userIntentPointTagsList) {
            if (userIntentPointTagsList == null) {
                return null;
            }

            return new Gson().toJson(userIntentPointTagsList);
        }
    }

    public static class EncounterConditionConverter implements PropertyConverter<List<Condition>, String> {

        @Override
        public List<Condition> convertToEntityProperty(String databaseValue) {
            if (databaseValue == null) {
                return null;
            }

            return new Gson().fromJson(databaseValue, new TypeToken<List<Condition>>() {
            }.getType());
        }

        @Override
        public String convertToDatabaseValue(List<Condition> conditionList) {
            if (conditionList == null) {
                return null;
            }

            return new Gson().toJson(conditionList);
        }
    }

}