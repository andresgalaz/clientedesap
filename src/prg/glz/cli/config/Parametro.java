package prg.glz.cli.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import prg.glz.cli.db.ControlHSQL;

public class Parametro {
    private static String      dir;
    private static String      servidor;
    private static String      usuario;
    private static String      password;
    private static String      passwordSave;
    private static ControlHSQL hsql;

    static {
        if (servidor == null) {
            new Parametro().carga();
        }
    }

    public Parametro() {
    }

    public void carga() {
        Properties props = new Properties();
        InputStream is = null;

        // First try loading from the current directory
        try {
            File f = new File( "server.properties" );
            is = new FileInputStream( f );
        } catch (Exception e) {
            is = null;
        }

        try {
            if (is == null) {
                // Try loading from classpath
                is = getClass().getResourceAsStream( "server.properties" );
            }

            // Try loading properties from the file (if found)
            props.load( is );
        } catch (Exception e) {
        }

        servidor = props.getProperty( "servidor", "http://servidor:8080/webDesap" );
        usuario = props.getProperty( "usuario", "" );
        password = props.getProperty( "password", "" );
        passwordSave = props.getProperty( "passwordSave", "0" );
        dir = props.getProperty( "dir", "" );
    }

    public void graba() throws IOException {
        Properties props = new Properties();
        props.setProperty( "servidor", servidor == null ? "" : servidor );
        props.setProperty( "usuario", usuario == null ? "" : usuario );
        props.setProperty( "password", password == null ? "" : password );
        props.setProperty( "passwordSave", passwordSave == null ? "0" : passwordSave );
        props.setProperty( "dir", dir == null ? "" : dir );
        File f = new File( "server.properties" );
        OutputStream out = new FileOutputStream( f );
        props.store( out, "Configuracion Sincronizador Framework Compustrom" );
    }

    public static String getDir() {
        return dir;
    }

    public static void setDir(String dir) {
        Parametro.dir = dir;
    }

    public static String getServidor() {
        return servidor;
    }

    public static void setServidor(String servidor) {
        Parametro.servidor = servidor;
    }

    public static ControlHSQL getHsql() {
        return hsql;
    }

    public static void setHsql(ControlHSQL hsql) {
        Parametro.hsql = hsql;
    }

    public static String getUsuario() {
        return usuario;
    }

    public static void setUsuario(String usuario) {
        Parametro.usuario = usuario;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        Parametro.password = password;
    }

    public static String getPasswordSave() {
        return passwordSave;
    }

    public static void setPasswordSave(String passwordSave) {
        Parametro.passwordSave = passwordSave;
    }
}
