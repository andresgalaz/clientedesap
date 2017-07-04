package prg.glz.cli.frm;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import prg.glz.FrameworkException;
import prg.glz.cli.Principal;
import prg.glz.cli.config.Constante;
import prg.glz.cli.config.Parametro;
import prg.glz.cli.sync.Sincroniza;
import prg.glz.cli.sync.WatchDir;
import prg.glz.cli.ws.LoginFrwk;

@SuppressWarnings("serial")
public class PnParams extends JPanel {
	private static final int ESTADO_DESCONECTADO = 1;
	private static final int ESTADO_CONECTADO = 2;
	private static final int ESTADO_CORRIENDO = 4;

	private static final int TOP = 10;
	private static final int LEFT = 10;
	private static final int SEPARADOR_X = 10;
	private static final int LABEL_WIDTH = 100;
	private static final int SIZE_LINE = 26;
	private static final int SEPARADOR_Y = 6;

	// Objetos accesibles del panel
	private int nEstado = ESTADO_DESCONECTADO;
	public static FrmPrincipal frmPrincipal;
	private DlgOpSync dlgOpcionSync;
	private JTextField txServer;
	private JTextField txUsuario;
	private JLabel lbUsuarioNombre;
	private JPasswordField txPassword;
	private JCheckBox chPasswordSave;
	private JTextField txDir;
	private JFileChooser fcDir;
	private JButton buConectar;
	private JButton buSyncUpdate;
	private JButton buSyncCommit;
	private JButton buComenzar;
	private JButton buDetener;
	private JButton buSelecc;
	private JButton buDesconectar;
	private JButton buIconizar;

	// Busca cambios de archivos y dispara sincronizador
	private WatchDir watchDir;

	public PnParams(FrmPrincipal frmPadre) {
		PnParams.frmPrincipal = frmPadre;
		init();
		// Se utiliza para interactuar al momento de sincronizar archivos
		this.dlgOpcionSync = new DlgOpSync(frmPadre, "Sincroniza Archivos", true);
		this.setEstado(ESTADO_DESCONECTADO);
	}

