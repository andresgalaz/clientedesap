package prg.glz.cli.ws;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import prg.glz.FrameworkException;
import prg.glz.cli.Principal;
import prg.glz.data.entity.TUsuario;
import prg.util.cnv.ConvertException;
import prg.util.cnv.ConvertMap;

public class LoginFrwk extends AbstractFrwk {
    private static Logger logger = Logger.getLogger( LoginFrwk.class );

    public LoginFrwk(String cUrl) throws FrameworkException {
        super( cUrl );
    }

    public TUsuario login(String cUsuario, String cPassword) throws FrameworkException {
        // Inicializa usuario conectado
        Principal.usuario = null;

        try {
            // First set the default cookie manager.
            CookieHandler.setDefault( new CookieManager( null, CookiePolicy.ACCEPT_ALL ) );

            // Se llama a esta porque es método GET, se usa solo para recibir las COOKIES
            final String urlEstadoSesion = super.getUrlServer() + "/do/estadoSesion";
            URL url = new URL( urlEstadoSesion );
            URLConnection con = url.openConnection();
            super.setCookies( con.getHeaderFields().get( "Set-Cookie" ) );

            // Revisa el estado de la sesión cada 5 minutos
            Timer timer = new Timer();
            timer.schedule( new TimerTask() {
                byte [] bResp = new byte[2048]; 
                public void run() {
                    try {
                        URL url = new URL( urlEstadoSesion );
                        URLConnection con = url.openConnection();
                        InputStream in = con.getInputStream();
                        int ln = in.read( bResp );
                        logger.debug( new String(bResp,0,ln) );
                        in.close();
                    } catch (Exception e) {
                        logger.error( "NO está logeado", e );
                    }
                }
             }, 0, 5 * 60 * 1000);            

            // Con las COOKIES almacendas, se procede a hacer el login
            url = new URL( super.getUrlServer() + "/do/login" );
            con = url.openConnection();
            super.sendHeader( con );

            con.setDoOutput( true );
            OutputStream out = con.getOutputStream();
            out.write( ("CPassword=" + cPassword + "&CUsuario=" + cUsuario).getBytes( "UTF-8" ) );
            out.close();

            Map<String, Object> mUsr = AbstractFrwk.getJsonMap( con.getInputStream() );
            if (!(Boolean) mUsr.get( "success" )) {
                String m = (String) mUsr.get( "message" );
                if (m == null)
                    throw new FrameworkException( "No se pudo conectar, credenciales no válidas" );
                else
                    throw new FrameworkException( m );
            }

            try {
                return (TUsuario) ConvertMap.toObject( mUsr, TUsuario.class );
            } catch (ConvertException e) {
                logger.error( "Al convertir Map a TUsuarop", e );
                throw new FrameworkException( "Error inésperado al conectar.\nSe esperaba un JSON con la información del usuario", e );
            }

        } catch (FileNotFoundException e) {
            throw new FrameworkException( "No se pudo abrir " + super.getUrlServer() + "\n" + "Solo indique la raiz del Servidor web.", e );
        } catch (IOException e) {
            logger.error( "Al conectar al sitio " + super.getUrlServer(), e );
            throw new FrameworkException( "No se pudo abrir " + super.getUrlServer() + "\n", e );
        }
    }

    public boolean isConnected() throws FrameworkException {
        try {
            URLConnection con = new URL( super.getUrlServer() + "/do/estadoSesion" ).openConnection();
            super.sendHeader( con );
            Map<String, Object> m = AbstractFrwk.getJsonMap( con.getInputStream() );
            Boolean bOut = (Boolean) m.get( "bConectado" );
            return bOut == null ? false : bOut;
        } catch (Exception e) {
            String cMsg;
            logger.error( cMsg = "No se pudo verificar estado de la sesión:" + super.getUrlServer(), e );
            throw new FrameworkException( cMsg, e );
        }
    }

}
