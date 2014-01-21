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
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class VRC3 extends Board {
    private int irqCounter = 0;
    private int irqReload = 0;
    private boolean irqMode = false;
    private boolean irqEnabled = false;
    private boolean irqAcknowledge = false;
    
    public VRC3(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF000) {
            case 0x8000:
                irqReload = (irqReload & 0xFFF0) | (data & 0xF);
                break;
            case 0x9000:
                irqReload = (irqReload & 0xFF0F) | ((data & 0xF) << 4);
                break;
            case 0xA000:
                irqReload = (irqReload & 0xF0FF) | ((data & 0xF) << 8);
                break;
            case 0xB000:
                irqReload = (irqReload & 0x0FFF) | ((data & 0xF) << 12);
                break;
            case 0xC000:
                irqAcknowledge = Tools.getbit(data, 0);
                irqEnabled = Tools.getbit(data, 1);
                irqMode = Tools.getbit(data, 2);
                
                if (irqEnabled) {
                    irqCounter = irqReload;
                }
            
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xD000:
                irqEnabled = irqAcknowledge;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xF000:
                super.switch16kPRGbank(data & 0xF, 0x8000);
                break;
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (irqEnabled) {
            if (irqMode) {
                irqCounter = (irqCounter & 0xFF00) | (((irqCounter & 0xFF) + 1) & 0xFF);
                
                if ((irqCounter & 0xFF) == 0xFF) {
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
                    irqCounter = (irqCounter & 0xFF00) | (irqReload & 0x00FF);
                }
            } else {
                irqCounter++;
                
                if (irqCounter == 0xFFFF) {
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
                    irqCounter = irqReload;
                }
            }
        }
    }
}