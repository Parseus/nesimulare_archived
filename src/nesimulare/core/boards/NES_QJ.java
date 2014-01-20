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

/**
 *
 * @author Parseus
 */
public class NES_QJ extends TxROM {
    private int blockSelect = 0;
    
    public NES_QJ(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        blockSelect = 0;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        if (wramEnable && !wramWriteProtect) {
            blockSelect = data & 1;
            setupPRG();
            setupCHR();
        }
    }
    
    @Override
    protected void setupPRG() {
        if (prgMode) {
            super.switch8kPRGbank((prgRegister[2] & 0xF) | (blockSelect << 4), 0x8000);
            super.switch8kPRGbank((prgRegister[1] & 0xF) | (blockSelect << 4), 0xA000);
            super.switch8kPRGbank((prgRegister[0] & 0xF) | (blockSelect << 4), 0xC000);
            super.switch8kPRGbank((prgRegister[3] & 0xF) | (blockSelect << 4), 0xE000);
        } else {
            super.switch8kPRGbank((prgRegister[0] & 0xF) | (blockSelect << 4), 0x8000);
            super.switch8kPRGbank((prgRegister[1] & 0xF) | (blockSelect << 4), 0xA000);
            super.switch8kPRGbank((prgRegister[2] & 0xF) | (blockSelect << 4), 0xC000);
            super.switch8kPRGbank((prgRegister[3] & 0xF) | (blockSelect << 4), 0xE000);
        }
    }
    
    @Override
    protected void setupCHR() {
        final int blockOR = (blockSelect << 7);
        
        if (chrMode) {
            super.switch1kCHRbank((chrRegister[0] & 0x7F) | blockOR, 0x1000);
            super.switch1kCHRbank(((chrRegister[0] + 1) & 0x7F) | blockOR, 0x1400);
            super.switch1kCHRbank((chrRegister[1] & 0x7F) | blockOR, 0x1800);
            super.switch1kCHRbank(((chrRegister[1] + 1) & 0x7F) | blockOR, 0x1C00);
            super.switch1kCHRbank((chrRegister[2] & 0x7F) | blockOR, 0x0000);
            super.switch1kCHRbank((chrRegister[3] & 0x7F) | blockOR, 0x0400);
            super.switch1kCHRbank((chrRegister[4] & 0x7F) | blockOR, 0x0800);
            super.switch1kCHRbank((chrRegister[5] & 0x7F) | blockOR, 0x0C00);
        } else {
            super.switch1kCHRbank((chrRegister[0] & 0x7F) | blockOR, 0x0000);
            super.switch1kCHRbank(((chrRegister[0] + 1) & 0x7F) | blockOR, 0x0400);
            super.switch1kCHRbank((chrRegister[1] & 0x7F) | blockOR, 0x0800);
            super.switch1kCHRbank(((chrRegister[1] + 1) & 0x7F) | blockOR, 0x0C00);
            super.switch1kCHRbank((chrRegister[2] & 0x7F) | blockOR, 0x1000);
            super.switch1kCHRbank((chrRegister[3] & 0x7F) | blockOR, 0x1400);
            super.switch1kCHRbank((chrRegister[4] & 0x7F) | blockOR, 0x1800);
            super.switch1kCHRbank((chrRegister[5] & 0x7F) | blockOR, 0x1C00);
        }
    }
}