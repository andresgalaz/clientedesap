package prg.glz.cli.ws;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import prg.glz.FrameworkException;
import prg.glz.cli.sync.MultipartUtility;
import prg.glz.data.entity.TFormObjetoMigra;
import prg.util.cnv.ConvertString;

public class MigraFrwk extends AbstractFrwk {
    private static Logger logger = Logger.getLogger( MigraFrwk.class );

    public MigraFrwk(String cUrl) throws FrameworkException {
        super( cUrl );
    }

    public TFormObjetoMigra getFormByCIdForm(String cIdForm) throws FrameworkException {
        List<TFormObjetoMigra> lis = getListForm( cIdForm );
        if (lis == null || lis.size() == 0)
            return null;
        return lis.get( 0 );
    }

    public List<TFormObjetoMigra> getListForm() throws FrameworkException {
        return getListForm( null );
    }

    @SuppressWarnings("unchecked")
    private List<TFormObjetoMigra> getListForm(String cIdForm) throws FrameworkException {
        ObjectInput in = null;
        try {
            URLConnection con = new URL( super.getUrlServer() + "/do/formMigraEnvia" ).openConnection();
            super.sendHeader( con );
            if (!ConvertString.isEmpty( cIdForm )) {
                con.setDoOutput( true );
                OutputStream out = con.getOutputStream();
                out.write( ("prm_cIdForm=" + cIdForm).getBytes( "UTF-8" ) );
                out.close();
            }
            in = new ObjectInputStream( con.getInputStream() );

            Object obj = in.readObject();
            return (List<TFormObjetoMigra>) obj;
        } catch (IOException e) {
            String cMsg;
            logger.error( cMsg = "No se pudo leer URL:" + super.getUrlServer(), e );
            throw new FrameworkException( cMsg, e );
        } catch (ClassNotFoundException e) {
            String cMsg;
            logger.error( cMsg = "Error inesperado no existe la clase, al leer URL:" + super.getUrlServer(), e );
            throw new FrameworkException( cMsg, e );
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>
     * <Sube el archivo Form al servidor/p>
     * 
     * @param fileLocal
     */
    public Map<String, Object> upload(File fileLocal, TFormObjetoMigra formRemoto) {
        Map<String, Object> mresp = new HashMap<String, Object>();

        logger.debug( "Archivo " + fileLocal + " inicio sincronización" );
        if (!fileLocal.isFile()) {
            mresp.put( "success", false );
            mresp.put( "message", fileLocal.toString() + " no es un archivo " );
            return mresp;
        }
        if (!fileLocal.canRead()) {
            mresp.put( "success", false );
            mresp.put( "message", fileLocal.toString() + " no se puede leer" );
            return mresp;
        }
        if (fileLocal.length() == 0) {
            mresp.put( "success", false );
            mresp.put( "message", fileLocal.toString() + " está vacío" );
            return mresp;
        }
        if (formRemoto == null) {
            mresp.put( "success", false );
            mresp.put( "message", fileLocal.toString() + " está vacío" );
            return mresp;
        }

        try {
            MultipartUtility mPart = new MultipartUtility( super.getUrlServer() + "/do/formMigraUpfile", "UTF-8" );
            mPart.addHeaderField( "User-Agent", "Framework" );
            mPart.addFormField( "prm_idForm", formRemoto.getcIdForm() );
            mPart.addFormField( "prm_tpObjeto", formRemoto.getfTpObjeto() );
            mPart.addFilePart( "dataArchivo", fileLocal );
            return mresp = mPart.finish();
        } catch (IOException e) {
            logger.error( "No pudo sincronizar formulario " + formRemoto.getcIdForm() + " en el servidor:" + super.getUrlServer(), e );
            mresp.put( "success", false );
            mresp.put( "message", "No pudo sincronizar formulario " + formRemoto.getcIdForm() + " en el servidor:" + super.getUrlServer() );
            return mresp;
        }
    }

    /**
     * 
     * @param formRemoto
     * @throws FrameworkException
     */
    public Map<String, Object> remove(TFormObjetoMigra formRemoto) {
        try {
            URLConnection con = new URL( super.getUrlServer() + "/do/formObjetoDel" ).openConnection();
            super.sendHeader( con );
            con.setDoOutput( true );
            OutputStream out = con.getOutputStream();
            out.write( ("formObjeto_PFORMOBJETO=" + formRemoto.getpFormObjeto()
                    + "&formObjeto_CIDFORM=" + formRemoto.getcIdForm()).getBytes( "UTF-8" ) );
            out.close();

            return AbstractFrwk.getJsonMap( con.getInputStream() );
        } catch (Exception e) {
            String cMsg;
            logger.error( cMsg = "No se pudo borrar formulario en el servidor:" + formRemoto.getcIdForm(), e );
            Map<String, Object> mresp = new HashMap<String, Object>();
            mresp.put( "success", false );
            mresp.put( "message", cMsg );
            return mresp;
        }
    }

}
