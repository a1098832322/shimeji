package com.group_finity.mascot;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;
import com.group_finity.mascot.sound.Sounds;
import com.group_finity.mascot.win.WindowsInteractiveWindowForm;
import com.joconner.i18n.Utf8ResourceBundleControl;
import com.wishes.constant.Constant;
import com.wishes.fix.OriginEngineFix;
import com.wishes.update.DownloadDialog;
import com.wishes.utils.FormatUtils;
import com.wishes.utils.UpdateChecker;
import org.apache.log4j.xml.DOMConfigurator;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Program entry point.
 * <p>
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 *
 * @modify: by Wishes 2018年10月
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());
    // Action that matches the "Gather Around Mouse!" context menu command
    static final String BEHAVIOR_GATHER = "ChaseMouse";

    static {
        try {
            //加载log4j
            DOMConfigurator.configureAndWatch("/log4j.xml");
            LogManager.getLogManager().readConfiguration(OriginEngineFix.getInstance().propertiesLoder(Constant.isDevEnvironment, "logging.properties"));
        } catch (final SecurityException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private final Manager manager = new Manager();
    private ArrayList<String> imageSets = new ArrayList<>();
    private Hashtable<String, Configuration> configurations = new Hashtable<>();
    private static Main instance = new Main();
    private Properties properties = new Properties();
    private Platform platform;
    private Properties languageBundle;

    private JDialog form;

    public static Main getInstance() {
        return instance;
    }

    private static JFrame frame = new JFrame();

    public static void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(final String[] args) {
        try {
            //检测更新
            new Thread(() -> {
                try {
                    if (UpdateChecker.checkUpdate(
                            UpdateChecker.CHECK_UPDATE_TYPE
                                    .MANUAL) != null) {
                        //显示更新提示框
                        new DownloadDialog();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    log.log(Level.SEVERE, "检测更新时请求数据失败！", e);
                }
            }).start();

            //实例化程序主体
            getInstance().run();
        } catch (OutOfMemoryError err) {
            log.log(Level.SEVERE, "Out of Memory Exception.  There are probably have too many "
                    + "Shimeji mascots in the image folder for your computer to handle.  Select fewer"
                    + " image sets or move some to the img/unused folder and try again.", err);
            Main.showError("Out of Memory.  There are probably have too many \n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the \n"
                    + "img/unused folder and try again.");
            System.exit(0);
        }
    }


    public void run() {
        // test operating system
        if (!System.getProperty("sun.arch.data.model").equals("64"))
            platform = Platform.x86;
        else
            platform = Platform.x86_64;

        //load setting
        properties = OriginEngineFix.getInstance().getInstanceSettingProp();

        // load langauges
        try {
            loadLanguageProp();
        } catch (Exception ex) {
            Main.showError("The default language file could not be loaded. Ensure that you have the latest shimeji language.properties in your conf directory.");
            exit();
        }

        //使用beauty eye皮肤包替代NimRODLookAndFeel皮肤包
        try {
            /* 设置皮肤属性 */
            BeautyEyeLNFHelper.frameBorderStyle = BeautyEyeLNFHelper.FrameBorderStyle.generalNoTranslucencyShadow;
            UIManager.put("RootPane.setupButtonVisible", false);
            BeautyEyeLNFHelper.translucencyAtFrameInactive = false;// 是否在窗口失焦时变成半透明状态
            org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.launchBeautyEyeLNF();// 加载皮肤
        } catch (Exception e) {
            log.log(Level.SEVERE, "Look & Feel unsupported.", e);
            exit();
        }

        // Get the image sets to use
        List<String> roleList = Arrays.asList(properties.getProperty("ActiveShimeji", "").split("/"));

        //填充完整路径
        roleList.forEach((s) -> {
                    s = OriginEngineFix.getInstance().getBASE_IMG_PATH() + s;
                    s = FormatUtils.formatImagePath(s);//格式化路径
                    imageSets.add(s);
                }
        );


        if (imageSets.get(0).trim().isEmpty()) {
            imageSets = new ImageSetChooser(frame, true).display();
            if (imageSets == null) {
                exit();
            }
        }

        // Load settings
        for (int index = 0; index < imageSets.size(); index++) {
            if (loadConfiguration(imageSets.get(index)) == false) {
                // failed validation
                configurations.remove(imageSets.get(index));
                imageSets.remove(imageSets.get(index));
                index--;
            }
        }
        if (imageSets.isEmpty()) {
            exit();
        }

        // 创建底部托盘图标
        createTrayIcon();

        // 创建第一个角色
        for (String imageSet : imageSets) {
            createMascot(imageSet);
        }

        getManager().start();
    }


    /**
     * 读取语言配置文件
     *
     * @throws IOException
     */
    private void loadLanguageProp() throws IOException {
        String languageFileName = "language_" + properties.getProperty("Language", "en-GB");
        FileInputStream input = (FileInputStream) OriginEngineFix.getInstance()
                .propertiesLoder(Constant.isDevEnvironment
                        , languageFileName.substring(0, languageFileName.indexOf("-"))
                                + ".properties");
        languageBundle = new Properties();
        languageBundle.load(new InputStreamReader(input, "utf-8"));
    }

    /**
     * 加载动作配置
     *
     * @param imageSet
     * @return
     */
    private boolean loadConfiguration(String imageSet) {
        try {
            String actionsFile = OriginEngineFix.getInstance()
                    .getBASE_ENVIRONMENT_PATH() + "actions.xml";
            if (new File(OriginEngineFix.getInstance()
                    .getBASE_ENVIRONMENT_PATH() + imageSet + "/actions.xml").exists()) {
                imageSet = OriginEngineFix.getInstance()
                        .getBASE_ENVIRONMENT_PATH() + imageSet;
                actionsFile = imageSet + "/actions.xml";
            } else if (new File(OriginEngineFix.getInstance().getBASE_IMG_PATH() + imageSet + "/conf/actions.xml").exists()) {
                imageSet = OriginEngineFix.getInstance().getBASE_IMG_PATH() + imageSet;
                actionsFile = imageSet + "/conf/actions.xml";
            } else {
                imageSet = FormatUtils.formatImagePath(imageSet);
            }

            log.log(Level.INFO, imageSet + " Read Action File ({0})", actionsFile);

            final Document actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new FileInputStream(new File(actionsFile)));

            Configuration configuration = new Configuration();
            configuration.load(new Entry(actions.getDocumentElement()), imageSet);

            String behaviorsFile = OriginEngineFix.getInstance()
                    .getBASE_ENVIRONMENT_PATH() + "behaviors.xml";
            if (new File(OriginEngineFix.getInstance()
                    .getBASE_ENVIRONMENT_PATH() + imageSet + "/behaviors.xml").exists()) {
                behaviorsFile = OriginEngineFix.getInstance()
                        .getBASE_ENVIRONMENT_PATH() + imageSet + "/behaviors.xml";
            } else if (new File(OriginEngineFix.getInstance().getBASE_IMG_PATH() + imageSet + "/conf/behaviors.xml").exists()) {
                behaviorsFile = OriginEngineFix.getInstance().getBASE_IMG_PATH() + imageSet + "/conf/behaviors.xml";
            }

            log.log(Level.INFO, imageSet + " Read Behavior File ({0})", behaviorsFile);

            final Document behaviors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new FileInputStream(new File(behaviorsFile)));

            configuration.load(new Entry(behaviors.getDocumentElement()), imageSet);

            configuration.validate();

            configurations.put(imageSet, configuration);

            // born mascot bit goes here...
            for (final Entry list : new Entry(actions.getDocumentElement()).selectChildren("ActionList")) {
                for (final Entry node : list.selectChildren("Action")) {
                    if (node.getAttributes().containsKey("BornMascot")) {
                        if (!configurations.containsKey(node.getAttribute("BornMascot"))) {
                            loadConfiguration(node.getAttribute("BornMascot"));
                        }
                    }
                }
            }

            return true;
        } catch (final IOException e) {
            log.log(Level.SEVERE, "Failed to load configuration files", e);
            Main.showError(languageBundle.getProperty("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
        } catch (final SAXException e) {
            log.log(Level.SEVERE, "Failed to load configuration files", e);
            Main.showError(languageBundle.getProperty("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
        } catch (final ParserConfigurationException e) {
            log.log(Level.SEVERE, "Failed to load configuration files", e);
            Main.showError(languageBundle.getProperty("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
        } catch (final ConfigurationException e) {
            log.log(Level.SEVERE, "Failed to load configuration files", e);
            Main.showError(languageBundle.getProperty("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Failed to load configuration files", e);
            Main.showError(languageBundle.getProperty("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
        }

        return false;
    }

    /**
     * Create a tray icon.
     *
     * @ Throws AWTException
     * @ Throws IOException
     */
    private void createTrayIcon() {
        log.log(Level.INFO, "create a tray icon");

        try {
            // Create the tray icon
            final TrayIcon icon = new TrayIcon(ImageIO.read(
                    new FileInputStream(OriginEngineFix.getInstance().getBASE_IMG_PATH()
                            + "icon.png")), languageBundle.getProperty("ShimejiEE"));

            // attach menu
            icon.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent event) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        // close the form if it's open
                        if (form != null)
                            form.dispose();

                        // create the form and border
                        form = new JDialog(frame, false);
                        final JPanel panel = new JPanel();
                        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
                        form.add(panel);

                        // buttons and action handling
                        JButton btnCallShimeji = new JButton(languageBundle.getProperty("CallShimeji"));
                        btnCallShimeji.addActionListener(new ActionListener() {
                            public void actionPerformed(final ActionEvent event) {
                                createMascot();
                                form.dispose();
                            }
                        });

                        JButton btnFollowCursor = new JButton(languageBundle.getProperty("FollowCursor"));
                        btnFollowCursor.addActionListener(new ActionListener() {
                            public void actionPerformed(final ActionEvent event) {
                                getManager().setBehaviorAll(BEHAVIOR_GATHER);
                                form.dispose();
                            }
                        });

                        JButton btnReduceToOne = new JButton(languageBundle.getProperty("ReduceToOne"));
                        btnReduceToOne.addActionListener(new ActionListener() {
                            public void actionPerformed(final ActionEvent event) {
                                getManager().remainOne();
                                form.dispose();
                            }
                        });

                        JButton btnRestoreWindows = new JButton(languageBundle.getProperty("RestoreWindows"));
                        btnRestoreWindows.addActionListener(new ActionListener() {
                            public void actionPerformed(final ActionEvent event) {
                                NativeFactory.getInstance().getEnvironment().restoreIE();
                                form.dispose();
                            }
                        });

                        final JButton btnAllowedBehaviours = new JButton(languageBundle.getProperty("AllowedBehaviours"));
                        btnAllowedBehaviours.addMouseListener(new MouseListener() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                            }

                            @Override
                            public void mousePressed(MouseEvent e) {
                            }

                            @Override
                            public void mouseReleased(MouseEvent e) {
                                btnAllowedBehaviours.setEnabled(true);
                            }

                            @Override
                            public void mouseEntered(MouseEvent e) {
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {
                            }
                        });
                        btnAllowedBehaviours.addActionListener(event1 -> {
                            // "Disable Breeding" menu item
                            final JCheckBoxMenuItem breedingMenu = new JCheckBoxMenuItem(languageBundle.getProperty("BreedingCloning"), Boolean.parseBoolean(properties.getProperty("Breeding", "true")));
                            breedingMenu.addItemListener(e -> {
                                if (Boolean.parseBoolean(properties.getProperty("Breeding", "true"))) {
                                    breedingMenu.setState(false);
                                    properties.setProperty("Breeding", "false");
                                } else {
                                    breedingMenu.setState(true);
                                    properties.setProperty("Breeding", "true");
                                }
                                NativeFactory.getInstance().getEnvironment().refreshCache();
                                try {
                                    FileOutputStream output = new FileOutputStream(OriginEngineFix.getInstance()
                                            .getBASE_ENVIRONMENT_PATH() + "settings.properties");
                                    try {
                                        properties.store(output, "Shimeji-ee Configuration Options");
                                    } finally {
                                        output.close();
                                    }
                                } catch (Exception unimportant) {
                                }
                                btnAllowedBehaviours.setEnabled(true);
                            });

                            // "Throwing Windows" menu item
                            final JCheckBoxMenuItem throwingMenu = new JCheckBoxMenuItem(languageBundle.getProperty("ThrowingWindows"), Boolean.parseBoolean(properties.getProperty("Throwing", "true")));
                            throwingMenu.addItemListener(new ItemListener() {
                                public void itemStateChanged(final ItemEvent e) {
                                    if (Boolean.parseBoolean(properties.getProperty("Throwing", "true"))) {
                                        throwingMenu.setState(false);
                                        properties.setProperty("Throwing", "false");
                                    } else {
                                        throwingMenu.setState(true);
                                        properties.setProperty("Throwing", "true");
                                    }
                                    NativeFactory.getInstance().getEnvironment().refreshCache();
                                    try {
                                        FileOutputStream output = new FileOutputStream(OriginEngineFix.getInstance()
                                                .getBASE_ENVIRONMENT_PATH() + "settings.properties");
                                        try {
                                            properties.store(output, "Shimeji-ee Configuration Options");
                                        } finally {
                                            output.close();
                                        }
                                    } catch (Exception unimportant) {
                                    }
                                    btnAllowedBehaviours.setEnabled(true);
                                }
                            });

                            // "Mute Sounds" menu item
                            final JCheckBoxMenuItem soundsMenu = new JCheckBoxMenuItem(languageBundle.getProperty("SoundEffects"), Boolean.parseBoolean(properties.getProperty("Sounds", "true")));
                            soundsMenu.addItemListener(new ItemListener() {
                                public void itemStateChanged(final ItemEvent e) {
                                    if (Boolean.parseBoolean(properties.getProperty("Sounds", "true"))) {
                                        soundsMenu.setState(false);
                                        properties.setProperty("Sounds", "false");
                                        Sounds.setMuted(true);
                                    } else {
                                        soundsMenu.setState(true);
                                        properties.setProperty("Sounds", "true");
                                        Sounds.setMuted(false);
                                    }
                                    NativeFactory.getInstance().getEnvironment().refreshCache();
                                    try {
                                        FileOutputStream output = new FileOutputStream(OriginEngineFix.getInstance()
                                                .getBASE_ENVIRONMENT_PATH() + "settings.properties");
                                        try {
                                            properties.store(output, "Shimeji-ee Configuration Options");
                                        } finally {
                                            output.close();
                                        }
                                    } catch (Exception unimportant) {
                                    }
                                    btnAllowedBehaviours.setEnabled(true);
                                }
                            });

                            JPopupMenu behaviourPopup = new JPopupMenu();
                            behaviourPopup.add(breedingMenu);
                            behaviourPopup.add(throwingMenu);
                            behaviourPopup.add(soundsMenu);
                            behaviourPopup.addPopupMenuListener(new PopupMenuListener() {
                                @Override
                                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                                }

                                @Override
                                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                                    if (panel.getMousePosition() != null) {
                                        btnAllowedBehaviours.setEnabled(!(panel.getMousePosition().x > btnAllowedBehaviours.getX() &&
                                                panel.getMousePosition().x < btnAllowedBehaviours.getX() + btnAllowedBehaviours.getWidth() &&
                                                panel.getMousePosition().y > btnAllowedBehaviours.getY() &&
                                                panel.getMousePosition().y < btnAllowedBehaviours.getY() + btnAllowedBehaviours.getHeight()));
                                    } else {
                                        btnAllowedBehaviours.setEnabled(true);
                                    }
                                }

                                @Override
                                public void popupMenuCanceled(PopupMenuEvent e) {
                                }
                            });
                            behaviourPopup.show(btnAllowedBehaviours, 0, btnAllowedBehaviours.getHeight());
                            btnAllowedBehaviours.requestFocusInWindow();
                        });

                        //检测更新
                        final JButton btnUpdate = new JButton(languageBundle.getProperty("check"));
                        btnUpdate.addActionListener(e -> {
                            form.dispose();
                            new DownloadDialog();

                        });

                        final JButton btnSettings = new JButton(languageBundle.getProperty("Settings"));
                        btnSettings.addMouseListener(new MouseListener() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                            }

                            @Override
                            public void mousePressed(MouseEvent e) {
                            }

                            @Override
                            public void mouseReleased(MouseEvent e) {
                                btnSettings.setEnabled(true);
                            }

                            @Override
                            public void mouseEntered(MouseEvent e) {
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {
                            }
                        });
                        btnSettings.addActionListener(e -> {
                            JMenuItem chooseShimejiMenu = new JMenuItem(languageBundle.getProperty("ChooseShimeji"));
                            chooseShimejiMenu.addActionListener(e1 -> {
                                form.dispose();
                                boolean isExit = getManager().isExitOnLastRemoved();
                                getManager().setExitOnLastRemoved(false);
                                getManager().disposeAll();

                                // Get the image sets to use
                                ArrayList<String> temporaryImageSet;
                                temporaryImageSet = new ImageSetChooser(frame, true).display();
                                if (temporaryImageSet != null) {
                                    imageSets = temporaryImageSet;
                                }

                                // Load settings
                                for (String imageSet : imageSets) {
                                    loadConfiguration(imageSet);
                                }

                                // Create the first mascot
                                for (String imageSet : imageSets) {
                                    createMascot(imageSet);
                                }

                                Main.this.getManager().setExitOnLastRemoved(isExit);
                            });

                            // "Interactive Windows" menu item
                            JMenuItem interactiveMenu = new JMenuItem(languageBundle.getProperty("ChooseInteractiveWindows"));
                            interactiveMenu.addActionListener(e12 -> {
                                form.dispose();
                                new WindowsInteractiveWindowForm(frame, true).display();
                                NativeFactory.getInstance().getEnvironment().refreshCache();
                            });

                            JPopupMenu settingsPopup = new JPopupMenu();
                            settingsPopup.add(chooseShimejiMenu);
                            settingsPopup.add(interactiveMenu);
                            settingsPopup.addPopupMenuListener(new PopupMenuListener() {
                                @Override
                                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                                }

                                @Override
                                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                                    if (panel.getMousePosition() != null) {
                                        btnSettings.setEnabled(!(panel.getMousePosition().x > btnSettings.getX() &&
                                                panel.getMousePosition().x < btnSettings.getX() + btnSettings.getWidth() &&
                                                panel.getMousePosition().y > btnSettings.getY() &&
                                                panel.getMousePosition().y < btnSettings.getY() + btnSettings.getHeight()));
                                    } else {
                                        btnSettings.setEnabled(true);
                                    }
                                }

                                @Override
                                public void popupMenuCanceled(PopupMenuEvent e) {
                                }
                            });
                            settingsPopup.show(btnSettings, 0, btnSettings.getHeight());
                            btnSettings.requestFocusInWindow();
                        });

                        //"i once decided it was time to write a long sentence but I wasn't sure how to start it so I just started writing about how I was going to write a long sentence but I wasn't sure how to start it so I just started writing"
                        final JButton btnLanguage = new JButton(languageBundle.getProperty("Language"));
                        btnLanguage.addMouseListener(new MouseListener() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                            }

                            @Override
                            public void mousePressed(MouseEvent e) {
                            }

                            @Override
                            public void mouseReleased(MouseEvent e) {
                                btnLanguage.setEnabled(true);
                            }

                            @Override
                            public void mouseEntered(MouseEvent e) {
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {
                            }
                        });
                        btnLanguage.addActionListener(new ActionListener() {
                            public void actionPerformed(final ActionEvent e) {
                                // English menu item
                                final JMenuItem englishMenu = new JMenuItem("English");
                                englishMenu.addActionListener(new ActionListener() {
                                    public void actionPerformed(final ActionEvent e) {
                                        form.dispose();
                                        if (!properties.getProperty("Language", "en-GB").equals("en-GB")) {
                                            properties.setProperty("Language", "en-GB");
                                            refreshLanguage();
                                        }
                                        NativeFactory.getInstance().getEnvironment().refreshCache();
                                        try {
                                            FileOutputStream output = new FileOutputStream(OriginEngineFix.getInstance()
                                                    .getBASE_ENVIRONMENT_PATH() + "settings.properties");
                                            try {
                                                properties.store(output, "Shimeji-ee Configuration Options");
                                            } finally {
                                                output.close();
                                            }
                                        } catch (Exception unimportant) {
                                        }
                                    }
                                });

                                // Chinese menu item
                                final JMenuItem chineseMenu = new JMenuItem("\u7b80\u4f53\u4e2d\u6587");
                                chineseMenu.addActionListener(new ActionListener() {
                                    public void actionPerformed(final ActionEvent e) {
                                        form.dispose();
                                        if (!properties.getProperty("Language", "en-GB").equals("zh-CN")) {
                                            properties.setProperty("Language", "zh-CN");
                                            refreshLanguage();
                                        }
                                        NativeFactory.getInstance().getEnvironment().refreshCache();
                                        try {
                                            FileOutputStream output = new FileOutputStream(OriginEngineFix.getInstance()
                                                    .getBASE_ENVIRONMENT_PATH() + "settings.properties");
                                            try {
                                                properties.store(output, "Shimeji-ee Configuration Options");
                                            } finally {
                                                output.close();
                                            }
                                        } catch (Exception unimportant) {
                                        }
                                    }
                                });


                                JPopupMenu languagePopup = new JPopupMenu();
                                languagePopup.add(englishMenu);
                                languagePopup.addSeparator();
                                languagePopup.add(chineseMenu);
                                languagePopup.addPopupMenuListener(new PopupMenuListener() {
                                    @Override
                                    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                                    }

                                    @Override
                                    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                                        if (panel.getMousePosition() != null) {
                                            btnLanguage.setEnabled(!(panel.getMousePosition().x > btnLanguage.getX() &&
                                                    panel.getMousePosition().x < btnLanguage.getX() + btnLanguage.getWidth() &&
                                                    panel.getMousePosition().y > btnLanguage.getY() &&
                                                    panel.getMousePosition().y < btnLanguage.getY() + btnLanguage.getHeight()));
                                        } else {
                                            btnLanguage.setEnabled(true);
                                        }
                                    }

                                    @Override
                                    public void popupMenuCanceled(PopupMenuEvent e) {
                                    }
                                });
                                languagePopup.show(btnLanguage, 0, btnLanguage.getHeight());
                                btnLanguage.requestFocusInWindow();
                            }
                        });

                        JButton btnDismissAll = new JButton(languageBundle.getProperty("DismissAll"));
                        btnDismissAll.addActionListener(e -> exit());

                        // layout
                        panel.setLayout(new java.awt.GridBagLayout());
                        GridBagConstraints gridBag = new GridBagConstraints();
                        gridBag.fill = GridBagConstraints.HORIZONTAL;
                        gridBag.gridx = 0;
                        gridBag.gridy = 0;
                        panel.add(btnCallShimeji, gridBag);
                        gridBag.insets = new Insets(5, 0, 0, 0);
                        gridBag.gridy++;
                        panel.add(btnFollowCursor, gridBag);
                        gridBag.gridy++;
                        panel.add(btnReduceToOne, gridBag);
                        gridBag.gridy++;
                        panel.add(btnRestoreWindows, gridBag);
                        gridBag.gridy++;
                        panel.add(new JSeparator(), gridBag);
                        gridBag.gridy++;
                        panel.add(btnAllowedBehaviours, gridBag);
                        gridBag.gridy++;
                        panel.add(btnSettings, gridBag);
                        gridBag.gridy++;
                        panel.add(btnLanguage, gridBag);
                        gridBag.gridy++;
                        panel.add(new JSeparator(), gridBag);
                        gridBag.gridy++;
                        panel.add(btnUpdate, gridBag);
                        gridBag.gridy++;
                        panel.add(btnDismissAll, gridBag);

                        try {
                            form.setIconImage(ImageIO.read(new FileInputStream(
                                    OriginEngineFix.getInstance()
                                            .getBASE_IMG_PATH() + "icon.png")));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        form.setTitle(languageBundle.getProperty("ShimejiEE"));
                        form.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                        form.setAlwaysOnTop(true);

                        // set the form width
                        java.awt.FontMetrics metrics = btnCallShimeji.getFontMetrics(btnCallShimeji.getFont());
                        int width = metrics.stringWidth(btnCallShimeji.getText());
                        width = Math.max(metrics.stringWidth(btnFollowCursor.getText()), width);
                        width = Math.max(metrics.stringWidth(btnReduceToOne.getText()), width);
                        width = Math.max(metrics.stringWidth(btnRestoreWindows.getText()), width);
                        width = Math.max(metrics.stringWidth(btnAllowedBehaviours.getText()), width);
                        width = Math.max(metrics.stringWidth(btnSettings.getText()), width);
                        width = Math.max(metrics.stringWidth(btnLanguage.getText()), width);
                        width = Math.max(metrics.stringWidth(btnDismissAll.getText()), width);
                        form.setMinimumSize(new Dimension(width + 80, 400));

                        // setting location of the form
                        form.setLocation(event.getPoint().x - form.getWidth(), event.getPoint().y - form.getHeight());

                        // make sure that it is on the screen if people are using exotic taskbar locations
                        Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                        if (form.getX() < screen.getX()) {
                            form.setLocation(event.getPoint().x, form.getY());
                        }
                        if (form.getY() < screen.getY()) {
                            form.setLocation(form.getX(), event.getPoint().y);
                        }
                        form.setVisible(true);
                    } else if (event.getButton() == MouseEvent.BUTTON1) {
                        //createMascot();//鼠标左键点击事件，创建一个新的对象
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });

            // Show tray icon
            SystemTray.getSystemTray().add(icon);
        } catch (final IOException e) {
            log.log(Level.SEVERE, "Failed to create tray icon", e);
            Main.showError(languageBundle.getProperty("FailedDisplaySystemTrayErrorMessage") + "\n" + languageBundle.getProperty("SeeLogForDetails"));
            exit();
        } catch (final AWTException e) {
            log.log(Level.SEVERE, "Failed to create tray icon", e);
            Main.showError(languageBundle.getProperty("FailedDisplaySystemTrayErrorMessage") + "\n" + languageBundle.getProperty("SeeLogForDetails"));
            exit();
        }
    }

    // Randomly creates a mascot
    public void createMascot() {
        int length = imageSets.size();
        int random = (int) (length * Math.random());
        createMascot(imageSets.get(random));
    }

    /**
     * Create a mascot
     */
    public void createMascot(String imageSet) {
        log.log(Level.INFO, "create a mascot");

        // Create one mascot
        final Mascot mascot = new Mascot(imageSet);

        // Create it outside the bounds of the screen
        mascot.setAnchor(new Point(-1000, -1000));

        // Randomize the initial orientation
        mascot.setLookRight(Math.random() < 0.5);

        try {
            mascot.setBehavior(getConfiguration(imageSet).buildBehavior(null, mascot));
            this.getManager().add(mascot);
        } catch (final BehaviorInstantiationException e) {
            log.log(Level.SEVERE, "Failed to initialize the first action", e);
            Main.showError(languageBundle.getProperty("FailedInitialiseFirstActionErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
            mascot.dispose();
        } catch (final CantBeAliveException e) {
            log.log(Level.SEVERE, "Fatal Error", e);
            Main.showError(languageBundle.getProperty("FailedInitialiseFirstActionErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
            mascot.dispose();
        } catch (Exception e) {
            log.log(Level.SEVERE, imageSet + " fatal error, can not be started.", e);
            Main.showError(languageBundle.getProperty("CouldNotCreateShimejiErrorMessage") + imageSet + ".\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
            mascot.dispose();
        }
    }

    private void refreshLanguage() {
        ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl(false);
        try {
            loadLanguageProp();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Fail to load Properties", e);
            e.printStackTrace();
        }
        boolean isExit = getManager().isExitOnLastRemoved();
        getManager().setExitOnLastRemoved(false);
        getManager().disposeAll();

        // Load settings
        for (String imageSet : imageSets) {
            loadConfiguration(imageSet);
        }

        // Create the first mascot
        for (String imageSet : imageSets) {
            createMascot(imageSet);
        }

        getManager().setExitOnLastRemoved(isExit);
    }

    public Configuration getConfiguration(String imageSet) {
        Configuration conf = configurations.get(imageSet);
        return conf;
    }

    private Manager getManager() {
        return this.manager;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Properties getProperties() {
        return properties;
    }

    public Properties getLanguageBundle() {
        return languageBundle;
    }

    public void exit() {
        this.getManager().disposeAll();
        this.getManager().stop();

        System.exit(0);
    }
}
