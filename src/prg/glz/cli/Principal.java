package prg.glz.cli;

import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.log4j.Logger;

import prg.glz.cli.frm.FrmPrincipal;
import prg.glz.data.entity.TUsuario;

public class Principal {
    private static Logger  logger = Logger.getLogger( Principal.class );
    public static TUsuario usuario;

    // Permite usar HTTPS sin bajar el certificado localmente
    {
        TrustManager[] trustAllCertificates = new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    // No need to implement.
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    // No need to implement.
                }
            }
        };

        try {
            SSLContext sc = SSLContext.getInstance( "SSL" );
            sc.init( null, trustAllCertificates, new SecureRandom() );
            HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory() );
        } catch (Exception e) {
            throw new ExceptionInInitializerError( e );
        }
    }

    public static void main(String[] args) {
        try {
            String cClassLook = null;
            // Define apariencia
            // Linux : Metal, Nimbus, CDE/Motif, GTK+
            // Windows : Metal, Nimbus, CDE/Motif, Windows, Windows Classic
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals( info.getName() )) {
                    cClassLook = info.getClassName();
                    break;
                }
            }
            if (cClassLook == null) {
                for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("GTK+".equals( info.getName() )) {
                        cClassLook = info.getClassName();
                        break;
                    }
                }
            }
            if (cClassLook == null) {
                for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus-x".equals( info.getName() )) {
                        cClassLook = info.getClassName();
                        break;
                    }
                }
            }
            if (cClassLook != null)
                UIManager.setLookAndFeel( cClassLook );
        } catch (Exception e) {
            logger.warn( "No se pudo configurar apariencia", e );
        }
        new FrmPrincipal();
        // new DlgExtension().setVisible( true );
    }
}
