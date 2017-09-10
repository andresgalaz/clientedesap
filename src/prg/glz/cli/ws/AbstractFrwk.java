package prg.glz.cli.ws;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public abstract class AbstractFrwk {
    private static Logger logger = Logger.getLogger( AbstractFrwk.class );

    private String        cUrlOrigen;
    private List<String>  cookies;

    public AbstractFrwk(String cUrl) {
        /* Start of Fix */
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

        } };

        SSLContext sc;
        try {
            sc = SSLContext.getInstance( "SSL" );
            sc.init( null, trustAllCerts, new java.security.SecureRandom() );
            HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory() );

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier( allHostsValid );
        } catch (Exception e) {
            logger.error( "Al iniciar SSL", e );
        }
        /* End of the fix */
        this.setUrlServer( cUrl );

    }

    // protected abstract void init() throws FrameworkException;
    public void sendHeader(URLConnection con) {
        if (getCookies() != null)
            for (String cookie : getCookies())
                con.addRequestProperty( "Cookie", cookie.split( ";", 2 )[0] );
        con.setRequestProperty( "User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:40.0) Gecko/20100101 Firefox/40.0" );
        con.setRequestProperty( "Connection", "keep-alive" );
        con.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8" );
    }

    public static Map<String, Object> getJsonMap(InputStream in) throws IOException {
        try {
            StringBuffer sb = new StringBuffer();
            byte[] b = new byte[1024];
            int l;
            while ((l = in.read( b )) >= 0)
                sb.append( new String( b, 0, l, "UTF-8" ) );
            Map<String, Object> m = new ObjectMapper().readValue( sb.toString(), new TypeReference<HashMap<String, Object>>() {
            } );
            return m;
        } finally {
            in.close();
        }
    }

    public String getUrlServer() {
        return cUrlOrigen;
    }

    public void setUrlServer(String cUrlOrigen) {
        this.cUrlOrigen = cUrlOrigen;
    }

    public List<String> getCookies() {
        return cookies;
    }

    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }
}
