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
 *
 * @author Parseus
 */
public class Mapper245 extends TxROM {
    private boolean prgMode2 = false;
    
    public Mapper245(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void writePRG(int address, int data ) {
        if (address == 0x8001) {
            switch (register) {
                case 0:
                    prgMode2 = Tools.getbit(chrRegister[0], 1);
                    chrRegister[0] = data;
                    setupPRG();
                    setupCHR();
                    break;
                case 1: case 2: case 3: case 4: case 5:
                    chrRegister[register] = data;
                    setupCHR();
                    break;
                case 6: case 7:
                    prgRegister[register & 1] = data & 0x3F;
                    setupPRG();
                    break;
                default:
                    break;
            }
        } else {
            super.writePRG(address, data);
        }
    }
    
    @Override
    protected void setupPRG() {
        final int prgOR = prgMode2 ? 0x40 : 0x00;
        
        if (prgMode) {
            super.switch8kPRGbank((prgRegister[2] & 0x3F) | prgOR, 0x8000);
            super.switch8kPRGbank((prgRegister[1] & 0x3F) | prgOR, 0xA000);
            super.switch8kPRGbank((prgRegister[0] & 0x3F) | prgOR, 0xC000);
            super.switch8kPRGbank((prgRegister[3] & 0x3F) | prgOR, 0xE000);
        } else {
            super.switch8kPRGbank((prgRegister[0] & 0x3F) | prgOR, 0x8000);
            super.switch8kPRGbank((prgRegister[1] & 0x3F) | prgOR, 0xA000);
            super.switch8kPRGbank((prgRegister[2] & 0x3F) | prgOR, 0xC000);
            super.switch8kPRGbank((prgRegister[3] & 0x3F) | prgOR, 0xE000);
        }
    }
    
    @Override
    protected void setupCHR() {
        if (haschrram) {
            if (chrMode) {
                super.switch4kCHRbank(1, 0x0000);
                super.switch4kCHRbank(0, 0x1000);
            } else {
                super.switch4kCHRbank(0, 0x0000);
                super.switch4kCHRbank(1, 0x1000);
            }
        } else {
            super.setupCHR();
        }
    }
}