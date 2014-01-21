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
public class Sunsoft3 extends Board {
    private int irqCounter = 0;
    private boolean irqEnabled = false;
    private boolean irqToggle = false;

    public Sunsoft3(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
        irqCounter = 0;
        irqEnabled = false;
        irqToggle = false;
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF800) {
            case 0x8800:
                super.switch2kCHRbank(data, 0x0000);
                break;
            case 0x9800:
                super.switch2kCHRbank(data, 0x0800);
                break;
            case 0xA800:
                super.switch2kCHRbank(data, 0x1000);
                break;
            case 0xB800:
                super.switch2kCHRbank(data, 0x1800);
                break;
            case 0xC800:
                if (irqToggle) {
                    irqCounter = (irqCounter & 0xFF00) | data;
                } else {
                    irqCounter = (irqCounter & 0xFF00) | (data << 8);
                }
                
                irqToggle ^= true;
                break;
            case 0xD800:
                irqEnabled = Tools.getbit(data, 4);
                irqToggle = false;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xE800:
                switch (data & 0x3) {
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
            case 0xF800:
                super.switch16kPRGbank(data, 0x8000);
                break;
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (irqEnabled) {
            irqCounter--;
            
            if (irqCounter == 0) {
                irqCounter = 0xFFFF;
                irqEnabled = false;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
            }
        }
    }
}