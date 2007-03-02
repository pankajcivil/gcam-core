package ModelInterface.ModelGUI2;

import ModelInterface.ConfigurationEditor.guihelpers.XMLFileFilter;
import ModelInterface.ConfigurationEditor.utils.FileUtils;
import ModelInterface.ModelGUI2.tables.BaseTableModel;
import ModelInterface.ModelGUI2.tables.ComboTableModel;
import ModelInterface.ModelGUI2.tables.MultiTableModel;
import ModelInterface.ModelGUI2.tables.CopyPaste;
import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;
import ModelInterface.MenuAdder;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreePath;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import org.jfree.report.JFreeReport;
/*
import org.w3c.dom.ls.*;
import org.w3c.dom.bootstrap.*;
*/
import org.apache.xpath.domapi.XPathEvaluatorImpl;
import org.jfree.chart.JFreeChart;
import org.w3c.dom.xpath.*;

import org.jfree.report.Group;
import org.jfree.report.modules.gui.base.ExportPluginFactory;
import org.jfree.report.JFreeReportBoot;
import org.jfree.report.ElementAlignment;
import org.jfree.report.ReportProcessingException;
import org.jfree.report.modules.gui.base.PreviewDialog;
//import org.jfree.report.elementfactory.TextFieldElementFactory;
import org.jfree.report.elementfactory.DrawableFieldElementFactory;
import org.jfree.ui.FloatDimension;

import org.apache.poi.hssf.usermodel.*;

import com.sleepycat.dbxml.*;

import ModelInterface.InterfaceMain;

public class DbViewer implements ActionListener, MenuAdder {
	private JFrame parentFrame;

	private Document queriesDoc;

	public static XMLDB xmlDB;

	private static String controlStr = "DbViewer";

	private JTable jTable; // does this still need to be a field?
		
	private DOMImplementationLS implls;


	protected Vector scns;
	protected JList scnList;
	protected JList regionList;
	protected Vector regions;
	protected BaseTableModel bt; // does this still need to be a field?
	protected JScrollPane jsp; // does this still need to be a field?
	protected QueryTreeModel queries;
	private JTabbedPane tablesTabs = new JTabbedPane();
	private JSplitPane scenarioRegionSplit;
	private JSplitPane queriesSplit;
	private JSplitPane tableCreatorSplit;

