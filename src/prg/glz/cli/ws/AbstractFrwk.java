package prg.glz.cli.ws;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import prg.glz.FrameworkException;

public abstract class AbstractFrwk {
    // private static Logger logger = Logger.getLogger( AbstractFrwk.class );

    private String       cUrlOrigen;
    private List<String> cookies;

    public AbstractFrwk(String cUrl) throws FrameworkException {
        this.setUrlServer( cUrl );
        // init();
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
