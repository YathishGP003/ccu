package a75f.io.alerts;

/**
 * Created by samjithsadasivan on 4/23/18.
 */

import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;

import java.util.ArrayList;

import a75f.io.api.haystack.Alert;
import a75f.io.logger.CcuLog;
/**
 * The format for alerts defined in json.
 */

/**
 * A sample definition
 *
    [
        {
            "conditionals":[
                             {
                             "order" : "1",
                             "key" : “current and temp and his“,
                             "value" : “80”,
                             "condition" :”>”
                             },
                             {
                             "order" : "2",
                             "operator" : "&&"
                             },
                             {
                             "order" : "3",
                             "key" : "current and temp and his“,
                             "value" : “100”,
                             "condition": "<"
                             }
                            ],
             "offset": "0",
             "alert": {
             
                         "mTitle": “Temperature breach detected“,
                         "mMessage": "Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1 and less than #condval3”,
                         "mNotificationMsg": "Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1 and less than #condval3",
                         "mSeverity": “WARN”,
                         "mEnabled": "true"
                         }
        }
    ]
 */

public class AlertDefinition
{
    String _id;
    
    public ArrayList<Conditional> conditionals;
    public String                 offset;
    public Alert                  alert;
    public boolean                custom;

    public AlertDefinition(){
    
    }

    /** evaluate this alert definition against current values
     * @return a string describing the evaluation, for inspection */
    public void evaluate(SharedPreferences sharedPrefs) {
        for (Conditional c : conditionals)
        {
            if (c.operator == null)
            {
                c.evaluate(sharedPrefs);
            }
        }
    }

    public boolean validate() {
        if (conditionals.size() % 2 == 0) {
            logValidatation("Incorrect number of conditionals "+conditionals.size());
            return false;
        }
        
        for (int i = 0; i < conditionals.size() ; i+=2) {
            Conditional c = conditionals.get(i);
            if ( (c.isThisOperator() && c.operator != null) || (c.isThisCondition() && c.operator == null)) {
                logValidatation("Operator not allowed "+c.toString());
                return false;
            }
            if (c.isThisCondition() && (c.grpOperation != null) && !c.grpOperation.equals("") && !c.grpOperation.equals("equip") && !c.grpOperation.equals("average")
                        && !c.grpOperation.contains("top") && !c.grpOperation.contains("bottom")
                        && !c.grpOperation.contains("min") && !c.grpOperation.contains("max")) {
                logValidatation("grpOperator not supported "+c.grpOperation);
                return false;
            }
        }
        return true;
    }
    
    
    
    private void logValidatation(String msg) {
        CcuLog.d("CCU_ALERTS","Invalid Alert Definition : "+msg);
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("AlertDefinition, Title: "+alert.mTitle+" Message "+alert.mMessage);
        for (Conditional c : conditionals) {
            b.append("{"+c.toString()+"} ");
        }
        return b.toString();
    }
}
