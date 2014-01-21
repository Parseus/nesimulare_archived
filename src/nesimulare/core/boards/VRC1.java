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

import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class VRC1 extends Board {
    private int chrRegister[] = new int[2];
    
    public VRC1(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF000) {
            case 0x8000:
                super.switch8kPRGbank(data, 0x8000);
                break;
            case 0x9000:
                chrRegister[0] = (chrRegister[0] & 0xF) | ((data & 0x2) << 3);
                chrRegister[1] = (chrRegister[1] & 0xF) | ((data & 0x4) << 2);
                
                if (nes.getLoader().mirroring != PPUMemory.Mirroring.FOURSCREEN) {
                    nes.ppuram.setMirroring(Tools.getbit(data, 0) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
                }
            
                setupCHR();
                break;
            case 0xA000:
                super.switch8kPRGbank(data, 0xA000);
                break;
            case 0xC000:
                super.switch8kPRGbank(data, 0xC000);
                break;
            case 0xE000:
                chrRegister[0] = (chrRegister[0] & 0x10) | (data & 0xF);
                setupCHR();
                break;
            case 0xF000:
                chrRegister[0] = (chrRegister[0] & 0x10) | (data & 0xF);
                setupCHR();
                break;
            default:
                break;
        }
    }
    
    private void setupCHR() {
        super.switch4kCHRbank(chrRegister[0], 0x0000);
        super.switch4kCHRbank(chrRegister[1], 0x1000);
    }
}