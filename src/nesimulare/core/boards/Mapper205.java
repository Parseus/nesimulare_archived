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
public class Mapper205 extends TxROM {
    private int chrAND = 0;
    private int chrOR = 0;
    private int prgAND = 0;
    private int prgOR = 0;
    
    public Mapper205(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        chrAND = 0xFF;
        chrOR = 0x000;
        prgAND = 0x1F;
        prgOR = 0x00;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        switch (data & 0x3) {
            case 0:
                chrAND = 0xFF;
                chrOR = 0x000;
                prgAND = 0x1F;
                prgOR = 0x00;
                break;
            case 1:
                chrAND = 0xFF;
                chrOR = 0x080;
                prgAND = 0x1F;
                prgOR = 0x10;
                break;
            case 2:
                chrAND = 0x7F;
                chrOR = 0x100;
                prgAND = 0x0F;
                prgOR = 0x20;
                break;
            case 3:
                chrAND = 0x7F;
                chrOR = 0x180;
                prgAND = 0x0F;
                prgOR = 0x30;
                break;
            default:
                break;
        }
        
        setupPRG();
        setupCHR();
    }
    
    @Override
    protected void setupPRG() {
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
    
    @Override
    protected void setupCHR() {
        if (chrMode) {
            super.switch1kCHRbank((chrRegister[0] & chrAND) | chrOR, 0x1000);
            super.switch1kCHRbank(((chrRegister[0] & chrAND) + 1) | chrOR, 0x1400);
            super.switch1kCHRbank((chrRegister[1] & chrAND) | chrOR, 0x1800);
            super.switch1kCHRbank(((chrRegister[1] & chrAND) + 1) | chrOR, 0x1C00);
            super.switch1kCHRbank((chrRegister[2] & chrAND) | chrOR, 0x0000);
            super.switch1kCHRbank((chrRegister[3] & chrAND) | chrOR, 0x0400);
            super.switch1kCHRbank((chrRegister[4] & chrAND) | chrOR, 0x0800);
            super.switch1kCHRbank((chrRegister[5] & chrAND) | chrOR, 0x0C00);
        } else {
            super.switch1kCHRbank((chrRegister[0] & chrAND) | chrOR, 0x0000);
            super.switch1kCHRbank(((chrRegister[0] & chrAND) + 1) | chrOR, 0x0400);
            super.switch1kCHRbank((chrRegister[1] & chrAND) | chrOR, 0x0800);
            super.switch1kCHRbank(((chrRegister[1] & chrAND) + 1) | chrOR, 0x0C00);
            super.switch1kCHRbank((chrRegister[2] & chrAND) | chrOR, 0x1000);
            super.switch1kCHRbank((chrRegister[3] & chrAND) | chrOR, 0x1400);
            super.switch1kCHRbank((chrRegister[4] & chrAND) | chrOR, 0x1800);
            super.switch1kCHRbank((chrRegister[5] & chrAND) | chrOR, 0x1C00);
        }
    }
}