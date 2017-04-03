package prg.glz.cli;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.log4j.Logger;

import prg.glz.cli.frm.FrmPrincipal;
import prg.glz.data.entity.TUsuario;

public class Principal {
    private static Logger  logger = Logger.getLogger( Principal.class );
    public static TUsuario usuario;

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
                    if ("Nimbus".equals( info.getName() )) {
                        cClassLook = info.getClassName();
                        break;
                    }
                }
            }
            UIManager.setLookAndFeel( cClassLook );
        } catch (Exception e) {
            logger.warn( "No se pudo configurar apariencia", e );
        }
        new FrmPrincipal();
    }
}
