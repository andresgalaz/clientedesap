package prg.glz.cli.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class ControlHSQL {
    private static Logger       logger = Logger.getLogger( ControlHSQL.class );
    private static final String DBH    = "jdbc:hsqldb:file:";
    private Connection          con    = null;
    private String              DBHDir;

    public void connectDB(String cDir) throws SQLException {
        try {
            Class.forName( "org.hsqldb.jdbcDriver" ); // Load HSQLDB driver
        } catch (Exception e) {
            throw new SQLException( "Falló al cargar Drivers de HSQLDB JDBC", e );
        }
        this.DBHDir = cDir + ".frmwkDb";
        String cNombreDB = DBH + DBHDir + "/xformgen";
        try {
            con = DriverManager.getConnection( cNombreDB );
        } catch (Exception e) {
            throw new SQLException( "No se pudo conectar a la base:" + cNombreDB, e );
        }
    }

    public String getDatabaseDir() {
        return DBHDir;
    }

    public void close() {
        // Save temporal data and close
        try {
            Statement st = con.createStatement();
            st.executeUpdate( "SHUTDOWN" );
            st.close();
        } catch (Exception e) {
            logger.warn( "Al hacer SHUTDOWN" );
        }

        try {
            con.close();
        } catch (Exception e) {
            logger.error( "Al cerrar la base", e );
        }
    }

    public Connection getConnection() {
        return con;
    }

}