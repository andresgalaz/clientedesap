package prg.glz.cli.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import prg.glz.FrameworkException;
import prg.glz.cli.config.Parametro;
import prg.glz.cli.db.ControlHSQL;
import prg.glz.cli.frm.DlgOpSync;
import prg.glz.cli.frm.PnParams;
import prg.glz.cli.ws.MigraFrwk;
import prg.glz.data.dao.TFormObjectMigraDAO;
import prg.glz.data.entity.TFormObjetoMigra;
import prg.util.cnv.ConvertException;
import prg.util.cnv.ConvertFile;
import prg.util.cnv.ConvertString;
import prg.util.cnv.ConvertTimestamp;
import prg.util.sec.ChecksumMD5;

public class Sincroniza {
    private static Logger       logger     = Logger.getLogger( Sincroniza.class );

    // Acciones de sincronzación
    private static int          ACC_UPDATE = 1;
    private static int          ACC_COMMIT = 2;

    private MigraFrwk           migraFrwk;
    private DlgOpSync           dialogo;
    private String              dirLocal;
    private String              cUrlRemota;

    // Database local
    private ControlHSQL         hsql;
    private TFormObjectMigraDAO formObjetoLocalDao;

    public Sincroniza(String cUrl, String cDir, DlgOpSync dialogo) throws FrameworkException {
        if (ConvertString.isEmpty( cUrl ))
            throw new FrameworkException( "No se puede migrar sin URL del servidor" );
        // Ajusta nombre del directorio
        if (ConvertString.isEmpty( cDir ))
            throw new FrameworkException( "No se puede migrar sin definir el directorio local de trabajo" );
        File fDir = new File( cDir );
        if (!fDir.isDirectory())
            throw new FrameworkException( "No existe el directorio " + cDir );
        if (dialogo == null)
            throw new FrameworkException( "No se indicó el panel de dialogos" );

        this.cUrlRemota = cUrl;
        this.dialogo = dialogo;

        if (cDir.charAt( cDir.length() - 1 ) != File.separatorChar)
            cDir += File.separatorChar;
        this.dirLocal = cDir;
        init();
    }

    private void init() throws FrameworkException {
        // Crea BD local
        hsql = new ControlHSQL();
        try {
            hsql.connectDB( this.dirLocal );
            formObjetoLocalDao = new TFormObjectMigraDAO( this.hsql.getConnection() );
        } catch (SQLException e) {
            throw new FrameworkException( "Al conectar a la base:" + this.dirLocal, e );
        }

        // Crea conexión al Framework
        this.migraFrwk = new MigraFrwk( this.cUrlRemota );
    }

    private boolean uploadServerAndUpd(File fLocal, TFormObjetoMigra formRemoto) throws SQLException {
        // Sube al servidor
        Map<String, Object> mapForm = this.migraFrwk.upload( fLocal, formRemoto );

        // Verifica retorno
        if ((Boolean) mapForm.get( "success" )) {
            formRemoto.setpFormObjeto( (Integer) mapForm.get( "PFORMOBJETO" ) );
            formRemoto.setnVersion( (Integer) mapForm.get( "NVERSION" ) );
            try {
                formRemoto.settModif( ConvertTimestamp.toTimestamp( mapForm.get( "TMODIF" ) ) );
            } catch (ConvertException e) {
                // V3.0: String cIdForm =
                // ConvertFile.sinExtension(fLocal.getName());
                String cIdForm = getRutaRelativa( fLocal.getPath() );
                logger.warn( "No se pudo convertir TMODIF al actualizar el formulario" + cIdForm + "\nTMODIF=" + mapForm.get( "TMODIF" ), e );
            }

            formObjetoLocalDao.update( formRemoto );

            return true;
        }
        throw new SQLException( (String) mapForm.get( "message" ) );
    }

