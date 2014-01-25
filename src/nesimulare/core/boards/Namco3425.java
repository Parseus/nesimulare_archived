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
public class Namco3425 extends Board {
    private int register = 0;
    private int chrRegister[] = new int[6];
    private int prgRegister[] = new int[2];
    private boolean chrMode = false;
    
    public Namco3425(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        register = 0;
        chrRegister = new int[6];
        prgRegister = new int[2];
        super.switch8kPRGbank((prg.length - 0x4000) >> 13, 0xC000);
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
        setupPRG();
        chrMode = false;
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xE001) {
            case 0x8000:
                register = data & 0x7;
                break;
            case 0x8001:
                if (register <= 5) {
                    chrRegister[register] = data;
                    setupCHR();
                } else {
                    prgRegister[register & 1] = data & 0x3F;
                    setupPRG();
                }
                break;
            default:
                break;
        }
    }
    
    @Override
    public int readNametable(int address) {
        switch ((address >> 10) & 0x3) {
            case 0:
                return nes.ppuram.nmt[(chrRegister[chrMode ? 2 : 0] & 0x20) >> 5][(address & 0x03FF)];
            case 1:
                return nes.ppuram.nmt[(chrRegister[chrMode ? 3 : 0] & 0x20) >> 5][(address & 0x03FF)];
            case 2:
                return nes.ppuram.nmt[(chrRegister[chrMode ? 4 : 1] & 0x20) >> 5][(address & 0x03FF)];
            case 3:
                return nes.ppuram.nmt[(chrRegister[chrMode ? 5 : 1] & 0x20) >> 5][(address & 0x03FF)];
            default:
                return (address >> 8 & 0xe0);
        }
    }
    
    @Override
    public void writeNametable(int address, int data) {
        switch ((address >> 10) & 0x3) {
            case 0:
                nes.ppuram.nmt[(chrRegister[chrMode ? 2 : 0] & 0x20) >> 5][(address & 0x03FF)] = data;
                break;
            case 1:
                nes.ppuram.nmt[(chrRegister[chrMode ? 3 : 0] & 0x20) >> 5][(address & 0x03FF)] = data;
                break;
            case 2:
                nes.ppuram.nmt[(chrRegister[chrMode ? 4 : 1] & 0x20) >> 5][(address & 0x03FF)] = data;
                break;
            case 3:
                nes.ppuram.nmt[(chrRegister[chrMode ? 5 : 1] & 0x20) >> 5][(address & 0x03FF)] = data;
                break;
            default:
                break;
        }
    }
    
    private void setupPRG() {
        super.switch8kPRGbank(prgRegister[0], 0x8000);
        super.switch8kPRGbank(prgRegister[1], 0xA000);
    }
    
    private void setupCHR() {
        super.switch2kCHRbank(chrRegister[0] >> 1, 0x0000);
        super.switch2kCHRbank(chrRegister[1] >> 1, 0x0800);
        super.switch1kCHRbank(chrRegister[2], 0x1000);
        super.switch1kCHRbank(chrRegister[3], 0x1400);
        super.switch1kCHRbank(chrRegister[4], 0x1800);
        super.switch1kCHRbank(chrRegister[5], 0x1C00);
    }
}