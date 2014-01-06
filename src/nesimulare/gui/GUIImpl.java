/*
 * The MIT License
 *
 * Copyright 2013 Parseus.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package nesimulare.gui;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import nesimulare.core.NES;
import nesimulare.core.Region;
import nesimulare.core.input.Joypad;

/**
 *
 * @author Parseus
 */
public class GUIImpl extends JFrame implements GUIInterface {
    
    NES nes;
    Joypad joypad1, joypad2;
    private final Listener listener = new Listener();
    final DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
    final Date date = new Date();
    private Canvas canvas;
    private BufferStrategy buffer;
    private Renderer renderer;
    private BufferedImage screen;
    private GraphicsDevice gd;
    private int screenScaleFactor;
    private boolean bilinearFiltering, inFullScreen = false;
    private static final int NES_HEIGHT = 224; 
    private static final int NES_WIDTH = 256;
    
    public GUIImpl(NES nes) {
        super();

        this.nes = nes;
        joypad1 = new Joypad(this, 1);
        joypad2 = new Joypad(this, 2);
        nes.setControllers(joypad1, joypad2);
        joypad1.startEventQueue();
        joypad2.startEventQueue();
    }
    
    public synchronized void setRenderOptions() {
        if (canvas != null) {
            this.remove(canvas);
        }
        
        screenScaleFactor = PrefsSingleton.get().getInt("screenScaling", 2);
        bilinearFiltering = PrefsSingleton.get().getBoolean("bilinearFiltering", false);
        renderer = new RGBRenderer();
        
        // Create canvas for painting
        canvas = new Canvas();
        canvas.setSize(NES_WIDTH * screenScaleFactor, NES_HEIGHT * screenScaleFactor);
        canvas.setEnabled(false); //otherwise it steals input events.
        // Add canvas to game window
        this.add(canvas);
        this.pack();
        canvas.createBufferStrategy(2);
        buffer = canvas.getBufferStrategy();
    }
    
    @Override
    public final synchronized void setFrame(int[][] frame) {
        final double fps = 1.0 / nes.frameLimiter.currentFrameTime;
        this.setTitle(String.format("NESimulare (%s) - %s, %2.2f fps",
            dateFormat.format(date), nes.getCurrentRomName(), fps));
        
        screen = renderer.render(frame);
        render();
    }