    private boolean subeArchivoExistente(String cLocal, TFormObjetoMigra formRemoto)
            throws FrameworkException, SQLException {
        File fLocal = new File( this.dirLocal + cLocal );
        // No es un archivo o no existe, no hace nada
        if (!fLocal.isFile() || !fLocal.exists())
            return true;
        if (!verificaTpArchivo( cLocal ))
            return true;

        String cIdForm = fixUnixPath( getRutaRelativa( fLocal.getPath() ) );
        TFormObjetoMigra frmLocal = formObjetoLocalDao.getByCIdForm( cIdForm );
        if (formRemoto == null)
            formRemoto = migraFrwk.getFormByCIdForm( cIdForm );

        dialogo.setRespuesta( DlgOpSync.DLG_COMMIT );
        if (formRemoto == null) {
            // El archivo nunca estuvo en el Sitio, solo está en el disco, se
            // pregunta si se hace commit o borrar el
            // local, si se hace commit se pide además el tipo de formulario
            dialogo.commitNuevo( cLocal );

            // Copia desde la base local
            formRemoto = new TFormObjetoMigra();
            formRemoto.setcIdForm( cIdForm );
            // V3.0
            // formRemoto.setfTpObjeto(TTpFormObjeto.getTpFormObjetoByExtension(fLocal.getName()));
            // if ("JS".equalsIgnoreCase(ext))
            // formRemoto.setfTpObjeto(dialogo.getTpObjeto());
        } else if (frmLocal != null) {
            long cmpTModif = ConvertTimestamp.compareTo( frmLocal.gettModif(), formRemoto.gettModif() );
            if (cmpTModif < 0) {
                dialogo.cofirmaCommit( cIdForm );
            } else if (cmpTModif == 0) {
                if (isEqualCFuente( fLocal, formRemoto ))
                    dialogo.setRespuesta( DlgOpSync.DLG_SALTAR );
            }
        } else {
            // frmLocal == null y frmRemoto != null
            if (isEqualCFuente( fLocal, formRemoto ))
                dialogo.setRespuesta( DlgOpSync.DLG_SALTAR );
        }

        if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL)
            return false;
        if (dialogo.getRespuesta() == DlgOpSync.DLG_COMMIT) {
            return uploadServerAndUpd( fLocal, formRemoto );
        }
        if (dialogo.getRespuesta() == DlgOpSync.DLG_UPDATE) {
            if ("1".equals( Parametro.getMd5Diff() )) {
                // Dado que cFuente trajo ChecksumMD5 se necesita leer contenido real
                formRemoto = migraFrwk.getFormByCIdForm( formRemoto.getcIdForm() );
            }

            FileOutputStream fo = null;
            try {
                fo = new FileOutputStream( fLocal );
                fo.write( formRemoto.getcFuente() );
                fo.flush();
            } catch (Exception e) {
                throw new FrameworkException( "No se pudo bajar el archivo al disco:" + cLocal, e );
            } finally {
                try {
                    if (fo != null)
                        fo.close();
                } catch (IOException e) {
                }
            }
            formObjetoLocalDao.deleteByCIdForm( formRemoto.getcIdForm() );
            frmLocal = formRemoto.clone();
            frmLocal.setpFormObjeto( null );
            formObjetoLocalDao.insert( frmLocal );
            return true;
        }
        // Salta archivo
        return true;
    }

    public void deleteFile(File fLocal) throws FrameworkException, SQLException {
        // Si el archivo existe
        if (fLocal == null || fLocal.exists())
            return;
        // V3.0
        // String ext = ConvertFile.extension(fLocal);
        // if (!TTpFormObjeto.isExtensionValida(ext))
        // return;

        // V3.0: String cIdForm = ConvertFile.sinExtension(fLocal.getName());
        String cIdForm = fixUnixPath( getRutaRelativa( fLocal.getPath() ) );
        TFormObjetoMigra formRemoto = migraFrwk.getFormByCIdForm( cIdForm );
        // NO existe en sevidor remoto, no se hace nada, porque ya està borrado
        if (formRemoto == null)
            return;

        dialogo.setRespuesta( 0 );
        dialogo.commitBorrar( cIdForm );
        if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL)
            return;
        if (dialogo.getRespuesta() == DlgOpSync.DLG_COMMIT) {
            // Verifica si existe el archivo local en la base local
            this.formObjetoLocalDao.deleteByCIdForm( cIdForm );
            this.migraFrwk.remove( formRemoto );
        }
    }

    public void syncFile(File fLocal) throws FrameworkException, SQLException {
        // No es un archivo o no existe, no hace nada
        if (fLocal == null || !fLocal.isFile() || !fLocal.exists())
            return;

        // V3.0: String cIdForm = ConvertFile.sinExtension(fLocal.getName());
        String cIdForm = fixUnixPath( getRutaRelativa( fLocal.getPath() ) );
        TFormObjetoMigra formRemoto = migraFrwk.getFormByCIdForm( cIdForm );
        TFormObjetoMigra frmLocal = formObjetoLocalDao.getByCIdForm( cIdForm );
        dialogo.setRespuesta( DlgOpSync.DLG_COMMIT );
        if (frmLocal == null) {
            if (formRemoto == null) {
                // El archivo nunca estuvo en el Sitio, solo está en el disco,
                // se pregunta si se hace commit o borrar el
                // local, si se hace commit se pide además el tipo de formulario
                dialogo.commitNuevo( cIdForm );
            }
        } else {
            if (formRemoto == null) {
                // El archivo alguna vez fue bajado, pero ahora ya no está en el
                // sitio, se pregunta si se hace commit o
                // borrar, avisando que este formulario es probable que ya haya
                // sido borrado del sitio
                dialogo.commitBorrado( cIdForm );
            } else {
                // La fecha de modificación del archivo local, es mayor que la
                // fecha de modificación del archivo remoto,
                // se pide confirmación para hacer UPDATE, sin embargo el
                // usuario puede solicitar COMMIT
                long cmpTModif = ConvertTimestamp.compareTo( frmLocal.gettModif(), formRemoto.gettModif() );
                if (cmpTModif < 0) {
                    dialogo.cofirmaCommit( cIdForm );
                } else if (cmpTModif == 0) {
                    if (isEqualCFuente( fLocal, formRemoto )) {
                        dialogo.setRespuesta( DlgOpSync.DLG_SALTAR );
                    }
                }
            }
        }
        if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL)
            return;
        if (dialogo.getRespuesta() == DlgOpSync.DLG_COMMIT) {
            if (formRemoto == null) {
                if (frmLocal == null) {
                    // Copia desde la base local
                    formRemoto = new TFormObjetoMigra();
                    formRemoto.setcIdForm( cIdForm );
                    // V3.0
                    // formRemoto.setfTpObjeto(TTpFormObjeto.getTpFormObjetoByExtension(fLocal.getName()));
                    // if ("JS".equalsIgnoreCase(ext))
                    // formRemoto.setfTpObjeto(dialogo.getTpObjeto());

                } else
                    formRemoto = frmLocal;
            }
            uploadServerAndUpd( fLocal, formRemoto );
            return;
        }
        if (dialogo.getRespuesta() == DlgOpSync.DLG_UPDATE) {
            if (formRemoto == null) {
                fLocal.delete();
                // formObjetoLocalDao.deleteByCIdForm(ConvertFile.sinExtension(fLocal.getName()));
                formObjetoLocalDao.deleteByCIdForm( cIdForm );
            } else {
                // Dado que cFuente trajo ChecksumMD5 se necesita leer contenido real
                formRemoto = migraFrwk.getFormByCIdForm( formRemoto.getcIdForm() );

                FileOutputStream fo = null;
                try {
                    fo = new FileOutputStream( fLocal );
                    fo.write( formRemoto.getcFuente() );
                    fo.flush();
                } catch (Exception e) {
                    throw new FrameworkException( "No se pudo bajar el archivo al disco:" + cIdForm, e );
                } finally {
                    try {
                        if (fo != null)
                            fo.close();
                    } catch (IOException e) {
                    }
                }
                formObjetoLocalDao.deleteByCIdForm( formRemoto.getcIdForm() );
                frmLocal = formRemoto.clone();
                frmLocal.setpFormObjeto( null );
                formObjetoLocalDao.insert( frmLocal );
            }
            return;
        }
        // Salta archivo
    }

    private boolean subeArchivoNuevo(String cArchLocal) throws FrameworkException, SQLException {
        File fLocal = new File( this.dirLocal + cArchLocal );
        // No es un archivo o no existe, no hace nada
        if (!fLocal.isFile() || !fLocal.exists())
            return true;

        if (!verificaTpArchivo( cArchLocal ))
            return true;

        String cIdForm = fixUnixPath( getRutaRelativa( fLocal.getPath() ) );
        dialogo.setRespuesta( 0 );
        dialogo.commitNuevo( cArchLocal );

        if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL)
            return false;
        if (dialogo.getRespuesta() == DlgOpSync.DLG_COMMIT) {
            // Copia desde la base local
            TFormObjetoMigra formRemoto = new TFormObjetoMigra();
            formRemoto.setcIdForm( cIdForm );
            // V3.0
            // formRemoto.setfTpObjeto(TTpFormObjeto.getTpFormObjetoByExtension(fLocal.getName()));
            // if ("JS".equalsIgnoreCase(ext))
            // formRemoto.setfTpObjeto(dialogo.getTpObjeto());
            return uploadServerAndUpd( fLocal, formRemoto );
        }
        if (dialogo.getRespuesta() == DlgOpSync.DLG_UPDATE) {
            fLocal.delete();
            // formObjetoLocalDao.deleteByCIdForm(ConvertFile.sinExtension(fLocal.getName()));
            formObjetoLocalDao.deleteByCIdForm( cIdForm );
            return true;
        }
        // Salta archivo
        return true;
    }

    private boolean bajaArchivoExistente(TFormObjetoMigra formRemoto) throws FrameworkException, SQLException {
        // Nombre archivo local, archivo a sincronizar
        // V3.0
        // File fLocal = new File( this.dirLocal + formRemoto.getcIdForm() + "."
        // + TTpFormObjeto.getExtension(formRemoto.getfTpObjeto()));
        File fLocal = new File( this.dirLocal + formRemoto.getcIdForm() );
        String cIdForm = fixUnixPath( getRutaRelativa( fLocal.getPath() ) );
        FileOutputStream fo = null;
        try {
            // Se define por defecto que se va a actualizar
            dialogo.setRespuesta( DlgOpSync.DLG_UPDATE );

            // Lee estado archivo desde la BD local
            TFormObjetoMigra frmLocal = formObjetoLocalDao.getByCIdForm( formRemoto.getcIdForm() );
            /*
             * if (frmLocal == null) { // Si el archivo está en el disco, pero no está en la base local, no se puede
             * determinar fecha // tModif del archivo, la fecha en el disco no es del todo relevante por las diferencias
             * de UTC, se // crea un frmLocal MOCK frmLocal = new TFormObjetoMigra(); // frmLocal.setpFormObjeto(
             * formRemoto.getpFormObjeto() ); frmLocal.setcIdForm( formRemoto.getcIdForm() ); frmLocal.setfTpObjeto(
             * formRemoto.getfTpObjeto() ); frmLocal.settModif( new Timestamp( 0 ) ); }
             */
            // Decide si hacer commit o update del archivo
            if (frmLocal != null) {
                // La fecha de modificación del archivo local, es mayor que la
                // fecha de modificación del archivo remoto,
                // se pide confirmación para hacer UPDATE, sin embargo el
                // usuario puede solicitar COMMIT
                long cmpTModif = ConvertTimestamp.compareTo( frmLocal.gettModif(), formRemoto.gettModif() );
                if (cmpTModif > 0) {
                    dialogo.cofirmaUpdate( cIdForm, false );
                } else if (cmpTModif == 0) {
                    if (isEqualCFuente( fLocal, formRemoto )) {
                        dialogo.setRespuesta( DlgOpSync.DLG_SALTAR );
                    } else {
                        dialogo.cofirmaUpdate( cIdForm, true );
                    }
                } else {
                    if (isEqualCFuente( fLocal, formRemoto ))
                        dialogo.setRespuesta( DlgOpSync.DLG_SALTAR );
                    else
                        logger.debug( "Diferencias de horas nada mas" );
                }
            }

            // Cancela la sincronización
            if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL) {
                return false;
            }

            // Actualiza archivo local
            if (dialogo.getRespuesta() == DlgOpSync.DLG_UPDATE) {
                if (isEqualCFuente( fLocal, formRemoto )) {
                    // Si el fuente es igual, solo se actuailiza la BD
                } else {
                    if ("1".equals( Parametro.getMd5Diff() )) {
                        // Dado que cFuente trajo ChecksumMD5 se necesita leer contenido real
                        formRemoto = migraFrwk.getFormByCIdForm( cIdForm );
                    }
                    fo = new FileOutputStream( fLocal );
                    fo.write( formRemoto.getcFuente() );
                    fo.flush();
                }
                formObjetoLocalDao.deleteByCIdForm( formRemoto.getcIdForm() );
                frmLocal = formRemoto.clone();
                frmLocal.settModif( formRemoto.gettModif() );
                frmLocal.setpFormObjeto( null );
                formObjetoLocalDao.insert( frmLocal );
                return true;
            }

            // Actualiza archivo remoto
            if (dialogo.getRespuesta() == DlgOpSync.DLG_COMMIT)
                return uploadServerAndUpd( fLocal, formRemoto );

            // Salta y no hace nada
            return true;

        } catch (IOException e) {
            logger.error( "No se pudo escribir archivo:" + fLocal, e );
            return false;
        } finally {
            try {
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
            }
        }
    }

    private void bajaArchivoNuevo(TFormObjetoMigra formRemoto) throws FrameworkException, SQLException {
        File fLocal = null;
        // Nombre archivo local, archivo a sincronizar
        // V3.0: fLocal = new File( this.dirLocal + formRemoto.getcIdForm() +
        // "." + TTpFormObjeto.getExtension(formRemoto.getfTpObjeto()));
        fLocal = new File( this.dirLocal + formRemoto.getcIdForm() );

        FileOutputStream fo = null;
        try {
            // Verifica si existe el archivo local en la base local
            this.formObjetoLocalDao.deleteByCIdForm( formRemoto.getcIdForm() );
            if ("1".equals( Parametro.getMd5Diff() )) {
                // Dado que cFuente trajo ChecksumMD5 se necesita leer contenido real
                formRemoto = migraFrwk.getFormByCIdForm( formRemoto.getcIdForm() );
            }
            buildRutaRelativa( formRemoto.getcIdForm() );

            fo = new FileOutputStream( fLocal );
            fo.write( formRemoto.getcFuente() );
            fo.flush();
            formObjetoLocalDao.deleteByCIdForm( formRemoto.getcIdForm() );
            TFormObjetoMigra formLocal = formRemoto.clone();
            formLocal.setpFormObjeto( null );
            formObjetoLocalDao.insert( formLocal );

        } catch (IOException e) {
            logger.error( "No se pudo escribir archivo:" + fLocal, e );
            throw new FrameworkException( "No se pudo escribir archivo:" + fLocal, e );
        } finally {
            try {
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
            }
        }
    }

    private boolean isEqualCFuente(File fLocal, TFormObjetoMigra frmRemoto) {
        byte[] cFuenteLocal;
        FileInputStream fi = null;
        try {
            fi = new FileInputStream( fLocal );
            {
                byte[] sb = new byte[(int) fLocal.length()];
                int l, n = 0;
                byte[] b = new byte[1024 * 4];
                while ((l = fi.read( b )) > 0) {
                    System.arraycopy( b, 0, sb, n, l );
                    n += l;
                }
                fi.close();
                cFuenteLocal = sb;
            }
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (fi != null)
                    fi.close();
            } catch (Exception e2) {
            }
        }
        boolean sinCambios = false;
        if ("1".equals( Parametro.getMd5Diff() )) {
            // Dado que cFuenteLocal no es ChecksumMD5 se necesita convertir para comparar
            sinCambios = Arrays.equals( ChecksumMD5.getByte( cFuenteLocal ), frmRemoto.getcFuente() );
        } else {
            sinCambios = Arrays.equals( cFuenteLocal, frmRemoto.getcFuente() );
        }
        // DEPURACION
        // if (!sinCambios) {
        //
        // try {
        // String cName = frmRemoto.getcIdForm().replaceAll( "\\/", "_" );
        // FileOutputStream fo = new FileOutputStream( new File( cName + "_L" ) );
        // fo.write( cFuenteLocal );
        // fo.flush();
        // fo.close();
        // fo = new FileOutputStream( new File( cName + "_R" ) );
        // fo.write( frmRemoto.getcFuente() );
        // fo.flush();
        // fo.close();
        // } catch (Exception e) {
        // logger.error( e );
        // }
        // logger.debug( "Diferencia" );
        // }

        return sinCambios;
    }

    private boolean eliminaRemoto(TFormObjetoMigra formRemoto) throws SQLException, FrameworkException {
        if (formRemoto == null)
            return true;
        // Nombre archivo local, archivo a sincronizar
        // V3.0: File fSync = new File( this.dirLocal + formRemoto.getcIdForm()
        // + "." + TTpFormObjeto.getExtension(formRemoto.getfTpObjeto()));
        File fSync = new File( this.dirLocal + formRemoto.getcIdForm() );

        dialogo.setRespuesta( 0 );
        dialogo.commitBorrar( fSync.getName() );
        // Cancela la sincronización
        if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL) {
            return false;
        }

        FileOutputStream fo = null;
        try {
            // Actualiza archivo local
            if (dialogo.getRespuesta() == DlgOpSync.DLG_UPDATE) {
                bajaArchivoNuevo( formRemoto );
                return true;
            }
        } finally {
            try {
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
            }
        }
        // Borrar archivo remoto
        if (dialogo.getRespuesta() == DlgOpSync.DLG_COMMIT) {
            // Verifica si existe el archivo local en la base local
            this.formObjetoLocalDao.deleteByCIdForm( formRemoto.getcIdForm() );

            this.migraFrwk.remove( formRemoto );
            return true;
        }
        return true;
    }

    private boolean eliminaLocal(int nAccion, String cLocal) throws SQLException, FrameworkException {
        if (ConvertString.isEmpty( cLocal ))
            return true;
        dialogo.setRespuesta( 0 );
        if (nAccion == ACC_COMMIT)
            dialogo.commitNuevo( cLocal );
        else if (nAccion == ACC_UPDATE)
            dialogo.updateBorrar( cLocal );

        // Cancela la sincronización
        if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL) {
            return false;
        }

        if (dialogo.getRespuesta() == DlgOpSync.DLG_UPDATE) {
            File fSync = null;
            fSync = new File( this.dirLocal + cLocal );
            fSync.delete();
            formObjetoLocalDao.deleteByCIdForm( ConvertFile.sinExtension( cLocal ) );
            return true;
        }
        // Sube como archivo nuevo
        if (dialogo.getRespuesta() == DlgOpSync.DLG_COMMIT) {
            return subeArchivoNuevo( cLocal );
        }
        return true;
    }

    /**
     * <p>
     * Trae los archivos desde el servidor al disco local. Sincroniza si hay cambios
     * </p>
     * 
     * @param cDir
     * @return
     * @throws FrameworkException
     * @throws SQLException
     */
    public int syncUpdate() throws FrameworkException, SQLException {
        return syncForms( ACC_UPDATE );
    }

    public int syncCommit() throws FrameworkException, SQLException {
        return syncForms( ACC_COMMIT );
    }

    public ControlHSQL getHsql() {
        return hsql;
    }

    /**
     * <p>
     * Verifica que la extensión del archivos sea válida y exista, y no se de las del tipo rechazadas. Da la opción de
     * crear la extensionen caso que no exista.
     * </p>
     * 
     * @param cArch
     * @return
     * @throws FrameworkException
     */
    public boolean verificaTpArchivo(String cArch) throws FrameworkException {
        int nArchivoOK = NombreArchivo.aceptados( cArch );
        if (nArchivoOK == NombreArchivo.TP_ARCHIVO_RECHAZADO) {
            return false;
        }
        if (nArchivoOK == NombreArchivo.TP_ARCHIVO_NOEXISTE) {
            String cExt = ConvertFile.extension( cArch ).toLowerCase();
            // No existe extensión, pregunta si desea crearla
            int nResp = JOptionPane.showConfirmDialog( PnParams.frmPrincipal, "Tipo de archivo con extensión (" + cExt + "), no existe.\n¿Desea crear esta extensión?" );
            if (nResp == JOptionPane.NO_OPTION)
                return false;
            if (nResp == JOptionPane.CANCEL_OPTION)
                throw new FrameworkException( "Sincronización cancelada por el usuario" );
            try {
                this.migraFrwk.updateTpForm( cExt );
            } catch (FrameworkException e) {
                logger.error( "No se pudo crear la extensión del archivo:" + cArch, e );
                JOptionPane.showMessageDialog( PnParams.frmPrincipal, e.getMessage() );
                return false;
            }
            NombreArchivo.setLisTpForm( this.migraFrwk.getAllTpForm() );
        }
        // Todo OK
        return true;
    }

    public int syncForms(int nAccion) throws FrameworkException, SQLException {
        if (!new File( this.dirLocal ).isDirectory())
            throw new FrameworkException( "No existe el directorio " + this.dirLocal );
        // Lee todos los archivos del directorio
        List<String> lisFileLocal = leerDirRecursivo( this.dirLocal, new ArrayList<String>() );

        Collections.sort( lisFileLocal );
        // V3.0
        // // Ordena ambas Listas antes de parear
        // Collections.sort(lisFileLocal, new Comparator<String>() {
        // @Override
        // public int compare(String c1, String c2) {
        // c1 = ConvertFile.sinExtension(c1);
        // c2 = ConvertFile.sinExtension(c2);
        // return c1.compareTo(c2);
        // }
        // });

        // Lee formularios desde el servidor
        List<TFormObjetoMigra> lisFormSite;
        lisFormSite = this.migraFrwk.getListForm();
        Collections.sort( lisFormSite );

        // Parea los arreglos lisFileLocal y lisFormSite, comparando por 'Nombre
        // del Archivo' y 'cIdForm'
        Iterator<String> itLocal = lisFileLocal.iterator();
        Iterator<TFormObjetoMigra> itRemoto = lisFormSite.iterator();
        String cLocal = null;
        TFormObjetoMigra formRemoto = null;
        // Sincroniza todos los archivos remotos y locales en el disco
        int nFiles = 0;
        while (itLocal.hasNext() && itRemoto.hasNext()) {
            if (cLocal == null)
                cLocal = itLocal.next();
            if (formRemoto == null)
                formRemoto = itRemoto.next();
            int nComp = cLocal.compareTo( formRemoto.getcIdForm() );
            logger.debug( "Comparando " + formRemoto.getcIdForm() );
            /*
             * V3.0
             */
            // int nComp = ConvertFile.sinExtension( cLocal ).compareTo(
            // formRemoto.getcIdForm() );
            // if (nComp == 0) {
            // String extLocal = ConvertFile.extension( cLocal );
            // String extRemoto = TTpFormObjeto.getExtension(
            // formRemoto.getfTpObjeto() );
            // nComp = extLocal.compareTo( extRemoto );
            // }
            if (nComp > 0) {
                // El formulario remoto no está en el disco, o el archivo local
                // fue borrado
                if (nAccion == ACC_UPDATE) {
                    // Se baja el archivo desde el servidor al disco local
                    bajaArchivoNuevo( formRemoto );
                } else if (nAccion == ACC_COMMIT) {
                    // Se solicia borrar el formulario remoto
                    if (!eliminaRemoto( formRemoto ))
                        throw new FrameworkException( "Se canceló la sincronización" );
                }
                // Avanza solo el itRemoto
                formRemoto = null;
            } else if (nComp < 0) {
                // El formulario local no está en el servidor
                if (nAccion == ACC_UPDATE) {
                    // Se solicia borrar el formulario local
                    if (!eliminaLocal( nAccion, cLocal ))
                        throw new FrameworkException( "Se canceló la sincronización" );
                } else if (nAccion == ACC_COMMIT) {
                    // Se sube y crea formulario remoto
                    if (!subeArchivoNuevo( cLocal ))
                        throw new FrameworkException( "Se canceló la sincronización" );
                }
                // Avanza solo el itLocal
                cLocal = null;
            } else {
                // Están ambos, se sincroniza
                if (nAccion == ACC_UPDATE) {
                    if (!bajaArchivoExistente( formRemoto ))
                        throw new FrameworkException( "Se canceló la sincronización" );
                } else if (nAccion == ACC_COMMIT) {
                    if (!subeArchivoExistente( cLocal, formRemoto ))
                        throw new FrameworkException( "Se canceló la sincronización" );
                }

                // Ambos avanzan
                formRemoto = null;
                cLocal = null;
            }
            nFiles++;
        }

        while (itLocal.hasNext()) {
            cLocal = itLocal.next();
            // El formulario local no está en el servidor
            if (nAccion == ACC_UPDATE) {
                // Se solicia borrar el formulario local
                if (!eliminaLocal( nAccion, cLocal ))
                    throw new FrameworkException( "Se canceló la sincronización" );
            } else if (nAccion == ACC_COMMIT) {
                // Se sube y crea formulario remoto
                if (!subeArchivoNuevo( cLocal ))
                    throw new FrameworkException( "Se canceló la sincronización" );
            }
            nFiles++;
        }

        while (itRemoto.hasNext()) {
            formRemoto = itRemoto.next();
            // El formulario remoto no está en el disco
            if (nAccion == ACC_UPDATE) {
                // Se baja el archivo desde el servidor al disco local
                bajaArchivoNuevo( formRemoto );
            } else if (nAccion == ACC_COMMIT) {
                // Se solicia borrar el formulario remoto
                if (!eliminaRemoto( formRemoto ))
                    throw new FrameworkException( "Se canceló la sincronización" );
            }
            nFiles++;
        }

        return nFiles;
    }

    private List<String> leerDirRecursivo(String cDir, List<String> lisFile) {
        for (String nombreArch : new File( cDir ).list( NombreArchivo.filtro )) {

            if (".git".equals( nombreArch ))
                continue;

            String cNombreCompleto = cDir + nombreArch;
            if (new File( cNombreCompleto ).isDirectory()) {
                leerDirRecursivo( cNombreCompleto + File.separator, lisFile );
            } else
                lisFile.add( fixUnixPath( getRutaRelativa( cNombreCompleto ) ) );
        }
        return lisFile;
        // V3.0
        // // Ordena ambas Listas antes de parear
        // Collections.sort(lisFileLocal, new Comparator<String>() {
        // @Override
        // public int compare(String c1, String c2) {
        // c1 = ConvertFile.sinExtension(c1);
        // c2 = ConvertFile.sinExtension(c2);
        // return c1.compareTo(c2);
        // }
        // });

    }

    private String getRutaRelativa(String cArchivo) {
        if (ConvertString.isEmpty( cArchivo ) || cArchivo.indexOf( File.separatorChar ) < 0)
            return cArchivo;

        if (Parametro.getDir().regionMatches( 0, cArchivo, 0, Parametro.getDir().length() ))
            return cArchivo.substring( Parametro.getDir().length() + 1 );
        return cArchivo;
    }

    private String fixUnixPath(String cArchivo) {
        if (cArchivo == null || cArchivo.indexOf( '\\' ) < 0)
            return cArchivo;
        return cArchivo.replaceAll( "\\\\", "/" );
    }

    private void buildRutaRelativa(String cArchivo) {
        int nPos = -1;
        if (ConvertString.isEmpty( cArchivo ) || (nPos = cArchivo.lastIndexOf( '/' )) < 0)
            return;
        File fDir = new File( Parametro.getDir() + "/" + cArchivo.substring( 0, nPos ) );
        if (!fDir.exists())
            fDir.mkdirs();
    }

}
