package a75f.io.api.haystack.util;


import java.util.HashMap;
import java.util.Locale;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;

public class StringUtil {

    private StringUtil(){

    }
    public static String processMessageForNumberFormatting(String message){
        String space = " ";
        String[] messageWords = message.split(space);
        StringBuilder modifiedMessage = new StringBuilder();
        for (String messageWord : messageWords){
            modifiedMessage.append(space);
            if(messageWord.matches("-?\\d+(\\.\\d+)?")){
                if(messageWord.contains(".")) {
                    modifiedMessage.append(String.format(Locale.US, "%,.2f", Double.parseDouble(messageWord)));
                }
                else {
                    modifiedMessage.append(String.format(Locale.US,"%,d", Long.parseLong(messageWord)));
                }
            }
            else{
                modifiedMessage.append(messageWord);
            }
        }
        return modifiedMessage.toString().trim();
    }

    public static String processMessageForNumberFormattingOnlyOtaUpdate(String message){
        String space = " ";
        String[] messageWords = message.split(space);
        StringBuilder modifiedMessage = new StringBuilder();
        for (String messageWord : messageWords){
            modifiedMessage.append(space);
            if(messageWord.matches("-?\\d+(\\.\\d+)?")){
                if(messageWord.contains(".")) {
                    String[] versionArray = messageWord.split("\\.");
                    modifiedMessage.append(String.format(Locale.US, "%,d", Long.parseLong(versionArray[0]))).append(".").append(versionArray[1]);
                }
                else {
                    modifiedMessage.append(String.format(Locale.US,"%,d", Long.parseLong(messageWord)));
                }
            }
            else{
                modifiedMessage.append(messageWord);
            }
        }
        return modifiedMessage.toString().trim();
    }
    public static String addAtSymbolIfMissing(String input) {
        if (input.startsWith("@")) {
            return input;
        } else {
            return "@" + input;
        }
    }

    public static String getDis(HashMap<Object, Object> roomMap, CCUHsApi ccuHsApi) {
        HashMap<Object, Object> floor = ccuHsApi.readMapById(roomMap.get("floorRef").toString());
        HashMap<Object, Object> siteMap = ccuHsApi.readEntity(Tags.SITE);
        String siteDis = siteMap.get("dis").toString();
        return siteDis + "-" + floor.get("dis").toString() + "-" + roomMap.get("dis");
    }
}
