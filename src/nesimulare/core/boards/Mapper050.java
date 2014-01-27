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
public class Mapper050 extends Board {
    private int irqCounter = 0;
    private boolean irqEnabled = false;
    
    public Mapper050(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
 
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch8kPRGbank(0x8, 0x8000);
        super.switch8kPRGbank(0x9, 0xA000);
        super.switch8kPRGbank(0x0, 0xC000);
        super.switch8kPRGbank(0xB, 0xE000);
    }
    
    @Override
    public int readSRAM(int address) {
        return prg[(address - 0x6000) + (0xF << 13)];
    }
    
    @Override
    public void writeEXP(int address, int data) {
        switch (address & 0x4120) {
            case 0x4020:
                final int page = (data & 0x8) | (data << 2 & 0x4) | (data >> 1 & 0x3);
                super.switch8kPRGbank(page, 0xC000);
                break;
            case 0x4120:
                irqEnabled = Tools.getbit(data, 0);
                
                if (!irqEnabled) {
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                    irqCounter = 0;
                }
                break;
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (irqEnabled) {
            if (++irqCounter == 0x1000) {
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
            }
        }
    }
}