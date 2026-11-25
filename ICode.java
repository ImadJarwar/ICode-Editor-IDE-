package icode;
import java.awt.Image;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.util.prefs.Preferences;
import java.util.ArrayList;
import java.util.List;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.ui.autocomplete.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.undo.UndoManager;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;
// imports for tab color issue
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.net.URL;
import java.util.prefs.Preferences;
public class ICode extends JFrame implements ActionListener {

private JPanel terminalTabStrip;
private JPanel terminalTabButtons;
private JButton killTerminalButton;
private JButton closeTerminalButton;
//private JTextArea t;
private RSyntaxTextArea t;
private UndoManager undoManager = new UndoManager();
private JTabbedPane editorTabs;
private EditorDocument activeDocument;
private int untitledCounter = 1;
private static final int MAX_OPEN_FILES = 10;
// ================= WORKSPACE EXPLORER =================

private File workspaceFolder;

private JTree explorerTree;
private DefaultTreeModel explorerTreeModel;
private DefaultMutableTreeNode workspaceRoot;
private class EditorDocument {
    JLabel tabIconLabel;
    RSyntaxTextArea area;
    UndoManager undoManager;
    File file;
    boolean dirty;
    String displayName;
    JLabel dirtyIndicator;
    JLabel tabTitleLabel;
    Timer autoSaveTimer;

    EditorDocument(RSyntaxTextArea area, File file) {
        this.area = area;
        this.file = file;
        this.undoManager = new UndoManager();
        this.dirty = false;

        // Restore saved font size
        this.area.setFont(
            this.area.getFont().deriveFont(
                (float) getSavedFontSize()
            )
        );

        JLabel tabTitleLabel;
        JLabel languageIconLabel;
    }
}

    private JMenuBar menuBar;
    private JPanel activityBar;
    private JPanel sidePanel;
    private JPanel editorPanel;
    private JPanel leftContainer;
    private boolean sideBarVisible = true;
    
    private JLabel sideTitle;
    private JLabel fileName;

    private JButton explorerBtn;
    private JButton searchBtn;
    private JButton sourceBtn;
    private JButton extensionsBtn;
    private JButton settingsBtn;
    private JButton publishBtn;
    private JToggleButton autoSaveButton;
    private JLabel saveStatusLabel;
    private boolean autoSaveEnabled = false;

    private File currentFile = null;

//    private final Color BG = new Color(30, 30, 30);
    private final Color BG = new Color(18, 18, 18);
    private final Color welcomeBG = new Color(30, 30, 30);
//    private final Color SIDEBAR = new Color(37, 37, 38);
    private final Color SIDEBAR = new Color(30, 30, 30);
//    private final Color ACTIVITY = new Color(51, 51, 51);
    private final Color ACTIVITY = new Color(37, 37, 38);
    private final Color PANEL = new Color(37, 37, 38);
    private final Color BORDER = new Color(65, 65, 65);
    private final Color TEXT = new Color(210, 210, 210);
    private final Color MUTED = new Color(140, 140, 140);
    private final Color BLUE = new Color(0, 122, 204);

// Multiple independent terminal sessions
    private JPanel terminalPanel;
    private JPanel bottomPanel;
    private JPanel findPanel;
    private JLabel findResultLabel;
    private JTabbedPane terminalTabs;
    private int terminalCounter = 1;
    private static final int MAX_TERMINALS = 3;
    private JButton addTerminalButton;

    private boolean explorerActionsVisible = true;
    private final Preferences editorPrefs =
        Preferences.userRoot().node("ICode/Editor");

    private int getSavedFontSize() {
        return editorPrefs.getInt("fontSize", 14);
    }

    private void saveFontSize(int size) {
        editorPrefs.putInt("fontSize", size);
    }
    
    private class TerminalSession {
        JPanel panel;
        JTextArea output;
        JTextField input;
        File directory;
        
    volatile Process process;
    volatile boolean killed = false;
    
        TerminalSession() {
            directory = new File(System.getProperty("user.home"));
        }
        
        JButton tabButton;
    }
    private Preferences projectPreferences =
            Preferences.userNodeForPackage(ICode.class);
    private boolean autoSavePreferenceLoaded =
            projectPreferences.getBoolean("auto_save_enabled", false);

    private List<String> recentProjects = new ArrayList<String>();

    private static final String RECENT_PROJECTS_KEY = "recent_projects";
    private static final String LAST_PROJECT_KEY = "last_opened_project";
    private JTextField searchField;
    public ICode() {

        autoSaveEnabled = autoSavePreferenceLoaded;
        setTitle("ICode");
        setSize(1100, 700);
        setMinimumSize(new Dimension(800, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        createMenuBar();
        createActivityBar();
        createSidePanel();

        // IMPORTANT: Create terminalPanel before createEditor().
        // createEditor() adds terminalPanel to editorPanel.
        createTerminal();

        createEditor();

        // Main application layout
        leftContainer = new JPanel(new BorderLayout());
        leftContainer.add(activityBar, BorderLayout.WEST);
        leftContainer.add(sidePanel, BorderLayout.CENTER);

        add(leftContainer, BorderLayout.WEST);
        add(editorPanel, BorderLayout.CENTER);

        createStatusBar();

        // Enable pointer cursors throughout the interface
        installPointerCursors(getContentPane());

        // Apply pointer cursor to menu items
        installPointerCursors(menuBar);

//        terminal hide unhide: 
        setupTerminalShortcuts();
        setupSideBarShortcuts();
        setupFindShortcut();
        restoreLastProject();
        // ICode Window and Windows Taskbar Icon
        URL iconURL = getClass().getResource(
            "/icode/resources/app-icon.png"
        );

        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);

            // Windows title bar + Windows taskbar
            setIconImage(icon.getImage());
        }
        setVisible(true); 
    }

    // ================= MENU BAR =================


private void createMenuBar() {

    menuBar = new JMenuBar();

    // =========================
    // FILE MENU
    // =========================

    JMenu file = new JMenu("File");

//    JMenuItem welcome = new JMenuItem("Welcome");
//    welcome.addActionListener(e -> addWelcomeTab());
//    file.add(welcome);

    file.addSeparator();

    String[] fileItems = {
        "New", "Open", "Save", "Save As"
    };

    for (String item : fileItems) {
        JMenuItem mi = new JMenuItem(item);
        mi.addActionListener(this);
        file.add(mi);
    }

    file.addSeparator();

    // Close current file
    JMenuItem closeFile = new JMenuItem("Close File");
    closeFile.addActionListener(e -> {
        int index = editorTabs.getSelectedIndex();

        if (index >= 0) {
            closeTab(index);
        }
    });
    file.add(closeFile);

    // Close all files
    JMenuItem closeAllFiles = new JMenuItem("Close All Files");
    closeAllFiles.addActionListener(e -> {
        while (editorTabs.getTabCount() > 0) {
            int index = editorTabs.getSelectedIndex();
            closeTab(index);
        }
    });
    file.add(closeAllFiles);

    // Close current project
    JMenuItem closeProject = new JMenuItem("Close Folder");
    closeProject.addActionListener(e -> closeWorkspace());
    file.add(closeProject);

    // =========================
    // EDIT MENU
    // =========================

    JMenu edit = new JMenu("Edit");

    String[] editItems = {
        "Undo", "Redo", "Cut", "Copy", "Paste", "Select All"
    };

    for (String item : editItems) {
        JMenuItem mi = new JMenuItem(item);
        mi.addActionListener(this);
        edit.add(mi);
    }

    // =========================
    // RUN MENU
    // =========================

    JMenu run = new JMenu("Run");

    JMenuItem runHtml = new JMenuItem("Run HTML in Browser");

    runHtml.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)
    );

    runHtml.addActionListener(e -> runHtmlInBrowser());

    run.add(runHtml);

    // =========================
    // WINDOW CLOSE
    // =========================

    JMenuItem close = new JMenuItem("Close");
    close.addActionListener(this);

    // =========================
    // ADD MENUS
    // =========================

    menuBar.add(file);
    menuBar.add(edit);
    menuBar.add(run);
    menuBar.add(close);

    setJMenuBar(menuBar);
}

    // ================= ACTIVITY BAR =================


private void createActivityBar() {

    activityBar = new JPanel();
    activityBar.setLayout(new BoxLayout(activityBar, BoxLayout.Y_AXIS));
    activityBar.setBackground(ACTIVITY);
    activityBar.setPreferredSize(new Dimension(55, 0));

    // Activity buttons with professional icons

    explorerBtn = createActivityButton("explorer.png", "Explorer");
//    searchBtn = createActivityButton("search.png", "Search");

    sourceBtn = createActivityButton(
            "source-control.png", "Source Control"
    );

    publishBtn = createActivityButton("github.png", "Publish Project to GitHub");
    extensionsBtn = createActivityButton("extentions.png", "Extensions");
    settingsBtn = createActivityButton("settings.png", "Settings");

    activityBar.add(explorerBtn);
//    activityBar.add(searchBtn);
    activityBar.add(sourceBtn);
    activityBar.add(publishBtn);
    activityBar.add(extensionsBtn);

    activityBar.add(Box.createVerticalGlue());

    activityBar.add(settingsBtn);

    // Explorer
    explorerBtn.addActionListener(e -> {

        if (sideBarVisible) {
            sideBarVisible = false;
            sidePanel.setVisible(false);

        } else {
            sideBarVisible = true;
            sidePanel.setVisible(true);
            showSidePanel("EXPLORER");
        }

        leftContainer.revalidate();
        leftContainer.repaint();
        editorPanel.revalidate();
        editorPanel.repaint();
    });

    // Search
//    searchBtn.addActionListener(e -> {
//        showSideBar();
//        showSidePanel("SEARCH");
//    });

    // Source Control
    sourceBtn.addActionListener(e -> {
        showSideBar();
        showSidePanel("SOURCE CONTROL");
    });

    // GitHub
    publishBtn.addActionListener(e -> publishProjectToGitHub());

    // Extensions
    extensionsBtn.addActionListener(e -> {
        showSideBar();
        showSidePanel("EXTENSIONS");
    });

    // Settings
    settingsBtn.addActionListener(e -> {
        showSideBar();
        showSidePanel("SETTINGS");
    });

    selectActivityButton(explorerBtn);
}



private JButton createActivityButton(String iconFile, String tooltip) {

    JButton btn = new JButton();

    if (iconFile != null && !iconFile.trim().isEmpty()) {

        ImageIcon icon = createTintedIcon(iconFile, TEXT);

        if (icon != null) {
            btn.setIcon(icon);
        }
    }

    btn.setToolTipText(tooltip);

    btn.setPreferredSize(new Dimension(55, 55));
    btn.setMaximumSize(new Dimension(55, 55));
    btn.setMinimumSize(new Dimension(55, 55));

    btn.setAlignmentX(Component.CENTER_ALIGNMENT);

    btn.setBackground(ACTIVITY);
    btn.setForeground(TEXT);

    btn.setFocusPainted(false);
    btn.setBorderPainted(false);
    btn.setContentAreaFilled(true);

    return btn;
}

    private void selectActivityButton(JButton selected) {

        JButton[] buttons = {
            explorerBtn,
//            searchBtn,
            sourceBtn,
            publishBtn,
            extensionsBtn,
            settingsBtn
        };

        for (JButton b : buttons) {

            b.setBackground(ACTIVITY);
            b.setBorder(null);
        }

        selected.setBackground(ACTIVITY);
        selected.setBorder(
            BorderFactory.createMatteBorder(
                0, 3, 0, 0, BLUE
            )
        );
    }

    // ================= SIDE PANEL =================

    private void createSidePanel() {

        sidePanel = new JPanel(new BorderLayout());
        sidePanel.setBorder(
            BorderFactory.createMatteBorder(
                0, 0, 0, 1,
                new Color(45, 55, 75)
            )
        );
        sidePanel.setBackground(SIDEBAR);
        sidePanel.setPreferredSize(new Dimension(230, 0));

        sideTitle = new JLabel("EXPLORER");
        sideTitle.setForeground(TEXT);
        sideTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sideTitle.setBorder(
            BorderFactory.createEmptyBorder(18, 15, 18, 5)
        );

        sidePanel.add(sideTitle, BorderLayout.NORTH);

        showExplorer();
    }

    private void showSidePanel(String panel) {

        sidePanel.removeAll();

        sideTitle.setText(panel);

        sidePanel.add(sideTitle, BorderLayout.NORTH);

        selectActivityButton(
            panel.equals("EXPLORER") ? explorerBtn :
//            panel.equals("SEARCH") ? searchBtn :
            panel.equals("SOURCE CONTROL") ? sourceBtn :
            panel.equals("EXTENSIONS") ? extensionsBtn :
            settingsBtn
        );

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(SIDEBAR);
        content.setBorder(
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );

        if (panel.equals("EXPLORER")) {

            showExplorer();
            return;

        } else if (panel.equals("SOURCE CONTROL")) {

            content.add(sideLabel("SOURCE CONTROL"));
            content.add(Box.createVerticalStrut(15));
            content.add(sideLabel("Publish this project to GitHub."));
            content.add(Box.createVerticalStrut(10));
            JButton publishProject = new JButton("↑  Publish to GitHub");
            styleButton(publishProject);
            publishProject.addActionListener(e -> publishProjectToGitHub());
            content.add(publishProject);
            content.add(Box.createVerticalStrut(10));
            content.add(sideLabel("Requires Git for Windows"
                    + " and a GitHub account."));

        } else if (panel.equals("EXTENSIONS")) {

            content.add(sideLabel("EXTENSIONS"));
            content.add(Box.createVerticalStrut(15));
            content.add(sideLabel("No extensions installed."));
            content.add(Box.createVerticalStrut(8));
            content.add(sideLabel("Extension support is not implemented."));

        } else if (panel.equals("SETTINGS")) {

           
        content.add(sideLabel("EDITOR SETTINGS"));
        content.add(Box.createVerticalStrut(15));

        JButton fontPlus = new JButton("Increase Font Size");
        JButton fontMinus = new JButton("Decrease Font Size");
        JButton fontReset = new JButton("Reset Default Font Size");

        styleButton(fontPlus);
        styleButton(fontMinus);
        styleButton(fontReset);

        // Increase font size
        fontPlus.addActionListener(e -> {
            if (t != null) {
                Font f = t.getFont();
                int newSize = f.getSize() + 1;

                t.setFont(f.deriveFont((float) newSize));
                saveFontSize(newSize);
            }
        });

        // Decrease font size
        fontMinus.addActionListener(e -> {
            if (t != null) {
                Font f = t.getFont();

                if (f.getSize() > 8) {
                    int newSize = f.getSize() - 1;

                    t.setFont(f.deriveFont((float) newSize));
                    saveFontSize(newSize);
                }
            }
        });

        // Reset to default font size
        fontReset.addActionListener(e -> {
            if (t != null) {
                int defaultSize = 14;

                Font f = t.getFont();
                t.setFont(f.deriveFont((float) defaultSize));

                saveFontSize(defaultSize);
            }
        });

        content.add(fontPlus);
        content.add(Box.createVerticalStrut(8));
        content.add(fontMinus);
        content.add(Box.createVerticalStrut(8));
        content.add(fontReset);

        }

        sidePanel.add(content, BorderLayout.CENTER);

        sidePanel.revalidate();
        sidePanel.repaint();

        installPointerCursors(sidePanel);
    }





