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

/**
 *
 * @author Parseus
 */
public class Mapper142 extends Board {
    private int control = 0;
    private int irqCounter = 0;
    private int prgBank = 0;
    private boolean irqEnabled = false;
    
    public Mapper142(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        control = 0;
        irqCounter = 0;
        prgBank = 0;
        irqEnabled = false;
    }
    
    @Override
    public int readSRAM(int address) {
        return prg[(prgBank << 13) | (address & 0x1FFF)];
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF000) {
            case 0x8000:
                irqCounter = (irqCounter & 0xFFF0) | (data & 0xF);
                break;
            case 0x9000:
                irqCounter = (irqCounter & 0xFF0F) | ((data & 0xF) << 4);
                break;
            case 0xA000:
                irqCounter = (irqCounter & 0xF0FF) | ((data & 0xF) << 8);
                break;
            case 0xB000:
                irqCounter = (irqCounter & 0x0FFF) | ((data & 0xF) << 12);
                break;  
            case 0xC000:
                irqEnabled = (data & 0xF) == 0xF;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xE000:
                control = data;
                break;
            case 0xF000:
                final int addr = (control & 0xF) - 1;
                
                if (addr < 3) {
                    super.switch8kPRGbank(data, addr << 13);
                } else if (address < 4) {
                    prgBank = data;
                }
                break;
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (irqEnabled && irqCounter++ == 0xFFFF) {
            irqEnabled = false;
            irqCounter = 0;
            nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
        }
    }
}