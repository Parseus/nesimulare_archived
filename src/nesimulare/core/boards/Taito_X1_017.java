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
public class Taito_X1_017 extends Board{
    private boolean chrMode = false;
    private boolean wramEnabled1 = true;
    private boolean wramEnabled2 = true;
    private boolean wramEnabled3 = true;

    public Taito_X1_017(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        sram = new int[5120];
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
        chrMode = false;
        wramEnabled1 = wramEnabled2 = wramEnabled3 = true;
    }
    
    @Override
    public int readSRAM(int address) {
        if (address <= 0x67FF && wramEnabled1) {
            return super.readSRAM(address);
        } else if (address <= 0x6FFF && wramEnabled2) {
            return super.readSRAM(address);
        } else if (address <= 0x73FF && wramEnabled3) {
            return super.readSRAM(address);
        }
        
        return address >> 8;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        if (address <= 0x67FF && wramEnabled1) {
            super.writeSRAM(address, data);
        } else if (address <= 0x6FFF && wramEnabled2) {
            super.writeSRAM(address, data);
        } else if (address <= 0x73FF && wramEnabled3) {
            super.writeSRAM(address, data);
        } else {
            switch (address) {
                case 0x7EF0:
                    super.switch2kCHRbank(data >> 1, chrMode ? 0x1000 : 0x0000);
                    break;
                case 0x7EF1:
                    super.switch2kCHRbank(data >> 1, chrMode ? 0x1800 : 0x0800);
                    break;
                case 0x7EF2:
                    super.switch1kCHRbank(data, chrMode ? 0x0000 : 0x1000);
                    break;
                case 0x7EF3:
                    super.switch1kCHRbank(data, chrMode ? 0x0400 : 0x1400);
                    break;
                case 0x7EF4:
                    super.switch1kCHRbank(data, chrMode ? 0x0800 : 0x1800);
                    break;
                case 0x7EF5:
                    super.switch1kCHRbank(data, chrMode ? 0x0000 : 0x1000);
                    break;
                case 0x7EF6:
                    chrMode = Tools.getbit(data, 1);
                    nes.ppuram.setMirroring(Tools.getbit(data, 0) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
                    break;
                case 0x7EF7:
                    wramEnabled1 = (data == 0xCA);
                    break;
                case 0x7EF8:
                    wramEnabled2 = (data == 0x69);
                    break;
                case 0x7EF9:
                    wramEnabled3 = (data == 0x84);
                    break;
                case 0x7EFA:
                    super.switch8kPRGbank(data >> 2, 0x8000);
                    break;
                case 0x7EFB:
                    super.switch8kPRGbank(data >> 2, 0xA000);
                    break;
                case 0x7EFC:
                    super.switch8kPRGbank(data >> 2, 0xC000);
                    break;
            }
        }
    }
}