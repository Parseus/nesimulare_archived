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

import nesimulare.core.audio.Namco163SoundChip;
import nesimulare.core.cpu.CPU;
import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class Namco163 extends Board {
    private Namco163SoundChip soundChip;
    private int chrram[];
    private int irqCounter = 0;
    private boolean chrLow = false;
    private boolean chrHigh = false;
    private boolean irqEnabled = false;
    
    public Namco163(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        soundChip = new Namco163SoundChip(nes.region);
        nes.apu.addExpansionSoundChip(soundChip);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
        chrram = new int[0x8000];
        
        if (nes.loader.mirroring == PPUMemory.Mirroring.VERTICAL) {
            nes.ppuram.nmtBank[0] = 0xE0;
            nes.ppuram.nmtBank[1] = 0xE1;
            nes.ppuram.nmtBank[2] = 0xE0;
            nes.ppuram.nmtBank[3] = 0xE1;
        } else if (nes.loader.mirroring == PPUMemory.Mirroring.HORIZONTAL) {
            nes.ppuram.nmtBank[0] = 0xE0;
            nes.ppuram.nmtBank[1] = 0xE0;
            nes.ppuram.nmtBank[2] = 0xE1;
            nes.ppuram.nmtBank[3] = 0xE1;
        }
    }
    
    @Override
    public int readEXP(int address) {
        switch (address) {
            case 0x4800:
                return soundChip.readData(address);
            case 0x5000:
                return (irqCounter & 0x00FF);
            case 0x5800:
                return ((irqCounter & 0x7F00) >> 8) & 0xFF;
            default:
                return address >> 8;
        }
    }
    
    @Override
    public void writeEXP(int address, int data) {
        switch (address) {
            case 0x4800:
                soundChip.writeData(address, data);
                break;
            case 0x5000:
                irqCounter = ((irqCounter & 0x7F00) | data);
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0x5800:
                irqEnabled = Tools.getbit(data, 7);
                irqCounter = ((irqCounter & 0x00FF) | (data << 8));
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            default:
                break;
        }
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF800) {
            case 0x8000:
                if ((data < 0xE0) || chrLow) {
                    super.switch1kCHRbank(data, 0x0000);
                } else {
                    super.switch1kCHRbank((data & 0x1F) + (chr.length >> 10), 0x0000);
                }
                break;
            case 0x8800:
                if ((data < 0xE0) || chrLow) {
                    super.switch1kCHRbank(data, 0x0400);
                } else {
                    super.switch1kCHRbank((data & 0x1F) + (chr.length >> 10), 0x0400);
                }
                break;
            case 0x9000:
                if ((data < 0xE0) || chrLow) {
                    super.switch1kCHRbank(data, 0x0800);
                } else {
                    super.switch1kCHRbank((data & 0x1F) + (chr.length >> 10), 0x0800);
                }
                break;
            case 0x9800:
                if ((data < 0xE0) || chrLow) {
                    super.switch1kCHRbank(data, 0x0C00);
                } else {
                    super.switch1kCHRbank((data & 0x1F) + (chr.length >> 10), 0x0C00);
                }
                break;
            case 0xA000:
                if ((data < 0xE0) || chrHigh) {
                    super.switch1kCHRbank(data, 0x1000);
                } else {
                    super.switch1kCHRbank((data & 0x1F) + (chr.length >> 10), 0x1000);
                }
                break;
            case 0xA800:
                if ((data < 0xE0) || chrHigh) {
                    super.switch1kCHRbank(data, 0x1400);
                } else {
                    super.switch1kCHRbank((data & 0x1F) + (chr.length >> 10), 0x1400);
                }
                break;
            case 0xB000:
                if ((data < 0xE0) || chrHigh) {
                    super.switch1kCHRbank(data, 0x1800);
                } else {
                    super.switch1kCHRbank((data & 0x1F) + (chr.length >> 10), 0x1800);
                }
                break;
            case 0xB800:
                if ((data < 0xE0) || chrHigh) {
                    super.switch1kCHRbank(data, 0x1C00);
                } else {
                    super.switch1kCHRbank((data & 0x1F) + (chr.length >> 10), 0x1C00);
                }
                break;
            case 0xC000:
                nes.ppuram.nmtBank[0] = data;
                break;
            case 0xC800:
                nes.ppuram.nmtBank[1] = data;
                break;
            case 0xD000:
                nes.ppuram.nmtBank[2] = data;
                break;
            case 0xD800:
                nes.ppuram.nmtBank[3] = data;
                break;
            case 0xE000:
                super.switch8kPRGbank(data & 0x3F, 0x8000);
                break;
            case 0xE800:
                chrHigh = Tools.getbit(data, 7);
                chrLow = Tools.getbit(data, 6);
                super.switch8kPRGbank(data & 0x3F, 0xA000);
                break;
            case 0xF000:
                super.switch8kPRGbank(data & 0x3F, 0xC000);
                break;
            case 0xF800:
                soundChip.writeRegister(data);
                break;
            default:
                break;
        }
    }
    
    @Override
    public int readCHR(int address) {
        final int addr = decodeCHRAddress(address);
        
        if (addr < chr.length) {
            return chr[address];
        } else {
            return chrram[addr - chr.length];
        }
    }
    
    @Override
    public void writeCHR(int address, int data) {
        final int addr = decodeCHRAddress(address);
        
        if (addr >= chr.length) {
            chrram[addr - chr.length] = data;
        }
    }
    
    @Override
    public int readNametable(int address) {
        if (nes.ppuram.nmtBank[(address >> 10) & 0x3] < 0xE0) {
            return chr[(nes.ppuram.nmtBank[(address >> 10) & 0x3] << 10) | (address & 0x3FF)];
        } else {
            return nes.ppuram.nmt[(nes.ppuram.nmtBank[(address >> 10) & 0x3] - 0xE0) & 1][address & 0x3FF];
        }
    }
    
    @Override
    public void writeNametable(int address, int data) {
        if (nes.ppuram.nmtBank[(address >> 10) & 0x3] >= 0xE0) {
            nes.ppuram.nmt[(nes.ppuram.nmtBank[(address >> 10) & 0x3] - 0xE0) & 1][address & 0x3FF] = data;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (irqEnabled) {
            if ((irqCounter - 0x8000 < 0x7FFF) && (++irqCounter == 0xFFFF)) {
                irqEnabled = false;
                irqCounter = 0;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
            }
        }
    }
}