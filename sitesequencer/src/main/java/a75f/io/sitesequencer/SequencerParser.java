package a75f.io.sitesequencer;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import a75f.io.logger.CcuLog;


public class SequencerParser {

    public static String TAG_CCU_SITE_SEQUENCER = "CCU_SITE_SEQUENCER";
    ObjectMapper objectMapper;

    public SequencerParser() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(new KotlinModule());
    }

    public ArrayList<SiteSequencerDefinition> parseSequencerString(String json) {

        ArrayList<SiteSequencerDefinition> sequencerList = null;
        try {
            SiteSequencerDefinition[] pojos = objectMapper.readValue(json, SiteSequencerDefinition[].class);
            sequencerList = new ArrayList<>(Arrays.asList(pojos));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return sequencerList;
    }

    /**
     * Used to create String to write to prefs, using ObjectMapper.
     */
    public String sequencerDefsToString(List<SiteSequencerDefinition> sequencerDefs) {

        try {
            SiteSequencerDefinition[] defsArray = sequencerDefs.toArray(new SiteSequencerDefinition[sequencerDefs.size()]);
            return objectMapper.writeValueAsString(defsArray);
        } catch (IOException e) {
            CcuLog.e(TAG_CCU_SITE_SEQUENCER, "Error serializing seq defs.", e);

            // We probably want the app to crash here.  Fail-fast.   (and fix)
            return null;
        }
    }
}
