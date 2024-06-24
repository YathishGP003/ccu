package a75f.io.api.haystack.sync;

import android.net.SSLCertificateSocketFactory;
import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLContext;

public class ClientSSLSocketFactory extends SSLCertificateSocketFactory {
    private static SSLContext sslContext;
    /**
     * @deprecated
     */
    public ClientSSLSocketFactory(int handshakeTimeoutMillis) {
        super(handshakeTimeoutMillis);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
        return sslContext.getSocketFactory().createSocket();
    }
}