	public DbViewer(JFrame pf) {
		parentFrame = pf;
		parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(evt.getPropertyName().equals("Control")) {
					if(evt.getOldValue().equals(controlStr) || evt.getOldValue().equals(controlStr+"Same")) {
						xmlDB.closeDB();
						xmlDB = null;
						try {
							if(queries.hasChanges() && JOptionPane.showConfirmDialog(
									parentFrame, 
									"The Queries have been modified.  Do You want to save them?",
									"Confirm Save Queries", JOptionPane.YES_NO_OPTION,
									JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
								Document tempDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
									.getDOMImplementation().createDocument(null, "queries", null);
								queries.getAsNode(tempDoc);
								//writeDocument(tempDoc, queryFile);
								writeFile(new File(((InterfaceMain)parentFrame).getProperties().getProperty("queryFile"))
									, tempDoc);
							}
						} catch(Exception e) {
							e.printStackTrace();
						}
						Properties prop = ((InterfaceMain)parentFrame).getProperties();
						prop.setProperty("scenarioRegionSplit", String.valueOf(scenarioRegionSplit.getDividerLocation()));
						prop.setProperty("queriesSplit", String.valueOf(queriesSplit.getDividerLocation()));
						prop.setProperty("tableCreatorSplit", String.valueOf(tableCreatorSplit.getDividerLocation()));
						((InterfaceMain)parentFrame).getUndoManager().discardAllEdits();
						((InterfaceMain)parentFrame).refreshUndoRedo();
						parentFrame.getContentPane().removeAll();
					}
					if(evt.getNewValue().equals(controlStr)) {
						String queryFileName;
						Properties prop = ((InterfaceMain)parentFrame).getProperties();
						// I should probably stop being lazy
						prop.setProperty("queryFile", queryFileName = prop.getProperty("queryFile", "queries.xml"));
						// TODO: move to load preferences
						scenarioRegionSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
						scenarioRegionSplit.setResizeWeight(.5);
						queriesSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
						//queriesSplit.setLeftComponent(scenarioRegionSplit);
						queriesSplit.setResizeWeight(.5);
						tableCreatorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false);
						String tempInt;
						try {
							if((tempInt = prop.getProperty("scenarioRegionSplit")) != null) {
								scenarioRegionSplit.setDividerLocation(Integer.valueOf(tempInt));
							}
							if((tempInt = prop.getProperty("queriesSplit")) != null) {
								queriesSplit.setDividerLocation(Integer.valueOf(tempInt));
							}
							if((tempInt = prop.getProperty("tableCreatorSplit")) != null) {
								tableCreatorSplit.setDividerLocation(Integer.valueOf(tempInt));
							}
						} catch(NumberFormatException nfe) {
							System.out.println("Invalid split location preference: "+nfe);
						}
						queriesDoc = readQueries(new File(queryFileName));
					}
				}
			}
		});



		try {
			System.setProperty(DOMImplementationRegistry.PROPERTY,
					"com.sun.org.apache.xerces.internal.dom.DOMImplementationSourceImpl");
					//"org.apache.xerces.dom.DOMImplementationSourceImpl");
			DOMImplementationRegistry reg = DOMImplementationRegistry
					.newInstance();
			implls = (DOMImplementationLS)reg.getDOMImplementation("XML 3.0");
			if (implls == null) {
				System.out.println("Could not find a DOM3 Load-Save compliant parser.");
				JOptionPane.showMessageDialog(parentFrame,
						"Could not find a DOM3 Load-Save compliant parser.",
						"Initialization Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		} catch (Exception e) {
			System.err.println("Couldn't initialize DOMImplementation: " + e);
			JOptionPane.showMessageDialog(parentFrame,
					"Couldn't initialize DOMImplementation\n" + e,
					"Initialization Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	private JMenuItem makeMenuItem(String title) {
		JMenuItem ret = new JMenuItem(title);
		ret.addActionListener(this);
		return ret;
	}

	public void addMenuItems(InterfaceMain.MenuManager menuMan) {
		JMenuItem menuItem = new JMenuItem("DB Open");
		menuItem.addActionListener(this);
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).
			getSubMenuManager(InterfaceMain.FILE_OPEN_SUBMENU_POS).addMenuItem(menuItem, 30);

		final JMenuItem menuManage = makeMenuItem("Manage DB");
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addMenuItem(menuManage, 10);
		menuManage.setEnabled(false);
		final JMenuItem menuBatch = makeMenuItem("Batch Query");
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addMenuItem(menuBatch, 11);
		menuBatch.setEnabled(false);
		parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(evt.getPropertyName().equals("Control")) {
					if(evt.getOldValue().equals(controlStr) || 
						evt.getOldValue().equals(controlStr+"Same")) {
						menuManage.setEnabled(false);
						menuBatch.setEnabled(false);
					} 
					if(evt.getNewValue().equals(controlStr)) {
						menuManage.setEnabled(true);
						menuBatch.setEnabled(true);
					}
				}
			}
		});
		final JMenuItem menuExpPrn = makeMenuItem("Export / Print");
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addMenuItem(menuExpPrn,  20);
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addSeparator(20);
		menuExpPrn.setEnabled(false);
		parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
			private int numQueries = 0;
			public void propertyChange(PropertyChangeEvent evt) {
				if(evt.getPropertyName().equals("Control")) {
					if(evt.getOldValue().equals(controlStr) || 
						evt.getOldValue().equals(controlStr+"Same")) {
						menuExpPrn.setEnabled(false);
					}
				} else if(evt.getPropertyName().equals("Query") && evt.getOldValue() == null) {
					menuExpPrn.setEnabled(true);
					++numQueries;
				} else if(evt.getPropertyName().equals("Query") && evt.getNewValue() == null) {
					if(--numQueries == 0) {
						menuExpPrn.setEnabled(false);
					}
				}
			}
		});
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("DB Open")) {
			final FileFilter dbFilter = (new javax.swing.filechooser.FileFilter() {
				public boolean accept(File f) {
					return f.getName().toLowerCase().endsWith(".dbxml") || f.isDirectory();
				}
				public String getDescription() {
					return "BDB XML Container (*.dbxml)";
				}
			});
			FileChooser fc = FileChooserFactory.getFileChooser();
			// Now open chooser
			File[] dbFiles = fc.doFilePrompt(parentFrame, "Choose XML Database", FileChooser.LOAD_DIALOG, 
					new File(((InterfaceMain)parentFrame).getProperties().getProperty("lastDirectory", ".")),
					dbFilter);

			if(dbFiles != null) {
				((InterfaceMain)parentFrame).fireControlChange(controlStr);
				doOpenDB(dbFiles[0]);
			}
		} else if(e.getActionCommand().equals("Manage DB")) {
			manageDB();
		} else if(e.getActionCommand().equals("Batch Query")) {
			FileChooser fc = FileChooserFactory.getFileChooser();
			// Now open chooser
			final FileFilter xmlFilter = new XMLFilter();
			File[] batchFiles = fc.doFilePrompt(parentFrame, "Open bath Query File", FileChooser.LOAD_DIALOG, 
					new File(((InterfaceMain)parentFrame).getProperties().getProperty("lastDirectory", ".")),
					xmlFilter);

			if(batchFiles == null) {
				return;
			} else {
				((InterfaceMain)parentFrame).getProperties().setProperty("lastDirectory", batchFiles[0].getParent());

				final FileFilter xlsFilter = (new javax.swing.filechooser.FileFilter() {
					public boolean accept(File f) {
						return f.getName().toLowerCase().endsWith(".xls") || f.isDirectory();
					}
					public String getDescription() {
						return "Microsoft Excel File(*.xls)";
					}
				});
				File[] xlsFiles = fc.doFilePrompt(parentFrame, "Select Where to Save Output", FileChooser.SAVE_DIALOG, 
						new File(((InterfaceMain)parentFrame).getProperties().getProperty("lastDirectory", ".")),
						xlsFilter);
				if(xlsFiles == null) {
					return;
				} else {
					((InterfaceMain)parentFrame).getProperties().setProperty("lastDirectory", xlsFiles[0].getParent());
					batchQuery(batchFiles[0], xlsFiles[0]);
				}
			}
		} else if(e.getActionCommand().equals("Export / Print")) {
			createReport();
		}
	}

	private void doOpenDB(File dbFile) {
		((InterfaceMain)parentFrame).getProperties().setProperty("lastDirectory", dbFile.getParent());
		xmlDB = new XMLDB(dbFile.getAbsolutePath(), parentFrame);
		createTableSelector();
		parentFrame.setTitle("["+dbFile+"] - ModelInterface");
	}

	private Vector getScenarios() {
		XmlValue temp;
		Vector ret = new Vector();
		try {
			XmlResults res = xmlDB.createQuery("/scenario", null, null);
			while(res.hasNext()) {
				temp = res.next();
				XmlDocument tempDoc = temp.asDocument();
				//ret.add(tempDoc.getName().replace(' ', '_')+" "+XMLDB.getAttr(temp, "name")+ " "+XMLDB.getAttr(temp, "date"));
				ret.add(new ScenarioListItem(tempDoc.getName(), XMLDB.getAttr(temp, "name"), XMLDB.getAttr(temp, "date")));
				tempDoc.delete();
				temp.delete();
			}
			res.delete();
		} catch(XmlException e) {
			e.printStackTrace();
		}
		xmlDB.printLockStats("getScenarios");
		return ret;
	}

	// A simple class to hold scenario info that will be displayed in a JList
	// don't want to display the document name becuase it adds too much clutter
	// do I just ovrride the toString to only display what we want, and I will
	// still have the docName for use when managing the database.
	// I am leaveing the fields open to direct access as this is merely a struct
	// and there is no point in create getter/setters
	private class ScenarioListItem {
		String docName;
		String scnName;
		String scnDate;
		public ScenarioListItem(String docName, String scnName, String scnDate) {
			this.docName = docName;
			this.scnName = scnName;
			this.scnDate = scnDate;
		}
		public String toString() {
			// do not display docName to avoid clutter
			return scnName+' '+scnDate;
		}
	}

	protected Vector getRegions() {
		Vector funcTemp = new Vector<String>(1,0);
		funcTemp.add("distinct-values");
		Vector ret = new Vector();
		try {
			XmlResults res = xmlDB.createQuery("/scenario/world/"+
					ModelInterface.ModelGUI2.queries.QueryBuilder.regionQueryPortion+"/@name", null, funcTemp);
			while(res.hasNext()) {
				ret.add(res.next().asString());
			}
			res.delete();
		} catch(XmlException e) {
			e.printStackTrace();
		}
		ret.add("Global");
		funcTemp = null;
		xmlDB.printLockStats("getRegions");
		return ret;
	}

	protected QueryTreeModel getQueries() {
		Vector ret = new Vector();
		XPathEvaluatorImpl xpeImpl = new XPathEvaluatorImpl(queriesDoc);
		XPathResult res = (XPathResult)xpeImpl.createExpression("/queries", xpeImpl.createNSResolver(queriesDoc.getDocumentElement())).evaluate(queriesDoc.getDocumentElement(), XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
		return new QueryTreeModel(res.iterateNext());
	}

	protected String createFilteredQuery(Vector scns, int[] scnSel/*, Vector regions, int[]regionSel*/) {
		StringBuffer ret = new StringBuffer("/");
		boolean added = false;
		for(int i = 0; i < scnSel.length; ++i) {
			//String[] attrs = ((String)scns.get(scnSel[i])).split("\\s");
			ScenarioListItem temp = (ScenarioListItem)scns.get(scnSel[i]);
			if(!added) {
				ret.append("scenario[ ");
				added = true;
			} else {
				ret.append(" or ");
			}
			//ret.append("(@name='").append(attrs[1]).append("' and @date='").append(attrs[2]).append("')");
			ret.append("(@name='").append(temp.scnName).append("' and @date='").append(temp.scnDate).append("')");
		}
		ret.append(" ]/world/");
		System.out.println(ret);
		return ret.toString();
		//xmlDB.setQueryFilter(ret.toString());
	}

	protected void createTableSelector() {
		JPanel listPane = new JPanel();
		JLabel listLabel;
		JPanel allLists = new JPanel();
		//final JSplitPane all = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		scns = getScenarios();
		regions = getRegions();
		queries = getQueries();
		scnList = new JList(scns);
		regionList = new JList(regions);
		if(scns.size() != 0) {
			scnList.setSelectedIndex(scns.size()-1);
		}
		if(regions.size() != 0) {
			regionList.setSelectedIndex(0);
		}
		final JTree queryList = new JTree(queries);
		queryList.setTransferHandler(new QueryTransferHandler(queriesDoc, implls));
		queryList.setDragEnabled(true);
		queryList.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		queryList.setSelectionRow(0);
		for(int i = 0; i < queryList.getRowCount(); ++i) {
			queryList.expandRow(i);
		}
		/*
		final JSplitPane sp = new JSplitPane();
		sp.setLeftComponent(null);
		sp.setRightComponent(null);
		*/

		listPane.setLayout( new BoxLayout(listPane, BoxLayout.Y_AXIS));
		listLabel = new JLabel("Scenario");
		listPane.add(listLabel);
		JScrollPane listScroll = new JScrollPane(scnList);
		listScroll.setPreferredSize(new Dimension(150, 150));
		listPane.add(listScroll);

		allLists.setLayout( new BoxLayout(allLists, BoxLayout.X_AXIS));
		allLists.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		scenarioRegionSplit.setLeftComponent(listPane);
		//allLists.add(listPane);
		//allLists.add(Box.createHorizontalStrut(10));

		listPane = new JPanel();
		listPane.setLayout( new BoxLayout(listPane, BoxLayout.Y_AXIS));
		listLabel = new JLabel("Regions");
		listPane.add(listLabel);
		listScroll = new JScrollPane(regionList);
		listScroll.setPreferredSize(new Dimension(150, 150));
		listPane.add(listScroll);
		scenarioRegionSplit.setRightComponent(listPane);
		allLists.add(scenarioRegionSplit);
		//allLists.add(listPane);
		//allLists.add(Box.createHorizontalStrut(10));

		listPane = new JPanel();
		listPane.setLayout( new BoxLayout(listPane, BoxLayout.Y_AXIS));
		listLabel = new JLabel("Queries");
		listPane.add(listLabel);
		listScroll = new JScrollPane(queryList);
		listScroll.setPreferredSize(new Dimension(150, 100));
		listPane.add(listScroll);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(Box.createHorizontalGlue());
		JButton createButton = new JButton("Create");
		JButton removeButton = new JButton("Remove");
		final JButton runQueryButton = new JButton("Query");
		final JButton editButton = new JButton("Edit");
		editButton.setEnabled(false);
		runQueryButton.setEnabled(false);
		buttonPanel.add(createButton);
		buttonPanel.add(removeButton);
		buttonPanel.add(editButton);
		buttonPanel.add(runQueryButton);
		listPane.add(buttonPanel);

		queriesSplit.setLeftComponent(scenarioRegionSplit);
		queriesSplit.setRightComponent(listPane);
		allLists.add(queriesSplit);
		//allLists.add(listPane);
		//all.setLayout( new BoxLayout(all, BoxLayout.Y_AXIS));
		//all.add(allLists, BorderLayout.PAGE_START);
		tableCreatorSplit.setLeftComponent(allLists);
		/*
		final JPanel tablePanel = new JPanel();
		tablePanel.setLayout( new BoxLayout(tablePanel, BoxLayout.X_AXIS));
		*/
		//final JTabbedPane tablesTabs = new JTabbedPane();
		tableCreatorSplit.setRightComponent(tablesTabs);


		queryList.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				if(queries.isLeaf(e.getPath().getLastPathComponent())) {
					editButton.setEnabled(true);
					runQueryButton.setEnabled(true);
				} else {
					editButton.setEnabled(false);
					runQueryButton.setEnabled(false);
				}
			}
		});
		queries.addTreeModelListener(new TreeModelListener() {
			public void treeNodesInserted(TreeModelEvent e) {
				// right now this is the only one I care about
				// so that I can set selection after a node is
				// inserted
				TreePath pathWithNewChild = e.getTreePath().pathByAddingChild(e.getChildren()[0]);
				queryList.setSelectionPath(pathWithNewChild);
				queryList.scrollPathToVisible(pathWithNewChild);
			}
			public void treeNodesChanged(TreeModelEvent e) {
				// do nothing..
			}
			public void treeNodesRemoved(TreeModelEvent e) {
				// do nothing..
			}
			public void treeStructureChanged(TreeModelEvent e) {
				// do nothing..
			}
		});

		createButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(queryList.getSelectionCount() != 1) {
					JOptionPane.showMessageDialog(parentFrame, "Please select one Query or Query Group before creating", 
						"Create Query Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				QueryGenerator qg = new QueryGenerator(parentFrame); 
				if(qg.getXPath().equals("")) {
					return;
				} else if(qg.getXPath().equals("Query Group")) {
					queries.add(queryList.getSelectionPath(), qg.toString());
				} else {
					queries.add(queryList.getSelectionPath(), qg);
				}
				queryList.updateUI();
			}
		});
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(queryList.getSelectionCount() == 0) {
					JOptionPane.showMessageDialog(parentFrame, "Please select a Query or Query Group to Remove", 
						"Query Remove Error", JOptionPane.ERROR_MESSAGE);
				} else {
					TreePath[] selPaths = queryList.getSelectionPaths();
					for(int i = 0; i < selPaths.length; ++i) {
						queries.remove(selPaths[i]);
					}
					queryList.updateUI();
				}
			}
		});
		runQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] scnSel = scnList.getSelectedIndices();
				int[] regionSel = regionList.getSelectedIndices();
				if(scnSel.length == 0) {
					JOptionPane.showMessageDialog(parentFrame, "Please select Scenarios to run the query against", 
						"Run Query Error", JOptionPane.ERROR_MESSAGE);
				} else if(regionSel.length == 0) {
					JOptionPane.showMessageDialog(parentFrame, "Please select Regions to run the query against", 
						"Run Query Error", JOptionPane.ERROR_MESSAGE);
				} else if(queryList.getSelectionCount() == 0) {
					JOptionPane.showMessageDialog(parentFrame, "Please select a query to run", 
						"Run Query Error", JOptionPane.ERROR_MESSAGE);
				} else {
					String tempFilterQuery = createFilteredQuery(scns, scnSel);
					parentFrame.getGlassPane().setVisible(true);
					TreePath[] selPaths = queryList.getSelectionPaths();
					boolean movedTabAlready = false;
					for(int i = 0; i < selPaths.length; ++i) {
						try {
							QueryGenerator qg = (QueryGenerator)selPaths[i].getLastPathComponent();
							Container ret = null;
							if(qg.isGroup()) {
								ret = createGroupTableContent(qg, tempFilterQuery);
							} else {
								ret = createSingleTableContent(qg, tempFilterQuery);
							}
							if(ret != null) {
								tablesTabs.addTab(qg.toString(), new TabCloseIcon(), ret, 
										createCommentTooltip(selPaths[i]));
								if(!movedTabAlready) {
									tablesTabs.setSelectedIndex(tablesTabs.getTabCount()-1);
									movedTabAlready = true;
								}
								// fire this here, or after they are all done??
								((InterfaceMain)parentFrame).fireProperty("Query", null, bt);
							}
						} catch(ClassCastException cce) {
							System.out.println("Warning: Caught "+cce+" likely a QueryGroup was in the selection");
						}
					}
					parentFrame.getGlassPane().setVisible(false);
					// need old value/new value?
					// fire off property or something we did query
				}
			}
		});

		editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(queryList.getSelectionCount() == 0) {
					JOptionPane.showMessageDialog(parentFrame, "Please select a query to edit", 
						"Edit Query Error", JOptionPane.ERROR_MESSAGE);
				} else {
					TreePath[] selPaths = queryList.getSelectionPaths();
					for(int i = 0; i < selPaths.length; ++i) {
						//QueryGenerator tempQG = (QueryGenerator)selPaths[i].getLastPathComponent();
						//tempQG.editDialog();
						queries.doEdit(selPaths[i]);
					}
				}
			}
		});

		Container contentPane = parentFrame.getContentPane();
		contentPane.add(tableCreatorSplit/*, BorderLayout.PAGE_START*/);
		//contentPane.add(new JScrollPane(all), BorderLayout.PAGE_START);
		parentFrame.setVisible(true);
	}

	private Container createGroupTableContent(QueryGenerator qg, String tempFilterQuery) {
		BaseTableModel btBefore = bt;
		try {
			bt = new MultiTableModel(qg, tempFilterQuery, regionList.getSelectedValues(), parentFrame);
		} catch(NullPointerException e) {
			System.out.println("Warning null pointer while creating MultiTableModel");
			System.out.println("Likely the query didn't get any results");
			bt = btBefore;
			return null;
		}
		btBefore = null;
		jTable = new JTable(bt);
		jTable.setCellSelectionEnabled(true);
		jTable.getColumnModel().getColumn(0).setCellRenderer(((MultiTableModel)bt).getCellRenderer(0,0));
		jTable.getColumnModel().getColumn(0).setCellEditor(((MultiTableModel)bt).getCellEditor(0,0));
		jsp = new JScrollPane(jTable);
		return jsp;
	}

	private Container createSingleTableContent(QueryGenerator qg, String tempFilterQuery) {
		BaseTableModel btBefore = bt;
		try {
			bt = new ComboTableModel(qg, tempFilterQuery, regionList.getSelectedValues(), parentFrame);
		} catch(NullPointerException e) {
			System.out.println("Warning null pointer while creating ComboTableModel");
			System.out.println("Likely the query didn't get any results");
			bt = btBefore;
			return null;
		}
		btBefore = null;
		//TableSorter sorter = new TableSorter(bt);
		jTable = new JTable(bt);
		new CopyPaste(jTable);
		//sorter.setTableHeader(jTable.getTableHeader());

		jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		jTable.setCellSelectionEnabled(true);

		javax.swing.table.TableColumn col;
		int j = 0;
		while(j < jTable.getColumnCount()) {
			col = jTable.getColumnModel().getColumn(j);
			if(jTable.getColumnName(j).equals("")) {
				col.setPreferredWidth(75);
			} else {
				col.setPreferredWidth(jTable.getColumnName(j).length()*5+30);
			}
			j++;
		}
		JLabel labelChart = new JLabel();
		try {
			JFreeChart chart = bt.createChart(0,0);
			Dimension chartDim = bt.getChartDimensions(chart);
			BufferedImage chartImage = chart.createBufferedImage(
					(int)chartDim.getWidth(), (int)chartDim.getHeight());
			/*
			BufferedImage chartImage = chart.createBufferedImage(
					350, 350);
					*/

			labelChart.setIcon(new ImageIcon(chartImage));
		} catch(Exception e) {
			e.printStackTrace();
			labelChart.setText("Cannot Create Chart");
		}
		JSplitPane sp = new JSplitPane();
		sp.setLeftComponent(new JScrollPane(jTable));
		sp.setRightComponent(labelChart);
		sp.setDividerLocation(parentFrame.getWidth()-350-15);
		//return sp;
		return jsp = new JScrollPane(sp);
	}
	
	/**
	 * A class which represents a dirty bit.
	 * @author Josh Lurz
	 *
	 */
	private class DirtyBit {
		/**
		 * Whether or not the dirty bit is set.
		 */
		private boolean mIsDirty;
		/**
		 * Constructor which initializes the dirty bit to false.
		 */
		public DirtyBit(){
			mIsDirty = false;
		}
		
		/**
		 * Set the dirty bit.
		 */
		public void setDirty(){
			mIsDirty = true;
		}
		
		/**
		 * Get the value of the dirty bit.
		 * @return Whether the dirty bit is set.
		 */
		public boolean isDirty() {
			return mIsDirty;
		}
	}
	
	private void manageDB() {
		final JDialog filterDialog = new JDialog(parentFrame, "Manage Database", true);
		JPanel listPane = new JPanel();
		JPanel buttonPane = new JPanel();
		JButton addButton = new JButton("Add");
		JButton removeButton = new JButton("Remove");
		JButton exportButton = new JButton("Export");
		JButton doneButton = new JButton("Done");
		listPane.setLayout( new BoxLayout(listPane, BoxLayout.Y_AXIS));
		Container contentPane = filterDialog.getContentPane();

		//Vector scns = getScenarios();
		final JList list = new JList(scns);
		
		final DirtyBit dirtyBit = new DirtyBit();
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FileChooser fc = FileChooserFactory.getFileChooser();
				final FileFilter xmlFilter = new XMLFilter();
				File[] xmlFiles = fc.doFilePrompt(parentFrame, "Open XML File", FileChooser.LOAD_DIALOG,
					new File(((InterfaceMain)parentFrame).getProperties().  getProperty("lastDirectory", ".")),
					xmlFilter);

				if(xmlFiles != null) {
					dirtyBit.setDirty();
					((InterfaceMain)parentFrame).getProperties().setProperty("lastDirectory", 
						 xmlFiles[0].getParent());
					parentFrame.getGlassPane().setVisible(true);
					xmlDB.addFile(xmlFiles[0].getAbsolutePath());
					scns = getScenarios();
					list.setListData(scns);
					parentFrame.getGlassPane().setVisible(false);
				}
			}
		});
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] remList = list.getSelectedValues();
				for(int i = 0; i < remList.length; ++i) {
					dirtyBit.setDirty();
					xmlDB.removeDoc(((ScenarioListItem)remList[i]).docName);
				}
				scns = getScenarios();
				list.setListData(scns);
			}
		});
		exportButton.addActionListener(new ActionListener() {
			/**
			 * Method called when the export button is clicked which allows the
			 * user to select a location to export the scenario to and exports
			 * the scenario.
			 * 
			 * @param aEvent
			 *            The event received.
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(ActionEvent aEvent) {
				Object[] selectedList = list.getSelectedValues();
				for (int i = 0; i < selectedList.length; ++i) {
					File exportLocation = FileUtils.selectFile(parentFrame,
							new XMLFileFilter(), null, true);
					if (exportLocation != null) {
						boolean success = xmlDB.exportDoc(((ScenarioListItem)selectedList[i]).docName, 
							exportLocation);
						/*
						boolean success = xmlDB.exportDoc(((String) selectedList[i]).substring(0,
								((String) selectedList[i]).indexOf(' ')),
								exportLocation);
						boolean success = xmlDB.exportDoc((String)selectedList[i],
								exportLocation);
								*/
						if(success) {
							JOptionPane.showMessageDialog(parentFrame, "Scenario export succeeded.");
						}
						else {
							JOptionPane.showMessageDialog(parentFrame, "Scenario export failed.", null, JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}

		});
		doneButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(dirtyBit.isDirty()) {
					xmlDB.addVarMetaData(parentFrame);
					scnList.setListData(scns);
					regions = getRegions();
					regionList.setListData(regions);
				}
				filterDialog.setVisible(false);
			}
		});

		buttonPane.setLayout( new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		buttonPane.add(addButton);
		buttonPane.add(Box.createHorizontalStrut(10));
		buttonPane.add(removeButton);
		buttonPane.add(Box.createHorizontalStrut(10));
		buttonPane.add(exportButton);
		buttonPane.add(Box.createHorizontalStrut(10));
		buttonPane.add(doneButton);
		buttonPane.add(Box.createHorizontalGlue());

		JScrollPane sp = new JScrollPane(list);
		sp.setPreferredSize(new Dimension(300, 300));
		listPane.add(new JLabel("Scenarios in Database:"));
		listPane.add(Box.createVerticalStrut(10));
		listPane.add(sp);
		listPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		contentPane.add(listPane, BorderLayout.PAGE_START);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		filterDialog.pack();
		filterDialog.setVisible(true);
	}

	public void createReport() {
		if(tablesTabs.getTabCount() == 0) {
			// error?
			return;
		}
		jsp = (JScrollPane)tablesTabs.getSelectedComponent();
		jTable = getJTableFromComponent(jsp);
		JFreeReportBoot.getInstance().start();
		JFreeReport report = new JFreeReport();
		java.awt.print.PageFormat pageFormat = new java.awt.print.PageFormat();
		pageFormat.setOrientation(java.awt.print.PageFormat.LANDSCAPE);
		report.setPageDefinition(new org.jfree.report.SimplePageDefinition(pageFormat));
		DrawableFieldElementFactory factory = new DrawableFieldElementFactory();
		Group g = new Group();
		float div = 1;
		int numRows = 0;
		if(jTable.getModel() instanceof MultiTableModel) {
			numRows = (int)jTable.getRowCount()/2;
			div = (float)(jTable.getRowCount()/2);
		} 
		factory.setAbsolutePosition(new Point2D.Float(0, 0));
		factory.setMinimumSize(new FloatDimension((float)800, (float)(jsp.getVerticalScrollBar().getMaximum()/div)));
		factory.setMaximumSize(new FloatDimension((float)800, (float)(jsp.getVerticalScrollBar().getMaximum()/div)));
		factory.setFieldname("0");
		g.addField("0");
		g.getHeader().addElement(factory.createElement());
		g.getHeader().setPagebreakBeforePrint(true);
		report.addGroup(g);
		final Vector fieldList = new Vector(numRows+1);
		fieldList.add("0");
		for(int i = 1; i < numRows; ++i) {
			g = new Group();
			factory.setFieldname(String.valueOf(i));
			fieldList.add(String.valueOf(i));
			g.setFields(fieldList);
			g.getHeader().addElement(factory.createElement());
			g.getHeader().setPagebreakBeforePrint(true);
			report.addGroup(g);
		}

		report.setData(new javax.swing.table.AbstractTableModel() {
			public int findColumn(String cName) {
				return Integer.parseInt(cName);
			}
			public String getColumnName(int col) {
				return String.valueOf(col);
			}
			public int getColumnCount() {
				return fieldList.size();
			}
			public int getRowCount() {
				return 1;
			}
			public Object getValueAt(int row, int col) {
				final int colf = col;
				return (new org.jfree.ui.Drawable() {
					public void draw(java.awt.Graphics2D graphics, java.awt.geom.Rectangle2D bounds) {
						double scaleFactor = bounds.getWidth() / jsp.getHorizontalScrollBar().getMaximum();
						graphics.scale(scaleFactor, scaleFactor);
						graphics.translate((double)0, 0-bounds.getHeight()*colf);
						if(!(jTable.getModel() instanceof MultiTableModel)) {
							jsp.printAll(graphics);
						} else {
							jTable.printAll(graphics);
						}

						graphics.setColor(Color.WHITE);
						graphics.fillRect(0, (int)bounds.getHeight()*(1+colf), (int)graphics.getClipBounds().getWidth(), (int)bounds.getHeight());
					}
				});
			}
		});

		try {
			report.getReportConfiguration().setConfigProperty("org.jfree.report.modules.gui.xls.Enable", "false");
			report.getReportConfiguration().setConfigProperty("org.jfree.report.modules.gui.plaintext.Enable", "false");
			report.getReportConfiguration().setConfigProperty("org.jfree.report.modules.gui.csv.Enable", "false");
			report.getReportConfiguration().setConfigProperty("org.jfree.report.modules.gui.html.Enable", "false");
			report.getReportConfiguration().setConfigProperty("org.jfree.report.modules.gui.rtf.Enable", "false");
			report.getReportConfiguration().setConfigProperty(MyExcelExportPlugin.enableKey, "true");
			ExportPluginFactory epf = ExportPluginFactory.getInstance();
			//MyExcelExportPlugin.bt = bt;
			MyExcelExportPlugin.bt = (BaseTableModel)jTable.getModel();
			epf.registerPlugin(MyExcelExportPlugin.class, "20", MyExcelExportPlugin.enableKey);
			PreviewDialog preview = new PreviewDialog(report, parentFrame, true);
			preview.setTitle(parentFrame.getTitle()+" - Export Preview");
			preview.pack();
			preview.setVisible(true);
		} catch(ReportProcessingException e) {
			e.printStackTrace();
		}
	}

	protected void batchQuery(File queryFile, File excelFile) {
		Node tempNode;
		int[] scnSel;
		HSSFWorkbook wb = null;
		HSSFSheet sheet = null;
		QueryGenerator qgTemp = null;
		Vector tempScns = getScenarios();
		Vector tempRegions = new Vector();

		// Create a Select Scenarios dialog to get which scenarios to run
		final JList scenarioList = new JList(tempScns); 
		final JDialog scenarioDialog = new JDialog(parentFrame, "Select Scenarios to Run", true);
		JPanel listPane = new JPanel();
		JPanel buttonPane = new JPanel();
		final JButton okButton = new JButton("Ok");
		okButton.setEnabled(false);
		JButton cancelButton = new JButton("Cancel");
		listPane.setLayout( new BoxLayout(listPane, BoxLayout.Y_AXIS));
		Container contentPane = scenarioDialog.getContentPane();

		scenarioList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(scenarioList.isSelectionEmpty()) {
					okButton.setEnabled(false);
				} else {
					okButton.setEnabled(true);
				}
			}
		});

		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scenarioDialog.dispose();
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scenarioList.clearSelection();
				scenarioDialog.dispose();
			}
		});

		buttonPane.setLayout( new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okButton);
		buttonPane.add(Box.createHorizontalStrut(10));
		buttonPane.add(cancelButton);

		JScrollPane sp = new JScrollPane(scenarioList);
		sp.setPreferredSize(new Dimension(300, 300));
		listPane.add(new JLabel("Select Scenarios:"));
		listPane.add(Box.createVerticalStrut(10));
		listPane.add(sp);
		listPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		contentPane.add(listPane, BorderLayout.PAGE_START);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		scenarioDialog.pack();
		scenarioDialog.setVisible(true);

		if(scenarioList.isSelectionEmpty()) {
			return;
		}
		scnSel = scenarioList.getSelectedIndices();

		// read the batch query file
		Document queries = readQueries( queryFile );
		XPathEvaluatorImpl xpeImpl = new XPathEvaluatorImpl(queries);
		XPathResult res = (XPathResult)xpeImpl.createExpression("/queries/node()", xpeImpl.createNSResolver(queries.getDocumentElement())).evaluate(queries.getDocumentElement(), XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);

		// read/create the output excel file
		if(excelFile.exists()) {
			try {
				wb = new HSSFWorkbook(new FileInputStream(excelFile));
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return;
			}
		}
		if(wb == null) {
			wb = new HSSFWorkbook();
		}
		//TODO: add a progress bar
		while((tempNode = res.iterateNext()) != null) {
			//tempScns.removeAllElements();
			tempRegions.removeAllElements();
			NodeList nl = tempNode.getChildNodes();
			for(int i = 0; i < nl.getLength(); ++i) {
				Element currEl = (Element)nl.item(i);
				/* Scenarios will be selected from a dialog
				if(currEl.getNodeName().equals("scenario")) {
					tempScns.add("a "+currEl.getAttribute("name")+' '+currEl.getAttribute("date"));
					*/
				if(currEl.getNodeName().equals("region")) {
					tempRegions.add(currEl.getAttribute("name"));
				} else {
					qgTemp = new QueryGenerator(currEl);
				}
			}
			/*
			scnSel = new int[tempScns.size()];
			for(int i = 0; i < scnSel.length; ++i) {
				scnSel[i] = i;
			}
			*/
			String tempFilterQuery = createFilteredQuery(tempScns, scnSel);
			sheet = wb.createSheet("Sheet"+String.valueOf(wb.getNumberOfSheets()+1));
			try {
				if(qgTemp.isGroup()) {
					(new MultiTableModel(qgTemp, tempFilterQuery, tempRegions.toArray(), parentFrame)).exportToExcel(sheet, wb, sheet.createDrawingPatriarch());
				} else {
					(new ComboTableModel(qgTemp, tempFilterQuery, tempRegions.toArray(), parentFrame)).exportToExcel(sheet, wb, sheet.createDrawingPatriarch());
				}
			} catch(NullPointerException e) {
				System.out.println("Warning possible that a query didn't get results");
				e.printStackTrace();
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(excelFile);
			wb.write(fos);
			fos.close();
			JOptionPane.showMessageDialog(parentFrame,
					"Sucessfully ran batch query",
					"Batch Query", JOptionPane.INFORMATION_MESSAGE);
		} catch(IOException ioe) {
			ioe.printStackTrace();
			JOptionPane.showMessageDialog(parentFrame,
					"There was an error while trying to write results",
					"Batch Query Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public boolean writeFile(File file, Document theDoc) {
		// specify output formating properties
		OutputFormat format = new OutputFormat(theDoc);
		format.setEncoding("UTF-8");
		format.setLineSeparator("\r\n");
		format.setIndenting(true);
		format.setIndent(3);
		format.setLineWidth(0);
		format.setPreserveSpace(false);
		format.setOmitDocumentType(true);

		// create the searlizer and have it print the document

		try {
			FileWriter fw = new FileWriter(file);
			XMLSerializer serializer = new XMLSerializer(fw, format);
			serializer.asDOMSerializer();
			serializer.serialize(theDoc);
			fw.close();
		} catch (java.io.IOException e) {
			System.err.println("Error outputing tree: " + e);
			return false;
		}
		return true;
	}
	public Document readQueries(File queryFile) {
		if(queryFile.exists()) {
			LSInput lsInput = implls.createLSInput();
			try {
				lsInput.setByteStream(new FileInputStream(queryFile));
			} catch(FileNotFoundException e) {
				// is it even possible to get here
				e.printStackTrace();
			}
			LSParser lsParser = implls.createLSParser(
					DOMImplementationLS.MODE_SYNCHRONOUS, null);
			lsParser.setFilter(new ParseFilter());
			return lsParser.parse(lsInput);
		} else {
			//DocumentType DOCTYPE = impl.createDocumentType("recent", "", "");
			return ((DOMImplementation)implls).createDocument("", "queries", null);
		}
	}
	public static BaseTableModel getTableModelFromComponent(java.awt.Component comp) {
		Object c;
		try {
			c = ((JScrollPane)comp).getViewport().getView();
			if(c instanceof JSplitPane) {
				return (BaseTableModel)((JTable)((JScrollPane)((JSplitPane)c).getLeftComponent()).getViewport().getView()).getModel();
			} else {
				return (BaseTableModel)((JTable)c).getModel();
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static JTable getJTableFromComponent(java.awt.Component comp) {
		Object c;
		try {
			c = ((JScrollPane)comp).getViewport().getView();
			if(c instanceof JSplitPane) {
				return (JTable)((JScrollPane)((JSplitPane)c).getLeftComponent()).getViewport().getView();
			} else {
				return (JTable)c;
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
			return null;
		}
	}
	private String createCommentTooltip(TreePath path) {
		QueryGenerator qg = (QueryGenerator)path.getLastPathComponent();
		StringBuilder ret = new StringBuilder("<html>");
		for(int i = 1; i < path.getPathCount() -1; ++i) {
			ret.append(path.getPathComponent(i)).append(":<br>");
		}
		ret.append(qg).append("<br><br>Comments:<br>")
			.append(qg.getComments()).append("</html>");
		return ret.toString();
	}
}