private void showExplorer() {

    sidePanel.removeAll();
    sideTitle.setText("EXPLORER");

    JPanel content = new JPanel(new BorderLayout());
    content.setBackground(SIDEBAR);

    // Show quick actions only before the first file/folder is opened
    if (explorerActionsVisible) {

        JPanel topPanel = new JPanel();
        topPanel.setLayout(
            new BoxLayout(topPanel, BoxLayout.Y_AXIS)
        );
        topPanel.setBackground(SIDEBAR);
        topPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );

        topPanel.add(sideLabel("WORKSPACE"));
        topPanel.add(Box.createVerticalStrut(10));

        JButton newFile = new JButton("+  New File");
        JButton openFile = new JButton("↗  Open File");
        JButton openFolder = new JButton("▣  Open Folder");

        styleButton(newFile);
        styleButton(openFile);
        styleButton(openFolder);

        newFile.addActionListener(e -> newDocument());
        openFile.addActionListener(e -> openDocument());
        openFolder.addActionListener(e -> openFolder());

        topPanel.add(newFile);
        topPanel.add(Box.createVerticalStrut(6));
        topPanel.add(openFile);
        topPanel.add(Box.createVerticalStrut(6));
        topPanel.add(openFolder);

        content.add(topPanel, BorderLayout.NORTH);
    }

    // Display workspace tree if a folder is open
    if (workspaceFolder != null && workspaceFolder.isDirectory()) {

        if (explorerTree == null || workspaceRoot == null) {
            createWorkspaceTree();
        }

        // Show the folder tree directly; close the workspace from its
        // right-click context menu instead of using a separate header panel.
        JScrollPane treeScroll = new JScrollPane(explorerTree);
        treeScroll.setBorder(null);
        treeScroll.getViewport().setBackground(SIDEBAR);

        content.add(treeScroll, BorderLayout.CENTER);

    } else if (explorerActionsVisible) {

        JLabel empty = new JLabel(
            "<html><center>No folder opened.<br><br>"
            + "Open a folder to get started.</center></html>"
        );

        empty.setForeground(MUTED);
        empty.setHorizontalAlignment(SwingConstants.CENTER);

        content.add(empty, BorderLayout.CENTER);
    }

    sidePanel.add(sideTitle, BorderLayout.NORTH);
    sidePanel.add(content, BorderLayout.CENTER);

    sidePanel.revalidate();
    sidePanel.repaint();

    installPointerCursors(sidePanel);
}
    private JLabel sideLabel(String text) {

        JLabel label = new JLabel(text);

        label.setForeground(MUTED);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private void styleButton(JButton button) {

        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(210, 32));
        button.setPreferredSize(new Dimension(210, 32));

        button.setBackground(new Color(50, 50, 50));
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setBorder(
            BorderFactory.createLineBorder(BORDER)
        );
    }

    //Ternial 
    
 
private void createTerminal() {

    terminalPanel = new JPanel(new BorderLayout());
    terminalPanel.setBackground(BG);
    terminalPanel.setPreferredSize(new Dimension(0, 230));

    // Single horizontal row: tabs on left, controls on right
    terminalTabStrip = new JPanel(new BorderLayout());
    terminalTabStrip.setBackground(PANEL);
    terminalTabStrip.setBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER)
    );
    terminalTabStrip.setPreferredSize(new Dimension(0,40));
    terminalTabButtons = new JPanel(
        new FlowLayout(FlowLayout.LEFT, 0, 0)
    );
    terminalTabButtons.setOpaque(false);

    // Right-side controls
    JPanel controls = new JPanel(
        new FlowLayout(FlowLayout.RIGHT, 5, 2)
    );
    controls.setOpaque(false);

    addTerminalButton = new JButton("+");
    killTerminalButton = new JButton("Kill Terminal");
    closeTerminalButton = new JButton("×");

    styleTerminalButton(addTerminalButton);
    styleTerminalButton(killTerminalButton);
    styleTerminalButton(closeTerminalButton);

    addTerminalButton.setToolTipText("New Terminal");
    killTerminalButton.setToolTipText(
        "Terminate and clear selected terminal"
    );
    closeTerminalButton.setToolTipText("Hide Terminal");

    addTerminalButton.addActionListener(
        e -> addTerminalSession()
    );

    killTerminalButton.addActionListener(
        e -> killSelectedTerminal()
    );

    closeTerminalButton.addActionListener(
        e -> closeTerminalSession()
    );

    controls.add(addTerminalButton);
    controls.add(killTerminalButton);
    controls.add(closeTerminalButton);

    terminalTabStrip.add(
        terminalTabButtons, BorderLayout.CENTER
    );

    terminalTabStrip.add(
        controls, BorderLayout.EAST
    );

    // Terminal content (hide Swing's original tab headers)
    terminalTabs = new JTabbedPane(JTabbedPane.TOP);

    terminalTabs.setBackground(PANEL);
    terminalTabs.setForeground(TEXT);
    terminalTabs.setFont(
        new Font("Segoe UI", Font.PLAIN, 12)
    );

    styleTerminalTabs();

    terminalTabs.addChangeListener(
        e -> updateTerminalTabButtons()
    );

    terminalPanel.add(
        terminalTabStrip, BorderLayout.NORTH
    );

    terminalPanel.add(
        terminalTabs, BorderLayout.CENTER
    );

    terminalPanel.setVisible(false);

    addTerminalSession();
}

// Keep terminal tabs visually consistent with the existing editor tabs.
// This is deliberately separate so editor tab styling is not modified.

private void styleTerminalTabs() {

    terminalTabs.setUI(new BasicTabbedPaneUI() {

        @Override
        protected int calculateTabAreaHeight(
                int placement,
                int runCount,
                int maxTabHeight) {
            return 0;
        }

        @Override
        protected void paintContentBorder(
                Graphics g,
                int placement,
                int selectedIndex) {

            g.setColor(BG);

            g.fillRect(
                0, 0,
                tabPane.getWidth(),
                tabPane.getHeight()
            );
        }
    });

    terminalTabs.setOpaque(true);
    terminalTabs.setBackground(BG);
    terminalTabs.setForeground(TEXT);
    terminalTabs.setBorder(null);
}

private void styleTerminalButton(JButton button) {
    button.setForeground(TEXT);
    button.setBackground(PANEL);
    button.setFocusPainted(false);
    button.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
}

private void addTerminalSession() {
    if (terminalTabs.getTabCount() >= MAX_TERMINALS) {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(
            this,
            "You can have up to 3 terminals open at once.\nClose a terminal before opening another.",
            "Terminal Limit Reached",
            JOptionPane.INFORMATION_MESSAGE
        );
        return;
    }

    final TerminalSession session = new TerminalSession();
    session.panel = new JPanel(new BorderLayout());
    session.panel.putClientProperty("terminalSession", session);
    session.panel.setBackground(BG);

    session.output = new JTextArea();
    session.output.setEditable(false);
    session.output.setBackground(BG);
    session.output.setForeground(new Color(190, 220, 190));
    session.output.setCaretColor(Color.WHITE);
    session.output.setFont(new Font("Consolas", Font.PLAIN, 13));
    session.output.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

    JScrollPane outputScroll = new JScrollPane(session.output);
    outputScroll.setBorder(null);

    JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
    inputPanel.setBackground(BG);
    inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 8, 10));

    JLabel prompt = new JLabel(">");
    prompt.setForeground(new Color(50, 220, 150));
    prompt.setFont(new Font("Consolas", Font.BOLD, 15));

    session.input = new JTextField();
    session.input.setBackground(new Color(40, 40, 40));
    session.input.setForeground(Color.WHITE);
    session.input.setCaretColor(Color.WHITE);
    session.input.setFont(new Font("Consolas", Font.PLAIN, 13));
    session.input.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
    session.input.addActionListener(e -> executeTerminalCommand(session, session.input.getText()));

    inputPanel.add(prompt, BorderLayout.WEST);
    inputPanel.add(session.input, BorderLayout.CENTER);
    session.panel.add(outputScroll, BorderLayout.CENTER);
    session.panel.add(inputPanel, BorderLayout.SOUTH);

    session.output.append("SimpleCodeEditor Terminal\nWindows Command Prompt\nType a command and press Enter.\n\n");
    terminalTabs.addTab("Terminal", session.panel);
    session.tabButton = new JButton("Terminal");

    session.tabButton.setFont(
        new Font("Segoe UI", Font.PLAIN, 12)
    );

    session.tabButton.setForeground(TEXT);
    session.tabButton.setBackground(PANEL);
    session.tabButton.setFocusPainted(false);
    session.tabButton.setBorder(BorderFactory.createEmptyBorder(9,20,9,20));
    session.tabButton.setPreferredSize(new Dimension(125, 36));
    session.tabButton.setMinimumSize(new Dimension(125, 36));
    session.tabButton.setCursor(
        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    );

    session.tabButton.addActionListener(e -> {
        terminalTabs.setSelectedComponent(session.panel);
    });

    terminalTabButtons.add(session.tabButton);
    terminalTabs.setSelectedComponent(session.panel);
    updateTerminalTabButtons();

    terminalTabButtons.revalidate();
    terminalTabButtons.repaint();
    updateTerminalAddButton();
}



//private void closeTerminalSession() {
//
//    if (terminalTabs == null || terminalTabs.getTabCount() == 0) {
//        return;
//    }
//
//    // If only one terminal remains, hide the panel
//    // instead of destroying the terminal session.
//    if (terminalTabs.getTabCount() == 1) {
//
//        terminalPanel.setVisible(false);
//
//        editorPanel.revalidate();
//        editorPanel.repaint();
//
//        return;
//    }
//
//    // If multiple terminals exist, close only the selected one.
//    int selected = terminalTabs.getSelectedIndex();
//
//    if (selected >= 0) {
//        terminalTabs.removeTabAt(selected);
//    }
//
//    updateTerminalAddButton();
//
//    editorPanel.revalidate();
//    editorPanel.repaint();
//}

private void closeTerminalSession() {

    terminalPanel.setVisible(false);

    if (bottomPanel != null && (findPanel == null || !findPanel.isVisible())) {
        bottomPanel.setVisible(false);
    }

    editorPanel.revalidate();
    editorPanel.repaint();

    if (t != null) {
        t.requestFocusInWindow();
    }
}

private void updateTerminalAddButton() {
    if (addTerminalButton != null && terminalTabs != null) {
        boolean canAdd = terminalTabs.getTabCount() < MAX_TERMINALS;
        addTerminalButton.setEnabled(canAdd);
        addTerminalButton.setToolTipText(canAdd
            ? "New Terminal (" + (MAX_TERMINALS - terminalTabs.getTabCount()) + " slot(s) available)"
            : "Maximum of 3 terminals reached. Close one to create another.");
    }
}

private void executeTerminalCommand(final TerminalSession session, String command) {
    final String cmd = command.trim();
    if (cmd.isEmpty()) return;
    session.input.setText("");
    session.output.append(session.directory.getAbsolutePath() + "> " + cmd + "\n");

    if (cmd.equalsIgnoreCase("cls")) {
        session.output.setText("");
        return;
    }
    if (cmd.equalsIgnoreCase("cd")) {
        session.output.append(session.directory.getAbsolutePath() + "\n");
        return;
    }
    if (cmd.toLowerCase().startsWith("cd ")) {
        String path = cmd.substring(3).trim();
        if (path.startsWith("\"") && path.endsWith("\"") && path.length() >= 2)
            path = path.substring(1, path.length() - 1);
        File newDirectory = new File(path);
        if (!newDirectory.isAbsolute()) newDirectory = new File(session.directory, path);
        if (newDirectory.isDirectory()) {
            try {
                session.directory = newDirectory.getCanonicalFile();
                session.output.append("Directory changed successfully.\n");
            } catch (IOException ex) {
                session.output.append(ex.getMessage() + "\n");
            }
        } else {
            session.output.append("Directory not found.\n");
        }
        return;
    }

    new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", cmd);
            builder.directory(session.directory);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            session.process = process;

            if (session.killed) {
                process.destroyForcibly();
                return "";
            }            
            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) result.append(line).append("\n");
            }
            process.waitFor();

            if (session.process == process) {
                session.process = null;
            }

            return result.toString();
        }

        @Override
        protected void done() {
            if (session.killed) return;
            try {
                session.output.append(get());
            } catch (Exception ex) {
                session.output.append("Error: " + ex.getMessage() + "\n");
            }
            session.output.setCaretPosition(session.output.getDocument().getLength());
        }
    }.execute();
}
//Ternial 
    
    // ================= EDITOR =================


private void createEditor() {

    editorPanel = new JPanel(new BorderLayout());
    editorPanel.setBackground(BG);


    fileName = new JLabel("  Untitled");
    fileName.setForeground(TEXT);
    fileName.setFont(new Font("Segoe UI", Font.PLAIN, 13));

    // Multiple editor tabs
    editorTabs = new JTabbedPane(JTabbedPane.TOP);

    editorTabs.setTabLayoutPolicy(
        JTabbedPane.SCROLL_TAB_LAYOUT
    );

    styleEditorTabs();

    editorTabs.addChangeListener(e -> updateActiveEditor());

    editorPanel.add(editorTabs, BorderLayout.CENTER);

    // Terminal and Find share the same bottom-panel area.
    bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.setBackground(BG);
    bottomPanel.add(terminalPanel, BorderLayout.CENTER);
    bottomPanel.setVisible(false);

    findPanel = createFindPanel();
    findPanel.setVisible(false);

    editorPanel.add(bottomPanel, BorderLayout.SOUTH);

    // Start with Welcome Page instead of an empty document.
    addWelcomeTab();
}

