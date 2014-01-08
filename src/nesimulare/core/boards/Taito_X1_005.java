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

import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class Taito_X1_005 extends Board {
    private boolean wramEnabled = true;
    
    public Taito_X1_005(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        wramEnabled = true;
        sram = new int[128];
        
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
    }    
    
    @Override
    public int readSRAM(int address) {
        if (address >= 0x7F00 && wramEnabled) {
            return sram[address & 0x7F];
        }
        
        return address >> 8;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        if (address >= 0x7F00 && wramEnabled) {
            sram[address & 0x7F] = data;
        } else {
            switch (address) {
                case 0x7EF0:
                    super.switch2kCHRbank(data >> 1, 0x0000);
                    break;
                case 0x7EF1:
                    super.switch2kCHRbank(data >> 1, 0x0800);
                    break;
                case 0x7EF2:
                    super.switch1kCHRbank(data, 0x1000);
                    break;
                case 0x7EF3:
                    super.switch1kCHRbank(data, 0x1400);
                    break;
                case 0x7EF4:
                    super.switch1kCHRbank(data, 0x1800);
                    break;
                case 0x7EF5:
                    super.switch1kCHRbank(data, 0x1C00);
                    break;
                case 0x7EF6: case 0x73F7:
                    nes.ppuram.setMirroring(Tools.getbit(data, 0) ? PPUMemory.Mirroring.VERTICAL : PPUMemory.Mirroring.HORIZONTAL);
                    break;
                case 0x7EF8: case 0x7EF9:
                    wramEnabled = (data == 0xA3);
                    break;
                case 0x7EFA: case 0x7EFB:
                    super.switch8kPRGbank(data, 0x8000);
                    break;
                case 0x7EFC: case 0x7EFD:
                    super.switch8kPRGbank(data, 0xA000);
                    break;
                case 0x7EFE: case 0x7EFF:
                    super.switch8kPRGbank(data, 0xC000);
                    break;
                default:
                    break;
            }
        }
    }
}