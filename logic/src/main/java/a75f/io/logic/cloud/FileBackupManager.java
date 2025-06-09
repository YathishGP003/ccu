package a75f.io.logic.cloud;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.cloudservice.FileBackupService;
import static a75f.io.logic.L.TAG_CCU_BACKUP;
import static a75f.io.logic.L.TAG_CCU_REPLACE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_GLOBAL;

import android.content.SharedPreferences;
import com.google.gson.Gson;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import a75f.io.logic.util.PreferenceUtil;
import a75f.io.logic.util.backupfiles.FileConstants;
import a75f.io.logic.util.backupfiles.FileOperationsUtil;
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

    private static final String BACNET_FD_CONFIGURATION = "bacnetFdConfiguration";
    private static final String BACNET_BBMD_CONFIGURATION = "bacnetBbmdConfiguration";
    private static final String BACNET_DEVICE_TYPE = "bacnetDeviceType";
    private static final String BACNET_FD_AUTO_STATE = "fdAutoState";

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
            CcuLog.d(TAG_CCU_BACKUP, file.getAbsolutePath() + " deleted from CCU after backing up.");
        }
    }

    public Map<String, Integer> getConfigFiles(String siteId, String ccuId, SharedPreferences bacnet_pref){
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

                NodeList stringList = doc.getElementsByTagName("string");
                for (int temp = 0; temp < stringList.getLength(); temp++) {
                    Node node = stringList.item(temp);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String name = element.getAttribute("name");
                        if(name.equals("BACnet_Config")){
                            String value = element.getTextContent();
                            CcuLog.e(TAG_CCU_REPLACE, "String : name:" + name +"-"+ "value:" + value);
                            bacnet_pref.edit().putString(name, value).apply();
                        }

                        if(name.equalsIgnoreCase(BACNET_BBMD_CONFIGURATION)){
                            String value = element.getTextContent();
                            CcuLog.e(TAG_CCU_REPLACE, "String : name:" + name +"-"+ "value:" + value);
                            bacnet_pref.edit().putString(name, value).apply();
                        }

                        if(name.equalsIgnoreCase(BACNET_FD_CONFIGURATION)){
                            String value = element.getTextContent();
                            CcuLog.e(TAG_CCU_REPLACE, "String : name:" + name +"-"+ "value:" + value);
                            bacnet_pref.edit().putString(name, value).apply();
                        }

                        if(name.equalsIgnoreCase(BACNET_DEVICE_TYPE)){
                            String value = element.getTextContent();
                            CcuLog.e(TAG_CCU_REPLACE, "String : name:" + name +"-"+ "value:" + value);
                            bacnet_pref.edit().putString(name, value).apply();
                        }

                        if(name.equalsIgnoreCase(PreferenceUtil.SELECTED_PROFILE_WITH_AHU)){
                            String value = element.getTextContent();
                            CcuLog.e(TAG_CCU_REPLACE, "String : name:" + name +"-"+ "value:" + value);
                            bacnet_pref.edit().putString(name, value).apply();
                        }
                    }
                }

                NodeList aBooleanList = doc.getElementsByTagName("boolean");
                for (int temp = 0; temp < aBooleanList.getLength(); temp++) {
                    Node node = aBooleanList.item(temp);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String name = element.getAttribute("name");
                        if(name.equals("isBACnetinitialized") || name.equals("isBACnetConfigFileCreated")){
                            String value = element.getAttribute("value");
                            CcuLog.e(TAG_CCU_REPLACE, "Boolean : name" + name +"-"+ "value:" + value);
                            bacnet_pref.edit().putBoolean(name, Boolean.parseBoolean(value)).apply();
                        }

                        if(name.equalsIgnoreCase(BACNET_FD_AUTO_STATE)){
                            String value = element.getAttribute("value");
                            CcuLog.e(TAG_CCU_REPLACE, "Boolean : name:" + name +"-"+ "value:" + value);
                            bacnet_pref.edit().putBoolean(name, Boolean.parseBoolean(value)).apply();
                        }

                        if(name.equalsIgnoreCase(IS_GLOBAL)){
                            String value = element.getAttribute("value");
                            CcuLog.e(TAG_CCU_REPLACE, "Boolean : name:" + name +"-"+ "value:" + value);
                            bacnet_pref.edit().putBoolean(name, Boolean.parseBoolean(value)).apply();
                        }
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

    private Retrofit getRetrofitForFileStorageService() {
        return new Retrofit.Builder()
                .baseUrl(RenatusServicesEnvironment.getInstance().getUrls().getRemoteStorageUrl())
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
