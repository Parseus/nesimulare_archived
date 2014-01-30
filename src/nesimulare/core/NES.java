/*
 * The MIT License
 *
 * Copyright 2013-2014 Parseus.
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
package nesimulare.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.ReentrantLock;
import nesimulare.gui.*;
import nesimulare.core.audio.APU;
import nesimulare.core.boards.Board;
import nesimulare.core.cpu.CPU;
import nesimulare.core.input.Controllers;
import nesimulare.core.input.Joypad;
import nesimulare.core.memory.CPUMemory;
import nesimulare.core.memory.PPUMemory;
import nesimulare.core.ppu.PPU;
import nesimulare.core.ppu.PaletteGenerator;

/**
 * Main class for the emulation core.
 *
 * @author Parseus
 */
public class NES extends Thread {
    public CPU cpu;
    public APU apu;
    public PPU ppu;
    public CPUMemory cpuram;
    public PPUMemory ppuram;
    public Controllers controllers = new Controllers();
    public Board board;
    public Region.System region = Region.NTSC;
    public FrameLimiter frameLimiter = new FrameLimiter(this);
    public GUIImpl gui = new GUIImpl(this);
    public ROMLoader loader;
    public AudioInterface audio;
    private final ReentrantLock lock = new ReentrantLock();
    public int sampleRate;

    public long framecount;
    private boolean coreEnabled = true;
    public boolean runEmulation = false;
    private boolean softResetRequest = false;
    private boolean hardResetRequest = false;
    public boolean frameAdvance = false;
    private String curRomPath, curRomName;
    public static boolean LOGGING = false;
    public static final boolean INTERIM = true;

