package a75f.io.logic.cloudservice;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface FileBackupService {

    @Multipart
    @PUT("/files/ccu-local-configs/{siteId}/{fileId}")
    public Call<ResponseBody> backupConfigFiles(
            @Path("siteId") String siteId,
            @Path("fileId") String fileId,
            @Part MultipartBody.Part file
    );

    @Multipart
    @PUT("/files/side-loaded-jsons/{siteId}/{fileId}")
    public Call<ResponseBody> backupModbusSideLoadedJsonsBackup(
            @Path("siteId") String siteId,
            @Path("fileId") String fileId,
            @Part MultipartBody.Part file
    );
}