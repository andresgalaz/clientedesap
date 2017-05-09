package prg.glz.cli.sync;

import java.io.File;
import java.io.FilenameFilter;

import prg.glz.cli.db.ControlHSQL;
import prg.util.cnv.ConvertFile;
import prg.util.cnv.ConvertString;

public class NombreArchivo {

	public static boolean aceptados(File f) {
		if (f == null)
			return false;
		return NombreArchivo.aceptados(f.getName());
	}

	public static boolean aceptados(String name) {
		if (name.equalsIgnoreCase(ControlHSQL.DBName))
			return false;
		String ext = ConvertFile.extension(name);
		if (ConvertString.isEmpty(ext))
			return false;
		if ("swp".equalsIgnoreCase(ext))
			return false;
		if (ext.indexOf('~') >= 0)
			return false;
		return true;
		// TTpFormObjeto.isExtensionValida( ext );
	}

	// V3.0: Se filtra archivos con extensiones JS, CSS, VM
	public static FilenameFilter filtro = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			if (ConvertString.isEmpty(ConvertFile.extension(name)))
				return true;
			return NombreArchivo.aceptados(name);
		}
	};

}