// Create a new independent editor tab
private void addNewTab(File file, String content) {

    if (countOpenDocuments() >= MAX_OPEN_FILES) {
        JOptionPane.showMessageDialog(
            this,
            "You can have a maximum of 10 open documents.\n"
            + "Please close a document before opening another.",
            "Maximum Documents Reached",
            JOptionPane.INFORMATION_MESSAGE
        );
        return;
    }

    // Create RSyntaxTextArea instead of JTextArea
    RSyntaxTextArea textArea = new RSyntaxTextArea();
    installEditorContextMenu(textArea);

    // Editor appearance
    textArea.setBackground(BG);
    textArea.setForeground(new Color(220, 220, 220));
    textArea.setCaretColor(Color.WHITE);
    textArea.setSelectionColor(new Color(38, 79, 120));
    textArea.setSelectedTextColor(Color.WHITE);
    textArea.setFont(new Font("Consolas", Font.PLAIN, 16));
    textArea.setTabSize(4);
    textArea.setLineWrap(false);

    // RSyntaxTextArea features
    textArea.setCodeFoldingEnabled(true);
    textArea.setAntiAliasingEnabled(true);
    textArea.setAutoIndentEnabled(true);
    textArea.setMarkOccurrences(true);
    textArea.setPaintTabLines(true);

    // Register language support first; changing the syntax style afterwards
    // lets RSTALanguageSupport attach the correct completion/parser for this file.
    // First identify the programming language
    setEditorLanguage(file, textArea);

    // Then register autocomplete and language intelligence
    installLanguageIntelligence(textArea, file);
    // Pair/markup assistance is enabled per editor tab.
    textArea.setCloseCurlyBraces(true);
    textArea.setCloseMarkupTags(true);
    textArea.setBracketMatchingEnabled(true);
    installSmartMarkupAutoClose(textArea);

    textArea.setBorder(
        BorderFactory.createEmptyBorder(12, 15, 12, 12)
    );

    textArea.setText(content);
    textArea.setCaretPosition(0);

    // Create independent document
    EditorDocument document =
        new EditorDocument(textArea, file);

    // Independent Undo history for every tab
    textArea.getDocument().addUndoableEditListener(e ->
        document.undoManager.addEdit(e.getEdit())
    );

    document.autoSaveTimer = new Timer(700, e -> autoSaveDocument(document));
    document.autoSaveTimer.setRepeats(false);

    textArea.getDocument().addDocumentListener(new DocumentListener() {
        private void changed() { markDocumentDirty(document); }
        @Override public void insertUpdate(DocumentEvent e) { changed(); }
        @Override public void removeUpdate(DocumentEvent e) { changed(); }
        @Override public void changedUpdate(DocumentEvent e) { changed(); }
    });

    // Undo / Redo shortcuts
    InputMap inputMap =
        textArea.getInputMap(JComponent.WHEN_FOCUSED);

    ActionMap actionMap = textArea.getActionMap();

    inputMap.put(
        KeyStroke.getKeyStroke(
            KeyEvent.VK_Z,
            InputEvent.CTRL_DOWN_MASK
        ),
        "editor-undo"
    );

    inputMap.put(
        KeyStroke.getKeyStroke(
            KeyEvent.VK_Z,
            InputEvent.CTRL_DOWN_MASK |
            InputEvent.SHIFT_DOWN_MASK
        ),
        "editor-redo"
    );

    inputMap.put(
        KeyStroke.getKeyStroke(
            KeyEvent.VK_Y,
            InputEvent.CTRL_DOWN_MASK
        ),
        "editor-redo"
    );

    actionMap.put("editor-undo", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            undo();
        }
    });

    actionMap.put("editor-redo", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            redo();
        }
    });

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
                 "editor-save");
    actionMap.put("editor-save", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveDocument(false);
        }
    });

    // Create RSyntaxTextArea scroll pane
    
// Create RSyntaxTextArea scroll pane
RTextScrollPane scroll = new RTextScrollPane(textArea);

// APPLY COMPLETE DARK EDITOR THEME
    applyDarkEditorTheme(textArea, scroll);
    scroll.setBorder(null);
    scroll.setLineNumbersEnabled(true);
    scroll.setFoldIndicatorEnabled(true);
    scroll.getViewport().setBackground(BG);
    scroll.getVerticalScrollBar().setUnitIncrement(16);


    scroll.setBorder(null);
    scroll.setLineNumbersEnabled(true);
    scroll.setFoldIndicatorEnabled(true);
    scroll.getViewport().setBackground(BG);
    scroll.getVerticalScrollBar().setUnitIncrement(16);

    // Create tab title
    String title = file == null
        ? "Untitled " + untitledCounter++
        : file.getName();

    document.displayName = title;
    scroll.putClientProperty("editorDocument", document);

    editorTabs.addTab(title, scroll);

    int tabIndex = editorTabs.indexOfComponent(scroll);

    editorTabs.setTabComponentAt(
        tabIndex,
        createTabHeader(title, scroll)
    );

    editorTabs.setSelectedComponent(scroll);

    updateActiveEditor();
}
// ============================================
// PROFESSIONAL LANGUAGE FILE ICONS
// ============================================

private Icon getLanguageIcon(File file) {
    if (file == null) return null;

    String name = file.getName().toLowerCase();

    if (name.endsWith(".html") || name.endsWith(".htm")) {
        return new LanguageFileIcon("5", new Color(228, 77, 38), true);
    }

    if (name.endsWith(".css")) {
        return new LanguageFileIcon("3", new Color(21, 114, 182), true);
    }

    if (name.endsWith(".js") || name.endsWith(".mjs")
            || name.endsWith(".cjs")) {
        return new LanguageFileIcon("JS", new Color(240, 219, 79), false);
    }

    return UIManager.getIcon("FileView.fileIcon");
}

private class LanguageFileIcon implements Icon {

    private final String text;
    private final Color color;
    private final boolean shield;

    LanguageFileIcon(String text, Color color, boolean shield) {
        this.text = text;
        this.color = color;
        this.shield = shield;
    }

    @Override
    public int getIconWidth() {
        return 22;
    }

    @Override
    public int getIconHeight() {
        return 22;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        if (shield) {

            // Distinctive HTML / CSS shield shape
            Polygon shieldShape = new Polygon();

            shieldShape.addPoint(x + 3, y + 2);
            shieldShape.addPoint(x + 19, y + 2);
            shieldShape.addPoint(x + 17, y + 17);
            shieldShape.addPoint(x + 11, y + 21);
            shieldShape.addPoint(x + 5, y + 17);

            g2.setColor(color);
            g2.fillPolygon(shieldShape);

            g2.setColor(new Color(255, 255, 255, 55));
            g2.drawLine(x + 11, y + 4, x + 11, y + 19);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            FontMetrics fm = g2.getFontMetrics();

            int textX = x + (22 - fm.stringWidth(text)) / 2;

            g2.drawString(text, textX, y + 15);

        } else {

            // JavaScript signature square
            g2.setColor(color);
            g2.fillRoundRect(x + 2, y + 2, 19, 19, 3, 3);

            g2.setColor(new Color(35, 35, 35));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));

            FontMetrics fm = g2.getFontMetrics();

            int textX = x + (22 - fm.stringWidth(text)) / 2;

            g2.drawString(text, textX, y + 16);
        }

        g2.dispose();
    }
}

private JPanel createTabHeader(String title, Component tabContent) {

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    header.setOpaque(false);

    EditorDocument document = null;

    if (tabContent instanceof JScrollPane) {
        document = (EditorDocument)
            ((JScrollPane) tabContent)
            .getClientProperty("editorDocument");
    }

    JLabel languageIcon = new JLabel();

    languageIcon.setPreferredSize(new Dimension(22, 22));

    if (document != null) {
        languageIcon.setIcon(getLanguageIcon(document.file));
        document.tabIconLabel = languageIcon;
    }

    JLabel tabTitle = new JLabel(title);
    tabTitle.setForeground(Color.WHITE);
    JLabel dirtyDot = new JLabel("●");
    dirtyDot.setForeground(new Color(230, 166, 55));
    dirtyDot.setToolTipText("Unsaved changes");
    dirtyDot.setVisible(false);

    if (tabContent instanceof JScrollPane) {
        document = (EditorDocument)
            ((JScrollPane) tabContent).getClientProperty("editorDocument");
    }
    if (document != null) {
        document.tabTitleLabel = tabTitle;
        document.dirtyIndicator = dirtyDot;
        dirtyDot.setVisible(document.dirty);
    }

    JButton closeButton = new JButton("×");
    closeButton.setFont(new Font("Arial", Font.PLAIN, 17));
    closeButton.setForeground(new Color(170, 170, 170));
    closeButton.setBackground(new Color(0, 0, 0, 0));
    closeButton.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
    closeButton.setFocusPainted(false);
    closeButton.setContentAreaFilled(false);
    closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    closeButton.setToolTipText("Close tab");

    closeButton.addActionListener(e -> {
        int index = editorTabs.indexOfComponent(tabContent);
        if (index >= 0) closeTab(index);
    });

    header.add(languageIcon);
    header.add(tabTitle);
    header.add(dirtyDot);
    header.add(closeButton);
    return header;
}


