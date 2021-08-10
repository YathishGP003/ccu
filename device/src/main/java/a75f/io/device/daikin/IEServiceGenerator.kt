package a75f.io.device.daikin

import a75f.io.device.R
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import android.content.Context
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class IEServiceGenerator {
    private val sslEnabled : Boolean = true
    companion object {
        @JvmStatic
        val instance: IEServiceGenerator by lazy {
            IEServiceGenerator()
        }
    }

    fun createService(baseUrl: String): IEService {
        return createRetrofit(
            baseUrl
        ).create(IEService::class.java)
    }

    fun createRetrofit(baseUrl: String): Retrofit {

        val okHttpClient = if (sslEnabled) getHttpsClient() else getHttpClient()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
    }

    fun getHttpClient() : OkHttpClient {

        return OkHttpClient.Builder().apply {
            addInterceptor(
                Interceptor { chain ->
                    val builder = chain.request().newBuilder()
                    builder.header("Authorization", "Bearer=11021962")
                    return@Interceptor chain.proceed(builder.build())
                }
            )
            addInterceptor(getHttpLoggingInterceptor())
            connectTimeout(30, TimeUnit.SECONDS)
        }.build()
    }

    fun getHttpsClient() : OkHttpClient {

            val trustManagerValidator = arrayOf<TrustManager>(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted( chain: Array<X509Certificate?>?, authType: String?) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) = try {

                        validateChain(chain)
                        chain!![0]!!.checkValidity()
                        CcuLog.i(L.TAG_CCU_DEVICE, "Certificate validation succeeded")
                    } catch (e: Exception) {
                        CcuLog.i(L.TAG_CCU_DEVICE, "Certificate validation failed")
                        throw CertificateException("Certificate not valid or trusted.$e")
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate?>? {
                        return arrayOf()
                    }
                }
            )

            // Install the all-trusting trust manager
            val keyStore = readKeyStore(Globals.getInstance().applicationContext)
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustManagerValidator, SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory
            val trustManagerFactory: TrustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

            trustManagerFactory.init(keyStore)
            val trustManagers: Array<TrustManager> = trustManagerFactory.trustManagers
            check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                "Unexpected default trust managers:" + trustManagers.contentToString()
            }

            val trustManager = trustManagers[0] as X509TrustManager

            return OkHttpClient.Builder().apply {
                        addInterceptor(
                            Interceptor { chain ->
                                val builder = chain.request().newBuilder()
                                builder.header("Authorization", "Bearer=11021962")
                                return@Interceptor chain.proceed(builder.build())
                            }
                        )
                        addInterceptor(getHttpLoggingInterceptor())
                        connectTimeout(30, TimeUnit.SECONDS)
                        sslSocketFactory(sslSocketFactory, trustManager)
                        hostnameVerifier { _, _ -> true }
                    }.build()
    }

    fun getHttpLoggingInterceptor() : HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    fun readKeyStore(context: Context): KeyStore? = KeyStore.getInstance(KeyStore.getDefaultType()).also{
        val inputStream = context.resources.openRawResource(R.raw.root_ca_daikin)
        //CcuLog.i(L.TAG_CCU_DEVICE, "Certificate $inputStream")
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val ca: Certificate = inputStream.use { inputStream ->
            cf.generateCertificate(inputStream)
        }
        it.load(null, null)
        //CcuLog.i(L.TAG_CCU_DEVICE,ca.toString())
        it.setCertificateEntry("ca",ca)
    }

    /**
     * Validate the certificate chain by comparing Issuer->Subject fields of
     * Server Certificate -> Intermediate Certificates -> upto the Root certificate.
     */
    private fun validateChain(chain: Array<X509Certificate?>?) {
        var issuerName : String? = null

        for ((index, cert) in chain!!.withIndex()) {
            issuerName?.run {
                check(validateCertificates(issuerName.toString(), cert?.subjectX500Principal?.name.toString()))
            }
            CcuLog.i(L.TAG_CCU_DEVICE, "Issuer $issuerName")
            CcuLog.i(L.TAG_CCU_DEVICE, "Subject ${cert?.subjectX500Principal?.name}")

            issuerName = if (issuerName == null) {
                cert?.issuerX500Principal?.name
            } else {
                cert?.subjectX500Principal?.name
            }

            if (index == chain.size - 1) {
                CcuLog.i(L.TAG_CCU_DEVICE, "Verify root certificate subject")
                val keyStore = readKeyStore(Globals.getInstance().applicationContext)
                val ca : X509Certificate= keyStore?.getCertificate("ca") as X509Certificate
                CcuLog.i(L.TAG_CCU_DEVICE, " root certificate subject ${ca.subjectX500Principal.name}")
                check(validateCertificates(issuerName.toString(), ca?.subjectX500Principal.name))
            }
        }
    }

    /**
     * Compare the organization fields of two certificates.
     */
    private fun validateCertificates(issuer : String, subject : String ) : Boolean {
        val issuerNames: List<String> = issuer.split(",")
        val subjectNames: List<String> = subject.split(",")
        CcuLog.i(L.TAG_CCU_DEVICE, "issuerNames $issuerNames")
        CcuLog.i(L.TAG_CCU_DEVICE, "subjectNames $subjectNames")
        return issuerNames.find {
                   name -> name.startsWith("O=")
               }.equals(subjectNames.find { name -> name.startsWith("O=") })
    }


}