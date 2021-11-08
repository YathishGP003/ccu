package a75f.io.logic.cloud;
import java.io.File;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.cloudservice.FileBackupService;
import static a75f.io.logic.L.TAG_CCU_BACKUP;
import android.util.Log;
import com.google.gson.Gson;
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
                    Request originalRequest = chain.request();
                    Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer "+ CCUHsApi.getInstance().getJwt())
                            .header("Accept-Encoding", "gzip, deflate, br")
                            .build();
                    return chain.proceed(newRequest);
                })
                .addInterceptor(loggingInterceptor)
                .build();
        return okHttpClient;
    }

    private void deleteZipFileFromCCU(File file){
        if(file.delete()){
            Log.i(TAG_CCU_BACKUP, file.getAbsolutePath() + " deleted from CCU after backing up.");
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

    public void uploadModbusSideLoadedJsonsFiles(File file, String siteId, String ccuId){
        RequestBody requestBody =RequestBody.create(MediaType.parse("application/zip"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RenatusServicesEnvironment.getInstance().getUrls().getRemoteStorageUrl())
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Call<ResponseBody> call = retrofit.create(FileBackupService.class).backupModbusSideLoadedJsonsBackup(siteId, ccuId, body);
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
}