// Switch the active editor when another tab is selected
    private void updateActiveEditor() {


    if (editorTabs == null ||
    editorTabs.getSelectedComponent() == null) {
    return;
    }

    Component selected = editorTabs.getSelectedComponent();

    // Welcome Page selected
    if (!(selected instanceof JScrollPane)) {

    activeDocument = null;
    t = null;
    currentFile = null;
    undoManager = new UndoManager();

    if (fileName != null) {
    fileName.setText("  Welcome");
    }

    return;
    }

    JScrollPane scroll = (JScrollPane) selected;

    activeDocument = (EditorDocument)
    scroll.getClientProperty("editorDocument");

    if (activeDocument == null) {
    return;
    }

    t = activeDocument.area;
    undoManager = activeDocument.undoManager;
    currentFile = activeDocument.file;

    String title = currentFile == null
    ? editorTabs.getTitleAt(editorTabs.getSelectedIndex())
    : currentFile.getName();

    fileName.setText("  " + title);

    if (sidePanel != null) {
    showExplorer();
    }
    }
    // ================= UNDO / REDO =================

    private void undo() {
        if (undoManager.canUndo()) {
            try {
                undoManager.undo();
            } catch (CannotUndoException ex) {
                Toolkit.getDefaultToolkit().beep();
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void redo() {
        if (undoManager.canRedo()) {
            try {
                undoManager.redo();
            } catch (CannotRedoException ex) {
                Toolkit.getDefaultToolkit().beep();
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    // ================= STATUS BAR =================

// ================= STATUS BAR =================

private void createStatusBar() {

    JPanel status = new JPanel(new BorderLayout());

    status.setBackground(BLUE);
    status.setPreferredSize(new Dimension(0, 27));

    // LEFT: Ready status
    JLabel left = new JLabel("  ● Ready");
    left.setForeground(Color.WHITE);
    left.setFont(new Font("Segoe UI", Font.PLAIN, 12));

    // CENTER: Terminal toggle button

    JButton terminalButton = new JButton(">_");

    terminalButton.setForeground(Color.WHITE);
    terminalButton.setBackground(BLUE);
    terminalButton.setFont(new Font("Consolas", Font.BOLD, 13));
    terminalButton.setFocusPainted(false);
    terminalButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
    terminalButton.setContentAreaFilled(false);
    terminalButton.setCursor(
        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    );
    terminalButton.setToolTipText("Toggle Terminal");
    terminalButton.addActionListener(e -> toggleTerminal());
//    JButton terminalButton = new JButton("TERMINAL");
//
//    terminalButton.setForeground(Color.WHITE);
//    terminalButton.setBackground(new Color(0, 95, 160));
//    terminalButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
//    terminalButton.setFocusPainted(false);
//    terminalButton.setBorder(
//        BorderFactory.createEmptyBorder(4, 12, 4, 12)
//    );
//    terminalButton.setCursor(
//        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
//    );
//
//    terminalButton.addActionListener(e -> toggleTerminal());

    // RIGHT: Editor information
    JLabel right = new JLabel(
        "Java Swing   |   UTF-8   |   Ln 1, Col 1  "
    );

    right.setForeground(Color.WHITE);
    right.setFont(new Font("Segoe UI", Font.PLAIN, 12));

    status.add(left, BorderLayout.WEST);

    JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    leftPanel.setOpaque(false);

    autoSaveButton = new JToggleButton();
    autoSaveButton.setSelected(autoSaveEnabled);
    updateAutoSaveButtonText();
    autoSaveButton.setForeground(Color.WHITE);
    autoSaveButton.setBackground(BLUE);
    autoSaveButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    autoSaveButton.setFocusPainted(false);
    autoSaveButton.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
    autoSaveButton.setContentAreaFilled(false);
    autoSaveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    autoSaveButton.addActionListener(e -> {
        autoSaveEnabled = autoSaveButton.isSelected();
        projectPreferences.putBoolean("auto_save_enabled", autoSaveEnabled);
        updateAutoSaveButtonText();
        if (autoSaveEnabled) {
            for (int i = 0; i < editorTabs.getTabCount(); i++) {
                Component component = editorTabs.getComponentAt(i);
                if (component instanceof JScrollPane) {
                    EditorDocument document = (EditorDocument)
                        ((JScrollPane) component).getClientProperty("editorDocument");
                    if (document != null && document.dirty) scheduleAutoSave(document);
                }
            }
        }
    });

    saveStatusLabel = new JLabel(autoSaveEnabled ? "  Auto Save enabled" : "  Manual Save");
    saveStatusLabel.setForeground(Color.WHITE);
    saveStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

    leftPanel.add(left);
    leftPanel.add(terminalButton);
    leftPanel.add(autoSaveButton);
    leftPanel.add(saveStatusLabel);

    status.add(leftPanel, BorderLayout.WEST);
    status.add(right, BorderLayout.EAST);

    add(status, BorderLayout.SOUTH);
}
    // ================= FILE OPERATIONS =================

private void newDocument() {

    if (editorTabs.getTabCount() >= MAX_OPEN_FILES) {

        JOptionPane.showMessageDialog(
            this,
            "Maximum of 10 open documents reached.\n"
            + "Close a document before creating another.",
            "Maximum Documents Reached",
            JOptionPane.INFORMATION_MESSAGE
        );

        return;
    }

    addNewTab(null, "");
    explorerActionsVisible = false;
    showExplorer();
}


private void openDocument() {

    JFileChooser chooser = new JFileChooser();

    if (workspaceFolder != null) {
        chooser.setCurrentDirectory(workspaceFolder);
    }

    if (chooser.showOpenDialog(this)
            != JFileChooser.APPROVE_OPTION) {
        return;
    }

    openFileInTab(chooser.getSelectedFile());
}


private void saveDocument(boolean saveAs) {
    if (activeDocument == null) {
        JOptionPane.showMessageDialog(this,
            "Open or create a document first.",
            "No Document", JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    saveEditorDocument(activeDocument, saveAs, true);
}

private void autoSaveDocument(EditorDocument document) {
    if (!autoSaveEnabled || document == null || !document.dirty) return;
    if (document.file == null) {
        if (saveStatusLabel != null) saveStatusLabel.setText("  Save As required for new file");
        return;
    }
    saveEditorDocument(document, false, false);
}

private void saveEditorDocument(EditorDocument document,
                                boolean saveAs,
                                boolean showDialog) {
    if (document == null) return;

    File targetFile = document.file;
    if (targetFile == null || saveAs) {
        JFileChooser chooser = new JFileChooser();
        if (workspaceFolder != null) chooser.setCurrentDirectory(workspaceFolder);
        if (targetFile != null) chooser.setSelectedFile(targetFile);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        targetFile = chooser.getSelectedFile();
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
        writer.write(document.area.getText());
        document.file = targetFile;
        document.displayName = targetFile.getName();

        // If an untitled document is saved with a real extension, update its
        // syntax mode and the matching language completions for that extension.
        setEditorLanguage(targetFile, document.area);
        installLanguageIntelligence(document.area, targetFile);

        document.dirty = false;
        if (document.autoSaveTimer != null) document.autoSaveTimer.stop();

        int index = findTabForDocument(document);
        if (index >= 0) editorTabs.setTitleAt(index, targetFile.getName());

        updateDocumentTabState(document);
        if (activeDocument == document) {
            currentFile = targetFile;
            if (fileName != null) fileName.setText("  " + targetFile.getName());
        }

        if (saveStatusLabel != null) {
            saveStatusLabel.setText(autoSaveEnabled ? "  All changes saved" : "  Saved");
        }
        if (sidePanel != null && workspaceFolder != null) showExplorer();
    } catch (IOException ex) {
        if (showDialog) showError(ex);
        else if (saveStatusLabel != null) saveStatusLabel.setText("  Auto Save failed");
    }
}

private int findTabForDocument(EditorDocument document) {
    if (document == null || editorTabs == null) return -1;
    for (int i = 0; i < editorTabs.getTabCount(); i++) {
        Component component = editorTabs.getComponentAt(i);
        if (component instanceof JScrollPane) {
            EditorDocument candidate = (EditorDocument)
                ((JScrollPane) component).getClientProperty("editorDocument");
            if (candidate == document) return i;
        }
    }
    return -1;
}

private void markDocumentDirty(EditorDocument document) {
    if (document == null) return;
    document.dirty = true;
    updateDocumentTabState(document);
    if (saveStatusLabel != null) {
        saveStatusLabel.setText(autoSaveEnabled ? "  Changes pending..." : "  Unsaved changes");
    }
    if (autoSaveEnabled) scheduleAutoSave(document);
}

private void scheduleAutoSave(EditorDocument document) {
    if (document == null || document.autoSaveTimer == null) return;
    if (document.file == null) {
        if (saveStatusLabel != null) saveStatusLabel.setText("  Save As required for new file");
        return;
    }
    document.autoSaveTimer.restart();
}

private void updateDocumentTabState(EditorDocument document) {
    if (document == null) return;
    if (document.tabTitleLabel != null) {
        document.tabTitleLabel.setText(
            document.displayName == null ? "Untitled" : document.displayName);
    }
    if (document.dirtyIndicator != null) {
        document.dirtyIndicator.setVisible(document.dirty);
    }
    if (editorTabs != null) editorTabs.repaint();
 
    if (document.tabIconLabel != null) {
    document.tabIconLabel.setIcon(
        getLanguageIcon(document.file)
    );
}
}

private void updateAutoSaveButtonText() {
    if (autoSaveButton != null) {
        autoSaveButton.setText(autoSaveEnabled ? "Auto Save: ON" : "Auto Save: OFF");
        autoSaveButton.setToolTipText(autoSaveEnabled
            ? "Auto Save is on. Files save after a short pause in typing."
            : "Auto Save is off. Use Ctrl+S to save manually.");
    }
}

    private void showError(Exception ex) {

        JOptionPane.showMessageDialog(
            this,
            ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

// ================= RUN HTML IN BROWSER =================

private void runHtmlInBrowser() {

    // Check whether an editor document is active
    if (activeDocument == null) {
        JOptionPane.showMessageDialog(
            this,
            "Please open an HTML file first.",
            "No HTML Document",
            JOptionPane.INFORMATION_MESSAGE
        );
        return;
    }

    // Check file extension
    File file = activeDocument.file;

    if (file != null) {

        String name = file.getName().toLowerCase();

        if (!name.endsWith(".html") && !name.endsWith(".htm")) {
            JOptionPane.showMessageDialog(
                this,
                "The current file is not an HTML file.",
                "Invalid File Type",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
    }

    // Save the document before running
    if (file == null) {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save HTML File");

        chooser.setSelectedFile(
            new File("index.html")
        );

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        file = chooser.getSelectedFile();

        String name = file.getName().toLowerCase();

        if (!name.endsWith(".html") && !name.endsWith(".htm")) {
            JOptionPane.showMessageDialog(
                this,
                "Please save the file with an .html or .htm extension.",
                "Invalid File Extension",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        activeDocument.file = file;
    }

    try {

        // Save current editor content
        try (BufferedWriter writer =
                new BufferedWriter(new FileWriter(file))) {

            writer.write(activeDocument.area.getText());
        }

        currentFile = file;

        // Update tab title
        int index = editorTabs.getSelectedIndex();

        if (index >= 0) {
            editorTabs.setTitleAt(index, file.getName());
        }

        fileName.setText("  " + file.getName());

        showExplorer();

        // Open in default browser
        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(
                this,
                "Desktop browser support is not available.",
                "Browser Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            JOptionPane.showMessageDialog(
                this,
                "Opening a browser is not supported on this system.",
                "Browser Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        desktop.browse(file.toURI());

    } catch (Exception ex) {
        showError(ex);
    }
}    
    // ================= MENU ACTIONS =================

    @Override
    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();
        if (t == null && (
//                command.equals("Print") ||
                command.equals("Cut") ||
                command.equals("Copy") ||
                command.equals("Paste") ||
                command.equals("Select All")
        )) {
            return;
        }
        
        try {

            switch (command) {

                case "New":
                    newDocument();
                    break;

                case "Open":
                    openDocument();
                    break;

                case "Save":
                    saveDocument(false);
                    break;

                case "Save As":
                    saveDocument(true);
                    break;

//                case "Print":
//                    t.print();
//                    break;

                case "Cut":
                    t.cut();
                    break;

                case "Copy":
                    t.copy();
                    break;

                case "Paste":
                    t.paste();
                    break;

                case "Select All":
                    t.selectAll();
                    break;

                case "Undo":
                    undo();
                    break;

                case "Redo":
                    redo();
                    break;

                case "Close":
                    dispose();
                    break;
            }

        } catch (Exception ex) {
            showError(ex);
        }
    }
    
    
    
// ================= POINTER CURSOR =================

private void installPointerCursors(Container container) {

    for (Component component : container.getComponents()) {

        // Hand cursor for buttons and clickable controls
        if (component instanceof AbstractButton) {
            component.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );
        }

        // Hand cursor only when hovering over a tab header
        if (component instanceof JTabbedPane) {

            JTabbedPane tabs = (JTabbedPane) component;

            tabs.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {

                    int tab = tabs.indexAtLocation(
                        e.getX(), e.getY()
                    );

                    tabs.setCursor(Cursor.getPredefinedCursor(
                        tab >= 0
                            ? Cursor.HAND_CURSOR
                            : Cursor.DEFAULT_CURSOR
                    ));
                }
            });
        }

        // Process nested panels and containers
        if (component instanceof Container) {
            installPointerCursors((Container) component);
        }
    }
}

// terminal hide and unhide with keyboard

private void toggleTerminal() {

    if (terminalPanel == null || bottomPanel == null) {
        return;
    }

    // Ctrl + ` switches to Terminal if Find is currently displayed.
    boolean show = !terminalPanel.isVisible() || !bottomPanel.isVisible();

    if (show) {
        if (findPanel != null) {
            findPanel.setVisible(false);
        }

        terminalPanel.setVisible(true);
        bottomPanel.removeAll();
        bottomPanel.add(terminalPanel, BorderLayout.CENTER);
        bottomPanel.setPreferredSize(new Dimension(0, 230));
        bottomPanel.setVisible(true);
        requestSelectedTerminalFocus();

    } else {
        terminalPanel.setVisible(false);
        bottomPanel.setVisible(false);

        if (t != null) {
            t.requestFocusInWindow();
        }
    }

    bottomPanel.revalidate();
    bottomPanel.repaint();
    editorPanel.revalidate();
    editorPanel.repaint();
}

private void requestSelectedTerminalFocus() {
    if (terminalTabs == null || terminalTabs.getSelectedComponent() == null) return;
    Component selected = terminalTabs.getSelectedComponent();
    if (selected instanceof JPanel) {
        for (Component c : ((JPanel) selected).getComponents()) {
            if (c instanceof JPanel) {
                for (Component input : ((JPanel) c).getComponents()) {
                    if (input instanceof JTextField) {
                        input.requestFocusInWindow();
                        return;
                    }
                }
            }
        }
    }
}

private void hideTerminal() {

    if (terminalPanel != null && terminalPanel.isVisible()) {

        terminalPanel.setVisible(false);

        if (bottomPanel != null &&
                (findPanel == null || !findPanel.isVisible())) {
            bottomPanel.setVisible(false);
        }

        editorPanel.revalidate();
        editorPanel.repaint();

        if (t != null) {
            t.requestFocusInWindow();
        }
    }
}


private void setupTerminalShortcuts() {

    InputMap input = getRootPane().getInputMap(
        JComponent.WHEN_IN_FOCUSED_WINDOW
    );

    ActionMap actions = getRootPane().getActionMap();

    // Ctrl + ` : Toggle terminal
    input.put(
        KeyStroke.getKeyStroke(
            KeyEvent.VK_BACK_QUOTE,
            InputEvent.CTRL_DOWN_MASK
        ),
        "toggle-terminal"
    );

    actions.put("toggle-terminal", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            toggleTerminal();
        }
    });

    // Escape : Hide terminal
    input.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        "hide-terminal"
    );

    actions.put("hide-terminal", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            hideTerminal();
        }
    });
}

// terminal hide and unhide with keyboard

// side bar hide undhie: 
// ================= SIDEBAR VISIBILITY =================

private void toggleSideBar() {

    sideBarVisible = !sideBarVisible;

    sidePanel.setVisible(sideBarVisible);

    leftContainer.revalidate();
    leftContainer.repaint();

    editorPanel.revalidate();
    editorPanel.repaint();
}

    private void showSideBar() {

        if (!sideBarVisible) {
            sideBarVisible = true;
            sidePanel.setVisible(true);

            leftContainer.revalidate();
            leftContainer.repaint();

            editorPanel.revalidate();
            editorPanel.repaint();
        }
    }
    
        private void setupSideBarShortcuts() {

        InputMap input = getRootPane().getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        ActionMap actions = getRootPane().getActionMap();

        input.put(
            KeyStroke.getKeyStroke(
                KeyEvent.VK_B,
                InputEvent.CTRL_DOWN_MASK
            ),
            "toggle-sidebar"
        );

        actions.put("toggle-sidebar", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSideBar();
            }
        });
    }
        
        // ================= CUSTOM EDITOR TAB COLORS =================

        private void styleEditorTabs() {

            editorTabs.setUI(new BasicTabbedPaneUI() {

                private final Color TAB_BACKGROUND = new Color(37, 37, 38);
                private final Color SELECTED_BACKGROUND = new Color(30, 30, 30);
                private final Color TAB_TEXT = new Color(190, 190, 190);
                private final Color SELECTED_TEXT = new Color(255, 255, 255);
                private final Color HOVER_BACKGROUND = new Color(55, 55, 55);
                private final Color ACCENT = new Color(0, 122, 204);

                @Override
                protected void installDefaults() {
                    super.installDefaults();

                    tabAreaInsets = new Insets(0, 0, 0, 0);
                    contentBorderInsets = new Insets(0, 0, 0, 0);
                }

                @Override
                protected void paintTabBackground(
                        Graphics g, int placement,
                        int index, int x, int y,
                        int width, int height,
                        boolean selected) {

                    Color background = selected
                            ? SELECTED_BACKGROUND
                            : TAB_BACKGROUND;

                    g.setColor(background);
                    g.fillRect(x, y, width, height);
                }

                @Override
                protected void paintTabBorder(
                        Graphics g, int placement,
                        int index, int x, int y,
                        int width, int height,
                        boolean selected) {

                    g.setColor(BORDER);
                    g.drawLine(x, y + height - 1,
                               x + width, y + height - 1);

                    if (selected) {
                        g.setColor(ACCENT);
                        g.fillRect(x, y + height - 2, width, 2);
                    }
                }

                @Override
                protected void paintContentBorder(
                        Graphics g, int placement, int selectedIndex) {

                    g.setColor(BG);

                    int x = tabPane.getInsets().left;
                    int y = calculateTabAreaHeight(
                            placement, runCount, maxTabHeight
                    ) + tabPane.getInsets().top;

                    g.fillRect(
                        x, y,
                        tabPane.getWidth() - x - tabPane.getInsets().right,
                        tabPane.getHeight() - y - tabPane.getInsets().bottom
                    );
                }

                @Override
                protected void paintText(
                        Graphics g, int placement,
                        Font font, FontMetrics metrics,
                        int index, String title,
                        Rectangle textRect, boolean selected) {

                    g.setFont(font);

                    g.setColor(selected
                            ? SELECTED_TEXT
                            : TAB_TEXT);

                    g.drawString(
                        title,
                        textRect.x,
                        textRect.y + metrics.getAscent()
                    );
                }

                @Override
                protected void paintFocusIndicator(
                        Graphics g, int placement,
                        Rectangle[] rects, int index,
                        Rectangle iconRect, Rectangle textRect,
                        boolean selected) {
                    // No default focus rectangle.
                }

                @Override
                protected int calculateTabHeight(
                        int placement, int index, int fontHeight) {

                    return 38;
                }

                @Override
                protected Insets getTabInsets(int placement, int index) {
                    return new Insets(0, 15, 0, 15);
                }
            });

            editorTabs.setOpaque(true);
            editorTabs.setBackground(PANEL);
            editorTabs.setForeground(TEXT);
            editorTabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            editorTabs.setBorder(null);

            editorTabs.repaint();
        }
// GIVE EDITOR LINE NO
        // ================= LINE NUMBERS =================

//        private JTextArea createLineNumberArea(final JTextArea editor) {
//
//            final JTextArea lineNumbers = new JTextArea("1");
//
//            lineNumbers.setEditable(false);
//            lineNumbers.setFocusable(false);
//
//            lineNumbers.setBackground(new Color(80, 37, 38));
//            lineNumbers.setForeground(new Color(110, 110, 110));
//
//            lineNumbers.setFont(editor.getFont());
//
//            lineNumbers.setBorder(
//                BorderFactory.createEmptyBorder(12, 8, 12, 8)
//            );
//
//            lineNumbers.setMargin(new Insets(0, 0, 0, 0));
//
//            lineNumbers.setComponentOrientation(
//                ComponentOrientation.RIGHT_TO_LEFT
//            );
//
//            lineNumbers.setCursor(
//                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
//            );
//
//            // Update line numbers whenever editor text changes
//            editor.getDocument().addDocumentListener(new DocumentListener() {
//
//                private void updateNumbers() {
//
//                    int totalLines = editor.getLineCount();
//
//                    StringBuilder numbers = new StringBuilder();
//
//                    for (int i = 1; i <= totalLines; i++) {
//                        numbers.append(i);
//
//                        if (i < totalLines) {
//                            numbers.append("\n");
//                        }
//                    }
//
//                    lineNumbers.setText(numbers.toString());
//                }
//
//                @Override
//                public void insertUpdate(DocumentEvent e) {
//                    updateNumbers();
//                }
//
//                @Override
//                public void removeUpdate(DocumentEvent e) {
//                    updateNumbers();
//                }
//
//                @Override
//                public void changedUpdate(DocumentEvent e) {
//                    updateNumbers();
//                }
//            });
//
//            return lineNumbers;
//        }
        
//        folder opening method
        
        private void openFolder() {

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Open Workspace Folder");

            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File selectedFolder = chooser.getSelectedFile();
            if (selectedFolder != null && selectedFolder.isDirectory()) {
                openWorkspace(selectedFolder);
            }
        }
        private void createWorkspaceTree() {

        workspaceRoot = new DefaultMutableTreeNode(workspaceFolder);

        explorerTreeModel = new DefaultTreeModel(workspaceRoot);

        explorerTree = new JTree(explorerTreeModel);

        // ==============================
        // EXPLORER TREE APPEARANCE
        // ==============================

        explorerTree.setRootVisible(true);
        explorerTree.setShowsRootHandles(true);
        explorerTree.setBackground(SIDEBAR);
        explorerTree.setForeground(TEXT);
        explorerTree.setOpaque(true);
        explorerTree.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        explorerTree.setRowHeight(25);

        explorerTree.setBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );

        explorerTree.setCellRenderer(new DefaultTreeCellRenderer() {

            private final Color selectedColor =
                new Color(55, 85, 115);

            @Override
            public Component getTreeCellRendererComponent(
                    JTree tree,
                    Object value,
                    boolean selected,
                    boolean expanded,
                    boolean leaf,
                    int row,
                    boolean hasFocus) {

                super.getTreeCellRendererComponent(
                    tree, value, selected,
                    expanded, leaf, row, hasFocus
                );

                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) value;

                Object object = node.getUserObject();

                // File and folder names and icons
                if (object instanceof File) {

                    File file = (File) object;

                    setText(file.getName());

                    if (file.isDirectory()) {
                        setIcon(UIManager.getIcon("FileView.directoryIcon"));
                    } else {
                        setIcon(getLanguageIcon(file));
                    }
                }

                // VS Code-style dark appearance
                setOpaque(true);

                setFont(new Font("Segoe UI", Font.PLAIN, 13));

                if (selected) {
                    setBackground(selectedColor);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(SIDEBAR);
                    setForeground(TEXT);
                }

                setBackgroundNonSelectionColor(SIDEBAR);
                setBackgroundSelectionColor(selectedColor);

                setTextNonSelectionColor(TEXT);
                setTextSelectionColor(Color.WHITE);

                setBorderSelectionColor(BLUE);

                setBorder(
                    BorderFactory.createEmptyBorder(2, 5, 2, 5)
                );

                return this;
            }
        });

    // ==============================
    // LOAD ROOT FOLDER CHILDREN
    // ==============================

    loadFolderChildren(workspaceRoot);

    explorerTree.expandRow(0);

    // ==============================
    // LAZY LOAD SUBFOLDERS
    // ==============================

    explorerTree.addTreeWillExpandListener(
        new javax.swing.event.TreeWillExpandListener() {

            @Override
            public void treeWillExpand(
                    javax.swing.event.TreeExpansionEvent event) {

                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode)
                    event.getPath().getLastPathComponent();

                if (node.getChildCount() == 1) {

                    DefaultMutableTreeNode child =
                        (DefaultMutableTreeNode) node.getChildAt(0);

                    if ("Loading...".equals(
                            child.getUserObject())) {

                        node.removeAllChildren();

                        loadFolderChildren(node);

                        explorerTreeModel.reload(node);
                    }
                }
            }

            @Override
            public void treeWillCollapse(
                    javax.swing.event.TreeExpansionEvent event) {
                // Nothing required
            }
        }
    );

    // ==============================
    // DOUBLE-CLICK TO OPEN FILE
    // ==============================

    explorerTree.addMouseListener(new MouseAdapter() {

        @Override
        public void mouseClicked(MouseEvent e) {

            if (e.getClickCount() != 2) {
                return;
            }

            TreePath path = explorerTree.getPathForLocation(
                e.getX(), e.getY()
            );

            if (path == null) {
                return;
            }

            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode)
                path.getLastPathComponent();

            Object object = node.getUserObject();

            if (object instanceof File) {

                File file = (File) object;

                if (file.isFile()) {
                    openFileInTab(file);
                }
            }
        }
    });

    // ==============================
    // EXPLORER RIGHT-CLICK MENU
    // ==============================

    explorerTree.addMouseListener(new MouseAdapter() {
        private void showExplorerPopup(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
            TreePath path = explorerTree.getPathForLocation(e.getX(), e.getY());
            if (path == null) return;

            explorerTree.setSelectionPath(path);
            final DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) path.getLastPathComponent();
            Object value = node.getUserObject();
            if (!(value instanceof File)) return;

            final File selected = (File) value;
            final boolean isRoot = node == workspaceRoot;
            final File targetDirectory = selected.isDirectory()
                ? selected : selected.getParentFile();

            JPopupMenu menu = new JPopupMenu();
            if (selected.isFile()) {
                JMenuItem openItem = new JMenuItem("Open File");
                openItem.addActionListener(a -> openFileInTab(selected));
                menu.add(openItem);

                JMenuItem closeItem = new JMenuItem("Close File");
                closeItem.setEnabled(findOpenTabForFile(selected) >= 0);
                closeItem.addActionListener(a -> closeFileFromExplorer(selected));
                menu.add(closeItem);
                menu.addSeparator();
            }

            if (targetDirectory != null) {
                JMenuItem newFileItem = new JMenuItem("New File...");
                newFileItem.addActionListener(a ->
                    createProjectFile(targetDirectory, node, selected.isFile()));
                menu.add(newFileItem);

                JMenuItem newFolderItem = new JMenuItem("New Folder...");
                newFolderItem.addActionListener(a ->
                    createProjectFolder(targetDirectory, node, selected.isFile()));
                menu.add(newFolderItem);
                menu.addSeparator();
            }

//            if (selected.isDirectory() && !isRoot) {
//                JMenuItem collapseItem = new JMenuItem("Collapse Folder");
//                collapseItem.addActionListener(a -> explorerTree.collapsePath(path));
//                menu.add(collapseItem);
//            }

            if (!isRoot) {
                JMenuItem deleteItem = new JMenuItem(selected.isDirectory()
                    ? "Delete Folder" : "Delete File");
                deleteItem.addActionListener(a -> deleteProjectItem(selected, node));
                menu.add(deleteItem);
            } else {
                JMenuItem closeFolderItem = new JMenuItem("Close Folder");
                closeFolderItem.addActionListener(a -> closeWorkspace());
                menu.add(closeFolderItem);
            }

//            JMenuItem refreshItem = new JMenuItem("Refresh");
//            refreshItem.addActionListener(a -> refreshExplorerNode(
//                selected.isDirectory() ? node :
//                (DefaultMutableTreeNode) node.getParent()));
//            menu.addSeparator();
//            menu.add(refreshItem);
            menu.show(explorerTree, e.getX(), e.getY());
        }

        @Override public void mousePressed(MouseEvent e) { showExplorerPopup(e); }
        @Override public void mouseReleased(MouseEvent e) { showExplorerPopup(e); }
    });

    // ==============================

    explorerTree.setCursor(
        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    );
}

        
                
