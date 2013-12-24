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

package nesimulare.core;

import java.io.IOException;
import nesimulare.gui.FrameLimiter;
import nesimulare.gui.GUIImpl;
import nesimulare.gui.GUIInterface;
import nesimulare.gui.ROMLoader;
import nesimulare.gui.Tools;
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
 *
 * @author Parseus
 */
public class NES implements Runnable {
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
    private ROMLoader loader;
    
    public boolean enableFrameLimiter = false;
    public long frameStartTime, framecount, frameDoneTime;
    public boolean runEmulation = false;
    private boolean softResetRequest = false;
    private boolean hardResetRequest = false;
    private String curRomPath, curRomName;
    public static final boolean LOGGING = false;
    
    public NES() {
        try {
            java.awt.EventQueue.invokeAndWait(gui);
        } catch (InterruptedException e) {
            System.err.println("Could not initialize GUI. Exiting.");
            System.exit(-1);
        } catch (java.lang.reflect.InvocationTargetException f) {
            System.err.println(f.getCause().toString());
            System.exit(-1);
        }
    }
    
    public void initialize() {
        //Memory first
        cpuram = new CPUMemory(this);
        ppuram = new PPUMemory(this);  
        cpu = new CPU(region, this);
        apu = new APU(region, cpu, this);
        ppu = new PPU(region, this, cpu, ppuram);
        
        generatePalette();
        ppu.initialize();
        apu.initialize();
        board.initialize();
        cpu.initialize();
    }
    
     public void run(final String romtoload) {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
        //set thread priority higher than the interface thread
        curRomPath = romtoload;
        loadROM(romtoload);
        run();
    }
    
//    Runnable render = new Runnable() {
//        @Override
//        public void run() {
//            gui.render();
//            Thread.yield();
//        }
//    };
    
    public void hardReset() {
        hardResetRequest = true;
        framecount = 0;
        runEmulation = false;
    }
    
    private void _hardReset() {
        generatePalette();
        cpuram.hardReset();
        ppuram.hardReset();
        board.hardReset();
        apu.hardReset();
        cpu.hardReset();
        ppu.hardReset();
    }
    
    public void softReset() {
        softResetRequest = true;
        framecount = 0;
        runEmulation = false;
    }
    
    private void _softReset() {
        board.softReset();
        apu.softReset();
        cpu.softReset();
    }
    
    @Override
    public void run() {
        if (board == null) {
            return;
        }
        
        ppu.hclock = 0;
        
        while (true) {
            if (runEmulation) {
                frameStartTime = System.nanoTime();
                
                if (LOGGING) {
                    try {
                        cpu.fw.write(cpu.getCPUState().toTraceEvent() + " CYC:" + ppu.hclock + " SL:" + ppu.vclock + "\n");
                
                        if (cpu.getCPUState().stepCounter == 0) {
                            cpu.fw.flush();
                        }
                    } catch (IOException ioe) {
                        messageBox("Cannot write to debug log: " + ioe.getMessage());
                    }
                }
                
                cpu.cycle();
                
                if (frameLimiter != null && enableFrameLimiter) {
                    frameLimiter.sleep();
                }
                
                frameDoneTime = System.nanoTime() - frameStartTime;
                //System.err.println(cpu.getCPUState().toTraceEvent() + " " + ppu.hclock + " " + ppu.vclock);
            } else { 
                frameLimiter.sleepFixed();
                
//                if (ppu != null && framecount > 1) {
//                    java.awt.EventQueue.invokeLater(render);
//                }
            
                if (softResetRequest) {
                    softResetRequest = false;
                    _softReset();
                    runEmulation = true;
                } else if (hardResetRequest) {
                    hardResetRequest = false;
                    _hardReset();
                    runEmulation = true;
                }
            }
            
            if ((framecount & 2047) == 0) {
                saveSRAM(true);
            }
            
            ++framecount;
        }  
    }

    public final long getFrameTime() {
        return frameDoneTime;
    }
    
    public void setGUI(GUIImpl gui) {
        this.gui = gui;
    }
    
    public void setControllers(Joypad joypad1, Joypad joypad2) {
        controllers.joypad1 = joypad1;
        controllers.joypad2 = joypad2;
    }
    
    public Joypad getJoypad1() {
        return controllers.joypad1;
    }

    public Joypad getJoypad2() {
        return controllers.joypad2;
    }
    
    public void setRegion(nesimulare.core.Region.System region) {
        this.region = region;
        hardReset();
    }
    
    public void finishFrame(GUIInterface gui) {
        if (apu != null) {
            apu.ai.flushFrame(enableFrameLimiter);
        }
        gui.setFrame(ppu.screen);
    }
    
    public void messageBox(final String message) {
        gui.messageBox(message);
    }
    
    public void generatePalette() {
        ppu.setupPalette(PaletteGenerator.generattePalette());
    }
    
    public synchronized void loadROM(final String filename) {
        runEmulation = false;
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
        } else {
            gui.messageBox("Could not load file:\nFile " + filename + "\n"
                    + "does not exist or is not a valid NES game.");
        }
    }
    
    public void saveSRAM(final boolean async) {
        if (board != null && loader.hasSRAM()) {
            if (async) {
                Tools.asyncwritetofile(board.getSRAM(), Tools.stripExtension(curRomPath) + ".sav");
            } else {
                Tools.writetofile(board.getSRAM(), Tools.stripExtension(curRomPath) + ".sav");
            }
        }
    }

    private void loadSRAM() {
        final String name = Tools.stripExtension(curRomPath) + ".sav";
        
        if (Tools.exists(name) && loader.hasSRAM()) {
            board.setSRAM(Tools.readfromfile(name));
        }
    }
    
    public final ROMLoader getLoader() {
        return loader;
    }
    
    public final String getCurrentRomName() {
        return curRomName;
    }
    
    public void toggleFrameLimiter() {
        enableFrameLimiter = !enableFrameLimiter;
    }
}