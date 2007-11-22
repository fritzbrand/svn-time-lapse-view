package com.jonathanaquino.svntimelapseview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.ScrollPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.SVNNodeEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class RepoBrowserDialog extends JDialog {

	private JTree repoTree;
	private String repoUrl;
	private String userName;
	private String password;
	private JScrollPane scrollPane;
	private ApplicationWindow applicationWindow;

	/**
	 * Constructs a new dialog (modal) that shows a repository
	 * @param frame
	 */
	public RepoBrowserDialog(Frame frame) {
		super(frame, "Repository browser", true);
		applicationWindow = (ApplicationWindow)frame;
		setSize(500, 309);
		getContentPane().setLayout(new BorderLayout());
		
		scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
	}
	
	public void load(String repoUrl, final String userName, final String password) {
		this.userName = userName;
		this.password = password;
		this.repoUrl = repoUrl;
		repoTree = new JTree();
		repoTree.setCellRenderer(new SVNTreeRenderer());
		
		MouseListener ml = new MouseAdapter() {
		     public void mousePressed(MouseEvent e) {
		         int selRow = repoTree.getRowForLocation(e.getX(), e.getY());
		         TreePath selPath = repoTree.getPathForLocation(e.getX(), e.getY());
		         if(selRow != -1) {
		        	 if (e.getClickCount() == 2) {
		        		 SVNNode selectedNode = (SVNNode)selPath.getLastPathComponent();
		        		 try {
							applicationWindow.load(selectedNode.getSVNDirEntry().getURL().toString(), userName, password, 100);
							setVisible(false);
						} catch (Exception e1) {
							throw new RuntimeException(e1);
						}
		        	 }
		         }
		     }
		};
		repoTree.addMouseListener(ml);
		 
		SVNTreeModel treeModel = new SVNTreeModel(getRepository(repoUrl, userName, password));
		repoTree.setModel(treeModel);
		scrollPane.setViewportView(repoTree);
		this.setVisible(true);
	}
	
	// TODO: We need this a central place
	private SVNRepository getRepository(String url, String username, String password) {
		try {
			DAVRepositoryFactory.setup();
			SVNRepositoryFactoryImpl.setup(); /* svn:// and svn+xxx:// */
			FSRepositoryFactory.setup(); /* file:// */
			SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
			repository.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager(username, password));
			repository.setTunnelProvider(SVNWCUtil.createDefaultOptions(true));
			return repository;
		} catch (SVNException e) {
			// TODO:
			throw new RuntimeException(e);
		}
	}
	
	private static class SVNTreeModel implements TreeModel {

		private SVNRepository repository;
		private SVNNode root;
	
		public SVNTreeModel(SVNRepository repository) {
			super();
			this.repository = repository;
		}

		public void addTreeModelListener(TreeModelListener arg0) {
			// TODO Auto-generated method stub
		}

		public Object getChild(Object obj, int index) {
			SVNNode node = (SVNNode)obj;
			if (node.getChildren() == null) {
				node.loadChildren(repository);
			}
			
			return node.getChildren().get(index);
		}

		public int getChildCount(Object obj) {
			SVNNode node = (SVNNode)obj;
			if (node.getChildren() == null) {
					node.loadChildren(repository);
			}
			return node.getChildren().size();
		}

		public int getIndexOfChild(Object arg0, Object arg1) {
			// TODO Auto-generated method stub
			return 0;
		}

		public Object getRoot() {
			if (root == null) {
				ArrayList children = new ArrayList();
				try {
					SVNDirEntry rootUrl = repository.getDir("/", -1, false, children);
					List svnNodeChildren = new ArrayList(children.size());
					for (Iterator iter = children.iterator() ; iter.hasNext() ; ) {
						SVNDirEntry child = (SVNDirEntry)iter.next();
						svnNodeChildren.add(new SVNNode(child, ""));
					}
					Collections.sort(svnNodeChildren, new SVNNodeComparator());
					root = new SVNNode(rootUrl, svnNodeChildren);
				} catch (SVNException e) {
					throw new RuntimeException(e);
				}
			}
			
			return root; 
		}

		public boolean isLeaf(Object obj) {
			SVNNode node = (SVNNode)obj;
			if (node.getChildren() == null) {
				node.loadChildren(repository);
			}
			return node.getChildren().size() == 0;
		}

		public void removeTreeModelListener(TreeModelListener arg0) {
			// TODO Auto-generated method stub
		}

		public void valueForPathChanged(TreePath arg0, Object arg1) {
			// TODO Auto-generated method stub
		}
	}
	
	private static class SVNNode {
		
		private List children;
		private SVNDirEntry svnDirEntry;
		private String parentPath;
		
		public SVNNode(SVNDirEntry svnDirEntry, String parentPath) {
			super();
			this.svnDirEntry = svnDirEntry;
			this.parentPath = parentPath;
		}

		public SVNNode(SVNDirEntry svnDirEntry, List children) {
			super();
			this.children = children;
			this.svnDirEntry = svnDirEntry;
		}

		public List getChildren() {
			return children;
		}

		public SVNDirEntry getSVNDirEntry() {
			return svnDirEntry;
		}
		
		public String getParentPath() {
			return parentPath;
		}

		public String toString() {
			if (svnDirEntry.getName().equals("")) {
				return svnDirEntry.getURL().toString();
			}
			return svnDirEntry.getName();
		}
		
		public void loadChildren(SVNRepository repository) {
			try {
				children = new ArrayList();
				if (svnDirEntry.getKind() == SVNNodeKind.DIR) {
					String path = (!parentPath.equals("") ? parentPath + "/" : "/") + svnDirEntry.getName();
					Collection svnChildren = repository.getDir(path, -1 , null , (Collection) null );
					Iterator iter = svnChildren.iterator();
		        	while (iter.hasNext()) {
		        		SVNDirEntry child = (SVNDirEntry)iter.next();
		        		SVNNode svnNodeChild = new SVNNode(child, path);
		        		children.add(svnNodeChild);
		        	}
				}
				Collections.sort(children, new SVNNodeComparator());
			} catch (SVNException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static class SVNNodeComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			SVNNode obj1 = (SVNNode)o1;
			SVNNode obj2 = (SVNNode)o2;
			if (obj1.getSVNDirEntry().getKind() == SVNNodeKind.DIR && obj2.getSVNDirEntry().getKind() == SVNNodeKind.FILE) {
				return -1;
			}
			else if (obj1.getSVNDirEntry().getKind() == SVNNodeKind.FILE && obj2.getSVNDirEntry().getKind() == SVNNodeKind.DIR) {
				return 1;
			}
			else {
				return obj1.getSVNDirEntry().getName().compareTo(obj2.getSVNDirEntry().getName());
			}
		}	
	}
	
	private static class SVNTreeRenderer extends DefaultTreeCellRenderer {
        public SVNTreeRenderer() {
        }

        public Component getTreeCellRendererComponent(
                            JTree tree,
                            Object value,
                            boolean sel,
                            boolean expanded,
                            boolean leaf,
                            int row,
                            boolean hasFocus) {

            super.getTreeCellRendererComponent(
                            tree, value, sel,
                            expanded, leaf, row,
                            hasFocus);
            
            SVNNode node = (SVNNode)value;
            
            if (node.getSVNDirEntry().getKind() == SVNNodeKind.DIR) {
            	if (expanded) setIcon(openIcon);
            	else setIcon(closedIcon);
            }
            
            return this;
        }
    }
}