// Create a file inside the selected project folder.
private void createProjectFile(File parentFolder,
                               DefaultMutableTreeNode selectedNode,
                               boolean selectedWasFile) {
    if (parentFolder == null || !parentFolder.isDirectory()) return;
    String name = JOptionPane.showInputDialog(
        this, "Enter the new file name:", "New File", JOptionPane.PLAIN_MESSAGE);
    if (name == null) return;
    name = name.trim();
    if (name.length() == 0 || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
        JOptionPane.showMessageDialog(this, "Enter a valid file name.");
        return;
    }
    File newFile = new File(parentFolder, name);
    try {
        if (!newFile.createNewFile()) {
            JOptionPane.showMessageDialog(this,
                "A file or folder with that name already exists.");
            return;
        }
        DefaultMutableTreeNode parentNode = selectedWasFile
            ? (DefaultMutableTreeNode) selectedNode.getParent() : selectedNode;
        refreshExplorerNode(parentNode);
        openFileInTab(newFile);
    } catch (IOException ex) {
        showError(ex);
    }
}

// Create a folder inside the selected project folder.
private void createProjectFolder(File parentFolder,
                                 DefaultMutableTreeNode selectedNode,
                                 boolean selectedWasFile) {
    if (parentFolder == null || !parentFolder.isDirectory()) return;
    String name = JOptionPane.showInputDialog(
        this, "Enter the new folder name:", "New Folder", JOptionPane.PLAIN_MESSAGE);
    if (name == null) return;
    name = name.trim();
    if (name.length() == 0 || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
        JOptionPane.showMessageDialog(this, "Enter a valid folder name.");
        return;
    }
    File newFolder = new File(parentFolder, name);
    if (!newFolder.mkdir()) {
        JOptionPane.showMessageDialog(this,
            "Could not create folder. It may already exist.");
        return;
    }
    DefaultMutableTreeNode parentNode = selectedWasFile
        ? (DefaultMutableTreeNode) selectedNode.getParent() : selectedNode;
    refreshExplorerNode(parentNode);
}

private void refreshExplorerNode(DefaultMutableTreeNode node) {
    if (node == null || !(node.getUserObject() instanceof File)) return;
    File folder = (File) node.getUserObject();
    if (!folder.isDirectory()) return;
    node.removeAllChildren();
    loadFolderChildren(node);
    explorerTreeModel.reload(node);
    explorerTree.expandPath(new TreePath(node.getPath()));
}

private void deleteProjectItem(File target, DefaultMutableTreeNode node) {
    if (target == null || node == workspaceRoot) return;
    String kind = target.isDirectory() ? "folder and ALL of its contents" : "file";
    int answer = JOptionPane.showConfirmDialog(
        this,
        "Permanently delete this " + kind + "?\n"
        + target.getAbsolutePath() + "\n\n"
        + "This cannot be undone. Open tabs for this item will be closed.",
        "Confirm Delete",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);
    if (answer != JOptionPane.YES_OPTION) return;

    if (!closeTabsForDeletedPath(target)) return;
    if (!deleteRecursively(target)) {
        JOptionPane.showMessageDialog(this,
            "Could not delete the item. Check that it is not in use and that you have permission.",
            "Delete Failed", JOptionPane.ERROR_MESSAGE);
        return;
    }
    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
    if (parent != null) refreshExplorerNode(parent);
}

private boolean closeTabsForDeletedPath(File target) {
    try {
        String rootPath = target.getCanonicalPath();
        for (int i = editorTabs.getTabCount() - 1; i >= 0; i--) {
            Component component = editorTabs.getComponentAt(i);
            if (!(component instanceof JScrollPane)) continue;
            EditorDocument document = (EditorDocument)
                ((JScrollPane) component).getClientProperty("editorDocument");
            if (document == null || document.file == null) continue;
            String filePath = document.file.getCanonicalPath();
            if (filePath.equalsIgnoreCase(rootPath)
                    || (target.isDirectory() && filePath.toLowerCase().startsWith(
                        (rootPath + File.separator).toLowerCase()))) {
                closeTab(i);
                if (i < editorTabs.getTabCount()
                        && editorTabs.getComponentAt(i) == component) {
                    return false;
                }
            }
        }
        return true;
    } catch (IOException ex) {
        showError(ex);
        return false;
    }
}

private boolean deleteRecursively(File file) {
    if (file.isDirectory()) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!deleteRecursively(child)) return false;
            }
        }
    }
    return file.delete();
}

// Returns the tab index for a file that is already open, or -1.
private int findOpenTabForFile(File file) {
    if (file == null || editorTabs == null) {
        return -1;
    }

    try {
        String wantedPath = file.getCanonicalPath();
        for (int i = 0; i < editorTabs.getTabCount(); i++) {
            Component component = editorTabs.getComponentAt(i);
            if (!(component instanceof JScrollPane)) {
                continue;
            }

            EditorDocument document = (EditorDocument)
                ((JScrollPane) component).getClientProperty("editorDocument");

            if (document != null && document.file != null
                    && wantedPath.equalsIgnoreCase(document.file.getCanonicalPath())) {
                return i;
            }
        }
    } catch (IOException ex) {
        showError(ex);
    }
    return -1;
}

// Closes the editor tab only. The real file on disk is never deleted.
private void closeFileFromExplorer(File file) {
    int index = findOpenTabForFile(file);
    if (index >= 0) {
        closeTab(index);
    }
}

private void loadFolderChildren(
        DefaultMutableTreeNode parent) {

    File directory = (File) parent.getUserObject();

    File[] files = directory.listFiles();

    if (files == null) {
        return;
    }

    // Sort folders first, then files
    java.util.Arrays.sort(files, new java.util.Comparator<File>() {

        @Override
        public int compare(File a, File b) {

            if (a.isDirectory() && !b.isDirectory()) {
                return -1;
            }

            if (!a.isDirectory() && b.isDirectory()) {
                return 1;
            }

            return a.getName().compareToIgnoreCase(b.getName());
        }
    });

    for (File file : files) {

        DefaultMutableTreeNode child =
            new DefaultMutableTreeNode(file);

        parent.add(child);

        // Add placeholder to expandable folders
        if (file.isDirectory()) {

            File[] children = file.listFiles();

            if (children != null && children.length > 0) {

                child.add(
                    new DefaultMutableTreeNode("Loading...")
                );
            }
        }
    }
}


private void openFileInTab(File selectedFile) {
        explorerActionsVisible = false;
        showExplorer();
    if (selectedFile == null || !selectedFile.isFile()) {
        return;
    }

    try {

        String selectedPath = selectedFile.getCanonicalPath();

        // Check if the file is already open
        for (int i = 0; i < editorTabs.getTabCount(); i++) {

        Component component = editorTabs.getComponentAt(i);

        if (!(component instanceof JScrollPane)) {
            continue;
        }

        JScrollPane scroll = (JScrollPane) component;
        
            EditorDocument document = (EditorDocument)
                scroll.getClientProperty("editorDocument");

            if (document != null && document.file != null) {

                if (document.file.getCanonicalPath()
                        .equalsIgnoreCase(selectedPath)) {

                    editorTabs.setSelectedIndex(i);
                    return;
                }
            }
        }

        // Read file
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader =
                new BufferedReader(
                    new FileReader(selectedFile))) {

            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        // Open in a new editor tab
        addNewTab(selectedFile, content.toString());

    } catch (IOException ex) {

        showError(ex);
    }
}




    // ================= WELCOME PAGE =================

    private JPanel welcomePage;

    private void addWelcomeTab() {

        if (welcomePage != null) {
            int existingIndex = editorTabs.indexOfComponent(welcomePage);

            if (existingIndex >= 0) {
                editorTabs.setSelectedComponent(welcomePage);
                return;
            }
        }

        welcomePage = createWelcomePage();

        editorTabs.addTab("Welcome", welcomePage);

        int index = editorTabs.indexOfComponent(welcomePage);

        editorTabs.setTabComponentAt(
            index,
            createTabHeader("Welcome", welcomePage)
        );

        editorTabs.setSelectedComponent(welcomePage);
        updateActiveEditor();
    }
    
