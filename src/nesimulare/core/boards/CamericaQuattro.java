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
 * Emulates a Camerica Quattro boardset (mapper 232).
 *
 * @author Parseus
 */
public class CamericaQuattro extends Board {
    private int bank = 3, game = 0;
    
    /**
     * Constructor for this class.
     * 
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM
     *                  False: PCB contains CHR-ROM
     */
    public CamericaQuattro(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();
        
        bank = 3; 
        game = 0;
        setupPRG();
    }
    
    /**
     * Writes data to a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writePRG(int address, int data) {
        if (address < 0xC000) {
            /**
             * $8000-$BFFF: Outer bank select
             * 7  bit  0
             * ---- ----
             * xxxB Bxxx
             *    | |
             *    +-+--- Select 64 KiB PRG ROM bank for CPU $8000-$FFFF
             */
            game = (data & 0x18) >> 1;
            setupPRG();
        } else {
            /**
             * $C000-$FFFF: Bank select
             * 7  bit  0
             * ---- ----
             * xxxx xxPP
             *        ||
             *        ++- Select 16 KiB PRG ROM bank for CPU $8000-$BFFF
             */
            bank = data & 3;
            setupPRG();
        }
    }
    
    /**
     * Sets up PRG bankswitching.
     */
    private void setupPRG() {
        super.switch16kPRGbank(game | bank, 0x8000);
        super.switch16kPRGbank(game | 0x3, 0xC000);
    }
}