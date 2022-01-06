package a75f.io.logic.util.backupfiles;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
        if(fileNames == null){
            return;
        }
        for(int index = 0; index < fileNames.length; index++){
            fileNames[index] = folderPath + fileNames[index];
        }
        new ZipUtility().zip(fileNames, folderPath + zipFileName + ".zip");
    }

    private static boolean isFolderEmpty(String folderPath){
        File directory = new File(folderPath);
        return (directory.list() != null && directory.list().length == 0);
    }

    public static void zipBytes(String filename, byte[] input) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(input);
        }
    }

    public static void unzipFile(String zipFilePath, String unzipFilePath) throws IOException{
        File unzipPath = new File(unzipFilePath);
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(unzipPath, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }
}