private JPanel createWelcomePage() {

    JPanel page = new JPanel(new GridBagLayout());
    page.setBackground(welcomeBG);

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBackground(welcomeBG);
    content.setPreferredSize(new Dimension(620, 440));

    JLabel logo = new JLabel("ICode");
    logo.setFont(new Font("Segoe UI", Font.BOLD, 36));
    logo.setForeground(Color.WHITE);
    logo.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel subtitle = new JLabel(
        "A simple environment for your code."
    );
    subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
    subtitle.setForeground(MUTED);
    subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel startTitle = new JLabel("Start");
    startTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
    startTitle.setForeground(TEXT);
    startTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

    content.add(logo);
    content.add(Box.createVerticalStrut(8));
    content.add(subtitle);
    content.add(Box.createVerticalStrut(45));
    content.add(startTitle);
    content.add(Box.createVerticalStrut(15));

    JPanel actions = new JPanel(new GridLayout(0, 2, 15, 12));
    actions.setBackground(welcomeBG);
    actions.setMaximumSize(new Dimension(620, 130));
    actions.setAlignmentX(Component.LEFT_ALIGNMENT);

    JButton newFile = createWelcomeButton(
        "＋   New File", "Create a blank document"
    );

    JButton openFile = createWelcomeButton(
        "↗   Open File", "Open an existing file"
    );

    JButton openFolder = createWelcomeButton(
        "▣   Open Folder", "Open a project folder"
    );

    JButton shortcuts = createWelcomeButton(
        "⌨   Keyboard Shortcuts", "View editor shortcuts"
    );

    newFile.addActionListener(e -> newDocument());
    openFile.addActionListener(e -> openDocument());
    openFolder.addActionListener(e -> openFolder());

    shortcuts.addActionListener(e -> {
        JOptionPane.showMessageDialog(
            this,
            "Ctrl + Z       Undo\n"
            + "Ctrl + Shift + Z   Redo\n"
            + "Ctrl + B       Toggle Sidebar\n"
            + "Ctrl + `       Toggle Terminal\n"
            + "Escape         Hide Terminal",
            "Keyboard Shortcuts",
            JOptionPane.INFORMATION_MESSAGE
        );
    });

    actions.add(newFile);
    actions.add(openFile);
    actions.add(openFolder);
    actions.add(shortcuts);

    content.add(actions);
    content.add(Box.createVerticalStrut(35));

    JLabel recentTitle = new JLabel("Recent Workspace");
    recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
    recentTitle.setForeground(TEXT);
    recentTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

    content.add(recentTitle);
    content.add(Box.createVerticalStrut(12));

    if (workspaceFolder != null) {

        JButton recent = createWelcomeButton(
            "📁   " + workspaceFolder.getName(),
            workspaceFolder.getAbsolutePath()
        );

        recent.addActionListener(e -> {
            workspaceFolder = workspaceFolder;
            explorerActionsVisible = false;
            explorerTree = null;
            workspaceRoot = null;
            showExplorer();
            selectEditorDocumentOrWelcome();
        });

        content.add(recent);

    } else {

        JLabel empty = new JLabel("No recent workspace available.");
        empty.setForeground(MUTED);
        empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(empty);
    }

    content.add(Box.createVerticalStrut(35));

    JLabel footer = new JLabel(
        "ICODE    •  READY TO CODE"
    );

    footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    footer.setForeground(MUTED);
    footer.setAlignmentX(Component.LEFT_ALIGNMENT);

    content.add(footer);

    page.add(content);

    return page;
}

private JButton createWelcomeButton(String title, String description) {

    JButton button = new JButton(
        "<html><div style='padding:5px;'>"
        + "<b>" + title + "</b><br>"
        + "<span style='color:#999999;font-size:10px;'>"
        + description
        + "</span></div></html>"
    );

    button.setHorizontalAlignment(SwingConstants.LEFT);
    button.setPreferredSize(new Dimension(290, 55));
    button.setBackground(PANEL);
    button.setForeground(TEXT);
    button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    button.setFocusPainted(false);
    button.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        )
    );

    button.setCursor(
        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    );

    button.addMouseListener(new MouseAdapter() {

        @Override
        public void mouseEntered(MouseEvent e) {
            button.setBackground(new Color(55, 55, 55));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            button.setBackground(PANEL);
        }
    });

    return button;
}

// Select the Welcome tab when no document is active.
private void selectEditorDocumentOrWelcome() {

    if (activeDocument == null) {
        editorTabs.setSelectedComponent(welcomePage);
    }
}

//helper method
private int countOpenDocuments() {

    int count = 0;

    for (int i = 0; i < editorTabs.getTabCount(); i++) {

        Component component = editorTabs.getComponentAt(i);

        if (component instanceof JScrollPane) {
            count++;
        }
    }

    return count;
}

// ================= SMART LANGUAGE SUPPORT =================

/**
 * Installs RSTALanguageSupport for the active language.  The JavaScript
 * implementation in RSTALanguageSupport 3.1.4 needs Rhino at runtime.
 * If Rhino is not on the classpath yet, AutoComplete still provides a small
 * language-aware fallback list instead of preventing the editor from opening.
 */

private void installLanguageIntelligence(RSyntaxTextArea area, File file) {
    if (area == null) {
        return;
    }

    String name = file == null
            ? ""
            : file.getName().toLowerCase();

    // Always use our custom completion for HTML, CSS and JavaScript.
    // This prevents Rhino language support from bypassing these suggestions.
    boolean webLanguage =
            name.endsWith(".html")
            || name.endsWith(".htm")
            || name.endsWith(".xhtml")
            || name.endsWith(".css")
            || name.endsWith(".js")
            || name.endsWith(".mjs")
            || name.endsWith(".cjs");

    if (webLanguage) {
        installFallbackCompletion(area, file);
        return;
    }

    // Keep the existing advanced language support for other languages.
    if (isRhinoAvailable()) {
        try {
            LanguageSupportFactory.get().register(area);
            return;
        } catch (LinkageError ex) {
            System.err.println(
                    "RSTALanguageSupport could not be initialized: " + ex
            );
        } catch (RuntimeException ex) {
            System.err.println(
                    "RSTALanguageSupport could not be initialized: " + ex
            );
        }
    }

    installFallbackCompletion(area, file);
}

private boolean isRhinoAvailable() {
    try {
        Class.forName("org.mozilla.javascript.Context");
        return true;
    } catch (ClassNotFoundException ex) {
        return false;
    } catch (LinkageError ex) {
        return false;
    }
}

/**
 * Reliable language-specific word completion for HTML, CSS and JavaScript.
 * Suggestions activate as soon as the user types a letter, including when
 * the word is typed on a blank line (for example, typing "p" in an HTML file).
 */
private void installFallbackCompletion(RSyntaxTextArea area, File file) {
    if (area == null) {
        return;
    }

    Object previousCompletion = area.getClientProperty("fallbackAutoCompletion");
    if (previousCompletion instanceof AutoCompletion) {
        ((AutoCompletion) previousCompletion).uninstall();
    }

    DefaultCompletionProvider provider = new DefaultCompletionProvider();
    // DefaultCompletionProvider does not activate on letters by default in
    // some library versions; enable it explicitly so typing "p" opens results.
    provider.setAutoActivationRules(true, "_$-");
    String name = file == null ? "" : file.getName().toLowerCase();
    String[] words;

    if (name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".xhtml")) {
        words = new String[] {
            // HTML document and semantic elements
            "html", "head", "title", "base", "link", "meta", "style", "script",
            "body", "header", "nav", "main", "section", "article", "aside",
            "footer", "address", "div", "span", "p", "h1", "h2", "h3", "h4",
            "h5", "h6", "a", "img", "picture", "source", "figure", "figcaption",
            "video", "audio", "iframe", "canvas", "svg", "ul", "ol", "li",
            "dl", "dt", "dd", "table", "thead", "tbody", "tfoot", "tr", "th",
            "td", "form", "label", "input", "textarea", "select", "option",
            "button", "fieldset", "legend", "datalist", "output", "details",
            "summary", "dialog", "template", "blockquote", "pre", "code", "br",
            "hr", "strong", "em", "small", "mark", "del", "ins", "sub", "sup",
            // Common HTML attributes
            "id", "class", "style", "title", "lang", "dir", "hidden", "tabindex",
            "contenteditable", "draggable", "role", "href", "target", "rel", "download",
            "src", "srcset", "alt", "width", "height", "loading", "type", "name",
            "value", "placeholder", "required", "disabled", "readonly", "checked",
            "selected", "multiple", "min", "max", "step", "pattern", "maxlength",
            "minlength", "autocomplete", "action", "method", "enctype", "for", "accept",
            "media", "charset", "content", "defer", "async", "controls", "autoplay",
            "loop", "muted", "poster", " colspan", "rowspan", "scope", "data-"
        };
    } else if (name.endsWith(".css")) {
        words = new String[] {
            // CSS selectors and at-rules
            "@media", "@import", "@font-face", "@keyframes", "@supports", "@layer",
            ":hover", ":focus", ":active", ":visited", ":first-child", ":last-child",
            "::before", "::after", "::placeholder", "!important",
            // Layout and display
            "display", "block", "inline", "inline-block", "flex", "inline-flex", "grid",
            "inline-grid", "none", "position", "static", "relative", "absolute", "fixed",
            "sticky", "top", "right", "bottom", "left", "z-index", "float", "clear",
            "overflow", "overflow-x", "overflow-y", "visibility", "box-sizing",
            "flex-direction", "flex-wrap", "flex-grow", "flex-shrink", "flex-basis",
            "justify-content", "align-items", "align-content", "align-self", "gap",
            "grid-template-columns", "grid-template-rows", "grid-column", "grid-row",
            "place-items", "order",
            // Size and spacing
            "width", "height", "min-width", "max-width", "min-height", "max-height",
            "margin", "margin-top", "margin-right", "margin-bottom", "margin-left",
            "padding", "padding-top", "padding-right", "padding-bottom", "padding-left",
            // Text and font
            "color", "font", "font-family", "font-size", "font-style", "font-weight",
            "line-height", "letter-spacing", "word-spacing", "text-align", "text-decoration",
            "text-transform", "text-shadow", "white-space", "word-break", "overflow-wrap",
            // Background, border and effects
            "background", "background-color", "background-image", "background-size",
            "background-position", "background-repeat", "border", "border-width",
            "border-style", "border-color", "border-radius", "border-top", "outline",
            "box-shadow", "opacity", "filter", "transform", "transform-origin",
            "transition", "transition-property", "transition-duration", "transition-timing-function",
            "animation", "animation-name", "animation-duration", "animation-delay",
            "cursor", "pointer-events", "object-fit", "object-position", "list-style",
            "vertical-align", "content", "resize", "user-select",
            // Common values
            "auto", "inherit", "initial", "unset", "transparent", "currentColor",
            "solid", "dashed", "dotted", "center", "start", "end", "space-between",
            "space-around", "space-evenly", "nowrap", "wrap", "cover", "contain",
            "pointer", "default", "hidden", "visible", "smooth", "ease", "linear",
            "rem", "em", "vh", "vw", "px", "%"
        };
    } else if (name.endsWith(".js") || name.endsWith(".mjs") || name.endsWith(".cjs")) {
        words = new String[] {
            // JavaScript declarations and control flow
            "const", "let", "var", "function", "return", "if", "else", "switch",
            "case", "default", "break", "continue", "for", "while", "do", "of", "in",
            "try", "catch", "finally", "throw", "new", "this", "class", "extends",
            "super", "constructor", "static", "get", "set", "async", "await", "yield",
            "import", "export", "from", "as", "typeof", "instanceof", "delete", "void",
            "true", "false", "null", "undefined", "NaN", "Infinity",
            // Browser objects and DOM
            "window", "document", "navigator", "location", "history", "console",
            "document.getElementById()", "document.querySelector()", "document.querySelectorAll()",
            "document.createElement()", "document.createTextNode()", "document.addEventListener()",
            "document.getElementsByClassName()", "document.getElementsByTagName()",
            "window.addEventListener()", "window.setTimeout()", "window.setInterval()",
            "addEventListener()", "removeEventListener()", "getElementById()", "querySelector()",
            "querySelectorAll()", "createElement()", "appendChild()", "removeChild()",
            "classList", "className", "innerHTML", "textContent", "value", "style",
            "preventDefault()", "stopPropagation()", "fetch()", "localStorage", "sessionStorage",
            // Console and common functions
            "console.log()", "console.error()", "console.warn()", "console.table()",
            "alert()", "prompt()", "confirm()", "parseInt()", "parseFloat()", "isNaN()",
            "setTimeout()", "setInterval()", "clearTimeout()", "clearInterval()",
            // Built-in objects and methods
            "Array", "Object", "String", "Number", "Boolean", "Math", "Date", "JSON",
            "Map", "Set", "Promise", "RegExp", "Error", "Symbol", "Intl",
            "forEach()", "map()", "filter()", "reduce()", "find()", "findIndex()",
            "includes()", "indexOf()", "push()", "pop()", "slice()", "splice()",
            "join()", "split()", "trim()", "toLowerCase()", "toUpperCase()",
            "toString()", "toFixed()", "keys()", "values()", "entries()", "then()",
            "catch()", "finally()", " stringify()", "parse()", "hasOwnProperty()",
            // Common browser event names
            "click", "dblclick", "input", "change", "submit", "load", "DOMContentLoaded",
            "keydown", "keyup", "keypress", "mouseover", "mouseout", "mouseenter",
            "mouseleave", "focus", "blur", "scroll", "resize", "mousemove"
        };
    } else if (name.endsWith(".java")) {
        words = new String[] {
            "public", "private", "protected", "static", "final", "void", "class",
            "interface", "extends", "implements", "new", "return", "if", "else",
            "for", "while", "switch", "try", "catch", "finally", "throw",
            "import", "package", "String", "System.out.println()", "ArrayList",
            "HashMap", "JFrame", "JPanel", "JButton", "JLabel"
        };
    } else {
        words = new String[] {"function", "return", "class", "public", "private", "const", "let"};
    }

    for (int i = 0; i < words.length; i++) {
        String word = words[i].trim();
        if (word.length() > 0) {
            provider.addCompletion(new BasicCompletion(provider, word));
        }
    }

    AutoCompletion completion = new AutoCompletion(provider);
    completion.setAutoCompleteEnabled(true);
    completion.setAutoActivationEnabled(true);
    completion.setAutoActivationDelay(250);
    completion.setShowDescWindow(false);
    completion.install(area);
    area.putClientProperty("fallbackAutoCompletion", completion);
}

/**
 * Completes an opening HTML/XML/PHP tag as soon as its closing angle bracket
 * is typed.  Void HTML elements (img, br, input, etc.) are intentionally
 * excluded.  The built-in RSyntaxTextArea markup completion remains enabled
 * as well, so typing "</" can complete a closing tag.
 */
private void installSmartMarkupAutoClose(final RSyntaxTextArea area) {
    area.addKeyListener(new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == '>') {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        insertMatchingMarkupCloseTag(area);
                    }
                });
            }
        }
    });
}

