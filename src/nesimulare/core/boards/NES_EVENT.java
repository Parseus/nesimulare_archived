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
public class NES_EVENT extends Board {
    private int register[] = new int[4];
    private int shift = 0;
    private int tmp = 0;
    private boolean wramEnabled = true;

    private int irqCounter = 0;
    private boolean irqControl = false;

    private int dipSwitchNumber = 0;
    private int dipSwitchIRQCounter = 0;

    public NES_EVENT(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }

    @Override
    public void hardReset() {
        super.hardReset();

        register = new int[4];
        register[0] = 0x0C;
        register[1] = register[2] = register[3] = 0;

        super.switch16kPRGbank(0, 0x8000);
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);

        wramEnabled = true;
        shift = 0;
        tmp = 0;

        dipSwitchNumber = 0;
        dipSwitchIRQCounter = 0x28000000;

        irqControl = true;
        irqCounter = 0;
    }

    @Override
    public void softReset() {
        super.softReset();

        dipSwitchNumber = (dipSwitchNumber + 1) & 0xF;
        dipSwitchIRQCounter = 0x20000000 | (dipSwitchNumber << 22);

        irqControl = true;
        irqCounter = 0;
    }

    @Override
    public int readSRAM(int address) {
        if (wramEnabled) {
            return super.readSRAM(address);
        }

        return (address >> 8 & 0xe0);
    }

    @Override
    public void writeSRAM(int address, int data) {
        if (wramEnabled) {
            super.writeSRAM(address, data);
        }
    }

    @Override
    public void writePRG(int address, int data) {
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

        write(reg);
    }
    
    private void write(int reg) {
        switch (reg) {
            case 0:
                switch (register[0] & 3) {
                    case 0:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENA);
                        break;
                    case 1:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENB);
                        break;
                    case 2:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.VERTICAL);
                        break;
                    case 3:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.HORIZONTAL);
                        break;
                    default:
                        break;
                }
                break;
            case 1:
                irqControl = Tools.getbit(register[1], 4);
                
                if (irqControl) {
                    super.switch32kPRGbank(0);
                    
                    irqCounter = 0;
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                } else {
                    if (!Tools.getbit(register[1], 3)) {
                        super.switch32kPRGbank((register[1] >> 1) & 0x3);
                    }
                }
                break;
            case 3:
                wramEnabled = !Tools.getbit(register[3], 4);
                
                if (irqControl) {
                    super.switch32kPRGbank(0);
                } else {
                    if (Tools.getbit(register[1], 3)) {
                        if (!Tools.getbit(register[0], 3)) {
                            super.switch32kPRGbank(((register[3] >> 1) & 0x3) + 4);
                        } else {
                            if (Tools.getbit(register[0], 2)) {
                                super.switch16kPRGbank(8 + (register[3] & 0x7), 0x8000);
                                super.switch16kPRGbank(15, 0xC000);
                            } else {
                                super.switch16kPRGbank(8, 0x8000);
                                super.switch16kPRGbank(8 + (register[3] & 0x7), 0xC000);
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (!irqControl) {
            irqCounter++;
            
            if (irqCounter == dipSwitchIRQCounter) {
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
            }
        }
    }
}