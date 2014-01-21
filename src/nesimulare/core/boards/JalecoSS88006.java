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
package nesimulare.core.boards;

import nesimulare.core.cpu.CPU;
import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class JalecoSS88006 extends Board {
    private int prgRegister[] = new int[3];
    private int chrRegister[] = new int[8];
    private boolean wramEnabled = true;
    private int irqCounter = 0;
    private int irqReload = 0;
    private int irqMask = 0;
    private boolean irqEnabled = false;
    
    public JalecoSS88006(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
        prgRegister = new int[3];
        chrRegister = new int[8];
        wramEnabled = true;
        irqCounter = irqReload = irqMask = 0xFFFF;
        irqEnabled = false;
    }
    
    @Override
    public int readSRAM(int address) {
        if (wramEnabled) {
            return super.readSRAM(address);
        }
        
        return 0;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        if (wramEnabled) {
            super.writeSRAM(address, data);
        }
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address) {
            case 0x8000:
                prgRegister[0] = (prgRegister[0] & 0xF0) | (data & 0xF);
                super.switch8kPRGbank(prgRegister[0], 0x8000);
                break;
            case 0x8001:
                prgRegister[0] = (prgRegister[0] & 0x0F) | ((data & 0xF) << 4);
                super.switch8kPRGbank(prgRegister[0], 0x8000);
                break;
            case 0x8002:
                prgRegister[1] = (prgRegister[1] & 0xF0) | (data & 0xF);
                super.switch8kPRGbank(prgRegister[1], 0xA000);
                break;
            case 0x8003:
                prgRegister[1] = (prgRegister[1] & 0x0F) | ((data & 0xF) << 4);
                super.switch8kPRGbank(prgRegister[1], 0xA000);
                break;
                
            case 0x9000:
                prgRegister[2] = (prgRegister[2] & 0xF0) | (data & 0xF);
                super.switch8kPRGbank(prgRegister[2], 0xC000);
                break;
            case 0x9001:
                prgRegister[2] = (prgRegister[2] & 0x0F) | ((data & 0xF) << 4);
                super.switch8kPRGbank(prgRegister[2], 0xC000);
                break;
            case 0x9002:
                wramEnabled = Tools.getbit(data, 0);
                break;
                
            case 0xA000:
                chrRegister[0] = (chrRegister[0] & 0xF0) | (data & 0xF);
                super.switch1kCHRbank(chrRegister[0], 0x0000);
                break;
            case 0xA001:
                chrRegister[0] = (chrRegister[0] & 0x0F) | ((data & 0xF) << 4);
                super.switch1kCHRbank(chrRegister[0], 0x0000);
                break;
            case 0xA002:
                chrRegister[1] = (chrRegister[1] & 0xF0) | (data & 0xF);
                super.switch1kCHRbank(chrRegister[1], 0x0400);
                break;
            case 0xA003:
                chrRegister[1] = (chrRegister[1] & 0x0F) | ((data & 0xF) << 4);
                super.switch1kCHRbank(chrRegister[1], 0x0400);
                break;
                
            case 0xB000:
                chrRegister[2] = (chrRegister[2] & 0xF0) | (data & 0xF);
                super.switch1kCHRbank(chrRegister[2], 0x0800);
                break;
            case 0xB001:
                chrRegister[2] = (chrRegister[2] & 0x0F) | ((data & 0xF) << 4);
                super.switch1kCHRbank(chrRegister[2], 0x0800);
                break;
            case 0xB002:
                chrRegister[3] = (chrRegister[3] & 0xF0) | (data & 0xF);
                super.switch1kCHRbank(chrRegister[3], 0x0C00);
                break;
            case 0xB003:
                chrRegister[3] = (chrRegister[3] & 0x0F) | ((data & 0xF) << 4);
                super.switch1kCHRbank(chrRegister[3], 0x0C00);
                break;
                
            case 0xC000:
                chrRegister[4] = (chrRegister[4] & 0xF0) | (data & 0xF);
                super.switch1kCHRbank(chrRegister[4], 0x1000);
                break;
            case 0xC001:
                chrRegister[4] = (chrRegister[4] & 0x0F) | ((data & 0xF) << 4);
                super.switch1kCHRbank(chrRegister[4], 0x1000);
                break;
            case 0xC002:
                chrRegister[5] = (chrRegister[5] & 0xF0) | (data & 0xF);
                super.switch1kCHRbank(chrRegister[5], 0x1400);
                break;
            case 0xC003:
                chrRegister[5] = (chrRegister[5] & 0x0F) | ((data & 0xF) << 4);
                super.switch1kCHRbank(chrRegister[5], 0x1400);
                break;    
                
            case 0xD000:
                chrRegister[6] = (chrRegister[6] & 0xF0) | (data & 0xF);
                super.switch1kCHRbank(chrRegister[6], 0x1800);
                break;
            case 0xD001:
                chrRegister[6] = (chrRegister[6] & 0x0F) | ((data & 0xF) << 4);
                super.switch1kCHRbank(chrRegister[6], 0x1800);
                break;
            case 0xD002:
                chrRegister[7] = (chrRegister[7] & 0xF0) | (data & 0xF);
                super.switch1kCHRbank(chrRegister[7], 0x1C00);
                break;
            case 0xD003:
                chrRegister[7] = (chrRegister[7] & 0x0F) | ((data & 0xF) << 4);
                super.switch1kCHRbank(chrRegister[7], 0x1D00);
                break;
                
            case 0xE000:
                irqReload = (irqReload & 0xFFF0) | (data & 0xF);
                break;
            case 0xE001:
                irqReload = (irqReload & 0xFF0F) | ((data & 0xF) << 4);
                break;
            case 0xE002:
                irqReload = (irqReload & 0xF0FF) | ((data & 0xF) << 8);
                break;
            case 0xE003:
                irqReload = (irqReload & 0x0FFF) | ((data & 0xF) << 12);
                break;
                
            case 0xF000:
                irqCounter = irqReload;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xF001:
                irqEnabled = Tools.getbit(data, 0);
                
                if (Tools.getbit(data, 3)) {
                    irqMask = 0x000F;
                } else if (Tools.getbit(data, 2)) {
                    irqMask = 0x00FF;
                } else if (Tools.getbit(data, 1)) {
                    irqMask = 0x0FFF;
                } else {
                    irqMask = 0xFFFF;
                }
            
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xF002:
                switch (data & 3) {
                    case 0:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.HORIZONTAL);
                        break;
                    case 1:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.VERTICAL);
                        break;
                    case 2:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENA);
                        break;
                    case 3:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENB);
                        break;
                    default:
                        break;
                }
                break;
                
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (irqEnabled) {
            if (((irqCounter & irqMask) > 0) && ((--irqCounter & irqMask) == 0)) {
                irqEnabled = false;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
            }
        }
    }
}