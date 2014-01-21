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
 *
 * @author Parseus
 */
public class NES_ZZ extends TxROM {
    private int blockSelect = 0;
    
    public NES_ZZ(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        blockSelect = 0;
    }
    
    @Override
    public int readSRAM(int address) {
        return address >> 8;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        if (wramEnable && !wramWriteProtect) {
            blockSelect = data & 7;
        }
    }
    
    @Override
    protected void setupPRG() {
        final int blockSelect1 = (blockSelect << 2 & 0x10) | ((blockSelect & 0x3) == 0x3 ? 0x08 : 0x00);
        final int blockSelect2 = (blockSelect << 1 | 0x7);
        
        if (prgMode) {
            super.switch8kPRGbank(blockSelect1 | (prgRegister[2] & blockSelect2), 0x8000);
            super.switch8kPRGbank(blockSelect1 | (prgRegister[1] & blockSelect2), 0xA000);
            super.switch8kPRGbank(blockSelect1 | (prgRegister[0] & blockSelect2), 0xC000);
            super.switch8kPRGbank(blockSelect1 | (prgRegister[3] & blockSelect2), 0xE000);
        } else {
            super.switch8kPRGbank(blockSelect1 | (prgRegister[0] & blockSelect2), 0x8000);
            super.switch8kPRGbank(blockSelect1 | (prgRegister[1] & blockSelect2), 0xA000);
            super.switch8kPRGbank(blockSelect1 | (prgRegister[2] & blockSelect2), 0xC000);
            super.switch8kPRGbank(blockSelect1 | (prgRegister[3] & blockSelect2), 0xE000);
        }
    }
    
    @Override
    protected void setupCHR() {
        final int blockSelect1 = (blockSelect << 5 & 0x80);
        
        if (chrMode) {
            super.switch2kCHRbank(blockSelect1 | (chrRegister[0] >> 1 & 0x7F), 0x1000);
            super.switch2kCHRbank(blockSelect1 | (chrRegister[1] >> 1 & 0x7F), 0x1800);
            super.switch1kCHRbank(blockSelect1 | (chrRegister[2] & 0x7F), 0x0000);
            super.switch1kCHRbank(blockSelect1 | (chrRegister[3] & 0x7F), 0x0400);
            super.switch1kCHRbank(blockSelect1 | (chrRegister[4] & 0x7F), 0x0800);
            super.switch1kCHRbank(blockSelect1 | (chrRegister[5] & 0x7F), 0x0C00);
        } else {
            super.switch1kCHRbank(blockSelect1 | (chrRegister[2] & 0x7F), 0x0000);
            super.switch1kCHRbank(blockSelect1 | (chrRegister[3] & 0x7F), 0x0400);
            super.switch1kCHRbank(blockSelect1 | (chrRegister[4] & 0x7F), 0x0800);
            super.switch1kCHRbank(blockSelect1 | (chrRegister[5] & 0x7F), 0x0C00);
            super.switch2kCHRbank(blockSelect1 | (chrRegister[0] >> 1 & 0x7F), 0x1000);
            super.switch2kCHRbank(blockSelect1 | (chrRegister[1] >> 1 & 0x7F), 0x1800);
        }
    }
}