    /**
     * Constructor for this class, which also creates GUI. If GUI can't be loaded, informs about it and shuts down the entire program.
     */
    public NES() {
        super("Emulation core");

        try {
            java.awt.EventQueue.invokeAndWait(gui);
        } catch (InterruptedException | InvocationTargetException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "Could not initialize GUI: " + e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Initializes components of a console. Memory needs to be initialized first, because other components depend on them.
     */
    public void initialize() {
        //Memory first
        cpuram = new CPUMemory(this);
        ppuram = new PPUMemory(this);

        cpuram.initialize();
        ppuram.initialize();

        frameLimiter = new FrameLimiter(this);
        cpu = new CPU(region, this);
        apu = new APU(region, cpu, this);
        ppu = new PPU(region, this, cpu, ppuram);

        generatePalette();
        ppu.initialize();
        apu.initialize();
        board.initialize();
        cpu.initialize();

        setupPlayback();
    }

    /**
     * Startsthe emulated console with a given ROM.
     *
     * @param romtoload Filename of ROM to be loaded
     */
    public void run(final String romtoload) {
        //set thread priority higher than the interface thread
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);

        curRomPath = romtoload;
        loadROM(romtoload);

        new Thread(this).start();
    }

    /**
     * Sends request for hard reset and stops an emulation.
     */
    public void hardReset() {
        hardResetRequest = true;
        framecount = 0;
        runEmulation = false;
    }

    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    private synchronized void _hardReset() {
        generatePalette();
        cpuram.hardReset();
        ppuram.hardReset();
        frameLimiter.hardReset();
        board.hardReset();
        apu.hardReset();
        cpu.hardReset();
        ppu.hardReset();
    }

    /**
     * Sends request for soft reset and stops an emulation.
     */
    public void softReset() {
        softResetRequest = true;
        framecount = 0;
        runEmulation = false;
    }

    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    private synchronized void _softReset() {
        board.softReset();
        apu.softReset();
        cpu.softReset();
    }

    /**
     * Starts console and performs every cycle in a loop.
     */
    @Override
    public synchronized void run() {
        if (board == null) {
            return;
        }

        lock.lock();
        try {
            while (coreEnabled) {
                if (runEmulation) {
                    if (LOGGING) {
                        try {
                            cpu.fw.write(cpu.getCPUState().toTraceEvent() + " CYC:" + ppu.hclock + " SL:" + ppu.vclock + "\n");

                            if (cpu.getCPUState().stepCounter == 0) {
                                cpu.fw.flush();
                            }
                        } catch (IOException ioe) {
                            messageBox("Cannot write to debug log: " + ioe.getMessage());
                        } finally {
                            System.err.println(cpu.getCPUState().toTraceEvent() + " " + ppu.hclock + " " + ppu.vclock);
                        }
                    }

                    cpu.cycle();
                } else {
                    if (frameLimiter != null) {
                        frameLimiter.sleepFixed();
                    }

                    if (softResetRequest) {
                        softResetRequest = false;
                        _softReset();
                        runEmulation = true;
                    } else if (hardResetRequest) {
                        hardResetRequest = false;
                        _hardReset();
                        runEmulation = true;
                    }

                    if (audio != null) {
                        audio.pause();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Connects emulation core with GUI.
     * 
     * @param gui       Emulator's GUI
     */
    public void setGUI(GUIImpl gui) {
        this.gui = gui;
    }

    /**
     * Connects emulation core with controllers.
     * 
     * @param joypad1       Joypad connected in port 1
     * @param joypad2       Joypad connected in port 2
     */
    public void setControllers(Joypad joypad1, Joypad joypad2) {
        controllers.joypad1 = joypad1;
        controllers.joypad2 = joypad2;
    }

    /**
     * Returns a joypad connected in port 1.
     * 
     * @return      Joypad connected in port 1
     */
    public Joypad getJoypad1() {
        return controllers.joypad1;
    }

    /**
     * Returns a joypad connected in port 2.
     * 
     * @return      Joypad connected in port 2
     */
    public Joypad getJoypad2() {
        return controllers.joypad2;
    }

    /**
     * Sets region of the emulated core (consoles from different regions have different timings).
     * 
     * @param region        Emulated region of the console
     */
    public void setRegion(nesimulare.core.Region.System region) {
        this.region = region;
        hardReset();
    }

    /**
     * Renders audio and video after completing a frame.
     * Also limits framerate if frame limiter is enabled.
     * 
     * @param gui       GUI, which will be using rendered audio and video
     */
    public void finishFrame(GUIInterface gui) {
        gui.setFrame(ppu.screen);

        if (audio != null) {
            audio.resume();
            apu.finishFrame();
        }

        if (frameLimiter != null) {
            frameLimiter.sleep();
        }

        if ((framecount & 2047) == 0) {
            saveSRAM(true);
        }

        ++framecount;

        if (frameAdvance) {
            frameAdvance = false;
            runEmulation = false;
        }
    }

    /**
     * Displays a message box with a message.
     * 
     * @param message       Message to be shown by a message box
     */
    public void messageBox(final String message) {
        gui.messageBox(message);
    }

    /**
     * Generates an internal palette for the PPU.
     */
    public void generatePalette() {
        ppu.setupPalette(PaletteGenerator.generattePalette());
    }

    /**
     * Given a filename, loads a ROM.
     * 
     * @param filename      Fileame of ROM to be loaded
     */
    private void loadROM(final String filename) {
        runEmulation = false;
        coreEnabled = false;
        interrupt();

        while (lock.isLocked()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                messageBox(ex.getMessage());
            }
        }

        if (Tools.exists(filename) && (Tools.getExtension(filename).equalsIgnoreCase(".nes"))) {
            if (apu != null) {
                //if rom already running save its sram before closing
                saveSRAM(false);
                apu = null;
                //also get rid of mapper etc.
                board = null;
                cpu = null;
                cpuram = null;
                ppu = null;
                ppuram = null;
            }

            loader = new ROMLoader(filename, gui);
            board = loader.loadROM();

            if (board != null) {
                board.setCore(this);
                initialize();
                framecount = 0;

                if (loader.hasSRAM()) {
                    loadSRAM();
                }

                curRomPath = filename;
                curRomName = Tools.getFilenamefromPath(filename);
                runEmulation = true;
            }

            coreEnabled = true;
        } else {
            gui.messageBox("Could not load file:\nFile " + filename + "\n"
                    + "does not exist or is not a valid NES game.");
        }
    }

    /**
     * Saves a battery-backed save RAM.
     * 
     * @param async         If true, SRAM is saved with an asynchronous file writer.
     *                      If false, SRAM is saved with a normal file writer.
     */
    public void saveSRAM(final boolean async) {
        if (board != null && loader.hasSRAM()) {
            if (async) {
                Tools.asyncwritetofile(board.getSRAM(), Tools.stripExtension(curRomPath) + ".sav");
            } else {
                Tools.writetofile(board.getSRAM(), Tools.stripExtension(curRomPath) + ".sav");
            }
        }
    }

    /**
     * Loads a battery-backed save RAM.
     */
    private void loadSRAM() {
        final String name = Tools.stripExtension(curRomPath) + ".sav";

        if (Tools.exists(name) && loader.hasSRAM()) {
            board.setSRAM(Tools.readfromfile(name));
        }
    }

    /**
     * Returns name of a currently loaded ROM.
     * 
     * @return      Name of a currently loaded ROM.
     */
    public final String getCurrentRomName() {
        return curRomName;
    }

    /**
     * Toggles frame limiter on/off.
     */
    public void toggleFrameLimiter() {
        frameLimiter.enabled ^= true;
    }

    /**
     * Sets up an audio playback depending on a sample rate selected in general options.
     */
    public void setupPlayback() {
        sampleRate = PrefsSingleton.get().getInt("sampleRate", 44100);

        if (audio != null) {
            audio.destroy();
        }

        audio = new Audio(this, sampleRate);
        apu.setupPlayback();
    }
}