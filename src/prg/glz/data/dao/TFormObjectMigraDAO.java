package prg.glz.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import prg.glz.data.entity.TFormObjetoMigra;
import prg.util.cnv.ConvertNumber;

public class TFormObjectMigraDAO {
    Connection             con;
    private static boolean bExisteTabla = false;

    public TFormObjectMigraDAO(Connection con) throws SQLException {
        this.con = con;
        this.createTables();
    }

    public void insert(TFormObjetoMigra frmCopyLocal) throws SQLException {
        PreparedStatement ps = null;
        try {
            String cSql = "INSERT INTO tFormObjeto ( pFormObjeto, cIdForm, nVersion, fTpObjeto, cObservacion, tModif ) " +
                    "VALUES ( ?, ?, ?, ?, ?, ? );";
            ps = this.con.prepareStatement( cSql );
            int nIdx = 1;
            if (ConvertNumber.isCero( frmCopyLocal.getpFormObjeto() ))
                ps.setNull( nIdx++, java.sql.Types.INTEGER );
            else
                ps.setInt( nIdx++, frmCopyLocal.getpFormObjeto() );
            ps.setString( nIdx++, frmCopyLocal.getcIdForm() );
            ps.setInt( nIdx++, frmCopyLocal.getnVersion() );
            ps.setInt( nIdx++, frmCopyLocal.getfTpObjeto() );
            ps.setString( nIdx++, frmCopyLocal.getcObservacion() );
            ps.setTimestamp( nIdx++, frmCopyLocal.gettModif() );
            ps.executeUpdate();
        } finally {
            if (ps != null)
                ps.close();
        }
    }

    public void update(TFormObjetoMigra frmCopyLocal) throws SQLException {
        // Actauliza: eliminando e insertando de nuevo
        deleteByCIdForm( frmCopyLocal.getcIdForm() );
        // Eventualmente el PK puede estar utilizado por otro cIdForm, en tal caso, se inicializa
        if (getByPFormObjeto( frmCopyLocal.getpFormObjeto() ) != null)
            frmCopyLocal.setpFormObjeto( null );
        insert( frmCopyLocal );
    }

    public List<TFormObjetoMigra> getAll() throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String cSql = "SELECT * FROM tFormObjeto";
            ps = this.con.prepareStatement( cSql );
            rs = ps.executeQuery();
            return rs2list( rs );
        } finally {
            rs.close();
            ps.close();
        }
    }

    public TFormObjetoMigra getByPFormObjeto(Integer pFormObjeto) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String cSql = "SELECT * FROM tFormObjeto WHERE pFormObjeto = ? ORDER BY tModif DESC";
            ps = this.con.prepareStatement( cSql );
            ps.setInt( 1, pFormObjeto );
            rs = ps.executeQuery();
            return rs2object( rs );
        } finally {
            rs.close();
            ps.close();
        }
    }

    public TFormObjetoMigra getByCIdForm(String cIdForm) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String cSql = "SELECT * FROM tFormObjeto WHERE cIdForm = ? ORDER BY tModif DESC";
            ps = this.con.prepareStatement( cSql );
            ps.setString( 1, cIdForm );
            rs = ps.executeQuery();
            return rs2object( rs );
        } finally {
            rs.close();
            ps.close();
        }
    }

    public void deleteByCIdForm(String cIdForm) throws SQLException {
        PreparedStatement ps = null;
        try {
            String cSql = "DELETE FROM tFormObjeto WHERE cIdForm = ?";
            ps = this.con.prepareStatement( cSql );
            ps.setString( 1, cIdForm );
            ps.executeUpdate();
        } finally {
            ps.close();
        }
    }

    private List<TFormObjetoMigra> rs2list(ResultSet rs) throws SQLException {
        List<TFormObjetoMigra> lis = new ArrayList<TFormObjetoMigra>();
        TFormObjetoMigra vo;
        while ((vo = rs2object( rs )) != null)
            lis.add( vo );
        return lis;
    }

    private TFormObjetoMigra rs2object(ResultSet rs) throws SQLException {
        if (!rs.next())
            return null;
        TFormObjetoMigra vo = new TFormObjetoMigra();
        vo.setpFormObjeto( rs.getInt( "pFormObjeto" ) );
        vo.setnVersion( rs.getInt( "nVersion" ) );
        vo.setcIdForm( rs.getString( "cIdForm" ) );
        vo.setcObservacion( rs.getString( "cObservacion" ) );
        vo.setfTpObjeto( rs.getInt( "fTpObjeto" ) );
        vo.settModif( rs.getTimestamp( "tModif" ) );
        return vo;
    }

    /**
     * <p>
     * Verifica si la tabla tFormObjeto, existe, si es necesario se crea, la verificaciòn solo se hace una sola vez
     * </p>
     * 
     * @throws SQLException
     */
    private void createTables() throws SQLException {
        if (bExisteTabla)
            return;
        Statement st = this.con.createStatement();
        String cSql = null;
        try {
            cSql = "CREATE TABLE tFormObjeto ( " +
                    "  pFormObjeto   INTEGER IDENTITY " +
                    ", cIdForm       VARCHAR(40) NOT NULL " +
                    ", nVersion      INTEGER NOT NULL " +
                    ", cObservacion  VARCHAR(400) NULL " +
                    ", fTpObjeto     INTEGER NOT NULL " +
                    ", tModif        TIMESTAMP NOT NULL " +
                    ");";
            st.executeUpdate( cSql );
        } catch (SQLException e) {
            cSql = "SELECT 1 FROM tFormObjeto WHERE 1=2;";
            ResultSet rs = st.executeQuery( cSql );
            rs.close();
            bExisteTabla = true;
        } finally {
            st.close();
        }
    }

}
