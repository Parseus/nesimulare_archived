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
 * Emulates a Camerica boardset (mapper 71).
 *
 * @author Parseus
 */
public class Camerica extends Board {
    /**
     * Constructor for this class.
     * 
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM
     *                  False: PCB contains CHR-ROM
     */
    public Camerica(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }

    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();

        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
    }

    /**
     * Writes data to a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF000) {
            /**
             * $C000-$FFFF: Bank select
             * 7  bit  0
             * ---- ----
             * xxxx PPPP
             *      ||||
             *      ++++- Select 16 KiB PRG ROM bank for CPU $8000-$BFFF
             */
            case 0xF000:
            case 0xE000:
            case 0xD000:
            case 0xC000:
                super.switch16kPRGbank(data & 0xF, 0x8000);
                break;
                
            /**
             * $8000-$9FFF: Mirroring (only on Camerica BF-9097 - Fire Hawk)
             * 7  bit  0
             * ---- ----
             * xxxM xxxx
             *    |
             *    +----- Select 1 KiB CIRAM bank for PPU $2000-$2FFF
             */
            case 0x9000:
            case 0x8000:
                if (Tools.getbit(data, 4)) {
                    nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENB);
                } else {
                    nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENA);
                }
                break;
            default:
                break;
        }
    }
}