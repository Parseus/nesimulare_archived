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

import nesimulare.gui.Tools;

/**
 * Emulates a BANDAI-74*161/02/74 board (mapper 96).
 * It is specifically designed to turn the PPU into an all-points-addressable 2bpp bitmap 
 * without needing timed code, a scanline counter, or audio channel abuse.
 * Oeka Kids tablet is not currently supported.
 *
 * @author Parseus
 */
public class BANDAI_74_161_02_74 extends Board {
    private boolean chrBlockSelect = false;
    
    /**
     * Constructor for this class.
     * 
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM
     *                  False: PCB contains CHR-ROM
     */
    public BANDAI_74_161_02_74(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    /**
     * Initializes the board.
     */
    @Override
    public void initialize() {
        super.initialize();
        
        chr = new int[32768];
        chrmask = chr.length - 1;
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();
        
        chrBlockSelect = false;
    }
    
    /**
     * Writes data to a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writePRG(int address, int data) {
        /**
         * $8000-$FFFF: Outer bank control
         * 7  bit  0
         * xxxx xCPP
         *       |||
         *       |++- Select 32KiB PRG bank
         *       +--- Select 16KiB outer CHR bank
         */
        
        chrBlockSelect = Tools.getbit(data, 2);
        super.switch32kPRGbank(data & 0x3);
    }
    
    /**
     * Updates PPU on a given address while rising A12 address line.
     * 
     * @param address       Address to update PPU
     */
    @Override
    public void updateAddressLines(int address) {
        if ((address >= 0x2000 & address <= 0x2FFF) || (address >= 0x6000 & address <= 0x6FFF)
             || (address >= 0xA000 & address <= 0xAFFF) || (address >= 0xE000 & address <= 0xEFFF)) {
            if ((address & 0x3FF) < 0x3C0) {
                super.switch4kCHRbank(((address & 0x300) >> 8) + (chrBlockSelect ? 0xF : 0x0), 0x0000);
                super.switch4kCHRbank(chrBlockSelect ? 0x12 : 0x3, 0x1000);
            }
        }
    }
}