package ModelInterface;

//import ModelInterface.ModelGUI2.FileChooserDemo;

import java.util.*;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;

import ModelInterface.ConfigurationEditor.configurationeditor.ConfigurationEditor;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Cursor;

public class InterfaceMain extends JFrame implements ActionListener {
	/**
	 * Unique identifier used for serializing.
	 */
	private static final long serialVersionUID = -9137748180688015902L;
	
	public static int FILE_MENU_POS = 0;
	public static int EDIT_MENU_POS = 1;
	public static int FILE_OPEN_SUBMENU_POS = 0;
	public static int FILE_SAVE_MENUITEM_POS = 2;
	public static int FILE_QUIT_MENUITEM_POS = 50;
	public static int EDIT_COPY_MENUITEM_POS = 10;
	public static int EDIT_PASTE_MENUITEM_POS = 11;
	
	/**
	 * Position of the "Configuration..." item in the Edit menu.
	 */
	private static int EDIT_CONFIGURATION_MENUITEM_POS = 12;
	
	public static String oldControl;
	private static InterfaceMain main;
	private JMenuItem saveMenu;
	private JMenuItem quitMenu;
	private JMenuItem copyMenu;
	private JMenuItem pasteMenu;
	/**
	 * Main function, creates a new thread for the gui and runs it.
	 */
	public static void main(String[] args) {

		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// warn the user.. should be ok to keep going
			System.out.println("Error setting look and feel: " + e);
		}
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});

	}

	/**
	 * Create a new instance of this class and makes it visible
	 */
	private static void createAndShowGUI() {
		main = null;
		main  = new InterfaceMain("Model Interface");
		//main.pack();
		main.setVisible(true);
	}

	private InterfaceMain(String title) {
		super(title);
		Container contentPane = getContentPane();

		contentPane.setLayout(new BorderLayout());

		oldControl = "ModelInterface";

		MenuManager menuMan = new MenuManager(null);

		JMenu m = new JMenu("File");
		menuMan.addMenuItem(m, FILE_MENU_POS);
		JMenu submenu;
		JMenuItem menuItem;

		submenu = new JMenu("Open");
		submenu.setMnemonic(KeyEvent.VK_S);
		menuMan.getSubMenuManager(FILE_MENU_POS).addMenuItem(submenu, FILE_OPEN_SUBMENU_POS);
		menuMan.getSubMenuManager(FILE_MENU_POS).addSeparator(FILE_OPEN_SUBMENU_POS);
		//m.add(submenu);
		//m.addSeparator();

		//m.add(makeMenuItem("Quit"));
		menuMan.getSubMenuManager(FILE_MENU_POS).addMenuItem(saveMenu = makeMenuItem("Save"), FILE_SAVE_MENUITEM_POS);
		menuMan.getSubMenuManager(FILE_MENU_POS).addSeparator(FILE_SAVE_MENUITEM_POS);
		saveMenu.setEnabled(false);
		menuMan.getSubMenuManager(FILE_MENU_POS).addMenuItem(quitMenu = makeMenuItem("Quit"), FILE_QUIT_MENUITEM_POS);

		menuMan.addMenuItem(new JMenu("Edit"), EDIT_MENU_POS);

		copyMenu = new JMenuItem("Copy");
		copyMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
				ActionEvent.CTRL_MASK));
		menuMan.getSubMenuManager(EDIT_MENU_POS).addMenuItem(copyMenu, EDIT_COPY_MENUITEM_POS);
		pasteMenu = new JMenuItem("Paste");
		pasteMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
				ActionEvent.CTRL_MASK));
		menuMan.getSubMenuManager(EDIT_MENU_POS).addMenuItem(pasteMenu, EDIT_PASTE_MENUITEM_POS);
		
		// Add a menu item to launch the configuration editor.
		final JMenuItem editConfMenuItem = new JMenuItem(
				"Configuration...");
		editConfMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
				ActionEvent.CTRL_MASK));
		
		// Add an action listener which will launch the configuration editor.
		// TODO: Using a UIAction here would save initialization time.
		editConfMenuItem.addActionListener(new ActionListener() {
			/**
			 * Method called when the menu item is clicked.
			 * @param aEvent The event received.
			 */
			public void actionPerformed(ActionEvent aEvent) {
				// Create and show the configuration editor.
				final ConfigurationEditor confEditor = new ConfigurationEditor();
				confEditor.pack();
				confEditor.setVisible(true);
			}
		});
		menuMan.getSubMenuManager(EDIT_MENU_POS).addMenuItem(editConfMenuItem,
				EDIT_CONFIGURATION_MENUITEM_POS);
		
		
		/*
		 * FileChooserDemo is being removed, but I will leave this here, This is
		 * how I envision the menuitems to be added and hopefully all the
		 * listeners would be set up correctly and we won't need to keep the
		 * pointer to the classes around FileChooserDemo fcd = new
		 * FileChooserDemo(this); fcd.addMenuItems(menuMan);
		 */

		copyMenu.setEnabled(false);
		pasteMenu.setEnabled(false);

		JMenuBar mb = menuMan.createMenu(); //new JMenuBar();
		//mb.add(m);

		setJMenuBar(mb);
		setSize(800/*windowWidth*/, 800/*windowHeight*/);

		// Add adapter to catch window closing event.
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
		});

		getGlassPane().addMouseListener( new MouseAdapter() {});
		getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private JMenuItem makeMenuItem(String title) {
		JMenuItem m = new JMenuItem(title);
		m.addActionListener(this);
		return m;
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("Quit")) {
			dispose();
		}
	}
	public static InterfaceMain getInstance() {
		return main;
	}
	public JMenuItem getSaveMenu() {
		return saveMenu;
	}
	public JMenuItem getQuitMenu() {
		return quitMenu;
	}
	public JMenuItem getCopyMenu() {
		return copyMenu;
	}
	public JMenuItem getPasteMenu() {
		return pasteMenu;
	}
	public class MenuManager {
		private JMenuItem menuValue;
		private Map subItems;
		private SortedSet sepList;
		MenuManager(JMenuItem menuValue) {
			this.menuValue = menuValue;
			sepList = null;
			if(menuValue == null || menuValue instanceof JMenu) {
				subItems = new TreeMap();
			} else {
				subItems = null;
			}
		}
		/*
		public JMenuItem getMenuValue() {
			return menuValue;
		}
		public Map getSubItems() {
			return subItems;
		}
		*/
		public void addSeparator(int where) {
			if(sepList == null) {
				sepList = new TreeSet();
			}
			sepList.add(where);
		}
		public int addMenuItem(JMenuItem menu, int where) {
			if(subItems.containsKey(where)) {
				return addMenuItem(menu, where+1);
			} else {
				subItems.put(where, new MenuManager(menu));
				return where;
			}
		}
		public MenuManager getSubMenuManager(int where) {
			if(!subItems.containsKey(where)) {
				// throw exception or just return null?
				return null;
			}
			return ((MenuManager)subItems.get(where));
		}
		JMenuBar createMenu() {
			JMenuBar ret = new JMenuBar();
			Object[] keys = subItems.keySet().toArray();
			for(int i = 0; i < keys.length; ++i) {
				ret.add(((MenuManager)subItems.get(keys[i])).createSubMenu());
			}
			return ret;
		}
		private JMenuItem createSubMenu() {
			if(subItems == null) {
				return menuValue;
			} else {
				Object[] keys = subItems.keySet().toArray();
				for(int i = 0; i < keys.length; ++i) {
					if(sepList != null && !sepList.isEmpty() && 
							((Integer)keys[i]).intValue() > ((Integer)sepList.first()).intValue()) {
						((JMenu)menuValue).addSeparator();
						sepList.remove(sepList.first());
					}
					menuValue.add(((MenuManager)subItems.get(keys[i])).createSubMenu());
				}
				return menuValue;
			}
		}
	}
}