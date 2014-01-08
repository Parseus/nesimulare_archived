/*
 * The MIT License
 *
 * Copyright 2013 Parseus.
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
public class IremH3001 extends Board {
    private int irqCounter = 0;
    private int irqReload = 0;
    private boolean irqEnabled = false;
    
    public IremH3001(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch8kPRGbank(0x00, 0x8000);
        super.switch8kPRGbank(0x01, 0xA000);
        super.switch8kPRGbank(0xFE, 0xC000);
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address) {
            case 0x8000: case 0xA000: case 0xC000:
                super.switch8kPRGbank(data, address);
                break;
            
            case 0xB000:
                super.switch1kCHRbank(data, 0x0000);
                break;
            case 0xB001:
                super.switch1kCHRbank(data, 0x0400);
                break;
            case 0xB002:
                super.switch1kCHRbank(data, 0x0800);
                break;
            case 0xB003:
                super.switch1kCHRbank(data, 0x0C00);
                break;
            case 0xB004:
                super.switch1kCHRbank(data, 0x1000);
                break;
            case 0xB005:
                super.switch1kCHRbank(data, 0x1400);
                break;
            case 0xB006:
                super.switch1kCHRbank(data, 0x1800);
                break;
            case 0xB007:
                super.switch1kCHRbank(data, 0x1C00);
                break;
                
            case 0x9001:
                nes.ppuram.setMirroring(Tools.getbit(data, 7) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
                break;
            case 0x9003:
                irqEnabled = Tools.getbit(data, 7);
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0x9004:
                irqCounter = irqReload;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0x9005:
                irqReload = (irqReload & 0x00FF) | (data << 8);
                break;
            case 0x9006:
                irqReload = (irqReload & 0xFF00) | data;
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
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
            }
        }
    }
}