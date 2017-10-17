package a75f.io.renatus.framework;

import android.os.Environment;
import android.util.Log;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.json.serializers.JsonSerializer;
/**
 * Created by samjithsadasivan on 9/26/17.
 */

/**
 * Class that aggregates the complete data/details of a test.
 */
public class SimulationTestInfo
{
    public String name;
    
    public String description;
    
    public SimulationResult simulationResult;
    
    public List<String[]> simulationInput = null;
    
    public CCUApplication inputCcuState = null;
    
    public TreeMap<Integer , ArrayList<SmartNodeParams>> resultParamsMap = new TreeMap<>();
    
    public String[] graphColumns;
        
    public SamplingProfile profile;
    
    public String getHtml() {
        String resultHtml = simulationResult.status == TestResult.PASS ? "<span style=color:green>PASS</span>"
                                    : "<span style=color:red>FAIL</span>";
        
        String resultDetails = "<a href="+name+".html>"+"......... Graphs and Logs "+"</a>";
    
        String testInfoRow = "<tr style=color:blue>"
                                     .concat("<td>").concat(" "+name+" ").concat("</td>")
                                     .concat("<td>").concat(description).concat("</td>")
                                     .concat("<td>").concat(resultHtml+resultDetails).concat("</td>")
                                     .concat("</tr>");
        return testInfoRow;
    }
    
    public String getHtmlDetails()
    {
        String inputString = "", stateString = "", paramsString = "";
        String prevParam = null;
        for (String[] row : simulationInput)
        {
            for (String s : row)
            {
                inputString += s + ", ";
            }
            inputString += "<br>";
        }
        try
        {
            stateString = JsonSerializer.toJson(inputCcuState, true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        for (Integer node : resultParamsMap.keySet()) {
            for (SmartNodeParams param : resultParamsMap.get(node))
            {
                String paramString = boldenEnabledParams(param.convertToJsonString());
                paramsString += decorateChangedParams(prevParam, paramString).concat("<br><br>");
                prevParam = paramString;
            }
        }
        
        String templateHtml = SimulationTestInfo.readFileFromAssets("templates/testdetails.html");
        Document doc = Jsoup.parse(templateHtml);
    
        doc.getElementById("name").text(name);
        doc.getElementById("analysis").text(simulationResult.analysis);
        doc.getElementById("graph").text("Charts");
        doc.getElementById("input").text(inputString);
        doc.getElementById("output").text(paramsString);
        doc.getElementById("ccustate").text(stateString);
    
        Elements els = doc.getElementsByTag("head");
        for (Element el : els) {
            Element j = el.appendElement("script").attr("type","text/javascript").attr("src",getGraphDataFile());
        }
      
        String htmlData = doc.toString();
        htmlData = htmlData.replaceAll("&lt;","<");
        htmlData = htmlData.replaceAll("&gt;",">");
       
        return htmlData;
    }
    
    public String getGraphDataFile() {
        if (graphColumns == null) {
            return null;
        }
        //TODO - for the time being  data points are hard-coded as chart1Data, chart2Data, chart3Data, chart4Data in below format
       /* var chart1Data = [{
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
        
        
        String chartData="";
    
        for (String g: graphColumns) {
            ArrayList<GraphData> gdArray = new ArrayList<>();
            String varName = null;
            GraphData gd = new GraphData();
            gd.type="line";
            int xCounter = 0;
            
            switch(g) {
                case "Relay1_Out":
                    gd.color = "red";
                    varName = "var chart1Data = ";
                    for (Integer node : resultParamsMap.keySet())
                    {
                        for (SmartNodeParams param : resultParamsMap.get(node))
                        {
                            if (gd.dataPoints.size() == 0)
                            {
                                gd.dataPoints.add(new DataPoint(xCounter, param.digital_out_1, node.toString()));
                            }
                            else
                            {
                                gd.dataPoints.add(new DataPoint(xCounter, param.digital_out_1));
                            }
                            xCounter += (profile.resultPeriodSecs / 60);
                        }
                    }
                    break;
                case "Relay2_Out":
                    gd.color = "green";
                    varName = "var chart2Data = ";
                    for (Integer node : resultParamsMap.keySet())
                    {
                        for (SmartNodeParams param : resultParamsMap.get(node))
                        {
                            if (gd.dataPoints.size() == 0)
                            {
                                gd.dataPoints.add(new DataPoint(xCounter, param.digital_out_2, node.toString()));
                            }
                            else
                            {
                                gd.dataPoints.add(new DataPoint(xCounter, param.digital_out_2));
                            }
                            xCounter += (profile.resultPeriodSecs / 60);
                        }
                    }
                    break;
                case "Analog1_Out":
                    gd.color="blue";
                    varName = "var chart3Data = ";
                    for (Integer node : resultParamsMap.keySet())
                    {
                        for (SmartNodeParams param : resultParamsMap.get(node))
                        {
                            if (gd.dataPoints.size() == 0)
                            {
                                gd.dataPoints.add(new DataPoint(xCounter, param.analog_out_1, node.toString()));
                            }
                            else
                            {
                                gd.dataPoints.add(new DataPoint(xCounter, param.analog_out_1));
                            }
                            xCounter += (profile.resultPeriodSecs / 60);
                        }
                    }
                    break;
                case "Analog2_Out":
                    gd.color="yellow";
                    varName = "var chart4Data = ";
                    for (Integer node : resultParamsMap.keySet())
                    {
                        for (SmartNodeParams param : resultParamsMap.get(node))
                        {
                            if (gd.dataPoints.size() == 0)
                            {
                                gd.dataPoints.add(new DataPoint(xCounter, param.analog_out_2, node.toString()));
                            }
                            else
                            {
                                gd.dataPoints.add(new DataPoint(xCounter, param.analog_out_2));
                            }
                            xCounter += (profile.resultPeriodSecs / 60);
                        }
                    }
                    break;
                    
            }
            gdArray.add(gd);
            chartData += varName+toJson(gdArray)+";\n";
        }
    
        try
        {
            String path = Environment.getExternalStorageDirectory().getPath();
            File graph = new File(path + "/simulation/", name + ".js");
            FileOutputStream out = new FileOutputStream(graph);
            byte[] data = chartData.getBytes();
            out.write(data);
            out.close();
        
        }catch (IOException e) {
            e.printStackTrace();
        }
        return name+".js";
    }
    
    private String toJson(Object o){
        ObjectMapper m  = new ObjectMapper();
        try
        {
            m.disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
            m.disable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
            return m.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
                    String newVal = "<span style=color:red><b>"+params2Tree.get(key2).asText()+"</b></span>";
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
