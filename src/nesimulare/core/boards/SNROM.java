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

import nesimulare.core.memory.PPUMemory.Mirroring;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class SNROM extends Board {
    protected boolean wramEnabled;
    protected int register[];
    protected int sramBank;
    protected int timer, shift, tmp;
    
    public SNROM(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        timer = 0;   
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        register = new int[4];
        register[0] = 0x0C;
        register[1] = register[2] = register[3] = 0;
        
        super.switch16kPRGbank(0, 0x8000);
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
        
        sramBank = 0;
        wramEnabled = true;
        shift = tmp = 0;
    }
    
    @Override
    public int readSRAM(int address) {
        if (wramEnabled) {
            return sram[sramBank | (address & 0x1FFF)];
        }
        
        return address >> 8;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        if (wramEnabled) {
            sram[sramBank | (address & 0x1FFF)] = data;
        }
    }
    
    @Override
    public void writePRG(int address, int data) {
        if (timer < 2) {
            return;
        }
        
        timer = 0;
        
        if (Tools.getbit(data, 7)) {
            register[0] |= 0x0C;
            shift = tmp = 0;
            return;
        }
        
        if (Tools.getbit(data, 0)) {
            tmp |= ((1 << shift) & 0xFF);
        }
        
        if (++shift < 5) {
            return;
        }
        
        final int reg = (address & 0x7FFF) >> 13;
        register[reg] = tmp;
        shift = tmp = 0;
        
        write(reg, data);
    }
    
    protected void write(int reg, int data) {
        switch (reg) {
            case 0:
                switch (register[0] & 3) {
                    case 0:
                        nes.ppuram.setMirroring(Mirroring.ONESCREENA);
                        break;
                    case 1:
                        nes.ppuram.setMirroring(Mirroring.ONESCREENB);
                        break;
                    case 2:
                        nes.ppuram.setMirroring(Mirroring.VERTICAL);
                        break;
                    case 3:
                        nes.ppuram.setMirroring(Mirroring.HORIZONTAL);
                        break;
                    default:
                        break;
                }
                break;
            case 1:
            case 2:
                if (Tools.getbit(register[0], 4)) {
                    super.switch4kCHRbank(register[1], 0);
                    super.switch4kCHRbank(register[2], 0x1000);
                } else {
                    super.switch8kCHRbank(register[1] >> 1);
                }
                break;
            case 3:
                wramEnabled = !Tools.getbit(register[3], 4);
                
                if (Tools.getbit(register[0], 3)) {
                    if (Tools.getbit(register[0], 2)) {
                        super.switch16kPRGbank(register[3], 0x8000);
                        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
                    } else {
                        super.switch16kPRGbank(0, 0x8000);
                        super.switch16kPRGbank(register[3], 0xC000);
                    }
                } else {
                    super.switch32kPRGbank(register[3] >> 1);
                }
                break;
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        timer++;
    }
    
    public static class SOROM extends SNROM {
        public SOROM(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
            super(prg, chr, trainer, haschrram);
        }
        
        @Override
        public void initialize() {
            super.initialize();
            
            sram = new int[0x4000];
        }
        
        @Override
        protected void write(int reg, int data) {
            switch (reg) {
                case 0:
                    switch (register[0] & 3) {
                        case 0:
                            nes.ppuram.setMirroring(Mirroring.ONESCREENA);
                            break;
                        case 1:
                            nes.ppuram.setMirroring(Mirroring.ONESCREENB);
                            break;
                        case 2:
                            nes.ppuram.setMirroring(Mirroring.VERTICAL);
                            break;
                        case 3:
                            nes.ppuram.setMirroring(Mirroring.HORIZONTAL);
                            break;
                        default:
                            break;
                    }
                    break;
                case 1:
                    if (Tools.getbit(register[0], 4)) {
                        super.switch4kCHRbank(register[1], 0);
                        super.switch4kCHRbank(register[2], 0x1000);
                    } else {
                        super.switch8kCHRbank(register[1] >> 1);
                    }
                    
                    sramBank = (register[1] & 0x10) << 9;
                    break;
                case 2:
                    if (Tools.getbit(register[0], 4)) {
                        super.switch4kCHRbank(register[1], 0);
                        super.switch4kCHRbank(register[2], 0x1000);
                        sramBank = (register[2] & 0x10) << 9;
                    } else {
                        super.switch8kCHRbank(register[1] >> 1);
                    }
                    break;
                case 3:
                    wramEnabled = !Tools.getbit(register[3], 4);
                
                    if (Tools.getbit(register[0], 3)) {
                        if (Tools.getbit(register[0], 2)) {
                            super.switch16kPRGbank(register[3], 0x8000);
                            super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
                        } else {
                            super.switch16kPRGbank(0, 0x8000);
                            super.switch16kPRGbank(register[3], 0xC000);
                        }
                    } else {
                        super.switch32kPRGbank(register[3] >> 1);
                    }
                    break;
                default:
                    break;
            }
        }
        
        @Override
        public int[] getSRAM() {
            final int[] newSRAM = new int[0x2000];
            
            System.arraycopy(sram, 0x2000, newSRAM, 0, 0x2000);
            
            return newSRAM.clone();
        }
        
        @Override
        public void setSRAM(int[] sram) {
            System.arraycopy(sram, 0, this.sram, 0x2000, 0x2000);
        }
    }
    
    public static class SUROM extends SNROM {
        public SUROM(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
            super(prg, chr, trainer, haschrram);
        }
        
        @Override
        public void hardReset() {
            super.hardReset();
        
            super.switch16kPRGbank(0, 0x8000);
            super.switch16kPRGbank(0xF, 0xC000);
    }
        
        @Override
        protected void write(int reg, int data) {
            if (reg == 0) {
                switch (register[0] & 3) {
                    case 0:
                        nes.ppuram.setMirroring(Mirroring.ONESCREENA);
                        break;
                    case 1:
                        nes.ppuram.setMirroring(Mirroring.ONESCREENB);
                        break;
                    case 2:
                        nes.ppuram.setMirroring(Mirroring.VERTICAL);
                        break;
                    case 3:
                        nes.ppuram.setMirroring(Mirroring.HORIZONTAL);
                        break;
                    default:
                        break;
                }
            }
            
            if (Tools.getbit(register[0], 4)) {
                super.switch4kCHRbank(register[1], 0x0000);
                super.switch4kCHRbank(register[2], 0x1000);
            } else {
                super.switch8kCHRbank(register[1] >> 1);
            }
            
            final int base = register[1] & 0x10;
            wramEnabled = !Tools.getbit(register[3], 4);
            
            if (Tools.getbit(register[0], 3)) {
                if (Tools.getbit(register[0], 2)) {
                    super.switch16kPRGbank(base + (register[3] & 0xF), 0x8000);
                    super.switch16kPRGbank(base + 0xF, 0xC000);
                } else {
                    super.switch16kPRGbank(base, 0x8000);
                    super.switch16kPRGbank(base + (register[3] & 0xF), 0xC000);
                }
            } else {
                super.switch32kPRGbank((register[3] & (0xF + base)) >> 1);
            }
        }
    }
}