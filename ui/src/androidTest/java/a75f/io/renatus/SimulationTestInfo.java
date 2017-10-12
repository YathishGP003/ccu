package a75f.io.renatus;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    
    public String description;
    
    public SimulationResult simulationResult;
    
    public List<String[]> simulationInput = null;
    
    public CCUApplication inputCcuState = null;
    
    public ArrayList<SmartNodeParams> nodeParams = new ArrayList<>();
    
    public String getHtml() {
        String resultHtml = simulationResult.result==TestResult.PASS ? "<span style=color:green>PASS</span>"
                                    : "<span style=color:red>FAIL</span>";
        
        String resultDetails = "<a href="+name+".html>"+" Details "+"</a>";
    
        String testInfoRow = "<tr style=color:blue>"
                                     .concat("<td>").concat(" "+name+" ").concat("</td>")
                                     .concat("<td>").concat(description).concat("</td>")
                                     .concat("<td>").concat(resultHtml+resultDetails).concat("</td>")
                                     .concat("</tr>");
        return testInfoRow;
    }
    
    public String getHtmlDetails() {
    
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
        
        String templateHtml = SimulationTestInfo.readFileFromAssets("templates/testdetails2.html");
        Document doc = Jsoup.parse(templateHtml);
    
        doc.getElementById("name").text(name);
        doc.getElementById("analysis").text("Coming soon");
        doc.getElementById("input").text(inputString);
        doc.getElementById("output").text(paramsString);
        doc.getElementById("ccustate").text(stateString);
        /*Element div = doc.getElementById("analog1ChartContainer");
        div.attr("style","display:none; height: 200px; width: 100%;");
        //doc.getElementById("analog1ChartContainer").attr("style","display:none; height: 200px; width: 100%;");
        for (TextNode tn : div.textNodes()) {
            String tagText = tn.text().trim();
        
            if (tagText.length() > 0) {
                //tn.text("");
            }
        }
        Elements tags = doc.select("*");
        for (Element tag : tags) {
            for (TextNode tn : tag.textNodes()) {
                String tagText = tn.text().trim();
            
                if (tagText.length() > 0) {
                    tn.text("");
                }
            }
        }*/
    
        return doc.toString();
    }
    
    public String createGraphData() {
        //TODO - for the time being  data points are hard-coded as char1Data, char2Data, chart3Data, chart4Data
       /* var dataG1 = [{
                        type: "stepLine",
                        color: "red",
                        dataPoints :[
                        { x: 1, y: 0, indexLabel:"relay1",markerColor: "red" ,color:"red" }, //dataPoint
                        { x: 2, y: 0},
                        { x: 3, y: 1},
                        { x: 4, y: 0}]
                        },
                        {
                        type: "stepLine",
                        dataPoints :[
                        { x: 1, y: 0, indexLabel:"room-temp",markerColor: "green"  }, //dataPoint
                        { x: 2, y: 7.3,indexLabel:"",markerColor: "green" },
                        { x: 3, y: 1, indexLabel:"",markerColor: "green" },
                        { x: 4, y: 0,indexLabel:"",markerColor: "green" }]
                        }]*/
    
        JSONObject snType = new JSONObject();
        try
        {
            snType.put("node_type", "smartnode");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return snType.toString();
    }
    
    public static String readFileFromAssets(String path){
        InputStream rawInput;
        ByteArrayOutputStream rawOutput = null;
        try {
            rawInput = SimulationContext.getInstance().getContext().getAssets().open(path);
            byte[] buffer = new byte[rawInput.available()];
            rawInput.read(buffer);
            rawOutput = new ByteArrayOutputStream();
            rawOutput.write(buffer);
            rawOutput.close();
            rawInput.close();
            ;
        } catch (IOException e) {
            Log.e("Error", e.toString());
        }
        return rawOutput != null ? rawOutput.toString() : null;
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
