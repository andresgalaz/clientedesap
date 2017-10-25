package prg.glz.cli.frm;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.Font;

@SuppressWarnings("serial")
public class DlgExtension extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private JTextField txtExtension;
    private final ButtonGroup buttonGroupTipo = new ButtonGroup();
    private JRadioButton rdbtnTexto;
    private JRadioButton rdbtnBinario;
    private JCheckBox chckbxExcluir;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        try {
            DlgExtension dialog = new DlgExtension();
            dialog.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
            dialog.setVisible( true );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the dialog.
     */
    public DlgExtension() {
        setBackground(UIManager.getColor("CheckBoxMenuItem.acceleratorForeground"));
        setResizable(false);
        setModal(true);
        setTitle("Nueva Extensión");
        setBounds( 100, 100, 333, 203 );
        getContentPane().setLayout( new BorderLayout() );
        contentPanel.setBackground(UIManager.getColor("CheckBoxMenuItem.acceleratorForeground"));
        contentPanel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
        getContentPane().add( contentPanel, BorderLayout.CENTER );
        contentPanel.setLayout(null);
        {
            txtExtension = new JTextField();
            txtExtension.setFont(new Font("Dialog", Font.PLAIN, 12));
            txtExtension.setEditable(false);
            txtExtension.setText("extension");
            txtExtension.setBounds(107, 31, 151, 19);
            contentPanel.add(txtExtension);
            txtExtension.setColumns(10);
        }
        
        JLabel lblExtensin = new JLabel("Extensión");
        lblExtensin.setFont(new Font("Dialog", Font.PLAIN, 12));
        lblExtensin.setForeground(UIManager.getColor("CheckBoxMenuItem.background"));
        lblExtensin.setBounds(27, 33, 70, 15);
        contentPanel.add(lblExtensin);
        
        rdbtnTexto = new JRadioButton("Texto");
        rdbtnTexto.setFont(new Font("Dialog", Font.PLAIN, 12));
        rdbtnTexto.setSelected(true);
        buttonGroupTipo.add(rdbtnTexto);
        rdbtnTexto.setForeground(UIManager.getColor("CheckBoxMenuItem.background"));
        rdbtnTexto.setBackground(UIManager.getColor("CheckBoxMenuItem.acceleratorForeground"));
        rdbtnTexto.setBounds(107, 90, 88, 23);
        contentPanel.add(rdbtnTexto);
        
        rdbtnBinario = new JRadioButton("Binario");
        rdbtnBinario.setFont(new Font("Dialog", Font.PLAIN, 12));
        buttonGroupTipo.add(rdbtnBinario);
        rdbtnBinario.setForeground(UIManager.getColor("CheckBoxMenuItem.background"));
        rdbtnBinario.setBackground(UIManager.getColor("CheckBoxMenuItem.acceleratorForeground"));
        rdbtnBinario.setBounds(199, 90, 106, 23);
        contentPanel.add(rdbtnBinario);
        
        JLabel lblTipo = new JLabel("Tipo");
        lblTipo.setFont(new Font("Dialog", Font.PLAIN, 12));
        lblTipo.setForeground(UIManager.getColor("CheckBoxMenuItem.background"));
        lblTipo.setBounds(27, 94, 70, 15);
        contentPanel.add(lblTipo);
        
        chckbxExcluir = new JCheckBox("Excluir");
        chckbxExcluir.setFont(new Font("Dialog", Font.PLAIN, 12));
        chckbxExcluir.setForeground(UIManager.getColor("CheckBoxMenuItem.background"));
        chckbxExcluir.setBackground(UIManager.getColor("CheckBoxMenuItem.acceleratorForeground"));
        chckbxExcluir.setBounds(27, 63, 129, 23);
        contentPanel.add(chckbxExcluir);
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setBackground(UIManager.getColor("CheckBox.focus"));
            buttonPane.setLayout( new FlowLayout( FlowLayout.RIGHT ) );
            getContentPane().add( buttonPane, BorderLayout.SOUTH );
            {
                JButton okButton = new JButton( "OK" );
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        
                        DlgExtension.this.setVisible( false );
                        System.out.println( "OK" );
                    }
                });
                okButton.setActionCommand( "OK" );
                buttonPane.add( okButton );
                getRootPane().setDefaultButton( okButton );
            }
            {
                JButton cancelButton = new JButton( "Cancelar" );
                cancelButton.setFont(new Font("Dialog", Font.PLAIN, 12));
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        DlgExtension.this.setVisible( false );
                        System.out.println( "Cancelar" );
                    }
                });
                cancelButton.setActionCommand( "Cancelar" );
                buttonPane.add( cancelButton );
            }
        }
    }
    
    public String getTpExtension()
    {  
        if(rdbtnBinario.isSelected())
            return( "Binario" );
        return "Texto";
    }    
}
