package prg.glz.cli.frm;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.FlowLayout;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;
import net.miginfocom.swing.MigLayout;
import javax.swing.JTree;
import java.awt.GridLayout;
import java.awt.Color;

public class PanParams extends JPanel {
    private JTextField textField;
    private JTextField textField_1;
    private JTextField textField_2;

    /**
     * Create the panel.
     */
    public PanParams() {
        setLayout(new BorderLayout(0, 0));
        
        JPanel panTop = new JPanel();
        panTop.setBackground(UIManager.getColor("CheckBoxMenuItem.acceleratorForeground"));
        panTop.setPreferredSize(new Dimension(10, 130));
        add(panTop, BorderLayout.NORTH);
        panTop.setLayout(null);
        
        JLabel label = new JLabel("Servidor");
        label.setForeground(UIManager.getColor("CheckBox.background"));
        label.setFont(new Font("Dialog", Font.PLAIN, 12));
        label.setBounds(12, 14, 70, 15);
        panTop.add(label);
        
        textField = new JTextField();
        textField.setText("server");
        textField.setColumns(10);
        textField.setBounds(100, 12, 423, 19);
        panTop.add(textField);
        
        JLabel label_1 = new JLabel("Usuario");
        label_1.setForeground(UIManager.getColor("CheckBox.background"));
        label_1.setFont(new Font("Dialog", Font.PLAIN, 12));
        label_1.setBounds(12, 37, 90, 15);
        panTop.add(label_1);
        
        textField_1 = new JTextField();
        textField_1.setText("Usuario");
        textField_1.setColumns(10);
        textField_1.setBounds(100, 35, 139, 19);
        panTop.add(textField_1);
        
        JLabel label_2 = new JLabel("Contraseña");
        label_2.setForeground(UIManager.getColor("CheckBox.background"));
        label_2.setFont(new Font("Dialog", Font.PLAIN, 12));
        label_2.setBounds(12, 61, 90, 15);
        panTop.add(label_2);
        
        textField_2 = new JTextField();
        textField_2.setText("Passsword");
        textField_2.setColumns(10);
        textField_2.setBounds(100, 59, 139, 19);
        panTop.add(textField_2);
        
        JLabel label_3 = new JLabel("No conectado");
        label_3.setForeground(UIManager.getColor("CheckBox.background"));
        label_3.setBounds(254, 37, 173, 15);
        panTop.add(label_3);
        
        JCheckBox checkBox = new JCheckBox("Graba Contraseña");
        checkBox.setForeground(UIManager.getColor("CheckBox.background"));
        checkBox.setBackground(UIManager.getColor("CheckBoxMenuItem.acceleratorForeground"));
        checkBox.setFont(new Font("Dialog", Font.PLAIN, 12));
        checkBox.setBounds(247, 57, 180, 23);
        panTop.add(checkBox);
        
        JButton btnConectar = new JButton("Conectar");
        btnConectar.setFont(new Font("Dialog", Font.PLAIN, 12));
        btnConectar.setBounds(406, 93, 117, 25);
        panTop.add(btnConectar);
        
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        add(tabbedPane, BorderLayout.CENTER);
        
        JPanel panDir = new JPanel();
        tabbedPane.addTab("Directorio", null, panDir, null);
        panDir.setLayout(new MigLayout("", "[grow]", "[][grow]"));
        
        JButton btnSeleccionar = new JButton("Seleccionar");
        panDir.add(new JButton("Refrescar"), "cell 0 0,grow");
        panDir.add(btnSeleccionar, "cell 0 0,grow");
        
        JTree tree = new JTree();
        panDir.add(tree, "flowx,cell 0 1,grow");
        
        JTree tree_1 = new JTree();
        panDir.add(tree_1, "flowx,cell 0 1,grow");
        
        JPanel panLog = new JPanel();
        tabbedPane.addTab("Log", null, panLog, null);
        panLog.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));
        
        JTextArea txtrLog = new JTextArea();
        txtrLog.setText("Log");
        panLog.add(txtrLog, "2, 2, fill, fill");
        
        JPanel panSur = new JPanel();
        panSur.setBackground(UIManager.getColor("CheckBoxMenuItem.acceleratorForeground"));
        panSur.setPreferredSize(new Dimension(10, 100));
        add(panSur, BorderLayout.SOUTH);
        panSur.setLayout(null);
        
        JButton btnCommit = new JButton("Commit");
        btnCommit.setFont(new Font("Dialog", Font.PLAIN, 12));
        btnCommit.setBounds(139, 12, 117, 25);
        panSur.add(btnCommit);
        
        JLabel lblSincroniza = new JLabel("Sincroniza");
        lblSincroniza.setForeground(UIManager.getColor("CheckBox.background"));
        lblSincroniza.setFont(new Font("Dialog", Font.PLAIN, 12));
        lblSincroniza.setBounds(24, 17, 70, 15);
        panSur.add(lblSincroniza);
        
        JButton btnUpdate = new JButton("Update");
        btnUpdate.setFont(new Font("Dialog", Font.PLAIN, 12));
        btnUpdate.setBounds(283, 12, 117, 25);
        panSur.add(btnUpdate);
        
        JLabel lblServicio = new JLabel("Servicio");
        lblServicio.setForeground(UIManager.getColor("CheckBox.background"));
        lblServicio.setFont(new Font("Dialog", Font.PLAIN, 12));
        lblServicio.setBounds(24, 54, 70, 15);
        panSur.add(lblServicio);
        
        JButton btnArrancar = new JButton("Arrancar");
        btnArrancar.setForeground(new Color(0, 128, 0));
        btnArrancar.setFont(new Font("Dialog", Font.PLAIN, 12));
        btnArrancar.setBounds(139, 49, 117, 25);
        panSur.add(btnArrancar);
        
        JButton btnDetener = new JButton("Detener");
        btnDetener.setForeground(new Color(255, 0, 0));
        btnDetener.setFont(new Font("Dialog", Font.PLAIN, 12));
        btnDetener.setBounds(283, 49, 117, 25);
        panSur.add(btnDetener);
        
        JButton btnDesconectar = new JButton("Desconectar");
        btnDesconectar.setFont(new Font("Dialog", Font.PLAIN, 12));
        btnDesconectar.setBounds(418, 12, 117, 25);
        panSur.add(btnDesconectar);

    }
}
