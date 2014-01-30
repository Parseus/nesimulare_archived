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
 * Emulates a AVE-NINA-03 board (mapper 79) and a AVE-NINA-06 board (mapper 113).
 * Mapper 113 features a mapper-controlled mirroring.
 *
 * @author Parseus
 */
public class AVE_NINA_03_06 extends Board {
    private boolean mirroring = false;
    
    /**
     * Constructor for this class.
     * 
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM
     *                  False: PCB contains CHR-ROM
     */
    public AVE_NINA_03_06(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    /**
     * Initializes the board.
     */
    @Override
    public void initialize() {
        super.initialize();
        
        mirroring = (nes.loader.mapperNumber == 113);
    }
    
    /**
     * Writes data to a given address within the range $4020-$5FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writeEXP(int address, int data) {
        /**
         * $4100-$5FFF, mask: $4100
         * [MCPP PCCC]
         * C = CHR Reg (8k @ $0000)
         * P = PRG Reg (32k @ $8000)
         * M = Mirroring (only on mapper 113):
                0 = Horizontal
                1 = Vertical
         */
        
        if ((address & 0x4100) == 0x4100) {
            super.switch32kPRGbank((data & 0x38) >> 3);
            super.switch8kCHRbank((data & 0x7) | ((data & 0x40) >> 3));
            
            if (mirroring) {
                nes.ppuram.setMirroring(Tools.getbit(data, 7) ? PPUMemory.Mirroring.VERTICAL : PPUMemory.Mirroring.HORIZONTAL);
            }
        }
    }
}