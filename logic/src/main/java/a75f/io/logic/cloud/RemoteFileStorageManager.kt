package a75f.io.logic.cloud

import a75f.io.logger.CcuLog
import a75f.io.logic.L.TAG_CCU
import a75f.io.logic.cloudservice.FileStorageService
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


/**
 * Post (put) files to our remote file storage on azure.
 *
 * @author Tony Case
 * Created on 1/8/21.
 */

class RemoteFileStorageManager(
   val service: FileStorageService
) {

   /**
    * Upload the specified file to the azure file storage service in your current environment.
    * NOTE: this call does the network call asynchronously, which is good, but we will be replacing
    * this with proper RxJava soon.
    *
    * @param file the file to upload
    * @param mimeType the file's mime type.  E.g. "application/zip"
    * @param container the pre-configured container of the storage service to put this in.  E.g. "floorplans", "cculogs".
    * @param siteId  Current global site id.  This can be the first file for this site uploaded.
    * @param fileId  Id of the file on the service.
    */
   fun uploadFile(
      file: File,
      mimeType: String,
      container: String,
      siteId: String,
      fileId: String,
   ) {
      // create RequestBody instance from file
      val requestBody: RequestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())

      // MultipartBody.Part is used to send also the actual file name
      val body: MultipartBody.Part = MultipartBody.Part.createFormData("file", file.name, requestBody)

      // Execute the request
      val call: Call<ResponseBody> = service.uploadFileTo(
         container,
         siteId,
         fileId,
         body)

      call.enqueue(object : Callback<ResponseBody> {
         override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            CcuLog.e(TAG_CCU, "Upload error:  ${t.message}", t)
         }

         override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            CcuLog.i(TAG_CCU, "Upload success")
         }
      })
   }
}
