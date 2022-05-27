package prg.glz.cli.ws;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import prg.glz.FrameworkException;
import prg.glz.cli.config.Parametro;
import prg.glz.cli.sync.MultipartUtility;
import prg.glz.data.entity.TFormObjetoMigra;
import prg.glz.data.entity.TTpFormObjeto;
import prg.util.cnv.ConvertException;
import prg.util.cnv.ConvertMap;
import prg.util.cnv.ConvertString;

public class MigraFrwk extends AbstractFrwk {
    private static Logger logger = Logger.getLogger( MigraFrwk.class );

    public MigraFrwk(String cUrl) throws FrameworkException {
        super( cUrl );
    }

    public TFormObjetoMigra getFormByCIdForm(String cIdForm) throws FrameworkException {
        List<TFormObjetoMigra> lis = getListForm( false, cIdForm );
        if (lis == null || lis.size() == 0)
            return null;
        return lis.get( 0 );
    }

    public List<TFormObjetoMigra> getListForm() throws FrameworkException {
        return getListForm( "1".equals( Parametro.getMd5Diff() ), null );
    }

    /**
     * <p>
     * Trae la tabla de tipos de formularios, que en realidad en la versión 4, serían solo las extensiones posibles
     * </p>
     * 
     * @return lista de objetos TTpFormObjeto
     * @throws FrameworkException
     */
    public List<TTpFormObjeto> getAllTpForm() throws FrameworkException {
        try {
            URLConnection con = new URL( super.getUrlServer() + "/do/jsonCall" ).openConnection();
            super.sendHeader( con );
            con.setDoOutput( true );
            OutputStream out = con.getOutputStream();
            out.write( "prm_dataSource=xgenJNDI&prm_funcion=xfg.db.listaTabla&prm_cNombreTabla=tTpFormObjeto".getBytes( "UTF-8" ) );
            out.close();

            Map<String, Object> mResp = AbstractFrwk.getJsonMap( con.getInputStream() );
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) mResp.get( "records" );
            List<TTpFormObjeto> lisTpForm = new ArrayList<TTpFormObjeto>( records.size() );
            // Convierte un ArrayList de MAP a un ArrayList de TTpFormObjeto
            for (Map<String, Object> m : records) {
                lisTpForm.add( (TTpFormObjeto) ConvertMap.toObject( m, TTpFormObjeto.class ) );
            }
            return lisTpForm;

        } catch (ConvertException e) {
            String cMsg;
            logger.error( cMsg = "Se esperaba un objeto del tipo tTpFormObjeto:" + super.getUrlServer(), e );
            throw new FrameworkException( cMsg, e );
        } catch (IOException e) {
            String cMsg;
            logger.error( cMsg = "No se pudo leer URL:" + super.getUrlServer(), e );
            throw new FrameworkException( cMsg, e );
        }
    }

    /**
     * <p>
     * Inserta nuevos tipos de formularios. Es decir nuevas extetensinoes de archivos
     * </p>
     * 
     * @param cExtension
     * @return
     * @throws FrameworkException
     */
    public void updateTpForm(String cExtension) throws FrameworkException {
        try {
            URLConnection con = new URL( super.getUrlServer() + "/do/jsonCall" ).openConnection();
            super.sendHeader( con );
            con.setDoOutput( true );
            OutputStream out = con.getOutputStream();
            {
                // Envía los parámetros
                String cPrm = "prm_dataSource=xgenJNDI";
                cPrm += "&prm_funcion=jStore.paForm.operTpFormUpd";
                cPrm += "&prm_cNombre=" + cExtension;
                out.write( cPrm.getBytes( "UTF-8" ) );
            }
            out.close();
            // Recibe la respuesta desde el servidor
            Map<String, Object> mResp = AbstractFrwk.getJsonMap( con.getInputStream() );
            if (!(Boolean) mResp.get( "success" ))
                throw new FrameworkException( (String) mResp.get( "message" ) );

        } catch (IOException e) {
            String cMsg;
            logger.error( cMsg = "No se pudo leer URL:" + super.getUrlServer(), e );
            throw new FrameworkException( cMsg, e );
        }
    }

    @SuppressWarnings("unchecked")
    private List<TFormObjetoMigra> getListForm(boolean bMd5, String cIdForm) throws FrameworkException {
        ObjectInput in = null;
        try {
            URLConnection con = new URL( super.getUrlServer() + "/do/formMigraEnvia" ).openConnection();
            super.sendHeader( con );
            con.setDoOutput( true );
            OutputStream out = con.getOutputStream();
            out.write( ("prm_bMd5=" + bMd5).getBytes( "UTF-8" ) );
            if (!ConvertString.isEmpty( cIdForm )) {
                out.write( ("&prm_cIdForm=" + cIdForm).getBytes( "UTF-8" ) );
            }
            out.close();
            logger.debug( "Envía petición al servidor" );
            in = new ObjectInputStream( con.getInputStream() );

            logger.debug( "Inicio des-serialización" );
            Object obj = in.readObject();
            logger.debug( "Fin des-serialización" );
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
