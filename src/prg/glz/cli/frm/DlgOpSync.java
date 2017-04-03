package prg.glz.cli.frm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import prg.glz.data.entity.TTpFormObjeto;
import prg.util.cnv.ConvertFile;

public class DlgOpSync extends JDialog {
    private static final long serialVersionUID = 1L;
    private JLabel            lbMsg;
    private JComboBox<String> cbTpForm;
    private JButton           coUpdate;
    private JButton           coCommit;
    private JButton           coSaltar;
    private JButton           coCancel;
    public static final int   DLG_UPDATE       = 1;
    public static final int   DLG_COMMIT       = 2;
    public static final int   DLG_SALTAR       = 3;
    public static final int   DLG_CANCEL       = 4;
    private int               respuesta;
    private int               tpObjeto;

    public int getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(int nRespuesta) {
        respuesta = nRespuesta;
    }

    public int getTpObjeto() {
        return tpObjeto;
    }

    private String buildMsg(String cMsg, String cArch) {
        return "<html>"
                + "<font size='4' color='#456560' face='Verdana'>" + cMsg + "</font><br>"
                + "<br>"
                + "<font size='5' color='#252540' face='Verdana'>" + cArch + "</font><br>"
                + "<html>";

    }

    public int cofirmaUpdate(String cArch) {
        lbMsg.setText( buildMsg( "El archivo local tiene fecha<br> posterior al archivo remoto:", cArch ) );
        coUpdate.setText( "Forzar Update" );
        coCommit.setText( "Commit" );
        cbTpForm.setVisible( false );
        pack();
        setVisible( true );
        return respuesta;
    }

    public int cofirmaCommit(String cArch) {
        lbMsg.setText( buildMsg( "El archivo local tiene fecha<br>anterior al archivo remoto:", cArch ) );
        coUpdate.setText( "Forzar Update" );
        coCommit.setText( "Commit" );
        cbTpForm.setVisible( false );
        pack();
        setVisible( true );
        return respuesta;
    }

    public int commitNuevo(String cArch) {
        lbMsg.setText( buildMsg( "El archivo local no existe en el<br>servidor, se creará en el servidor:", cArch ) );
        coUpdate.setText( "Borra Local" );
        coCommit.setText( "Commit Nuevo" );
        String ext = ConvertFile.extension( cArch );
        if ("js".equalsIgnoreCase( ext )) {
            cbTpForm.setVisible( true );
            tpObjeto = TTpFormObjeto.TP_FORMOBJETO_FUNCTION;
        } else {
            cbTpForm.setVisible( false );
            tpObjeto = TTpFormObjeto.getTpFormObjetoByExtension( ext );
        }
        pack();
        setVisible( true );
        return respuesta;
    }

    public int commitBorrado(String cArch) {
        lbMsg.setText( buildMsg( "El archivo fue borrado del servidor,<br>si quiere puede volver a subir:", cArch ) );
        coUpdate.setText( "Borra Local" );
        coCommit.setText( "Forzar Commit" );
        cbTpForm.setVisible( false );
        pack();
        setVisible( true );
        return respuesta;
    }

    public int commitBorrar(String cArch) {
        lbMsg.setText( buildMsg( "El archivo local no existe.<br>Se eliminará del servidor:", cArch ) );
        coUpdate.setText( "Forzar Update" );
        coCommit.setText( "Commit Delete" );
        cbTpForm.setVisible( false );
        pack();
        setVisible( true );
        return respuesta;
    }

    public int updateBorrar(String cArch) {
        lbMsg.setText( buildMsg( "El archivo local no existe en el servidor.<br>Se eliminará archivo local:", cArch ) );
        coUpdate.setText( "Delete Local" );
        coCommit.setText( "Forzar Commit" );
        cbTpForm.setVisible( false );
        pack();
        setVisible( true );
        return respuesta;
    }