	private void init() {
		JLabel lb;
		int nPosY = TOP;
		this.setBackground(Constante.COLOR_FONDO);
		// Layout Absoluto
		this.setLayout(null);
		{
			// Servidor
			int nPosX = LEFT;

			lb = new JLabel("Servidor");
			lb.setBounds(nPosX, nPosY, LABEL_WIDTH, SIZE_LINE);
			lb.setForeground(Constante.COLOR_LABEL);
			this.add(lb);
			nPosX += lb.getWidth() + SEPARADOR_X;

			String cServer = Parametro.getServidor();
			txServer = new JTextField(cServer != null ? cServer : "http://<servidor>:<port>/webDesap...");
			txServer.setBackground(Constante.COLOR_FONDO_TEXTO);
			;
			txServer.setBounds(nPosX, nPosY, 400, SIZE_LINE);
			this.add(txServer);
		}
		nPosY += SIZE_LINE + SEPARADOR_Y;
		{
			// Usuario
			int nPosX = LEFT;

			lb = new JLabel("Usuario");
			lb.setBounds(nPosX, nPosY, LABEL_WIDTH, SIZE_LINE);
			lb.setForeground(Constante.COLOR_LABEL);
			this.add(lb);
			nPosX += lb.getWidth() + SEPARADOR_X;

			String cUsuario = Parametro.getUsuario();
			txUsuario = new JTextField(cUsuario);
			txUsuario.setBackground(Constante.COLOR_FONDO_TEXTO);
			;
			txUsuario.setBounds(nPosX, nPosY, 200, SIZE_LINE);
			this.add(txUsuario);
			nPosX += txUsuario.getWidth() + SEPARADOR_X;

			lbUsuarioNombre = new JLabel("No conectado");
			lbUsuarioNombre.setForeground(Constante.COLOR_LABEL);
			lbUsuarioNombre.setBounds(nPosX, nPosY, 200, SIZE_LINE);
			this.add(lbUsuarioNombre);
		}
		nPosY += SIZE_LINE + SEPARADOR_Y;
		{
			// Password
			int nPosX = LEFT;
			lb = new JLabel("Password");
			lb.setForeground(Constante.COLOR_LABEL);
			lb.setBounds(nPosX, nPosY, LABEL_WIDTH, SIZE_LINE);
			this.add(lb);
			nPosX += lb.getWidth() + SEPARADOR_X;

			String cPassword = Parametro.getPassword();
			txPassword = new JPasswordField(cPassword);
			txPassword.setBounds(nPosX, nPosY, 200, SIZE_LINE);
			this.add(txPassword);
			nPosX += txPassword.getWidth() + SEPARADOR_X;

			String cPasswordSave = Parametro.getPasswordSave();
			chPasswordSave = new JCheckBox("Graba password");
			chPasswordSave.setForeground(Constante.COLOR_LABEL);
			chPasswordSave.setBounds(nPosX, nPosY, 190, SIZE_LINE);
			chPasswordSave.setSelected("1".equals(cPasswordSave));
			chPasswordSave.setBackground(new Color(98, 175, 244));
			this.add(chPasswordSave);
			nPosX += chPasswordSave.getWidth() + SEPARADOR_X;

			// Botón Conecta
			buConectar = new JButton("Conectar");
			buConectar.setBounds(nPosX, nPosY, 120, SIZE_LINE);
			buConectar.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						lbUsuarioNombre.setText("No conectado");

						// Conecta y pone el usuario como global
						LoginFrwk login = new LoginFrwk(txServer.getText());
						Principal.usuario = login.login(txUsuario.getText(), new String(txPassword.getPassword()));
						lbUsuarioNombre.setText(Principal.usuario.getcNombre());
						setEstado(ESTADO_CONECTADO);

						// Graba parámetros
						ejecGrabaParams();

						if (!login.isConnected())
							JOptionPane.showMessageDialog(PnParams.frmPrincipal, "La sesión está cerrada");

					} catch (FrameworkException e) {
						JOptionPane.showMessageDialog(PnParams.frmPrincipal, e.getMessage());
					} catch (Exception e) {
						JOptionPane.showMessageDialog(PnParams.frmPrincipal, "Error inesperado\n" + e.getMessage());
					}
					// Habilita componentes seg{un estado
				}
			});
			this.add(buConectar);
		}
		nPosY += SIZE_LINE * 2 + SEPARADOR_Y;
		{
			// Directorio a sincronizar
			int nPosX = LEFT;

			lb = new JLabel("Directorio");
			lb.setForeground(Constante.COLOR_LABEL);
			lb.setBounds(nPosX, nPosY, LABEL_WIDTH, SIZE_LINE);
			this.add(lb);
			nPosX += lb.getWidth() + SEPARADOR_X;

			txDir = new JTextField(Parametro.getDir());
			txDir.setBackground(Constante.COLOR_FONDO_TEXTO);
			;
			txDir.setBounds(nPosX, nPosY, 400, SIZE_LINE);
			this.add(txDir);
			nPosX += txDir.getWidth() + SEPARADOR_X;

			buSelecc = new JButton("Seleccionar");
			buSelecc.setBounds(nPosX, nPosY, 120, SIZE_LINE);
			buSelecc.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					// Si el objeto no ha sido creado lo instancia
					if (fcDir == null) {
						fcDir = new JFileChooser();
						fcDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					}
					try {
						File dirActual = new File(txDir.getText());
						if (dirActual.isDirectory())
							fcDir.setCurrentDirectory(dirActual);
					} catch (Exception e) {
					}
					// Si seleccionó
					if (fcDir.showOpenDialog(PnParams.frmPrincipal) == 0) {
						txDir.setText(fcDir.getSelectedFile().getPath());
						ejecGrabaParams();
					}
				}
			});
			this.add(buSelecc);
		}
		nPosY += SIZE_LINE + SEPARADOR_Y;
		{
			// Sincroniza
			int nPosX = LEFT;

			lb = new JLabel("Sincroniza");
			lb.setForeground(Constante.COLOR_LABEL);
			lb.setBounds(nPosX, nPosY, LABEL_WIDTH, SIZE_LINE);
			this.add(lb);
			nPosX += lb.getWidth() + SEPARADOR_X;

			// Botón sincronizar COMMIT
			buSyncCommit = new JButton("Commit");
			buSyncCommit.setBounds(nPosX, nPosY, 120, SIZE_LINE);
			buSyncCommit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					ejecSyncCommit();
				}
			});
			this.add(buSyncCommit);
			nPosX += buSyncCommit.getWidth() + SEPARADOR_X;

			// Botón sincronizar UPDATE
			buSyncUpdate = new JButton("Update");
			buSyncUpdate.setBounds(nPosX, nPosY, 120, SIZE_LINE);
			buSyncUpdate.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					ejecSyncUpdate();
				}
			});
			this.add(buSyncUpdate);

		}
		nPosY += SIZE_LINE + SEPARADOR_Y;
		{
			// Arranca / Detiene Servicio
			int nPosX = LEFT;

			lb = new JLabel("Servicio");
			lb.setForeground(Constante.COLOR_LABEL);
			lb.setBounds(nPosX, nPosY, LABEL_WIDTH, SIZE_LINE);
			this.add(lb);
			nPosX += lb.getWidth() + SEPARADOR_X;

			buComenzar = new JButton("Arrancar");
			buComenzar.setBounds(nPosX, nPosY, 120, SIZE_LINE);
			buComenzar.setForeground(new Color(20, 150, 40));
			buComenzar.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					setEstado(ESTADO_CORRIENDO);
					ejecCorre();
				}
			});
			this.add(buComenzar);
			nPosX += buComenzar.getWidth() + SEPARADOR_X;

			// Botón detener el proceso
			buDetener = new JButton("Detener");
			buDetener.setForeground(Color.RED);
			buDetener.setBounds(nPosX, nPosY, 120, SIZE_LINE);
			buDetener.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					setEstado(PnParams.ESTADO_CONECTADO);
					ejecDetiene();
				}
			});
			this.add(buDetener);
			nPosX += buDetener.getWidth() + SEPARADOR_X;

			// Botón Desconecta
			buDesconectar = new JButton("Desconectar");
			// Va en la misma columna que el bontón conectar
			buDesconectar.setBounds(buConectar.getBounds().x, nPosY, 120, SIZE_LINE);
			buDesconectar.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						ejecDetiene();
					} catch (Exception e) {
					}
					lbUsuarioNombre.setText("No conectado");
					Principal.usuario = null;
					setEstado(ESTADO_DESCONECTADO);
				}
			});
			this.add(buDesconectar);

		}
		nPosY += SIZE_LINE + SEPARADOR_Y;
		{
			// Botón Desconecta
			buIconizar = new JButton("Iconizar");
			// Va en la misma columna que el bontón conectar
			buIconizar.setBounds(buConectar.getBounds().x, nPosY, 120, SIZE_LINE);
			buIconizar.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					frmPrincipal.iconizar();
				}
			});
			this.add(buIconizar);
		}
	}

	private void ejecSyncCommit() {
		frmPrincipal.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		frmPrincipal.setEnabled(false);
		final Sincroniza frmWork;
		try {
			frmWork = new Sincroniza(txServer.getText(), txDir.getText(), this.dlgOpcionSync);
			int nFiles = frmWork.syncCommit();
			JOptionPane.showMessageDialog(PnParams.frmPrincipal, "Se procesaron " + nFiles + " archivos");
		} catch (FrameworkException | SQLException e) {
			JOptionPane.showMessageDialog(PnParams.frmPrincipal,
					"Problemas al traer formularios desde el Servidor WEB\n" + e.getMessage());
		} finally {
			frmPrincipal.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			frmPrincipal.setEnabled(true);
		}
	}

	private void ejecSyncUpdate() {
		frmPrincipal.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		frmPrincipal.setEnabled(false);
		final Sincroniza frmWork;
		try {
			frmWork = new Sincroniza(txServer.getText(), txDir.getText(), this.dlgOpcionSync);
			int nFiles = frmWork.syncUpdate();
			JOptionPane.showMessageDialog(PnParams.frmPrincipal, "Se procesaron " + nFiles + " archivos");
		} catch (FrameworkException | SQLException e) {
			JOptionPane.showMessageDialog(PnParams.frmPrincipal,
					"Problemas al traer formularios desde el Servidor WEB\n" + e.getMessage());
		} finally {
			frmPrincipal.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			frmPrincipal.setEnabled(true);
		}
	}

	private void ejecGrabaParams() {
		Parametro.setServidor(txServer.getText());
		Parametro.setDir(txDir.getText());
		Parametro.setUsuario(txUsuario.getText());
		Parametro.setPasswordSave(chPasswordSave.isSelected() ? "1" : "0");
		if (chPasswordSave.isSelected())
			Parametro.setPassword(new String(txPassword.getPassword()));
		else
			Parametro.setPassword(null);
		try {
			new Parametro().graba();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(PnParams.frmPrincipal, "No se pudo grabar parámetros\n" + e.getMessage());
			return;
		}
	}

	private void ejecCorre() {
		// Crea clase de comunicación con el servidor WEB
		Sincroniza frmWork;
		try {
			frmWork = new Sincroniza(txServer.getText(), txDir.getText(), this.dlgOpcionSync);
		} catch (FrameworkException e) {
			JOptionPane.showMessageDialog(PnParams.frmPrincipal,
					"Problemas al traer formularios desde el Servidor WEB\n" + e.getMessage());
			setEstado(ESTADO_CONECTADO);
			return;
		}
		// Corre sincronizador de archivos. Registra el directorio y comienza el
		// proceso
		Path dir = Paths.get(txDir.getText());
		try {
			this.watchDir = new WatchDir(dir, frmWork);

			// La clase WatchDir es Runnable para poder utilizarla dentro de un
			// Hilo (ver método run de WatchDir)
			new Thread(this.watchDir).start();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(PnParams.frmPrincipal,
					"No se pudo arrancar el servicio de sincronización\n" + e.getMessage());
			return;
		}
	}

	private void ejecDetiene() {
		this.watchDir.detener();
		this.setEstado(ESTADO_CONECTADO);
	}

	private void setEstado(int n) {
		this.nEstado = n;
		txServer.setEnabled((this.nEstado & ESTADO_DESCONECTADO) > 0);
		txUsuario.setEnabled((this.nEstado & ESTADO_DESCONECTADO) > 0);
		txPassword.setEnabled((this.nEstado & ESTADO_DESCONECTADO) > 0);
		chPasswordSave.setEnabled((this.nEstado & ESTADO_DESCONECTADO) > 0);
		buSelecc.setEnabled((this.nEstado & ESTADO_DESCONECTADO) > 0);
		buConectar.setEnabled((this.nEstado & ESTADO_DESCONECTADO) > 0);

		txDir.setEnabled((this.nEstado & ESTADO_CONECTADO) > 0);
		buSelecc.setEnabled((this.nEstado & ESTADO_CONECTADO) > 0);
		buSyncCommit.setEnabled((this.nEstado & ESTADO_CONECTADO) > 0);
		buSyncUpdate.setEnabled((this.nEstado & ESTADO_CONECTADO) > 0);

		buComenzar.setEnabled((this.nEstado & (ESTADO_CONECTADO)) > 0);
		buDetener.setEnabled((this.nEstado & (ESTADO_CORRIENDO)) > 0);
		buDesconectar.setEnabled((this.nEstado & (ESTADO_CONECTADO | ESTADO_CORRIENDO)) > 0);
		buIconizar.setEnabled((this.nEstado & ESTADO_CORRIENDO) > 0);
	}
}
