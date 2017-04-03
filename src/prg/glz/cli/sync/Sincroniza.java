package prg.glz.cli.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import prg.glz.FrameworkException;
import prg.glz.cli.db.ControlHSQL;
import prg.glz.cli.frm.DlgOpSync;
import prg.glz.cli.ws.MigraFrwk;
import prg.glz.data.dao.TFormObjectMigraDAO;
import prg.glz.data.entity.TFormObjetoMigra;
import prg.glz.data.entity.TTpFormObjeto;
import prg.util.cnv.ConvertException;
import prg.util.cnv.ConvertFile;
import prg.util.cnv.ConvertString;
import prg.util.cnv.ConvertTimestamp;

public class Sincroniza {
    private static Logger       logger     = Logger.getLogger( Sincroniza.class );

    // Acciones de sincronzación
    private static int          ACC_UPDATE = 1;
    private static int          ACC_COMMIT = 2;

    private MigraFrwk           migraMgr;
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

        if (cDir.charAt( cDir.length() - 1 ) != '/')
            cDir += '/';
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
        this.migraMgr = new MigraFrwk( this.cUrlRemota );
    }

    private boolean uploadServerAndUpd(File fLocal, TFormObjetoMigra formRemoto) throws SQLException {
        // Sube al servidor
        Map<String, Object> mapForm = this.migraMgr.upload( fLocal, formRemoto );

        // Verifica retorno
        if ((Boolean) mapForm.get( "success" )) {
            formRemoto.setpFormObjeto( (Integer) mapForm.get( "PFORMOBJETO" ) );
            formRemoto.setnVersion( (Integer) mapForm.get( "NVERSION" ) );
            try {
                formRemoto.settModif( ConvertTimestamp.toTimestamp( mapForm.get( "TMODIF" ) ) );
            } catch (ConvertException e) {
                String cIdForm = ConvertFile.sinExtension( fLocal.getName() );
                logger.warn( "No se pudo convertir TMODIF al actualizar el formulario" + cIdForm + "\nTMODIF=" + mapForm.get( "TMODIF" ), e );
            }

            formObjetoLocalDao.update( formRemoto );

            return true;
        }
        throw new SQLException( (String) mapForm.get( "message" ) );
    }

    private boolean subeArchivoExistente(String cLocal, TFormObjetoMigra formRemoto) throws FrameworkException, SQLException {
        File fLocal = new File( this.dirLocal + cLocal );
        // No es un archivo o no existe, no hace nada
        if (!fLocal.isFile() || !fLocal.exists())
            return true;
        String ext = ConvertFile.extension( fLocal );
        if (!TTpFormObjeto.isExtensionValida( ext ))
            return true;

        String cIdForm = ConvertFile.sinExtension( fLocal.getName() );
        TFormObjetoMigra frmLocal = formObjetoLocalDao.getByCIdForm( cIdForm );
        if (formRemoto == null)
            formRemoto = migraMgr.getFormByCIdForm( cIdForm );

        dialogo.setRespuesta( DlgOpSync.DLG_COMMIT );
        if (formRemoto == null) {
            // El archivo nunca estuvo en el Sitio, solo está en el disco, se pregunta si se hace commit o borrar el
            // local, si se hace commit se pide además el tipo de formulario
            dialogo.commitNuevo( cLocal );

            // Copia desde la base local
            formRemoto = new TFormObjetoMigra();
            formRemoto.setcIdForm( cIdForm );
            formRemoto.setfTpObjeto( TTpFormObjeto.getTpFormObjetoByExtension( fLocal.getName() ) );
            if ("JS".equalsIgnoreCase( ext ))
                formRemoto.setfTpObjeto( dialogo.getTpObjeto() );
        } else if (frmLocal != null) {
            long cmpTModif = ConvertTimestamp.compareTo( frmLocal.gettModif(), formRemoto.gettModif() );
            if (cmpTModif < 0) {
                dialogo.cofirmaCommit( fLocal.getName() );
            } else if (cmpTModif == 0) {
                if (isEqualCFuente( fLocal, formRemoto ) && frmLocal.getfTpObjeto().compareTo( formRemoto.getfTpObjeto() ) == 0)
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
            FileOutputStream fo = null;
            try {
                fo = new FileOutputStream( fLocal );
                fo.write( formRemoto.getcFuente().getBytes( "UTF-8" ) );
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
        String ext = ConvertFile.extension( fLocal );
        if (!TTpFormObjeto.isExtensionValida( ext ))
            return;

        String cIdForm = ConvertFile.sinExtension( fLocal.getName() );
        TFormObjetoMigra formRemoto = migraMgr.getFormByCIdForm( cIdForm );
        // NO existe en sevidor remoto, no se hace nada, porque ya està borrado
        if (formRemoto == null)
            return;

        dialogo.setRespuesta( 0 );
        dialogo.commitBorrar( fLocal.getName() );
        if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL)
            return;
        if (dialogo.getRespuesta() == DlgOpSync.DLG_COMMIT) {
            // Verifica si existe el archivo local en la base local
            this.formObjetoLocalDao.deleteByCIdForm( cIdForm );
            this.migraMgr.remove( formRemoto );
        }
    }

    public void syncFile(File fLocal) throws FrameworkException, SQLException {
        // No es un archivo o no existe, no hace nada
        if (fLocal == null || !fLocal.isFile() || !fLocal.exists())
            return;
        String ext = ConvertFile.extension( fLocal );
        if (!TTpFormObjeto.isExtensionValida( ext ))
            return;

        String cIdForm = ConvertFile.sinExtension( fLocal.getName() );
        TFormObjetoMigra formRemoto = migraMgr.getFormByCIdForm( cIdForm );
        TFormObjetoMigra frmLocal = formObjetoLocalDao.getByCIdForm( cIdForm );
        dialogo.setRespuesta( DlgOpSync.DLG_COMMIT );
        if (frmLocal == null) {
            if (formRemoto == null) {
                // El archivo nunca estuvo en el Sitio, solo está en el disco, se pregunta si se hace commit o borrar el
                // local, si se hace commit se pide además el tipo de formulario
                dialogo.commitNuevo( fLocal.getName() );
            }
        } else {
            if (formRemoto == null) {
                // El archivo alguna vez fue bajado, pero ahora ya no está en el sitio, se pregunta si se hace commit o
                // borrar, avisando que este formulario es probable que ya haya sido borrado del sitio
                dialogo.commitBorrado( fLocal.getName() );
            } else {
                // La fecha de modificación del archivo local, es mayor que la fecha de modificación del archivo remoto,
                // se pide confirmación para hacer UPDATE, sin embargo el usuario puede solicitar COMMIT
                long cmpTModif = ConvertTimestamp.compareTo( frmLocal.gettModif(), formRemoto.gettModif() );
                if (cmpTModif < 0) {
                    dialogo.cofirmaCommit( fLocal.getName() );
                } else if (cmpTModif == 0) {
                    if (isEqualCFuente( fLocal, formRemoto ) && frmLocal.getfTpObjeto() == formRemoto.getfTpObjeto()) {
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
                    formRemoto.setfTpObjeto( TTpFormObjeto.getTpFormObjetoByExtension( fLocal.getName() ) );
                    if ("JS".equalsIgnoreCase( ext ))
                        formRemoto.setfTpObjeto( dialogo.getTpObjeto() );

                } else
                    formRemoto = frmLocal;
            }
            uploadServerAndUpd( fLocal, formRemoto );
            return;
        }
        if (dialogo.getRespuesta() == DlgOpSync.DLG_UPDATE) {
            if (formRemoto == null) {
                fLocal.delete();
                formObjetoLocalDao.deleteByCIdForm( ConvertFile.sinExtension( fLocal.getName() ) );
            } else {
                FileOutputStream fo = null;
                try {
                    fo = new FileOutputStream( fLocal );
                    fo.write( formRemoto.getcFuente().getBytes( "UTF-8" ) );
                    fo.flush();
                } catch (Exception e) {
                    throw new FrameworkException( "No se pudo bajar el archivo al disco:" + fLocal.getName(), e );
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
        String ext = ConvertFile.extension( fLocal );
        if (!TTpFormObjeto.isExtensionValida( ext ))
            return true;

        String cIdForm = ConvertFile.sinExtension( fLocal.getName() );
        dialogo.setRespuesta( 0 );
        dialogo.commitNuevo( cArchLocal );

        if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL)
            return false;
        if (dialogo.getRespuesta() == DlgOpSync.DLG_COMMIT) {
            // Copia desde la base local
            TFormObjetoMigra formRemoto = new TFormObjetoMigra();
            formRemoto.setcIdForm( cIdForm );
            formRemoto.setfTpObjeto( TTpFormObjeto.getTpFormObjetoByExtension( fLocal.getName() ) );
            if ("JS".equalsIgnoreCase( ext ))
                formRemoto.setfTpObjeto( dialogo.getTpObjeto() );
            return uploadServerAndUpd( fLocal, formRemoto );
        }
        if (dialogo.getRespuesta() == DlgOpSync.DLG_UPDATE) {
            fLocal.delete();
            formObjetoLocalDao.deleteByCIdForm( ConvertFile.sinExtension( fLocal.getName() ) );
            return true;
        }
        // Salta archivo
        return true;
    }

    private boolean bajaArchivoExistente(TFormObjetoMigra formRemoto) throws FrameworkException, SQLException {
        // Nombre archivo local, archivo a sincronizar
        File fLocal = new File( this.dirLocal + formRemoto.getcIdForm() + "." + TTpFormObjeto.getExtension( formRemoto.getfTpObjeto() ) );

        FileOutputStream fo = null;
        try {
            // Se define por defacto que se va a actualizar
            dialogo.setRespuesta( DlgOpSync.DLG_UPDATE );

            // Lee estado archivo local
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
                // La fecha de modificación del archivo local, es mayor que la fecha de modificación del archivo remoto,
                // se pide confirmación para hacer UPDATE, sin embargo el usuario puede solicitar COMMIT
                long cmpTModif = ConvertTimestamp.compareTo( frmLocal.gettModif(), formRemoto.gettModif() );
                if (cmpTModif > 0) {
                    dialogo.cofirmaUpdate( fLocal.getName() );
                } else if (cmpTModif == 0) {
                    if (isEqualCFuente( fLocal, formRemoto ) && frmLocal.getfTpObjeto().equals( formRemoto.getfTpObjeto() )) {
                        dialogo.setRespuesta( DlgOpSync.DLG_SALTAR );
                    } else {
                        dialogo.cofirmaUpdate( fLocal.getName() );
                    }
                }
            }

            // Cancela la sincronización
            if (dialogo.getRespuesta() == DlgOpSync.DLG_CANCEL) {
                return false;
            }

            // Actualiza archivo local
            if (dialogo.getRespuesta() == DlgOpSync.DLG_UPDATE) {
                fo = new FileOutputStream( fLocal );
                fo.write( formRemoto.getcFuente().getBytes( "UTF-8" ) );
                fo.flush();
                formObjetoLocalDao.deleteByCIdForm( formRemoto.getcIdForm() );
                frmLocal = formRemoto.clone();
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
        fLocal = new File( this.dirLocal + formRemoto.getcIdForm() + "." + TTpFormObjeto.getExtension( formRemoto.getfTpObjeto() ) );

        FileOutputStream fo = null;
        try {
            // Verifica si existe el archivo local en la base local
            this.formObjetoLocalDao.deleteByCIdForm( formRemoto.getcIdForm() );

            fo = new FileOutputStream( fLocal );
            fo.write( formRemoto.getcFuente().getBytes( "UTF-8" ) );
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
        String cFuenteLocal;
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
                cFuenteLocal = new String( sb, "UTF-8" );
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
        return cFuenteLocal.compareTo( frmRemoto.getcFuente() ) == 0;
    }

    private boolean eliminaRemoto(TFormObjetoMigra formRemoto) throws SQLException, FrameworkException {
        if (formRemoto == null)
            return true;
        // Nombre archivo local, archivo a sincronizar
        File fSync = new File( this.dirLocal + formRemoto.getcIdForm() + "." + TTpFormObjeto.getExtension( formRemoto.getfTpObjeto() ) );

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

            this.migraMgr.remove( formRemoto );
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

    public int syncForms(int nAccion) throws FrameworkException, SQLException {
        File fDir = new File( this.dirLocal );
        if (!fDir.isDirectory())
            throw new FrameworkException( "No existe el directorio " + this.dirLocal );

        // Lee todos los archivos del directorio
        List<String> lisFileLocal = new ArrayList<String>();
        // Se filtra archivos con extensiones JS, CSS, VM
        FilenameFilter filtro = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String ext = ConvertFile.extension( name );
                return TTpFormObjeto.isExtensionValida( ext );
            }
        };
        for (String nombreArch : fDir.list( filtro )) {
            lisFileLocal.add( nombreArch );
        }
        // Ordena ambas Listas antes de parear
        Collections.sort( lisFileLocal, new Comparator<String>() {
            @Override
            public int compare(String c1, String c2) {
                c1 = ConvertFile.sinExtension( c1 );
                c2 = ConvertFile.sinExtension( c2 );
                return c1.compareTo( c2 );
            }
        } );

        // Lee formularios desde el servidor
        List<TFormObjetoMigra> lisFormSite;
        lisFormSite = this.migraMgr.getListForm();
        Collections.sort( lisFormSite );

        // Parea los arreglos lisFileLocal y lisFormSite, comparando por 'Nombre del Archivo' y 'cIdForm'
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
            int nComp = ConvertFile.sinExtension( cLocal ).compareTo( formRemoto.getcIdForm() );
            if (nComp == 0) {
                String extLocal = ConvertFile.extension( cLocal );
                String extRemoto = TTpFormObjeto.getExtension( formRemoto.getfTpObjeto() );
                nComp = extLocal.compareTo( extRemoto );
            }
            if (nComp > 0) {
                // El formulario remoto no está en el disco, o el archivo local fue borrado
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

    public ControlHSQL getHsql() {
        return hsql;
    }
}