private void insertMatchingMarkupCloseTag(RSyntaxTextArea area) {
    if (area == null) return;

    String style = area.getSyntaxEditingStyle();
    boolean markup = SyntaxConstants.SYNTAX_STYLE_HTML.equals(style)
            || SyntaxConstants.SYNTAX_STYLE_XML.equals(style)
            || SyntaxConstants.SYNTAX_STYLE_PHP.equals(style);
    if (!markup) return;

    int caret = area.getCaretPosition();
    if (caret < 1) return;

    try {
        String allText = area.getText();
        if (caret > allText.length()) return;

        // The just-typed '>' is excluded while examining the opening tag.
        String before = allText.substring(0, caret - 1);
        int open = before.lastIndexOf('<');
        int close = before.lastIndexOf('>');
        if (open < 0 || open < close) return;

        String lowerBefore = before.toLowerCase();
        if (lowerBefore.lastIndexOf("<!--") > lowerBefore.lastIndexOf("-->")) return;
        int scriptOpen = lowerBefore.lastIndexOf("<script");
        int scriptClose = lowerBefore.lastIndexOf("</script");
        if (scriptOpen > scriptClose && scriptOpen != open) return;
        int styleOpen = lowerBefore.lastIndexOf("<style");
        int styleClose = lowerBefore.lastIndexOf("</style");
        if (styleOpen > styleClose && styleOpen != open) return;

        String tagText = before.substring(open + 1).trim();
        if (tagText.length() == 0 || tagText.charAt(0) == '/'
                || tagText.charAt(0) == '!' || tagText.charAt(0) == '?'
                || tagText.endsWith("/")) {
            return;
        }

        Matcher matcher = Pattern.compile("^([A-Za-z][A-Za-z0-9:_-]*)(?:\\s+.*)?$")
                .matcher(tagText);
        if (!matcher.matches()) return;

        String tagName = matcher.group(1);
        String lowerTag = tagName.toLowerCase();
        String[] voidTags = {
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
        };
        for (int i = 0; i < voidTags.length; i++) {
            if (voidTags[i].equals(lowerTag)) return;
        }

        String after = allText.substring(caret).toLowerCase();
        if (after.matches("(?s)^\\s*</\\s*" + Pattern.quote(lowerTag) + "\\s*>.*")) return;

        area.getDocument().insertString(caret, "</" + tagName + ">", null);
        area.setCaretPosition(caret);
    } catch (BadLocationException ex) {
        // Ignore an invalid caret/document position; normal typing continues.
    }
}

// color sytax
private void setEditorLanguage(
        File file,
        RSyntaxTextArea area) {

    if (file == null) {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_NONE
        );
        return;
    }

    String name = file.getName().toLowerCase();

    if (name.endsWith(".java")) {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_JAVA
        );
    }
    else if (name.endsWith(".html") ||
             name.endsWith(".htm") ||
             name.endsWith(".xhtml")) {

        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_HTML
        );
    }
    else if (name.endsWith(".css")) {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_CSS
        );
    }
    else if (name.endsWith(".js") || name.endsWith(".mjs") || name.endsWith(".cjs")) {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
        );
    }
    else if (name.endsWith(".xml")) {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_XML
        );
    }
    else if (name.endsWith(".sql")) {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_SQL
        );
    }
    else if (name.endsWith(".json")) {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_JSON
        );
    }
    else if (name.endsWith(".php")) {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_PHP
        );
    }
    else if (name.endsWith(".py")) {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_PYTHON
        );
    }
    else {
        area.setSyntaxEditingStyle(
            SyntaxConstants.SYNTAX_STYLE_NONE
        );
    }
}

// set syntax colors for dark mode

private void applyDarkEditorTheme(RSyntaxTextArea area, RTextScrollPane scroll) {

//    area.setBackground(new Color(5, 12, 30));
    area.setBackground(BG);
    area.setForeground(new Color(220, 228, 240));
    area.setCaretColor(new Color(0, 210, 238));

    // GUTTER / LINE NUMBERS
    scroll.getGutter().setBackground(new Color(30, 30, 30));
    scroll.getGutter().setLineNumberColor(new Color(130, 140, 155));
    scroll.getGutter().setCurrentLineNumberColor(new Color(0, 210, 238));
    scroll.getGutter().setBorderColor(new Color(35, 55, 80));        

    // Selected text
    area.setSelectionColor(new Color(38, 79, 120));
    area.setSelectedTextColor(Color.WHITE);
    area.setRoundedSelectionEdges(false);

    // Current line
    area.setHighlightCurrentLine(true);
    area.setCurrentLineHighlightColor(new Color(32, 38, 46));
    area.setFadeCurrentLineHighlight(false);

    // Bracket matching
    area.setBracketMatchingEnabled(true);
    area.setMatchedBracketBGColor(new Color(35, 75, 105));
    area.setMatchedBracketBorderColor(new Color(0, 180, 220));

    // Syntax highlighting
    SyntaxScheme scheme = area.getSyntaxScheme();

    scheme.getStyle(TokenTypes.IDENTIFIER).foreground =
        new Color(220, 228, 240);

    scheme.getStyle(TokenTypes.RESERVED_WORD).foreground =
        new Color(86, 156, 214);

    scheme.getStyle(TokenTypes.RESERVED_WORD).font =
    new Font("Consolas", Font.BOLD, 16);

    scheme.getStyle(TokenTypes.RESERVED_WORD_2).foreground =
        new Color(86, 156, 214);

    scheme.getStyle(TokenTypes.RESERVED_WORD_2).font =
        new Font("Consolas", Font.BOLD, 16);

    scheme.getStyle(TokenTypes.DATA_TYPE).foreground =
        new Color(78, 201, 176);

    scheme.getStyle(TokenTypes.FUNCTION).foreground =
        new Color(220, 220, 170);

    scheme.getStyle(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE).foreground =
        new Color(206, 145, 120);

    scheme.getStyle(TokenTypes.LITERAL_CHAR).foreground =
        new Color(206, 145, 120);

    scheme.getStyle(TokenTypes.LITERAL_NUMBER_DECIMAL_INT).foreground =
        new Color(181, 206, 168);

    scheme.getStyle(TokenTypes.LITERAL_NUMBER_FLOAT).foreground =
        new Color(181, 206, 168);

    scheme.getStyle(TokenTypes.LITERAL_BOOLEAN).foreground =
        new Color(86, 156, 214);

    scheme.getStyle(TokenTypes.COMMENT_EOL).foreground =
        new Color(106, 153, 85);

    scheme.getStyle(TokenTypes.COMMENT_MULTILINE).foreground =
        new Color(106, 153, 85);

    scheme.getStyle(TokenTypes.COMMENT_DOCUMENTATION).foreground =
        new Color(106, 153, 85);

    scheme.getStyle(TokenTypes.OPERATOR).foreground =
        new Color(212, 212, 212);

    scheme.getStyle(TokenTypes.SEPARATOR).foreground =
        new Color(212, 212, 212);

    scheme.getStyle(TokenTypes.ANNOTATION).foreground =
        new Color(78, 201, 176);

    scheme.getStyle(TokenTypes.VARIABLE).foreground =
        new Color(156, 220, 254);

    scheme.getStyle(TokenTypes.MARKUP_TAG_NAME).foreground =
        new Color(86, 156, 214);

    scheme.getStyle(TokenTypes.MARKUP_TAG_ATTRIBUTE).foreground =
        new Color(156, 220, 254);

    scheme.getStyle(TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE).foreground =
        new Color(206, 145, 120);

    scheme.getStyle(TokenTypes.MARKUP_TAG_DELIMITER).foreground =
        new Color(128, 128, 128);

    area.setSyntaxScheme(scheme);
}


private JFileChooser createThemedFileChooser() {

    JFileChooser chooser = new JFileChooser();

    Color bg = BG;
    Color panel = SIDEBAR;
    Color text = TEXT;
    Color muted = MUTED;
    Color border = BORDER;

    chooser.setBackground(bg);
    chooser.setForeground(text);
    chooser.setFont(new Font("Segoe UI", Font.PLAIN, 13));

    // Apply theme recursively to chooser components
    styleChooserComponents(chooser, bg, panel, text, muted, border);

    chooser.updateUI();

    // Apply again after UI initialization
    chooser.setBackground(bg);
    styleChooserComponents(chooser, bg, panel, text, muted, border);

    return chooser;
}

private void styleChooserComponents(
        Component component,
        Color bg,
        Color panel,
        Color text,
        Color muted,
        Color border) {

    component.setForeground(text);

    if (component instanceof JPanel) {
        component.setBackground(panel);
    }

    if (component instanceof JList) {
        JList<?> list = (JList<?>) component;
        list.setBackground(bg);
        list.setForeground(text);
        list.setSelectionBackground(BLUE);
        list.setSelectionForeground(Color.WHITE);
    }

    if (component instanceof JTable) {
        JTable table = (JTable) component;
        table.setBackground(bg);
        table.setForeground(text);
        table.setSelectionBackground(BLUE);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(border);
    }

    if (component instanceof JTree) {
        JTree tree = (JTree) component;
        tree.setBackground(bg);
        tree.setForeground(text);
    }

    if (component instanceof JTextField) {
        JTextField field = (JTextField) component;
        field.setBackground(bg);
        field.setForeground(text);
        field.setCaretColor(text);
        field.setBorder(BorderFactory.createLineBorder(border));
    }

    if (component instanceof JComboBox) {
        JComboBox<?> combo = (JComboBox<?>) component;
        combo.setBackground(panel);
        combo.setForeground(text);
    }

    if (component instanceof JScrollPane) {
        JScrollPane scroll = (JScrollPane) component;
        scroll.setBackground(bg);
        scroll.getViewport().setBackground(bg);
        scroll.setBorder(BorderFactory.createLineBorder(border));
    }

    if (component instanceof JViewport) {
        component.setBackground(bg);
    }

    if (component instanceof JButton) {
        JButton button = (JButton) component;
        button.setBackground(panel);
        button.setForeground(text);
        button.setFocusPainted(false);
    }

    if (component instanceof JLabel) {
        component.setForeground(text);
    }

    if (component instanceof Container) {
        for (Component child :
                ((Container) component).getComponents()) {
            styleChooserComponents(child, bg, panel, text, muted, border);
        }
    }
}

// for saving history
    private void saveRecentProject(File folder) {

        if (folder == null || !folder.isDirectory()) {
            return;
        }

        String path = folder.getAbsolutePath();

        recentProjects.remove(path);
        recentProjects.add(0, path);

        // Keep the latest 15 projects
        while (recentProjects.size() > 15) {
            recentProjects.remove(recentProjects.size() - 1);
        }

        StringBuilder data = new StringBuilder();

        for (String project : recentProjects) {
            if (data.length() > 0) {
                data.append("|");
            }

            data.append(project);
        }

        projectPreferences.put(RECENT_PROJECTS_KEY, data.toString());
        projectPreferences.put(LAST_PROJECT_KEY, path);
    }
    
    private void loadRecentProjects() {

    recentProjects.clear();

    String saved = projectPreferences.get(
        RECENT_PROJECTS_KEY, ""
    );

    if (saved.isEmpty()) {
        return;
    }

    String[] projects = saved.split("\\|");

    for (String path : projects) {

        File folder = new File(path);

        if (folder.isDirectory() && !recentProjects.contains(path)) {
            recentProjects.add(path);
        }
    }
  }
    
private void removeRecentProject(String path) {

    recentProjects.remove(path);

    StringBuilder data = new StringBuilder();

    for (String project : recentProjects) {

        if (data.length() > 0) {
            data.append("|");
        }

        data.append(project);
    }

    projectPreferences.put(RECENT_PROJECTS_KEY, data.toString());

    if (path.equals(projectPreferences.get(LAST_PROJECT_KEY, ""))) {
        projectPreferences.remove(LAST_PROJECT_KEY);
    }
}

private void restoreLastProject() {

    loadRecentProjects();

    String lastProject = projectPreferences.get(
        LAST_PROJECT_KEY, ""
    );

    if (lastProject.isEmpty()) {
        return;
    }

    File folder = new File(lastProject);

    if (folder.isDirectory()) {
        openWorkspace(folder);
    }
}

private void openWorkspace(File folder) {

    if (folder == null || !folder.isDirectory()) {
        return;
    }

    try {
        workspaceFolder = folder.getCanonicalFile();
    } catch (IOException ex) {
        showError(ex);
        return;
    }

    saveRecentProject(workspaceFolder);
    explorerActionsVisible = false;
    explorerTree = null;
    explorerTreeModel = null;
    workspaceRoot = null;
    createWorkspaceTree();
    showExplorer();

    // Refresh an existing Welcome tab without creating another one.
    if (welcomePage != null) {
        int welcomeIndex = editorTabs.indexOfComponent(welcomePage);
        if (welcomeIndex >= 0) {
            welcomePage = createWelcomePage();
            editorTabs.setComponentAt(welcomeIndex, welcomePage);
            editorTabs.setTabComponentAt(
                welcomeIndex, createTabHeader("Welcome", welcomePage)
            );
        }
    }
}
// to save docuemnt

private void installEditorContextMenu(RSyntaxTextArea area) {

    JPopupMenu popup = new JPopupMenu();

    // Undo
    JMenuItem undoItem = new JMenuItem("Undo");
    undoItem.setAccelerator(
        KeyStroke.getKeyStroke("control Z")
    );

    undoItem.addActionListener(e -> {
        updateActiveEditor();
        undo();
    });

    popup.add(undoItem);

    // Redo
    JMenuItem redoItem = new JMenuItem("Redo");
    redoItem.setAccelerator(
        KeyStroke.getKeyStroke("control Y")
    );

    redoItem.addActionListener(e -> {
        updateActiveEditor();
        redo();
    });

    popup.add(redoItem);

    popup.addSeparator();

    // Cut
    JMenuItem cutItem = new JMenuItem("Cut");
    cutItem.setAccelerator(
        KeyStroke.getKeyStroke("control X")
    );
    cutItem.addActionListener(e -> area.cut());
    popup.add(cutItem);

    // Copy
    JMenuItem copyItem = new JMenuItem("Copy");
    copyItem.setAccelerator(
        KeyStroke.getKeyStroke("control C")
    );
    copyItem.addActionListener(e -> area.copy());
    popup.add(copyItem);

    // Paste
    JMenuItem pasteItem = new JMenuItem("Paste");
    pasteItem.setAccelerator(
        KeyStroke.getKeyStroke("control V")
    );
    pasteItem.addActionListener(e -> area.paste());
    popup.add(pasteItem);

    popup.addSeparator();

    // Select All
    JMenuItem selectAllItem = new JMenuItem("Select All");
    selectAllItem.setAccelerator(
        KeyStroke.getKeyStroke("control A")
    );
    selectAllItem.addActionListener(e -> area.selectAll());
    popup.add(selectAllItem);

    // Right-click listener
    area.addMouseListener(new MouseAdapter() {

        private void showPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {

                area.requestFocusInWindow();

                updateActiveEditor();

                boolean hasSelection =
                    area.getSelectionStart() !=
                    area.getSelectionEnd();

                cutItem.setEnabled(
                    hasSelection && area.isEditable()
                );

                copyItem.setEnabled(hasSelection);

                pasteItem.setEnabled(area.isEditable());

                undoItem.setEnabled(
                    undoManager != null && undoManager.canUndo()
                );

                redoItem.setEnabled(
                    undoManager != null && undoManager.canRedo()
                );

                popup.show(area, e.getX(), e.getY());
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }
    });
}
// to close the opened file folder from tree

