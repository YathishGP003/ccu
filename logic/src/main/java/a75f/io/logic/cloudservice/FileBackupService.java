package a75f.io.logic.cloudservice;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
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
    public Call<ResponseBody> backupModbusSideLoadedJsons(
            @Path("siteId") String siteId,
            @Path("fileId") String fileId,
            @Part MultipartBody.Part file
    );

    @GET("/files/ccu-local-configs/{siteId}/{fileId}")
    public Call<ResponseBody> getConfigFiles(
            @Path("siteId") String siteId,
            @Path("fileId") String fileId
    );

    @GET("/files/side-loaded-jsons/{siteId}/{fileId}")
    public Call<ResponseBody> getModbusSideLoadedJsons(
            @Path("siteId") String siteId,
            @Path("fileId") String fileId
    );
}