package prg.glz.cli.frm;

import java.awt.BorderLayout;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class FrmPrincipal extends JFrame {
    private static Logger logger      = Logger.getLogger( FrmPrincipal.class );
    private TrayIcon      trayIcon;
    private SystemTray    tray;
    private long          lastTime    = 0;
    private boolean       bIconizando = false;

    public FrmPrincipal() {
        try {
            init();
            this.setVisible( true );
            this.setTitle( "Cliente Sicronización Framwork Web - Compustrom v2.0e" );
        } catch (Exception e) {
            logger.error( "No se pudo inicializar pantalla principal", e );
        }
    }

    private void init() {
        // Configura pantalla actual
        this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        this.setLayout( new BorderLayout() );
        this.setBounds( 100, 100, 900, 400 );
        // Carga icono
        this.setIconImage( new ImageIcon( this.getClass().getResource( "freefilesync.png" ) ).getImage() );
        // Si hay soporte de iconos en barra de tareas
        if (SystemTray.isSupported()) {
            this.tray = SystemTray.getSystemTray();

            // Menú para cuando está iconizado
            PopupMenu popup = new PopupMenu();
            MenuItem defaultItem = new MenuItem( "Salir" );
            defaultItem.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit( 0 );
                }
            } );
            popup.add( defaultItem );

            defaultItem = new MenuItem( "Abrir" );
            defaultItem.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    FrmPrincipal.this.setVisible( true );
                    FrmPrincipal.this.setExtendedState( JFrame.NORMAL );
                    try {
                        Thread.sleep( 100 );
                        FrmPrincipal.this.setExtendedState( JFrame.NORMAL );
                    } catch (InterruptedException e1) {
                    }
                }
            } );
            popup.add( defaultItem );
            this.trayIcon = new TrayIcon( this.getIconImage(), "Sincroniza Framework Compustrom", popup );
            this.trayIcon.setImageAutoSize( true );
            this.trayIcon.addMouseListener( new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        FrmPrincipal.this.setVisible( true );
                        FrmPrincipal.this.setExtendedState( JFrame.NORMAL );
                        try {
                            Thread.sleep( 200 );
                            FrmPrincipal.this.setExtendedState( JFrame.NORMAL );
                        } catch (InterruptedException e1) {
                        }
                    }
                }
            } );

        } else {
            this.tray = null;
            logger.warn( "Minimizado en barra de tareas no está soportado" );
        }

        // Maneja estado para cuando se activa la pantalla
        this.addWindowStateListener( new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
                long newTime = new Date().getTime();

                // Si no hay System Tray se sale, dado que no hay nada adicional
                // que hacer
                if (FrmPrincipal.this.tray == null)
                    return;

                // se espera que entre 2 eventos haya al menos 100 milisegundos
                // de diferencia, esto porque el eventualmente el cambio de
                // estado deja estados fantasmas casi inmediatos que no deben
                // considerarse.
                if (newTime - lastTime < 100)
                    return;

                if ((e.getNewState() & JFrame.ICONIFIED) == JFrame.ICONIFIED) {
                    // Minimiza en la barra de tareas
                    try {
                        FrmPrincipal.this.tray.add( trayIcon );
                    } catch (Exception e2) {
                    }
                    if (FrmPrincipal.this.bIconizando) {
                        FrmPrincipal.this.setVisible( false );
                        FrmPrincipal.this.bIconizando = false;
                    }
                    lastTime = new Date().getTime();
                } else if ((e.getNewState() & JFrame.NORMAL) == JFrame.NORMAL) {
                    lastTime = new Date().getTime();
                    // Elimina el ícono de la barra de tareas
                    try {
                        FrmPrincipal.this.tray.remove( trayIcon );
                    } catch (Exception e2) {
                    }
                    FrmPrincipal.this.setVisible( true );
                }
            }
        } );

        // Armado de paneles
        {
            // North : Panel Parámetros y/o Configuración
            PnParams params = new PnParams( this );
            this.add( params, BorderLayout.CENTER );
        }
    }

    public void iconizar() {
        this.bIconizando = true;
        this.setState( JFrame.ICONIFIED );
        this.setExtendedState( this.getExtendedState() | JFrame.ICONIFIED );
        // this.setVisible( false );
    }
}