// Publish the opened workspace to GitHub using Git for Windows.
private void publishProjectToGitHub() {
    if (workspaceFolder == null || !workspaceFolder.isDirectory()) {
        JOptionPane.showMessageDialog(this,
            "Open a project folder before publishing it.",
            "No Project Open", JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    int openPage = JOptionPane.showConfirmDialog(
        this,
        "GitHub will open in your browser. Create an EMPTY repository "
        + "(do not add a README, license, or .gitignore), then copy its URL.\n\nContinue?",
        "Publish Project to GitHub",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.INFORMATION_MESSAGE);
    if (openPage != JOptionPane.YES_OPTION) return;

    try {
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("Browser integration is not available on this computer.");
        }
        Desktop.getDesktop().browse(new java.net.URI("https://github.com/new"));
    } catch (Exception ex) {
        showError(ex);
        return;
    }

    String remoteUrl = JOptionPane.showInputDialog(
        this, "Paste the new GitHub repository URL (HTTPS or SSH):",
        "GitHub Repository URL", JOptionPane.PLAIN_MESSAGE);
    if (remoteUrl == null) return;
    remoteUrl = remoteUrl.trim();
    if (!(remoteUrl.startsWith("https://github.com/")
            || remoteUrl.startsWith("git@github.com:"))) {
        JOptionPane.showMessageDialog(this,
            "Enter a GitHub URL such as https://github.com/username/project.git",
            "Invalid Repository URL", JOptionPane.WARNING_MESSAGE);
        return;
    }

    final String finalRemoteUrl = remoteUrl;
    final File project = workspaceFolder;

    final JDialog progress = new JDialog(this, "Publishing to GitHub", false);
    JTextArea output = new JTextArea(10, 55);
    output.setEditable(false);
    output.setBackground(BG);
    output.setForeground(TEXT);
    output.setFont(new Font("Consolas", Font.PLAIN, 12));
    progress.add(new JScrollPane(output));
    progress.pack();
    progress.setLocationRelativeTo(this);

    new SwingWorker<String, String>() {
        @Override
        protected String doInBackground() throws Exception {
            GitResult version = runGit(project, "--version");
            if (version.exitCode != 0) {
                throw new IOException("Git was not found. Install Git for Windows and make sure git.exe is on PATH.");
            }
            publish(version.output);

            GitResult check = runGit(project, "rev-parse", "--is-inside-work-tree");
            if (check.exitCode != 0) {
                GitResult init = runGit(project, "init");
                publish(init.output);
                if (init.exitCode != 0) throw new IOException(init.output);
            }

            GitResult add = runGit(project, "add", "-A");
            publish(add.output);
            if (add.exitCode != 0) throw new IOException(add.output);

            GitResult commit = runGit(project, "commit", "-m", "Publish from ICode");
            publish(commit.output);
            if (commit.exitCode != 0
                    && !commit.output.toLowerCase().contains("nothing to commit")) {
                throw new IOException(commit.output);
            }

            GitResult branch = runGit(project, "branch", "-M", "main");
            publish(branch.output);
            if (branch.exitCode != 0) throw new IOException(branch.output);

            GitResult remote = runGit(project, "remote", "get-url", "origin");
            GitResult setRemote = remote.exitCode == 0
                ? runGit(project, "remote", "set-url", "origin", finalRemoteUrl)
                : runGit(project, "remote", "add", "origin", finalRemoteUrl);
            publish(setRemote.output);
            if (setRemote.exitCode != 0) throw new IOException(setRemote.output);

            GitResult push = runGit(project, "push", "-u", "origin", "main");
            publish(push.output);
            if (push.exitCode != 0) throw new IOException(push.output);

            return "Project successfully pushed to GitHub.\n\n"
                + "For a static HTML/CSS/JavaScript website, open the repository's "
                + "Settings > Pages and select Deploy from a branch, branch main, folder /(root).";
        }

        @Override
        protected void process(List<String> chunks) {
            for (String chunk : chunks) output.append(chunk + "\n");
            output.setCaretPosition(output.getDocument().getLength());
        }

        @Override
        protected void done() {
            progress.dispose();
            try {
                JOptionPane.showMessageDialog(ICode.this, get(),
                    "GitHub Publish Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                showError(new Exception(cause.getMessage()));
            }
        }
    }.execute();

    progress.setVisible(true);
}

private static class GitResult {
    int exitCode;
    String output;
    GitResult(int exitCode, String output) {
        this.exitCode = exitCode;
        this.output = output;
    }
}

private GitResult runGit(File directory, String... arguments)
        throws IOException, InterruptedException {
    List<String> command = new ArrayList<String>();
    command.add("git");
    for (String argument : arguments) command.add(argument);

    ProcessBuilder builder = new ProcessBuilder(command);
    builder.directory(directory);
    builder.redirectErrorStream(true);
    Process process = builder.start();

    StringBuilder result = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) result.append(line).append("\n");
    }
    return new GitResult(process.waitFor(), result.toString());
}

private void closeWorkspace() {

    if (workspaceFolder == null) {
        return;
    }

    int choice = JOptionPane.showConfirmDialog(
        this,
        "Close the current workspace folder?\n"
        + "Files and folders on your computer will NOT be deleted.\n"
        + "The workspace remains in Recent Projects.",
        "Close Folder",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );

    if (choice != JOptionPane.YES_OPTION) {
        return;
    }

    workspaceFolder = null;
    explorerTree = null;
    explorerTreeModel = null;
    workspaceRoot = null;
    explorerActionsVisible = true;

    showExplorer();

    // Refresh the Welcome page if it is currently open.
    if (welcomePage != null) {
        int welcomeIndex = editorTabs.indexOfComponent(welcomePage);
        if (welcomeIndex >= 0) {
            welcomePage = createWelcomePage();
            editorTabs.setComponentAt(welcomeIndex, welcomePage);
            editorTabs.setTabComponentAt(
                welcomeIndex, createTabHeader("Welcome", welcomePage)
            );
        }
    }
}


// ================= CLOSE EDITOR TAB =================

private void closeTab(int index) {

    if (index < 0 || index >= editorTabs.getTabCount()) {
        return;
    }

    // Protect unsaved work before closing a document tab.
    Component closingComponent = editorTabs.getComponentAt(index);
    if (closingComponent instanceof JScrollPane) {
        EditorDocument pending = (EditorDocument)
            ((JScrollPane) closingComponent).getClientProperty("editorDocument");
        if (pending != null && pending.dirty) {
            String[] choices = {"Save", "Discard", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                this,
                "This file has unsaved changes. Save before closing?",
                "Unsaved Changes",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                choices,
                choices[0]);
            if (choice == 0) {
                saveEditorDocument(pending, false, true);
                if (pending.dirty) return;
            } else if (choice != 1) {
                return;
            }
        }
    }
    if (closingComponent instanceof JScrollPane) {
        EditorDocument closingDocument = (EditorDocument)
            ((JScrollPane) closingComponent).getClientProperty("editorDocument");
        if (closingDocument != null && closingDocument.autoSaveTimer != null) {
            closingDocument.autoSaveTimer.stop();
        }
    }
    if (closingComponent == welcomePage) {
        welcomePage = null;
    }
    editorTabs.removeTabAt(index);

    // Update the active editor
    if (editorTabs.getTabCount() > 0) {
        updateActiveEditor();
    } else {
        // No tabs remain
        activeDocument = null;
        t = null;
        currentFile = null;
        undoManager = new UndoManager();

        if (fileName != null) {
            fileName.setText("  No File Open");
        }
    }
}

// kill terminal
private void killSelectedTerminal() {

    int selected = terminalTabs.getSelectedIndex();

    if (selected < 0) {
        Toolkit.getDefaultToolkit().beep();
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(
        this,
        "This will permanently terminate this terminal and clear its data.\nContinue?",
        "Kill Terminal",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (confirm != JOptionPane.YES_OPTION) {
        return;
    }

    JPanel panel = (JPanel) terminalTabs.getComponentAt(selected);

    TerminalSession session =
        (TerminalSession) panel.getClientProperty("terminalSession");

    if (session != null) {

        session.killed = true;

        if (session.process != null) {
            session.process.destroyForcibly();
            session.process = null;
        }

        session.output.setText("");
        session.input.setText("");
    }
    if (session != null && session.tabButton != null) {
        terminalTabButtons.remove(session.tabButton);
    }
    terminalTabs.removeTabAt(selected);
    
    terminalTabButtons.revalidate();
    terminalTabButtons.repaint();

    updateTerminalTabButtons();
    if (terminalTabs.getTabCount() == 0) {
        terminalPanel.setVisible(false);
        if (bottomPanel != null &&
                (findPanel == null || !findPanel.isVisible())) {
            bottomPanel.setVisible(false);
        }
    }

    updateTerminalAddButton();

    terminalPanel.revalidate();
    terminalPanel.repaint();
    editorPanel.revalidate();
    editorPanel.repaint();
}

private void updateTerminalTabButtons() {

    if (terminalTabButtons == null || terminalTabs == null) {
        return;
    }

    for (int i = 0; i < terminalTabs.getTabCount(); i++) {

        JPanel panel = (JPanel) terminalTabs.getComponentAt(i);

        TerminalSession session =
            (TerminalSession) panel.getClientProperty(
                "terminalSession"
            );

        if (session != null && session.tabButton != null) {

            boolean selected =
                terminalTabs.getSelectedComponent() == panel;

            session.tabButton.setBackground(
                selected ? new Color(30, 30, 30) : PANEL
            );

            session.tabButton.setForeground(
                selected ? Color.WHITE : MUTED
            );

            session.tabButton.setBorder(
                BorderFactory.createMatteBorder(
                    0, 0, selected ? 2 : 0, 0,
                    selected ? BLUE : PANEL
                )
            );
        }
    }
}

private ImageIcon createTintedIcon(String iconFile, Color tint) {

    String resourcePath =
            "/icode/resources/" + iconFile;

    java.net.URL iconURL = getClass().getResource(resourcePath);

    if (iconURL == null) {
        System.out.println("ICON NOT FOUND: " + resourcePath);
        return null;
    }

    try {
        BufferedImage original = ImageIO.read(iconURL);

        BufferedImage tinted = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {

                int pixel = original.getRGB(x, y);

                int alpha = (pixel >> 24) & 0xff;

                if (alpha == 0) {
                    continue;
                }

                int newPixel =
                        (alpha << 24) |
                        (tint.getRed() << 16) |
                        (tint.getGreen() << 8) |
                        tint.getBlue();

                tinted.setRGB(x, y, newPixel);
            }
        }

        Image scaled = tinted.getScaledInstance(
                30, 30, Image.SCALE_SMOOTH
        );

        return new ImageIcon(scaled);

    } catch (Exception ex) {
        ex.printStackTrace();
        return null;
    }
}
private JPanel createFindPanel() {

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(PANEL);
    panel.setBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER)
    );

    JPanel findBar = new JPanel(
        new FlowLayout(FlowLayout.LEFT, 8, 9)
    );
    findBar.setBackground(PANEL);

    JLabel heading = new JLabel("FIND");
    heading.setForeground(TEXT);
    heading.setFont(new Font("Segoe UI", Font.BOLD, 12));

    searchField = new JTextField(28);
    searchField.setBackground(new Color(45, 45, 45));
    searchField.setForeground(TEXT);
    searchField.setCaretColor(Color.WHITE);
    searchField.setSelectionColor(new Color(38, 79, 120));
    searchField.setSelectedTextColor(Color.WHITE);
    searchField.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(5, 7, 5, 7)
        )
    );
    searchField.setPreferredSize(new Dimension(260, 30));

    JButton findButton = new JButton("Find");
    styleButton(findButton);
    findButton.setPreferredSize(new Dimension(65, 30));
    findButton.setMaximumSize(new Dimension(65, 30));

    findResultLabel = new JLabel(" ");
    findResultLabel.setForeground(MUTED);
    findResultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

    JButton closeButton = new JButton("×");
    closeButton.setForeground(TEXT);
    closeButton.setBackground(PANEL);
    closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 18));
    closeButton.setFocusPainted(false);
    closeButton.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
    closeButton.setContentAreaFilled(false);
    closeButton.setToolTipText("Close Find");
    closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    findButton.addActionListener(e -> performFind());
    searchField.addActionListener(e -> performFind());

    closeButton.addActionListener(e -> {
        findPanel.setVisible(false);
        if (bottomPanel != null) {
            bottomPanel.setVisible(false);
            bottomPanel.revalidate();
            bottomPanel.repaint();
        }
        if (t != null) {
            t.requestFocusInWindow();
        }
    });

    findBar.add(heading);
    findBar.add(searchField);
    findBar.add(findButton);
    findBar.add(findResultLabel);
    findBar.add(closeButton);

    panel.add(findBar, BorderLayout.NORTH);
    panel.setPreferredSize(new Dimension(0, 52));

    return panel;
}

private void performFind() {

    if (searchField == null) {
        return;
    }

    String query = searchField.getText();

    if (query == null || query.trim().isEmpty()) {
        findResultLabel.setText("Enter text to find");
        return;
    }

    if (t == null) {
        findResultLabel.setText("Open a document first");
        return;
    }

    String editorText = t.getText();
    int start = Math.max(0, t.getCaretPosition());
    int index = editorText.indexOf(query, start);

    if (index == -1 && start > 0) {
        index = editorText.indexOf(query);
    }

    if (index >= 0) {
        t.requestFocusInWindow();
        t.select(index, index + query.length());
        findResultLabel.setText("Match found");
    } else {
        findResultLabel.setText("No results found");
        Toolkit.getDefaultToolkit().beep();
    }
}

private void setupFindShortcut() {

    KeyStroke findKey = KeyStroke.getKeyStroke(
        KeyEvent.VK_F,
        InputEvent.CTRL_DOWN_MASK
    );

    getRootPane().getInputMap(
        JComponent.WHEN_IN_FOCUSED_WINDOW
    ).put(findKey, "open-find");

    getRootPane().getActionMap().put(
        "open-find",
        new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFindPanel();
            }
        }
    );
}

private void openFindPanel() {

    if (bottomPanel == null || findPanel == null) {
        return;
    }

    // Show Find in the same bottom area used by Terminal.
    terminalPanel.setVisible(false);
    bottomPanel.removeAll();
    bottomPanel.add(findPanel, BorderLayout.CENTER);
    bottomPanel.setPreferredSize(new Dimension(0, 52));
    findPanel.setVisible(true);
    bottomPanel.setVisible(true);

    bottomPanel.revalidate();
    bottomPanel.repaint();
    editorPanel.revalidate();
    editorPanel.repaint();

    SwingUtilities.invokeLater(() -> {
        searchField.requestFocusInWindow();
        searchField.selectAll();
    });
}
}

   