    public DlgOpSync(Frame owner, String title, boolean modal) {
        super( owner, title, modal );
        Point p = new Point( 400, 400 );
        setLocation( p.x, p.y );

        // Create a message
        JPanel messagePane = new JPanel();
        lbMsg = new JLabel( "Mensaje ..." );
        messagePane.add( lbMsg );
        // Crea combo para definir el tipo de formulario
        cbTpForm = new JComboBox<String>();
        cbTpForm.addItem( "function" );
        cbTpForm.addItem( "raw" );
        cbTpForm.setSize( new Dimension( 3200, 150 ) );
        cbTpForm.setMinimumSize( new Dimension( 3200, 150 ) );
        cbTpForm.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                DlgOpSync.this.tpObjeto = TTpFormObjeto.getTpFormObjetoByDescripcion( cbTpForm.getSelectedItem().toString() );
            }
        } );

        messagePane.add( cbTpForm );

        getContentPane().add( messagePane );

        // Create a button
        JPanel buttonPane = new JPanel( new GridLayout( 2, 1 ) );
        buttonPane.setBorder( BorderFactory.createEmptyBorder( 8, 8, 8, 8 ) );
        coUpdate = new JButton( "Update" );
        coCommit = new JButton( "Commit" );
        coSaltar = new JButton( "Saltar" );
        coCancel = new JButton( "Cancelar" );
        buttonPane.add( coUpdate );
        buttonPane.add( coCommit );
        buttonPane.add( coSaltar );
        buttonPane.add( coCancel );
        // set action listener on the button
        coUpdate.addActionListener( new MyActionListener() );
        coCommit.addActionListener( new MyActionListener() );
        coSaltar.addActionListener( new MyActionListener() );
        coCancel.addActionListener( new MyActionListener() );
        // Set tecla acceso rapido
        coUpdate.setMnemonic( KeyEvent.VK_U );
        coCommit.setMnemonic( KeyEvent.VK_M );
        coSaltar.setMnemonic( KeyEvent.VK_S );
        coCancel.setMnemonic( KeyEvent.VK_C );

        getContentPane().add( buttonPane, BorderLayout.PAGE_END );
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        pack();
        // setVisible( true );
    }

    // override the createRootPane inherited by the JDialog, to create the
    // rootPane.
    // create functionality to close the window when "Escape" button is pressed
    public JRootPane createRootPane() {
        JRootPane rootPane = new JRootPane();
        Action action = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                respuesta = 0;
                int bKey = 0;
                try {
                    bKey = e.getActionCommand().getBytes()[0];
                } catch (Exception ex) {

                }
                switch (bKey) {
                case KeyEvent.VK_U:
                case KeyEvent.VK_U + 32:
                    respuesta = DLG_UPDATE;
                    break;
                case KeyEvent.VK_M:
                case KeyEvent.VK_M + 32:
                    respuesta = DLG_COMMIT;
                    break;
                case KeyEvent.VK_ESCAPE:
                case KeyEvent.VK_S:
                case KeyEvent.VK_S + 32:
                    respuesta = DLG_SALTAR;
                    break;
                case KeyEvent.VK_C:
                case KeyEvent.VK_C + 32:
                    respuesta = DLG_CANCEL;
                    break;
                }

                if (respuesta > 0) {
                    setVisible( false );
                    dispose();
                }
            }
        };
        InputMap inputMap = rootPane.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
        inputMap.put( KeyStroke.getKeyStroke( "ESCAPE" ), "VALID_KEY" );
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_U, 0 ), "VALID_KEY" );
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_U, KeyEvent.SHIFT_DOWN_MASK ), "VALID_KEY" );
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_M, 0 ), "VALID_KEY" );
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_M, KeyEvent.SHIFT_DOWN_MASK ), "VALID_KEY" );
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_S, 0 ), "VALID_KEY" );
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK ), "VALID_KEY" );
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_C, 0 ), "VALID_KEY" );
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_C, KeyEvent.SHIFT_DOWN_MASK ), "VALID_KEY" );
        rootPane.getActionMap().put( "VALID_KEY", action );
        return rootPane;
    }
    // an action listener to be used when an action is performed
    // (e.g. button is pressed)
    class MyActionListener implements ActionListener {

        // close and dispose of the window.
        public void actionPerformed(ActionEvent e) {
            respuesta = 0;
            if (e.getSource() == coUpdate)
                respuesta = DLG_UPDATE;
            else if (e.getSource() == coCommit)
                respuesta = DLG_COMMIT;
            else if (e.getSource() == coSaltar)
                respuesta = DLG_SALTAR;
            else if (e.getSource() == coCancel)
                respuesta = DLG_CANCEL;
            setVisible( false );
            dispose();
        }
    }
}
