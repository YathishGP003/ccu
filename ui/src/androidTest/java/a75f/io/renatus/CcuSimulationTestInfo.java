package a75f.io.renatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.json.serializers.JsonSerializer;

/**
 * Created by samjithsadasivan on 9/26/17.
 */

public class CcuSimulationTestInfo
{
    public String name;
    
    public SimulationResult simulationResult;
    
    public List<String[]> simulationInput = null;
    
    public CCUApplication inputCcuState = null;
    
    public ArrayList<SmartNodeParams> nodeParams = new ArrayList<>();
    
    
    public String getHtml() {
    
        String input = "",state = "",params = "";
        
        for(String[] row : simulationInput) {
            for (String s : row) {
                input += " "+s;
            }
            input += "<br>";
        }
      
        try {
            state = JsonSerializer.toJson(inputCcuState,true);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
       
        for (SmartNodeParams param : nodeParams) {
            params += param.convertToJsonString()+"<br>";
        }
        
        String testInfoRow = "<tr style=color:blue>"
                             .concat("<td>").concat(name).concat("</td>")
                             .concat("<td>").concat(simulationResult.result.toString()).concat("</td>")
                             .concat("<td>").concat(input).concat("</td>")
                             .concat("<td>").concat(state).concat("</td>")
                             .concat("<td>").concat(params).concat("</td>")
                             .concat("</tr>");
        return testInfoRow;
    }
}
