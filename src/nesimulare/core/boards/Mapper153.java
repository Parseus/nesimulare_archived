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
public class Mapper153 extends Board {
    private int irqCounter = 0;
    private boolean irqEnabled = false;
    
    public Mapper153(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }

    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
        irqCounter = 0;
        irqEnabled = false;
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF) {
            case 0x0:
                super.switch1kCHRbank(data, 0x0000);
                break;
            case 0x1:
                super.switch1kCHRbank(data, 0x0400);
                break;
            case 0x2:
                super.switch1kCHRbank(data, 0x0800);
                break;
            case 0x3:
                super.switch1kCHRbank(data, 0x0C00);
                break;
            case 0x4:
                super.switch1kCHRbank(data, 0x1000);
                break;
            case 0x5:
                super.switch1kCHRbank(data, 0x1400);
                break;
            case 0x6:
                super.switch1kCHRbank(data, 0x1800);
                break;
            case 0x7:
                super.switch1kCHRbank(data, 0x1C00);
                break;
            case 0x8:
                super.switch16kPRGbank(data & 0xF, 0x8000);
                break;
            case 0x9:
                switch (data & 3) {
                    case 0:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.VERTICAL);
                        break;
                    case 1:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.HORIZONTAL);
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
            case 0xA:
                irqEnabled = Tools.getbit(data, 0);
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xB:
                irqCounter = (irqCounter & 0xFF00) | data;
                break;
            case 0xC:
                irqCounter = (irqCounter & 0x00FF) | (data << 8);
                break;
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (irqEnabled) {
            if (irqCounter > 0) {
                irqCounter--;
            }
            
            if (irqCounter == 0) {
                irqEnabled = false;
                irqCounter = 0xFFFF;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
            }
        }
    }
}