package com.group_finity.mascot;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.ConfigurationFiles;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.glados.GladosBehaviors;
import com.group_finity.mascot.glados.GladosEventReporter;
import com.group_finity.mascot.glados.GladosBridge;
import com.group_finity.mascot.glados.GladosOptions;
import com.group_finity.mascot.image.ImagePair;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;
import com.group_finity.mascot.sound.Sounds;
import com.group_finity.mascot.win.WindowsInteractiveWindowForm;
import com.joconner.i18n.Utf8ResourceBundleControl;
import com.wishes.constant.Constant;
import com.wishes.fix.OriginEngineFix;
import com.wishes.update.DownloadDialog;
import com.wishes.utils.FormatUtils;
import com.wishes.utils.UpdateChecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
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

/**
 * Program entry point.
 * <p>
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 *
 * @modify: by Wishes 2018年10月
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    // Action that matches the "Gather Around Mouse!" context menu command
    static final String BEHAVIOR_GATHER = "ChaseMouse";

    static {
        // 安装 SLF4JBridgeHandler 以捕获 java.util.logging 调用
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private final Manager manager = new Manager();
    private ArrayList<String> imageSets = new ArrayList<>();
    private Hashtable<String, Configuration> configurations = new Hashtable<>();
    private static Main instance = new Main();
    private Properties properties = new Properties();
    private Platform platform;
    private Properties languageBundle;

    private JDialog form;

    /** GLaDOS 启动参数（--glados-managed 等）。 */
    private GladosOptions gladosOptions = new GladosOptions(false, 0, null);

    /** GLaDOS 外部控制桥；仅受管模式启动。 */
    private GladosBridge gladosBridge;

    public static Main getInstance() {
        return instance;
    }

    private static JFrame frame = new JFrame();

    public static void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(final String[] args) {
        // 启用高 DPI 支持（Java 9+）
        // 必须在创建任何 Swing 组件之前设置
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("sun.java2d.dpiaware", "true");
        
        // 解析 GLaDOS 启动参数（必须在 run() 之前，供受管模式判断）
        // 用 parseAndMark 同步静态托管标记，深层调用栈（Configuration.isBehaviorEnabled）依赖它
        getInstance().gladosOptions = GladosOptions.parseAndMark(args);

        try {
            //检测更新（受管模式跳过：由 GLaDOS 管理生命周期，不自行更新）
            if (!getInstance().gladosOptions.isManaged()) {
                new Thread(() -> {
                    try {
                        if (UpdateChecker.checkUpdate(
                                UpdateChecker.CHECK_UPDATE_TYPE
                                        .MANUAL) != null) {
                            //显示更新提示框
                            new DownloadDialog();
                        }
                    } catch (java.net.SocketTimeoutException e) {
                        log.warn("检测更新时连接超时，已跳过更新检查", e);
                    } catch (java.net.ConnectException e) {
                        log.warn("检测更新时无法连接到服务器，已跳过更新检查", e);
                    } catch (IOException e) {
                        log.warn("检测更新时发生网络错误，已跳过更新检查: {}", e.getMessage(), e);
                    } catch (Exception e) {
                        log.error("检测更新时发生未知错误！", e);
                    }
                }).start();
            } else {
                log.info("GLaDOS managed mode: skip online update check");
            }

            //实例化程序主体
            getInstance().run();
        } catch (OutOfMemoryError err) {
            log.error("Out of Memory Exception.  There are probably have too many Shimeji mascots in the image folder for your computer to handle.", err);
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

        String recoveryHome = gladosOptions.getHome();
        java.nio.file.Path recoveryDir = recoveryHome != null && !recoveryHome.trim().isEmpty()
                ? java.nio.file.Paths.get(recoveryHome) : java.nio.file.Paths.get(System.getProperty("user.home"), ".shimeji");
        com.group_finity.mascot.mischief.RecoverableWindowMoves.initialize(recoveryDir.resolve("mischief-restore.json"));
        Runtime.getRuntime().addShutdownHook(new Thread(
                com.group_finity.mascot.mischief.RecoverableWindowMoves::shutdown, "mischief-restore"));

        // GLaDOS 托管模式：禁止自主增殖（SplitIntoTwo / PullUpShimeji）。
        if (gladosOptions.isManaged()) {
            properties.setProperty("Breeding", "false");
            log.info("GLaDOS managed mode: disable autonomous breeding");
        }

        if (Boolean.parseBoolean(properties.getProperty("MischiefMode", "false")))
            com.group_finity.mascot.mischief.MischiefMode.set(properties, true,
                Boolean.parseBoolean(properties.getProperty("Mischief.PushCursorEnabled", "false")));

        // load langauges
        try {
            loadLanguageProp();
        } catch (Exception ex) {
            Main.showError("The default language file could not be loaded. Ensure that you have the latest shimeji language.properties in your conf directory.");
            exit();
        }

        // 设置 LookAndFeel
        try {
            // 使用系统默认 LookAndFeel，确保最佳兼容性
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            log.info("Using system LookAndFeel: {}", UIManager.getLookAndFeel().getClass().getName());
        } catch (Exception e) {
            log.warn("Failed to set system LookAndFeel, using cross-platform default. Error: {}", e.getMessage());
            // 如果失败，使用跨平台默认 LookAndFeel
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                log.info("Using cross-platform LookAndFeel: {}", UIManager.getLookAndFeel().getClass().getName());
            } catch (Exception ex) {
                log.error("Failed to set any LookAndFeel", ex);
            }
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

        // 合并 GLaDOS AI 行为映射表（受管模式才需要；非受管模式保持原版行为树）
        if (gladosOptions.isManaged()) {
            for (String imageSet : imageSets) {
                Configuration config = configurations.get(imageSet);
                if (config != null) {
                    GladosBehaviors.loadInto(config, gladosOptions.getHome());
                }
            }
        }

        // 创建底部托盘图标（受管模式跳过：不显示设置窗口/托盘菜单入口）
        if (!gladosOptions.isManaged()) {
            createTrayIcon();
        }

        // 创建第一个角色
        for (String imageSet : imageSets) {
            createMascot(imageSet);
        }

        getManager().start();

        // 受管模式：启动 GLaDOS 外部控制桥
        if (gladosOptions.isManaged()) {
            // 受管模式下禁止"清空角色即退出"：setRole 会先 disposeAll 再 createMascot，
            // 若 tick 线程在中间检测到 0 角色会误触发退出。退出统一由 quit 命令控制。
            getManager().setExitOnLastRemoved(false);
            // 交互事件（点击）上报给宿主：GLaDOS 读到后唤起桌宠对话
            GladosEventReporter.configure(gladosOptions.getHome());
            gladosBridge = new GladosBridge(gladosOptions, manager);
            gladosBridge.start();
            log.info("GLaDOS managed mode enabled (port={}, home={})",
                    gladosOptions.getPort(), gladosOptions.getHome());
        }
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
    /**
     * 加载指定形象的动作/行为配置（含 GLaDOS AI 行为映射）。
     * <p>
     * 受管模式下由 GladosBridge 在切换角色前调用，确保目标角色配置已加载。
     *
     * @param imageSet 完整形象路径
     * @return 是否加载成功
     */
    /** 三级路径回退的解析结果；与改造前的 {@code loadConfiguration} 逻辑保持一致。 */
    private static final class ConfigPaths {
        final String actionsFile;
        final String behaviorsFile;
        final String effectiveImageSet;
        /** FR-10：选中皮肤级文件时，先把全局主文件作为基础层加载；否则为 null。 */
        final String baseActionsFile;
        final String baseBehaviorsFile;

        ConfigPaths(String actionsFile, String behaviorsFile, String effectiveImageSet,
                    String baseActionsFile, String baseBehaviorsFile) {
            this.actionsFile = actionsFile;
            this.behaviorsFile = behaviorsFile;
            this.effectiveImageSet = effectiveImageSet;
            this.baseActionsFile = baseActionsFile;
            this.baseBehaviorsFile = baseBehaviorsFile;
        }
    }

    /**
     * 解析动作/行为文件路径（全局 conf 与皮肤级 conf 的三级回退）。
     * 抽出来是为了让启动加载与显式重载（FR-03）走完全相同的路径规则。
     */
    private ConfigPaths resolveConfigPaths(String imageSet) {
        final String baseEnv = OriginEngineFix.getInstance().getBASE_ENVIRONMENT_PATH();
        final String baseImg = OriginEngineFix.getInstance().getBASE_IMG_PATH();

        final String globalActions = baseEnv + "actions.xml";
        final String globalBehaviors = baseEnv + "behaviors.xml";

        String effective = imageSet;
        String actionsFile = globalActions;
        if (new File(baseEnv + imageSet + "/actions.xml").exists()) {
            effective = baseEnv + imageSet;
            actionsFile = effective + "/actions.xml";
        } else if (new File(baseImg + imageSet + "/conf/actions.xml").exists()) {
            effective = baseImg + imageSet;
            actionsFile = effective + "/conf/actions.xml";
        } else {
            effective = FormatUtils.formatImagePath(imageSet);
        }

        String behaviorsFile = globalBehaviors;
        if (new File(baseEnv + effective + "/behaviors.xml").exists()) {
            behaviorsFile = baseEnv + effective + "/behaviors.xml";
        } else if (new File(baseImg + effective + "/conf/behaviors.xml").exists()) {
            behaviorsFile = baseImg + effective + "/conf/behaviors.xml";
        }

        // FR-10：皮肤级文件从「整份替换」改为「全局基础 + 皮肤增量」；
        // 两者指向同一全局文件时 base 为 null，避免重复加载（ConfigurationFiles 另有一层去重）
        final String baseActions = actionsFile.equals(globalActions) ? null : globalActions;
        final String baseBehaviors = behaviorsFile.equals(globalBehaviors) ? null : globalBehaviors;
        return new ConfigPaths(actionsFile, behaviorsFile, effective, baseActions, baseBehaviors);
    }

    public boolean loadConfiguration(String imageSet) {
        try {
            final ConfigPaths paths = resolveConfigPaths(imageSet);
            log.info("{} Read Action File ({})", paths.effectiveImageSet, paths.actionsFile);
            log.info("{} Read Behavior File ({})", paths.effectiveImageSet, paths.behaviorsFile);

            // FR-01：主文件 + actions.d/*.xml + behaviors.d/*.xml，全部加载完才统一 validate
            ConfigurationFiles.Result result = ConfigurationFiles.build(
                    paths.actionsFile, paths.behaviorsFile, paths.effectiveImageSet,
                    paths.baseActionsFile, paths.baseBehaviorsFile);

            configurations.put(paths.effectiveImageSet, result.getConfiguration());

            // born mascot bit goes here...（FR-01：动作包片段里声明的 BornMascot 同样生效）
            for (String born : result.getBornMascots()) {
                if (!configurations.containsKey(born)) {
                    loadConfiguration(born);
                }
            }

            return true;
        } catch (final IOException e) {
            log.error("Failed to load configuration files", e);
            Main.showError(languageBundle.getProperty("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
        } catch (final SAXException e) {
            log.error("Failed to load configuration files", e);
            Main.showError(languageBundle.getProperty("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
        } catch (final ParserConfigurationException e) {
            log.error("Failed to load configuration files", e);
            Main.showError(languageBundle.getProperty("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
        } catch (final ConfigurationException e) {
            log.error("Failed to load configuration files", e);
            Main.showError(languageBundle.getProperty("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
        } catch (final Exception e) {
            log.error("Failed to load configuration files", e);
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
        log.info("create a tray icon");

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
                    // macOS 上 isPopupTrigger() 不会触发，需要同时检查鼠标按钮
                    if (event.isPopupTrigger() || event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.BUTTON3) {
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
                        // 注意：TrayIcon 的 MouseEvent.getPoint() 返回的是相对坐标，不是屏幕绝对坐标
                        // 需要使用 MouseInfo 获取鼠标的屏幕绝对位置
                        Point mousePoint = java.awt.MouseInfo.getPointerInfo().getLocation();
                        
                        // 计算表单显示位置：默认在鼠标左上方
                        int formX = mousePoint.x - form.getWidth();
                        int formY = mousePoint.y - form.getHeight();
                        
                        form.setLocation(formX, formY);

                        // make sure that it is on the screen if people are using exotic taskbar locations
                        // 使用 DPIScaler 获取正确的屏幕边界（逻辑像素）
                        Rectangle screen = com.group_finity.mascot.util.DPIScaler.getLogicalWorkAreaBounds();
                        if (form.getX() < screen.getX()) {
                            form.setLocation(mousePoint.x, form.getY());
                        }
                        if (form.getY() < screen.getY()) {
                            form.setLocation(form.getX(), mousePoint.y);
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
            log.error("Failed to create tray icon", e);
            Main.showError(languageBundle.getProperty("FailedDisplaySystemTrayErrorMessage") + "\n" + languageBundle.getProperty("SeeLogForDetails"));
            exit();
        } catch (final AWTException e) {
            log.error("Failed to create tray icon", e);
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
        createMascotInstance(imageSet, null, "primary");
    }

    public Mascot createMascotInstance(String imageSet, Point anchor, String origin) {
        log.info("create a mascot");

        // Create one mascot
        final Mascot mascot = new Mascot(imageSet);
        mascot.setOrigin(origin);

        // 设置初始位置：X=10, Y=-1000（屏幕外顶部）
        // Shimeji 会从屏幕顶部掉落下来
        mascot.setAnchor(anchor == null ? new Point(10, -1000) : anchor);

        // Randomize the initial orientation
        mascot.setLookRight(Math.random() < 0.5);

        try {
            Configuration config = getConfiguration(imageSet);
            if (config == null) {
                log.error("Configuration not found for imageSet: {}", imageSet);
                Main.showError(languageBundle.getProperty("CouldNotCreateShimejiErrorMessage") + imageSet + ".\n" + languageBundle.getProperty("SeeLogForDetails"));
                mascot.dispose();
                return null;
            }
            mascot.setBehavior(config.buildNextBehavior(null, mascot));
            // buildNextBehavior may relocate an off-screen mascot; restore the requested clone anchor.
            if (anchor != null) mascot.setAnchor(new Point(anchor));
            if ("clone".equals(origin)) {
                int limit;
                try { limit = Math.max(1, Integer.parseInt(properties.getProperty("Glados.MaxInstances", "8"))); }
                catch (NumberFormatException e) { limit = 8; }
                if (!this.getManager().addIfBelowLimit(mascot, limit)) { mascot.dispose(); return null; }
            } else this.getManager().add(mascot);
            
            // 仅在设置了 -Ddebugwindow=true 时自动打开 DebugWindow
            if (com.wishes.constant.Constant.autoOpenDebugWindow) {
                mascot.openDebugWindow();
            }
        } catch (final BehaviorInstantiationException e) {
            log.error("Failed to initialize the first action", e);
            Main.showError(languageBundle.getProperty("FailedInitialiseFirstActionErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
            mascot.dispose();
        } catch (final CantBeAliveException e) {
            log.error("Fatal Error", e);
            Main.showError(languageBundle.getProperty("FailedInitialiseFirstActionErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
            mascot.dispose();
        } catch (Exception e) {
            log.error("{} fatal error, can not be started.", imageSet, e);
            Main.showError(languageBundle.getProperty("CouldNotCreateShimejiErrorMessage") + imageSet + ".\n" + e.getMessage() + "\n" + languageBundle.getProperty("SeeLogForDetails"));
            mascot.dispose();
        }
        return mascot.getManager() == null ? null : mascot;
    }

    private void refreshLanguage() {
        ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl(false);
        try {
            loadLanguageProp();
        } catch (IOException e) {
            log.error("Fail to load Properties", e);
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

    /**
     * 显式重载所有已加载角色的动作/行为配置（FR-03 / FR-04）。
     * <p>
     * 需求文档：docs/action-authoring-extensibility-requirements.md §5.1
     * <p>
     * 关键点：图片是在 {@code AnimationBuilder} 构造（即配置构建）时预加载的，
     * 且 {@code ImagePairLoader.load} 对已缓存路径会直接短路返回。因此必须
     * <b>先清缓存、再构建</b>，否则新增或替换的 PNG 不会生效（D-3）。
     * 为保证 AC-03-5「失败保留旧配置可用」，先取缓存快照，任一环节失败即整体回滚；
     * 旧 {@code configurations} 与现有角色在此之前完全没有被触碰。
     * <p>
     * 不读语言配置、不写 {@code settings.properties}、不弹任何窗口（AC-03-4）。
     *
     * @return {@code null} 表示成功；否则为面向调用方的错误描述
     */
    public String reloadConfigurations() {
        if (imageSets.isEmpty()) {
            return "no image set loaded";
        }
        final Map<String, ImagePair> snapshot = ImagePairs.snapshot();
        final Map<String, Configuration> fresh = new LinkedHashMap<>();

        try {
            for (String imageSet : imageSets) {
                log.info("Reload: clearing cached images for {}", imageSet);
                ImagePairs.clearForImageSet(imageSet);
            }

            for (String imageSet : imageSets) {
                final ConfigPaths paths = resolveConfigPaths(imageSet);
                final ConfigurationFiles.Result result = ConfigurationFiles.build(
                        paths.actionsFile, paths.behaviorsFile, paths.effectiveImageSet,
                        paths.baseActionsFile, paths.baseBehaviorsFile);

                // 托管模式的 AI 行为表必须每次重载后重新合并，
                // 否则 glados-behaviors.xml 的 Frequency=0 禁增殖覆盖会丢失（NFR-02）
                if (gladosOptions.isManaged()) {
                    GladosBehaviors.loadInto(result.getConfiguration(), gladosOptions.getHome());
                }

                // 兼容 createMascot(imageSet) 与皮肤级 effectiveImageSet 两种取法
                fresh.put(imageSet, result.getConfiguration());
                if (!paths.effectiveImageSet.equals(imageSet)) {
                    fresh.put(paths.effectiveImageSet, result.getConfiguration());
                }
            }
        } catch (final Exception e) {
            ImagePairs.restore(snapshot);
            log.error("Reload failed, previous configuration and image cache restored: {}", e.toString(), e);
            return "failed to reload configuration: " + e.getMessage();
        }

        configurations.putAll(fresh);

        // 重建角色时禁止「清空即退出」（与 refreshLanguage / 托管启动的既有保护一致）
        final boolean exitOnLast = getManager().isExitOnLastRemoved();
        getManager().setExitOnLastRemoved(false);
        try {
            getManager().disposeAll();
            for (String imageSet : imageSets) {
                createMascot(imageSet);
            }
        } finally {
            getManager().setExitOnLastRemoved(exitOnLast);
        }
        log.info("Reload: applied configuration for {} image set(s)", imageSets.size());
        return null;
    }

    private Manager getManager() {
        return this.manager;
    }

    /**
     * 获取 GLaDOS 控制桥（仅受管模式非 null）。
     */
    public GladosBridge getGladosBridge() {
        return gladosBridge;
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

    /**
     * 无 GUI 工具（设计器 / headless 校验）初始化语言包与基础路径。
     * <p>
     * 设计文档：docs/skin-action-designer-design.md §3.2、§5.4。
     * <p>
     * 引擎的 {@code ActionBuilder} / {@code BehaviorBuilder} / {@code AnimationBuilder}
     * 在抛错时会读 {@code Main.getInstance().getLanguageBundle()}。设计器只做 dry-run 校验、
     * 不起 Swing，因此需要一条不触碰 EDT 的初始化路径：本方法只读
     * {@code settings.properties} 与 {@code language_*.properties}，不创建窗口、不写回配置。
     * 正常运行时不会被调用，行为零变化（SD-NFR-1）。
     */
    public void initForHeadlessTooling() {
        try {
            if (properties == null || properties.isEmpty()) {
                properties = OriginEngineFix.getInstance().getInstanceSettingProp();
            }
            if (languageBundle == null) {
                try {
                    loadLanguageProp();
                } catch (Exception e) {
                    // 非 dev 模式下 loadLanguageProp 读的是 ./conf/language_xx.properties；
                    // 设计器/单测可能以别的 cwd 启动，这里退回 classpath 里的同一份文案，
                    // 保证引擎异常信息仍可读（不影响任何运行时行为）。
                    languageBundle = loadBundledLanguage();
                }
            }
        } catch (Exception e) {
            log.warn("Headless tooling init failed (diagnostics text may be less readable): {}", e.toString());
        }
    }

    private Properties loadBundledLanguage() {
        String lang = properties == null ? "en-GB" : properties.getProperty("Language", "en-GB");
        String code = lang.contains("-") ? lang.substring(0, lang.indexOf('-')) : lang;
        Properties fallback = new Properties();
        for (String candidate : new String[]{"language_" + code + ".properties", "language_en.properties"}) {
            try (InputStream in = Main.class.getResourceAsStream("/conf/" + candidate)) {
                if (in != null) {
                    fallback.load(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                    log.info("Headless tooling: loaded bundled language bundle {}", candidate);
                    return fallback;
                }
            } catch (IOException e) {
                log.warn("Headless tooling: failed to load bundled {}: {}", candidate, e.toString());
            }
        }
        log.warn("Headless tooling: no bundled language bundle for language '{}'", lang);
        return fallback;
    }

    public void exit() {
        com.group_finity.mascot.mischief.MischiefMode.set(properties, false, false);
        com.group_finity.mascot.mischief.RecoverableWindowMoves.shutdown();
        // 先停桥，避免 GLaDOS 侧收到端口文件后连不上
        if (gladosBridge != null) {
            gladosBridge.stop();
            gladosBridge = null;
        }
        this.getManager().disposeAll();
        this.getManager().stop();

        System.exit(0);
    }
}
