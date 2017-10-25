package prg.glz.cli.frm;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

@SuppressWarnings("serial")
public class PnParams2 extends JPanel {
    private JTextField txtServer;
    private JTextField txtUsuario;
    private JTextField txtPasssword;

    /**
     * Create the panel.
     */
    public PnParams2() {
        setLayout( null );
        
        JLabel lblServidor = new JLabel("Servidor");
        lblServidor.setFont(new Font("Dialog", Font.PLAIN, 12));
        lblServidor.setBounds(12, 12, 70, 15);
        add(lblServidor);
        
        txtServer = new JTextField();
        txtServer.setText("server");
        txtServer.setBounds(100, 10, 327, 19);
        add(txtServer);
        txtServer.setColumns(10);
        
        JLabel lblUsuario = new JLabel("Usuario");
        lblUsuario.setFont(new Font("Dialog", Font.PLAIN, 12));
        lblUsuario.setBounds(12, 35, 90, 15);
        add(lblUsuario);
        
        txtUsuario = new JTextField();
        txtUsuario.setText("Usuario");
        txtUsuario.setBounds(100, 33, 139, 19);
        add(txtUsuario);
        txtUsuario.setColumns(10);
        
        JLabel lblContrasea = new JLabel("Contraseña");
        lblContrasea.setFont(new Font("Dialog", Font.PLAIN, 12));
        lblContrasea.setBounds(12, 59, 90, 15);
        add(lblContrasea);
        
        txtPasssword = new JTextField();
        txtPasssword.setText("Passsword");
        txtPasssword.setBounds(100, 57, 139, 19);
        add(txtPasssword);
        txtPasssword.setColumns(10);
        
        JLabel lblNoConectado = new JLabel("No conectado");
        lblNoConectado.setBounds(254, 35, 173, 15);
        add(lblNoConectado);
        
        JCheckBox chckbxGrabaContrasea = new JCheckBox("Graba Contraseña");
        chckbxGrabaContrasea.setFont(new Font("Dialog", Font.PLAIN, 12));
        chckbxGrabaContrasea.setBounds(247, 55, 180, 23);
        add(chckbxGrabaContrasea);
        
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBounds(12, 86, 426, 202);
        add(tabbedPane);
        
        JPanel panDirectorio = new JPanel();
        tabbedPane.addTab("Directorio", null, panDirectorio, null);
        tabbedPane.setEnabledAt(0, true);
        panDirectorio.setLayout(new BorderLayout(0, 0));
        
        JScrollPane scrollPane = new JScrollPane();
        tabbedPane.addTab("Log", null, scrollPane, null);
    }
}