    @Override
    public void messageBox(final String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    @Override
    public synchronized void run() {
        //construct window
        this.setTitle("NESimulare (" + dateFormat.format(date) + ")");
        this.setResizable(false);
        
        buildMenus();
        setRenderOptions();
        
        this.getRootPane().registerKeyboardAction(listener, "Escape",
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.getRootPane().registerKeyboardAction(listener, "Toggle Fullscreen",
                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.getRootPane().registerKeyboardAction(listener, "Quit",
                KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.addWindowListener(listener);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.setVisible(true);
        // Create BackBuffer

        //now add the drag and drop handler.
        final TransferHandler handler = new TransferHandler() {
            @Override
            public boolean canImport(final TransferHandler.TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(final TransferHandler.TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                
                Transferable t = support.getTransferable();
                
                try {
                    File toload = (File) ((java.util.List) t.getTransferData(DataFlavor.javaFileListFlavor)).get(0);
                    loadROM(toload.getCanonicalPath());
                } catch (UnsupportedFlavorException | IOException e) {
                    return false;
                }
                
                return true;
            }
        };
        
        this.setTransferHandler(handler);
    }
    
    public void loadROM() {
        FileDialog fileDialog = new FileDialog(this);
        fileDialog.setMode(FileDialog.LOAD);
        fileDialog.setTitle("Select a ROM to load");
        //should open last folder used, and if that doesn't exist, the folder it's running in
        final String path = PrefsSingleton.get().get("filePath", System.getProperty("user.dir", ""));
        final File startDirectory = new File(path);
        
        if (startDirectory.isDirectory()) {
            fileDialog.setDirectory(path);
        }

        fileDialog.setFilenameFilter(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                final String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".nes");   
            }
        });
        
        boolean wasInFullScreen = false;
        
        if (inFullScreen) {
            wasInFullScreen = true;
            //load dialog won't show if we are in full screen, so this fixes for now.
            toggleFullScreen();
        }
        
        fileDialog.setVisible(true);
        
        if (fileDialog.getFile() != null) {
            PrefsSingleton.get().put("filePath", fileDialog.getDirectory());
            loadROM(fileDialog.getDirectory() + fileDialog.getFile());
        }
        
        if (wasInFullScreen) {
            toggleFullScreen();
        }
    }
    
    public void buildMenus() {
        JMenuBar menus = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem item;
        file.add(item = new JMenuItem("Open ROM"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        file.addSeparator();

        file.add(item = new JMenuItem("Toggle Fullscreen"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        item.addActionListener(listener);
        menus.add(file);

        file.add(item = new JMenuItem("Quit"));
        item.addActionListener(listener);
        menus.add(file);

        JMenu nesmenu = new JMenu("NES");
        nesmenu.add(item = new JMenuItem("Reset"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        nesmenu.add(item = new JMenuItem("Hard Reset"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        
        nesmenu.addSeparator();
        
        JMenu region = new JMenu("Region");
        ButtonGroup group = new ButtonGroup();
        region.add(item = new JRadioButtonMenuItem("NTSC", true));
        item.addItemListener(listener);
        group.add(item);
        
        region.add(item = new JRadioButtonMenuItem("PAL", false));
        item.addItemListener(listener);
        group.add(item);
        
        region.add(item = new JRadioButtonMenuItem("Dendy", false));
        item.addItemListener(listener);
        group.add(item);
        
        nesmenu.add(region);
        
        nesmenu.addSeparator();
        
        nesmenu.add(item = new JCheckBoxMenuItem("Toggle frame limiter", true));
        item.addItemListener(listener);
        
        menus.add(nesmenu);
        
        JMenu options = new JMenu("Options");
        options.add(item = new JMenuItem("General..."));
        item.addActionListener(listener);

        options.addSeparator();
        
        options.add(item = new JMenuItem("Controllers..."));
        item.addActionListener(listener);
        
        menus.add(options);

        JMenu debug = new JMenu("Debug");
        debug.add(item = new JCheckBoxMenuItem("Enable logging", false));
        item.addItemListener(listener);
        menus.add(debug);

        JMenu help = new JMenu("Help");
        help.add(item = new JMenuItem("About"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        menus.add(help);
        this.setJMenuBar(menus);
    }

    private double getmaxscale(final int width, final int height) {
        return Math.min(height / (double) NES_HEIGHT, width / (double) NES_WIDTH);
    }
    
    @Override
    public final synchronized void render() {
        final Graphics graphics = buffer.getDrawGraphics();
        
        if (bilinearFiltering) {
            ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        
        if (inFullScreen) {
            graphics.setColor(Color.BLACK);
            final DisplayMode dm = gd.getDisplayMode();
            final int scrnheight = dm.getHeight();
            final int scrnwidth = dm.getWidth();
            canvas.setSize(scrnwidth, scrnheight);
            graphics.fillRect(0, 0, scrnwidth, scrnheight);
            
            if (PrefsSingleton.get().getBoolean("maintainAspect", true)) {
                double scalefactor = getmaxscale(scrnwidth, scrnheight);
                int height = (int) (NES_HEIGHT * scalefactor);
                int width = (int) (256 * scalefactor * 1.1666667);
                graphics.drawImage(screen, ((scrnwidth / 2) - (width / 2)),
                        ((scrnheight / 2) - (height / 2)),
                        width, height, null);
            } else {
                graphics.drawImage(screen, 0, 0, scrnwidth, scrnheight, null);
            }
            
            graphics.setColor(Color.DARK_GRAY);
            graphics.drawString(this.getTitle(), 16, 16);
        } else {
            graphics.drawImage(screen, 0, 0, NES_WIDTH * screenScaleFactor, NES_HEIGHT * screenScaleFactor, null);
        }

        graphics.dispose();
        buffer.show();
    }
    
    public void toggleFullScreen() {
        if (inFullScreen) {
            this.dispose();
            gd.setFullScreenWindow(null);
            canvas.setSize(NES_HEIGHT * screenScaleFactor, NES_WIDTH * screenScaleFactor);
            this.setUndecorated(false);
            this.setVisible(true);
            inFullScreen = false;
            buildMenus();
        } else {
            setJMenuBar(null);
            gd = getGraphicsConfiguration().getDevice();
            if (!gd.isFullScreenSupported()) {
                //then fullscreen will give a window the size of the screen instead
                messageBox("Fullscreen is not supported by your OS or version of Java.");
            }
            this.dispose();
            this.setUndecorated(true);

            gd.setFullScreenWindow(this);
            this.setVisible(true);

            inFullScreen = true;
        }
    }
    
    private void loadROM(String path) {
        nes.run(path);
    }
    
    private void showGeneralOptions() {
        final GeneralOptionsDialog dialog = new GeneralOptionsDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isOKClicked()) {
            setRenderOptions();
            
            if (nes.apu != null) {
                nes.apu.setupPlayback();
            }
        }
    }
    
    private void showControlsDialog() {
        final ControlsDialog dialog = new ControlsDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isOKClicked()) {
            joypad1.setButtons();
            joypad2.setButtons();
        }
    }
    
    class Listener implements ActionListener, WindowListener, ItemListener {

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            // placeholder for more robust handler
            switch (arg0.getActionCommand()) {
                case "Quit":
                    nes.saveSRAM(false);
                    System.exit(0);
                    break;
                case "Reset":
                    nes.softReset();
                    break;
                case "Hard Reset":
                    nes.hardReset();
                    break;
                case "About":
                    messageBox("NESimulare (" + dateFormat.format(date) + ")"
                            + "\n"
                            + "This program is free software licensed under the MIT License and comes with \n"
                            + "NO WARRANTY of any kind.");
                    break;
                case "Open ROM":
                    loadROM();
                    break;
                case "Toggle Fullscreen":
                    toggleFullScreen();
                    break;
                case "Escape":
                    if (inFullScreen) {
                        toggleFullScreen();
                    } else {
                        nes.saveSRAM(false);
                        System.exit(0);
                    }   break;
                case "Controllers...":
                    showControlsDialog();
                    break;
                case "General...":
                    showGeneralOptions();
                    break;
                default:
                    break;
            }
        }
        
        @Override
        public void itemStateChanged(ItemEvent ie) {
            Object source = ie.getSource();
            
            if (source instanceof JRadioButtonMenuItem) {
                JRadioButtonMenuItem rb = (JRadioButtonMenuItem)source;
                
                switch (rb.getText()) {
                    case "NTSC":
                        nes.setRegion(Region.NTSC);
                        break;
                    case "PAL":
                        nes.setRegion(Region.PAL);
                        break;
                    case "Dendy":
                        nes.setRegion(Region.DENDY);
                        break;
                    default:
                        break;
                }
            } else if (source instanceof JCheckBoxMenuItem) {
                JCheckBoxMenuItem cb = (JCheckBoxMenuItem)source;
                
                switch (cb.getText()) {
                    case "Enable logging":
                        NES.LOGGING = (ie.getStateChange() == ItemEvent.SELECTED);
                        break;
                    case "Toggle frame limiter":
                        nes.toggleFrameLimiter();
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public void windowOpened(WindowEvent e) {
            //Nothing to see here, move along
        }

        @Override
        public void windowClosing(WindowEvent e) {
            joypad1.stopEventQueue();
            joypad2.stopEventQueue();
            nes.saveSRAM(false);
            System.exit(0);
        }

        @Override
        public void windowClosed(WindowEvent e) {
            //Nothing to see here, move along
        }

        @Override
        public void windowIconified(WindowEvent e) {
            //Nothing to see here, move along
        }

        @Override
        public void windowDeiconified(WindowEvent e) {
            //Nothing to see here, move along
        }

        @Override
        public void windowActivated(WindowEvent e) {
            //Nothing to see here, move along
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            //Nothing to see here, move along
        }
    }
}