package a75f.io.api.haystack.sync;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 7/19/18.
 */

public class InfluxDbUtil
{
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String READ = "read";
    public static final String WRITE = "write";
    
    
    public static class URLBuilder {
        String protocol ;
        String host ;
        int port ;
        String rw;
        String database;
        String user;
        String password;
        long timestamp;
        
        public URLBuilder getInstance() {
            return new URLBuilder();
        }
        public URLBuilder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }
        public URLBuilder setHost(String host) {
            this.host = host;
            return this;
        }
        public URLBuilder setPort(int port) {
            this.port = port;
            return this;
        }
        public URLBuilder setOp(String rw) {
            this.rw = rw;
            return this;
        }
        public URLBuilder setDatabse(String database) {
            this.database = database;
            return this;
        }
        public URLBuilder setUser(String user) {
            this.user = user;
            return this;
        }
        public URLBuilder setPassword(String password) {
            this.password = password;
            return this;
        }
        public URLBuilder setTimeStamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
    
        public String buildUrl(){
            StringBuilder url = new StringBuilder(protocol);
            url.append("://").append(host)
               .append(":").append(port)
               .append("/").append(rw).append("?")
               .append("db=").append(database)
               .append("&u=").append(user)
               .append("&p=").append(password)
               .append("&precision=ms");
        
            return url.toString();
        }
    
    }
    
    
    public static String writeData(String targetURL, String measurement, HashMap<String,String> data,long msTimeStamp)
    {
        if (data.size() == 0) {
            return "";
        }
        StringBuilder dataSb = null;
    
        for (Map.Entry<String, String> param : data.entrySet()) {
            if (dataSb == null) {
                dataSb = new StringBuilder(measurement+" ");
            } else {
                dataSb.append(",");
            }
            dataSb.append(param.getKey()).append("=").append(param.getValue());
        }
        
        dataSb.append(" "+msTimeStamp);
        
        String urlParams = dataSb.toString();
        CcuLog.i("CCU",targetURL);
        CcuLog.i("CCU",urlParams);
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection)url.openConnection();
            //connection = NetCipher.getHttpsURLConnection(url);//TODO - Hack for SSLException
            connection.setRequestMethod("POST");
            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            //Send request
            DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
            wr.writeBytes (urlParams);
            wr.flush ();
            wr.close ();
            
            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
            
        } catch (Exception e) {
            
            e.printStackTrace();
            return null;
            
        } finally {
            
            if(connection != null) {
                connection.disconnect();
            }
        }
    }
}
