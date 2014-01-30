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

/**
 * Emulates a AVE-NINA-01 board (part of mapper 34).
 *
 * @author Parseus
 */
public class AVE_NINA_01 extends Board {
    /**
     * Constructor for this class.
     * 
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM
     *                  False: PCB contains CHR-ROM
     */
    public AVE_NINA_01(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }

    /**
     * Writes data to a given address within the range $6000-$7FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writeSRAM(int address, int data) {
        switch (address) {
            /**
             * $7FFD: PRG bank select
             * 7  bit  0
             * ---- ----
             * xxxx xxxP
             *         |
             *         +- Select 32 KB PRG ROM bank for CPU $8000-$FFFF
             */
            case 0x7FFD:
                super.switch32kPRGbank(data);
                break;
                
            /**
             * $7FFE: CHR bank select 0
             * 7  bit  0
             * ---- ----
             * xxxx CCCC
             *      ||||
             *      ++++- Select 4 KB CHR ROM bank for PPU $0000-$0FFF
             */
            case 0x7FFE:
                super.switch4kCHRbank(data, 0x0000);
                break;
                
            /**
             * $7FFF: CHR bank select 1
             * 7  bit  0
             * ---- ----
             * xxxx CCCC
             *      ||||
             *      ++++- Select 4 KB CHR ROM bank for PPU $1000-$1FFF
             */
            case 0x7FFF:
                super.switch4kCHRbank(data, 0x1000);
                break;
                
            /**
             * Default: normal write to SRAM
             */
            default:
                super.writeSRAM(address, data);
                break;
        }
    }
}