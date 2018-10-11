package a75f.io.alerts;

/**
 * Created by samjithsadasivan on 4/23/18.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Map;
/**
 * The format for alerts defined in json.
 */

/**
 * A sample definition
 *
 *     {
 *      "conditionals":[
 *          {
 *          "key" : "Battery",
 *          "value" : "75",
 *          "condition" :"<="
 *          },
 *          {
 *          "key" : "Battery",
 *          "value" : "50",
 *          "condition" :">"
 *          },
 *          {
 *          "key" : "Charging",
 *          "value" : "==",
 *          "condition" :"false"
 *          }
 *     ],
 *      "offset": "0",
 *      "alert": {
 *                  "mAlertType": "BATTERY_LEVEL_WARN",
 *                  "mTitle": "Battery level low on CCU [Warn]",
 *                  "mMessage": "The battery level of your CCU [%s] has dropped below 75%% and is not charging.Please check that the tablet is secured to it's mount. if it is plugged in, please contact 75F support.",
 *                  "mNotificationMsg": "The battery level of your CCU has dropped below 75% and is not charging.Please check that the tablet is secured to it's mount. If it is plugged in, please contact 75F support.",
 *                  "mSeverity": "WARN",
 *                  "mEnabled": "true"
 *               }
 *      }
 */
@JsonIgnoreProperties({"offsetCount"})
public class AlertDefinition
{
    public ArrayList<Conditional> conditionals;
    public String offset;
    public Alert alert;
    
    //Not required for definition, used for processing.
    public int offsetCount;
    
    public boolean evaluate(Map<String,Object> tsData) {
        boolean alertStatus = true;
        for (Conditional c : conditionals) {
            alertStatus = alertStatus && c.evaluate(tsData);
            if (alertStatus == false) {
                return false;
            }
        }
        return true;
    }
}
