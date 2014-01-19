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
package nesimulare.core.boards;

import nesimulare.core.cpu.CPU;
import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class TxROM extends Board{
    protected boolean chrMode = false;
    protected boolean prgMode = false;
    protected int register = 0;
    protected int chrRegister[] = new int[6];
    protected int prgRegister[] = new int[4];
    
    protected boolean wramEnable = true;
    protected boolean wramWriteProtect = false;
    
    protected int irqReload = 0xFF;
    protected int irqCounter = 0;
    protected boolean irqEnable = false;
    protected boolean irqClear = false;
    protected int oldA12;
    protected int newA12;
    protected int irqTimer;
    
    public TxROM(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        chrMode = prgMode = false;
        register = 0;
        chrRegister = new int[6];
        prgRegister = new int[4];
        prgRegister[0] = 0;
        prgRegister[1] = 1;
        prgRegister[2] = (prg.length - 0x4000) >> 13;
        prgRegister[3] = (prg.length - 0x2000) >> 13;
        setupPRG();
        
        sram = new int[0x2000];
        wramEnable = true;
        wramWriteProtect = false;
        
        irqReload = 0xFF;
        irqCounter = 0;
        irqEnable = false;
        irqClear = false;
        oldA12 = newA12 = 0;
        irqTimer = 0;
    }
    
    @Override
    public int readSRAM(int address) {
        if (wramEnable) {
            return super.readSRAM(address);
        }
        
        return 0;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        if (wramEnable && !wramWriteProtect) {
            super.writeSRAM(address, data);
        }
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xE001) {
            case 0x8000:
                chrMode = Tools.getbit(data, 7);
                prgMode = Tools.getbit(data, 6);
                register = data & 0x7;
                setupPRG();
                setupCHR();
                break;
            case 0x8001:
                if (register <= 5) {
                    chrRegister[register] = data;
                    setupCHR();
                } else {
                    prgRegister[register & 1] = data & 0x3F;
                    setupPRG();
                }
                break;
            case 0xA000:
                if (nes.getLoader().mirroring != PPUMemory.Mirroring.FOURSCREEN) {
                    if (Tools.getbit(data, 0)) {
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.HORIZONTAL);
                    } else {
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.VERTICAL);
                    }
                }
                break;
            case 0xA001:
                wramEnable = Tools.getbit(data, 7);
                wramWriteProtect = Tools.getbit(data, 6);
                break;
            case 0xC000:
                irqReload = data;
                break;
            case 0xC001:
                irqClear = true;
                irqCounter = 0;
                break;
            case 0xE000:
                irqEnable = false;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xE001:
                irqEnable = true;
                break;
            default:
                break;
        }
    }
    
    @Override
    public void updateAddressLines(final int address) {
        oldA12 = newA12;
        newA12 = address & 0x1000;
        
        if (oldA12 < newA12) {
            if (irqTimer > 8) {
                final int oldCounter = irqCounter;
                
                if (irqCounter == 0 || irqClear) {
                    irqCounter = irqReload;
                } else {
                    irqCounter = (irqCounter - 1) & 0xFF;
                }
                
                if ((oldCounter != 0 || irqClear) && (irqCounter == 0) && irqEnable) {
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
                }
                
                irqClear = false;
            }
            
            irqTimer = 0;
        }
    }
    
    @Override
    public void clockPPUCycle() {
        irqTimer++;
    }
    
    protected void setupPRG() {
        if (prgMode) {
            super.switch8kPRGbank(prgRegister[2], 0x8000);
            super.switch8kPRGbank(prgRegister[1], 0xA000);
            super.switch8kPRGbank(prgRegister[0], 0xC000);
            super.switch8kPRGbank(prgRegister[3], 0xE000);
        } else {
            super.switch8kPRGbank(prgRegister[0], 0x8000);
            super.switch8kPRGbank(prgRegister[1], 0xA000);
            super.switch8kPRGbank(prgRegister[2], 0xC000);
            super.switch8kPRGbank(prgRegister[3], 0xE000);
        }
    }
    
    protected void setupCHR() {
        if (chrMode) {
            super.switch2kCHRbank(chrRegister[0] >> 1, 0x1000);
            super.switch2kCHRbank(chrRegister[1] >> 1, 0x1800);
            super.switch1kCHRbank(chrRegister[2], 0x0000);
            super.switch1kCHRbank(chrRegister[3], 0x0400);
            super.switch1kCHRbank(chrRegister[4], 0x0800);
            super.switch1kCHRbank(chrRegister[5], 0x0C00);
        } else {
            super.switch1kCHRbank(chrRegister[2], 0x0000);
            super.switch1kCHRbank(chrRegister[3], 0x0400);
            super.switch1kCHRbank(chrRegister[4], 0x0800);
            super.switch1kCHRbank(chrRegister[5], 0x0C00);
            super.switch2kCHRbank(chrRegister[0] >> 1, 0x1000);
            super.switch2kCHRbank(chrRegister[1] >> 1, 0x1800);
        }
    }
}