package a75f.io.logic.cloud;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.cloudservice.FileBackupService;
import static a75f.io.logic.L.TAG_CCU_BACKUP;
import static a75f.io.logic.L.TAG_CCU_REPLACE;

import android.util.Log;
import com.google.gson.Gson;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import a75f.io.logic.util.backupfiles.FileConstants;
import a75f.io.logic.util.backupfiles.FileOperationsUtil;
import a75f.io.modbusbox.EquipsManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FileBackupManager {

    private OkHttpClient getOkHttpClient(){
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String bearerToken = CCUHsApi.getInstance().getJwt();

                    Request originalRequest = chain.request();
                    Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + bearerToken)
                            .header("Accept-Encoding", "gzip, deflate, br")
                            .build();

                    CcuLog.d("CCU_HTTP_REQUEST", "FileBackupManager: [" + chain.request().method() + "] " + chain.request().url() + " - Token: " + bearerToken);

                    return chain.proceed(newRequest);
                })
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    okhttp3.Response response = chain.proceed(request);

                    CcuLog.d("CCU_HTTP_RESPONSE", "FileBackupManager: " + response.code() + " - [" + request.method() + "] " + request.url());
                    return response;
                })
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        return okHttpClient;
    }

    private void deleteZipFileFromCCU(File file){
        if(file.delete()){
            Log.i(TAG_CCU_BACKUP, file.getAbsolutePath() + " deleted from CCU after backing up.");
        }
    }

    public Map<String, Integer> getConfigFiles(String siteId, String ccuId){
        Retrofit retrofit = getRetrofitForFileStorageService();
        Call<ResponseBody> call = retrofit.create(FileBackupService.class).getConfigFiles(siteId, ccuId);
        Map<String, Integer> modbusConfigs = new HashMap<>();
        try {
            Response<ResponseBody> response = call.execute();
            if(response.isSuccessful()){
                String fileName = FileConstants.CCU_CONFIG_FILE_PATH + FileConstants.CCU_CONFIG_ZIP_FILE_NAME;
                FileOperationsUtil.zipBytes(fileName, response.body().bytes());
                FileOperationsUtil.unzipFile(fileName,
                        FileConstants.CCU_CONFIG_FILE_PATH);
                deleteZipFileFromCCU(new File(fileName));
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new File(FileConstants.CCU_CONFIG_FILE_PATH + FileConstants.CCU_CONFIG_FILE_NAME));
                doc.getDocumentElement().normalize();
                NodeList intList = doc.getElementsByTagName("int");
                for (int temp = 0; temp < intList.getLength(); temp++) {
                    Node node = intList.item(temp);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String name = element.getAttribute("name");
                        String value = element.getAttribute("value");
                        modbusConfigs.put(name, Integer.parseInt(value));
                        CcuLog.e(TAG_CCU_REPLACE, "name:" + name +"-"+ "value:" + value);
                    }
                }
            }
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            CcuLog.e(TAG_CCU_REPLACE,  e.getMessage());
        }
        finally{
            return modbusConfigs;
        }
    }

    public void getModbusSideLoadedJsonsFiles(String siteId, String ccuId){
        Retrofit retrofit = getRetrofitForFileStorageService();
        Call<ResponseBody> call = retrofit.create(FileBackupService.class).getModbusSideLoadedJsons(siteId, ccuId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    CcuLog.e(TAG_CCU_REPLACE, new Gson().toJson(response.errorBody()));
                    CcuLog.i(TAG_CCU_REPLACE,
                            "Error while Retrieving Modbus side loaded JSON files for Site ID " + siteId +" and CCU" +
                                    " ID " + ccuId);
                    return;
                }
                try {
                    String fileName = FileConstants.MODBUS_SIDE_LOADED_JSON_PATH + ccuId + ".zip";
                    FileOperationsUtil.zipBytes(fileName, response.body().bytes());
                    FileOperationsUtil.unzipFile(fileName, FileConstants.MODBUS_SIDE_LOADED_JSON_PATH);
                    deleteZipFileFromCCU(new File(fileName));
                    EquipsManager.getInstance().readExternalJSONFiles();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                CcuLog.e(TAG_CCU_REPLACE, t.getStackTrace().toString());
            }
        });
    }

    public void uploadBackupConfigFiles(File file, String siteId, String ccuId){
        RequestBody requestBody =RequestBody.create(MediaType.parse("application/zip"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RenatusServicesEnvironment.getInstance().getUrls().getRemoteStorageUrl())
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Call<ResponseBody> call = retrofit.create(FileBackupService.class).backupConfigFiles(siteId, ccuId, body);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    CcuLog.e(TAG_CCU_BACKUP, new Gson().toJson(response.errorBody()));
                    CcuLog.i(TAG_CCU_BACKUP, "CCU Config files back up " + file.getAbsolutePath() +" Upload failed");
                    deleteZipFileFromCCU(file);
                    return;
                }
                CcuLog.i(TAG_CCU_BACKUP, "CCU Config files back up " + file.getAbsolutePath() +" Upload successfully");
                deleteZipFileFromCCU(file);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                CcuLog.e(TAG_CCU_BACKUP, t.getStackTrace().toString());
                CcuLog.e(TAG_CCU_BACKUP, "CCU Config files back up upload error: "+t.getMessage(), t);
                deleteZipFileFromCCU(file);
            }
        });
    }

    public void uploadModbusSideLoadedJsonsFiles(File file, String siteId, String ccuId){
        RequestBody requestBody =RequestBody.create(MediaType.parse("application/zip"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        Retrofit retrofit = getRetrofitForFileStorageService();
        Call<ResponseBody> call = retrofit.create(FileBackupService.class).backupModbusSideLoadedJsons(siteId, ccuId, body);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    CcuLog.e(TAG_CCU_BACKUP, new Gson().toJson(response.errorBody()));
                    CcuLog.i(TAG_CCU_BACKUP, "Modbus side loaded json back up " + file.getAbsolutePath() +" Upload " +
                            "failed");
                    deleteZipFileFromCCU(file);
                    return;
                }
                CcuLog.i(TAG_CCU_BACKUP, "Modbus side loaded json back up " + file.getAbsolutePath() +" Upload " +
                        "successfully");
                deleteZipFileFromCCU(file);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                CcuLog.e(TAG_CCU_BACKUP, t.getStackTrace().toString());
                CcuLog.e(TAG_CCU_BACKUP, "Modbus side loaded json back up error: "+t.getMessage(), t);
                deleteZipFileFromCCU(file);
            }
        });
    }

    private Retrofit getRetrofitForFileStorageService() {
        return new Retrofit.Builder()
                .baseUrl(RenatusServicesEnvironment.getInstance().getUrls().getRemoteStorageUrl())
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
