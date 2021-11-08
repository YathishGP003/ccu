package a75f.io.logic.util.backupfiles;
import java.io.File;
import java.io.IOException;
import a75f.io.logic.filesystem.ZipUtility;

public class FileOperationsUtil {

    public static void zipSingleFile(String path, String sourceFileName, String zipFileName) throws IOException {
        new ZipUtility().zip(new String[]{path + sourceFileName}, path + zipFileName + ".zip");
    }

    public static void zipFolder(String folderPath, String zipFileName) throws IOException  {
        if(isFolderEmpty(folderPath)){
            return;
        }
        String[] fileNames = new File(folderPath).list();
        for(int index = 0; index < fileNames.length; index++){
            fileNames[index] = folderPath + fileNames[index];
        }
        new ZipUtility().zip(fileNames, folderPath + zipFileName + ".zip");
    }

    private static boolean isFolderEmpty(String folderPath){
        File directory = new File(folderPath);
        return (directory.list().length == 0);
    }
}