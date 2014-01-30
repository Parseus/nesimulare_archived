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
 * Emulates a Maxi 15 multicart (mapper 234).
 *
 * @author Parseus
 */
public class MLT_Maxi15 extends Board {
    private int register[] = new int[2];

    /**
     * Constructor for this class.
     *
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM False: PCB contains CHR-ROM
     */
    public MLT_Maxi15(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }

    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();

        register = new int[2];
    }

    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    @Override
    public void softReset() {
        register = new int[2];
    }
    
    /**
     * Reads data from a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    @Override
    public int readPRG(int address) {
        if (address >= 0xFF80 && address <= 0xFF9F) {
            final int data = prg[0x6000 + address - 0xE000];
            writePRG(address, data);
            
            return data;
        } else if (address >= 0xFFE8 && address <= 0xFFF7) {
            final int data = prg[0x6000 + address - 0xE000];
            writePRG(address, data);
            
            return data;
        }
        
        return super.readPRG(address);
    }

    /**
     * Writes data to a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writePRG(int address, int data) {
        if (address >= 0xFF80 && address <= 0xFF9F) {
            if ((register[0] & 0x3F) == 0) {
                register[0] = data;
                nes.ppuram.setMirroring(Tools.getbit(data, 7) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
                super.switch32kPRGbank((register[0] & 0xE) | (register[register[0] >> 6 & 0x1] & 0x1));
                super.switch8kCHRbank((register[0] << 2 & (register[0] >> 4 & 0x4 ^ 0x3C)) | (register[1] >> 4 & (register[0] >> 4 & 0x4 | 0x3)));
            }
        } else if (address >= 0xFFE8 && address <= 0xFFF7) {
            register[1] = data;
            super.switch32kPRGbank((register[0] & 0xE) | (register[register[0] >> 6 & 0x1] & 0x1));
            super.switch8kCHRbank((register[0] << 2 & (register[0] >> 4 & 0x4 ^ 0x3C)) | (register[1] >> 4 & (register[0] >> 4 & 0x4 | 0x3)));
        } else {
            super.writePRG(address, data);
        }
    }
}