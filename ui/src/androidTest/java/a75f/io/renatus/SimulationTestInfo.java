package a75f.io.renatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.json.serializers.JsonSerializer;

/**
 * Created by samjithsadasivan on 9/26/17.
 */

public class SimulationTestInfo
{
    public String name;
    
    public int nodeAddress;
    
    public long startTime;
    
    public SimulationResult simulationResult;
    
    public List<String[]> simulationInput = null;
    
    public CCUApplication inputCcuState = null;
    
    public ArrayList<SmartNodeParams> nodeParams = new ArrayList<>();
    
    
    public String getHtml() {
    
        String inputString = "",stateString = "",paramsString = "";
        
        String prevParam = null;
        
        for(String[] row : simulationInput) {
            for (String s : row) {
                inputString += s+", ";
            }
            inputString += "<br>";
        }
      
        try {
            stateString = JsonSerializer.toJson(inputCcuState,true);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
       
        for (SmartNodeParams param : nodeParams) {
            String paramString = boldenEnabledParams(param.convertToJsonString());
            paramsString += decorateChangedParams(prevParam, paramString).concat("<br><br>");
            prevParam = paramString;
        }
        
        String resultHtml = simulationResult.result==TestResult.PASS ? "<span style=color:green>PASS</span>"
                                                                    : "<span style=color:red>FAIL</span>";
                                                
        String testInfoRow = "<tr style=color:blue>"
                             .concat("<td>").concat(" "+name+" ").concat("</td>")
                             .concat("<td>").concat(resultHtml).concat("</td>")
                             .concat("<td>").concat(inputString).concat("</td>")
                             .concat("<td>").concat(stateString).concat("</td>")
                             .concat("<td>").concat(paramsString).concat("</td>")
                             .concat("</tr>");
        return testInfoRow;
    }
    
    public String boldenEnabledParams(String param){
        final ObjectMapper mapper = new ObjectMapper();
        try
        {
            final JsonNode paramsTree = mapper.readTree(param);
            Iterator<String> fields = paramsTree.fieldNames();
            while (fields.hasNext()) {
                String key = fields.next();
                if (paramsTree.get(key).asText() != "0" && paramsTree.get(key).asText() != "0.0") {
                    ObjectNode n = (ObjectNode) paramsTree;
                    String newField = "<em><b>"+paramsTree.get(key).asText()+"</b></em>";
                    n.put(key,newField);
                }
            }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(paramsTree);
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        return param;
    }
    
    public String decorateChangedParams(String params1, String params2) {
        
        if (params1 == null || params2 == null) {
            return params2;
        }
    
        final ObjectMapper mapper = new ObjectMapper();
        
        try
        {
            final JsonNode params1Tree = mapper.readTree(params1);
            final JsonNode params2Tree = mapper.readTree(params2);
    
            if (params1Tree.equals(params2Tree))
                return params2;
            Iterator<String> fields1 = params1Tree.fieldNames();
            Iterator<String> fields2 = params2Tree.fieldNames();
            while (fields1.hasNext()) {
                String key1 = fields1.next();
                String key2 = fields2.next();
                
                if (!params1Tree.get(key1).asText().equals(params2Tree.get(key2).asText()) && !key1.equals("timestamp")) {
                    ObjectNode n = (ObjectNode) params2Tree;
                    String newVal = "<span style=color:red>"+params2Tree.get(key2).asText()+"</span>";
                    n.put(key2,newVal);
                }
            }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(params2Tree);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return params2;
    }
}
