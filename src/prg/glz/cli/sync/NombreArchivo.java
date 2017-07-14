package prg.glz.cli.sync;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import prg.glz.cli.config.Parametro;
import prg.glz.cli.db.ControlHSQL;
import prg.glz.data.entity.TTpFormObjeto;
import prg.util.cnv.ConvertFile;
import prg.util.cnv.ConvertString;

public class NombreArchivo {
    public static final int            TP_ARCHIVO_RECHAZADO = 0;
    public static final int            TP_ARCHIVO_ACEPTADO  = 1;
    public static final int            TP_ARCHIVO_NOEXISTE  = 2;
    private static List<TTpFormObjeto> lisTpForm;

    public static int aceptados(File f) {
        if (f == null)
            return TP_ARCHIVO_RECHAZADO;
        return NombreArchivo.aceptados( f.getName() );
    }

    public static int aceptados(String name) {
        if (ConvertString.isEmpty( name ))
            return TP_ARCHIVO_RECHAZADO;
        if (name.indexOf( ControlHSQL.DBName ) == 0 || name.indexOf( Parametro.SERVER_PROPS_NAME ) == 0 )
            return TP_ARCHIVO_RECHAZADO;
        
        name = name.toLowerCase();
        String ext = ConvertFile.extension( name );
        if (ConvertString.isEmpty( ext ))
            return TP_ARCHIVO_RECHAZADO;
        if (ext.indexOf( '~' ) >= 0)
            return TP_ARCHIVO_RECHAZADO;
        // Busca la extención en TTpFormObjeto por nombre
        TTpFormObjeto tpForm = null;
        for (TTpFormObjeto reg : lisTpForm) {
            if (ext.equals( reg.getcNombre() )) {
                tpForm = reg;
                break;
            }
        }
        if (tpForm == null)
            return TP_ARCHIVO_NOEXISTE;
        return (tpForm.getbExcluido() ? TP_ARCHIVO_RECHAZADO : TP_ARCHIVO_ACEPTADO);
    }

    // Filtra de archivos que deben existir
    public static FilenameFilter filtro = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            if (ConvertString.isEmpty( ConvertFile.extension( name ) ))
                return true;
            return NombreArchivo.aceptados( name ) != TP_ARCHIVO_RECHAZADO;
        }
    };

    public static List<TTpFormObjeto> getLisTpForm() {
        return lisTpForm;
    }

    public static void setLisTpForm(List<TTpFormObjeto> lisTpForm) {
        NombreArchivo.lisTpForm = lisTpForm;
    }

}
