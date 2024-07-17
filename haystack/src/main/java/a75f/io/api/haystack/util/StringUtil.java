package a75f.io.api.haystack.util;


import java.util.Locale;

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
}
