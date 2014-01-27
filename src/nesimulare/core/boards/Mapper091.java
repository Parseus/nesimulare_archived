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
public class Mapper091 extends Board {
    private int irqReload = 0xFF;
    private int irqCounter = 0;
    private boolean irqEnable = false;
    private boolean irqClear = false;
    private int oldA12;
    private int newA12;
    private int irqTimer;
    
    public Mapper091(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();

        super.switch8kPRGbank((prg.length - 0x4000) >> 13, 0xC000);
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
        
        irqReload = 0xFF;
        irqCounter = 0;
        irqEnable = false;
        irqClear = false;
        oldA12 = newA12 = 0;
        irqTimer = 0;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        switch (address & 0x7003) {
            case 0x6000:
                super.switch2kCHRbank(data, 0x0000);
                break;
            case 0x6001:
                super.switch2kCHRbank(data, 0x0800);
                break;
            case 0x6002:
                super.switch2kCHRbank(data, 0x1000);
                break;
            case 0x6003:
                super.switch2kCHRbank(data, 0x1800);
                break;
            case 0x7000:
                super.switch8kPRGbank(data & 0xF, 0x8000);
                break;
            case 0x7001:
                super.switch8kPRGbank(data & 0xF, 0xA000);
                break;
            case 0x7002:
                irqEnable = false;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0x7003:
                irqReload = 0x7;
                irqClear = true;
                irqCounter = 0;
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
                    irqCounter--;
                }
                
                if (irqCounter == 0) {
                    if (oldCounter != 0 || irqClear) {
                        nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
                    }
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
}