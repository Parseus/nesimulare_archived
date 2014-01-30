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
 * Emulates mapper 52.
 *
 * @author Parseus
 */
public class Mapper052 extends TxROM {
    private int exreg = 0;
    private boolean writeEnabled = false;

    /**
     * Constructor for this class.
     *
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM False: PCB contains CHR-ROM
     */
    public Mapper052(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();
        
        exreg = 0;
        writeEnabled = false;
    }
    
    /**
     * Writes data to a given address within the range $6000-$7FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writeSRAM(int address, int data) {
        if (!writeEnabled) {
            if (wramEnable && !wramWriteProtect) {
                exreg = data;
                writeEnabled = true;
                
                setupPRG();
                setupCHR();
            }
        } else {
            if (wramEnable && !wramWriteProtect) {
                super.writeSRAM(address, data);
            }
        }
    }
    
    /**
     * Sets up PRG bankswitching depending on different masks.
     */
    @Override
    protected void setupPRG() {
        final int prgAND = (exreg << 1 & 0x10) ^ 0x1F;
        final int prgOR = ((exreg & 0x6) | (exreg >> 3 & exreg & 0x1)) << 4;
        
        if (prgMode) {
            super.switch8kPRGbank((prgRegister[2] & prgAND) | prgOR, 0x8000);
            super.switch8kPRGbank((prgRegister[1] & prgAND) | prgOR, 0xA000);
            super.switch8kPRGbank((prgRegister[0] & prgAND) | prgOR, 0xC000);
            super.switch8kPRGbank((prgRegister[3] & prgAND) | prgOR, 0xE000);
        } else {
            super.switch8kPRGbank((prgRegister[0] & prgAND) | prgOR, 0x8000);
            super.switch8kPRGbank((prgRegister[1] & prgAND) | prgOR, 0xA000);
            super.switch8kPRGbank((prgRegister[2] & prgAND) | prgOR, 0xC000);
            super.switch8kPRGbank((prgRegister[3] & prgAND) | prgOR, 0xE000);
        }
    }
    
    /**
     * Sets up CHR bankswitching depending on different masks.
     */
    @Override
    protected void setupCHR() {
        final int chrAND = ((exreg & 0x40) << 1) ^ 0xFF;
        final int chrOR = ((exreg >> 3 & 0x4) | (exreg >> 1 & 0x2) | ((exreg >> 6) & (exreg >> 4) & 0x1)) << 7;
        
        if (chrMode) {
            super.switch1kCHRbank((chrRegister[0] & chrAND) | chrOR, 0x1000);
            super.switch1kCHRbank(((chrRegister[0] + 1) & chrAND) | chrOR, 0x1400);
            super.switch1kCHRbank((chrRegister[1] & chrAND) | chrOR, 0x1800);
            super.switch1kCHRbank(((chrRegister[1] + 1) & chrAND) | chrOR, 0x1C00);
            super.switch1kCHRbank((chrRegister[2] & chrAND) | chrOR, 0x0000);
            super.switch1kCHRbank((chrRegister[3] & chrAND) | chrOR, 0x0400);
            super.switch1kCHRbank((chrRegister[4] & chrAND) | chrOR, 0x0800);
            super.switch1kCHRbank((chrRegister[5] & chrAND) | chrOR, 0x0C00);
        } else {
            super.switch1kCHRbank((chrRegister[0] & chrAND) | chrOR, 0x0000);
            super.switch1kCHRbank(((chrRegister[0] + 1) & chrAND) | chrOR, 0x0400);
            super.switch1kCHRbank((chrRegister[1] & chrAND) | chrOR, 0x0800);
            super.switch1kCHRbank(((chrRegister[1] + 1) & chrAND) | chrOR, 0x0C00);
            super.switch1kCHRbank((chrRegister[2] & chrAND) | chrOR, 0x1000);
            super.switch1kCHRbank((chrRegister[3] & chrAND) | chrOR, 0x1400);
            super.switch1kCHRbank((chrRegister[4] & chrAND) | chrOR, 0x1800);
            super.switch1kCHRbank((chrRegister[5] & chrAND) | chrOR, 0x1C00);
        }
    }
}