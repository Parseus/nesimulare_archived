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

import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class Mapper191 extends TxROM {
    private int chrRAM[] = new int[2048];

    public Mapper191(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        chrRAM = new int[2048];
    }
    
    @Override
    public int readCHR(int address) {
        final int chrLength = chr.length;
        final int newAddress = decodeCHRAddress(address);
        
        if (newAddress < chrLength) {
            return super.readCHR(address);
        } else {
            return chrRAM[newAddress - chrLength];
        }
    }
    
    @Override
    public void writeCHR(int address, int data) {
        final int chrLength = chr.length;
        
        if (address < 0x0400) {
            if (chrpage[0] < chrLength) {
                chr[((address & 0x03FF) | chrpage[0]) & chrmask] = data;
            } else {
                chrRAM[((address & 0x03FF) | (chrpage[0] - chrLength)) & 0x1FFF] = data;
            }
        } else if (address < 0x0800) {
            if (chrpage[1] < chrLength) {
                chr[((address & 0x03FF) | chrpage[1]) & chrmask] = data;
            } else {
                chrRAM[((address & 0x03FF) | (chrpage[1] - chrLength)) & 0x1FFF] = data;
            }
        } else if (address < 0x0C00) {
            if (chrpage[2] < chrLength) {
                chr[((address & 0x03FF) | chrpage[2]) & chrmask] = data;
            } else {
                chrRAM[((address & 0x03FF) | (chrpage[2] - chrLength)) & 0x1FFF] = data;
            }
        } else if (address < 0x1000) {
            if (chrpage[3] < chrLength) {
                chr[((address & 0x03FF) | chrpage[3]) & chrmask] = data;
            } else {
                chrRAM[((address & 0x03FF) | (chrpage[3] - chrLength)) & 0x1FFF] = data;
            }
        } else if (address < 0x1400) {
            if (chrpage[4] < chrLength) {
                chr[((address & 0x03FF) | chrpage[4]) & chrmask] = data;
            } else {
                chrRAM[((address & 0x03FF) | (chrpage[4] - chrLength)) & 0x1FFF] = data;
            }
        } else if (address < 0x1800) {
            if (chrpage[5] < chrLength) {
                chr[((address & 0x03FF) | chrpage[5]) & chrmask] = data;
            } else {
                chrRAM[((address & 0x03FF) | (chrpage[5] - chrLength)) & 0x1FFF] = data;
            }
        } else if (address < 0x1C00) {
            if (chrpage[6] < chrLength) {
                chr[((address & 0x03FF) | chrpage[6]) & chrmask] = data;
            } else {
                chrRAM[((address & 0x03FF) | (chrpage[6] - chrLength)) & 0x1FFF] = data;
            }
        } else {
            if (chrpage[7] < chrLength) {
                chr[((address & 0x03FF) | chrpage[7]) & chrmask] = data;
            } else {
                chrRAM[((address & 0x03FF) | (chrpage[7] - chrLength)) & 0x1FFF] = data;
            }
        }
    }
    
    @Override
    protected void setupCHR() {
        final int chrLength = chr.length;
        
        if (chrMode) {
            super.switch1kCHRbank(Tools.getbit(chrRegister[0], 7) ? chrRegister[0] : (chrLength >> 10), 0x0000);
            super.switch1kCHRbank(Tools.getbit(chrRegister[0], 7) ? (chrRegister[0] + 1) : (chrLength >> 10), 0x0400);
            super.switch1kCHRbank(Tools.getbit(chrRegister[1], 7) ? chrRegister[1] : (chrLength >> 10), 0x0800);
            super.switch1kCHRbank(Tools.getbit(chrRegister[1], 7) ? (chrRegister[1] + 1) : (chrLength >> 10), 0x0C00);
            super.switch1kCHRbank(Tools.getbit(chrRegister[2], 7) ? chrRegister[2] : (chrLength >> 10), 0x1000);
            super.switch1kCHRbank(Tools.getbit(chrRegister[3], 7) ? chrRegister[3] : (chrLength >> 10), 0x1400);
            super.switch1kCHRbank(Tools.getbit(chrRegister[4], 7) ? chrRegister[4] : (chrLength >> 10), 0x1800);
            super.switch1kCHRbank(Tools.getbit(chrRegister[5], 7) ? chrRegister[5] : (chrLength >> 10), 0x1C00);
        } else {
            super.switch1kCHRbank(Tools.getbit(chrRegister[0], 7) ? chrRegister[0] : (chrLength >> 10), 0x1000);
            super.switch1kCHRbank(Tools.getbit(chrRegister[0], 7) ? (chrRegister[0] + 1) : (chrLength >> 10), 0x1400);
            super.switch1kCHRbank(Tools.getbit(chrRegister[1], 7) ? chrRegister[1] : (chrLength >> 10), 0x1800);
            super.switch1kCHRbank(Tools.getbit(chrRegister[1], 7) ? (chrRegister[1] + 1) : (chrLength >> 10), 0x1C00);
            super.switch1kCHRbank(Tools.getbit(chrRegister[2], 7) ? chrRegister[2] : (chrLength >> 10), 0x0000);
            super.switch1kCHRbank(Tools.getbit(chrRegister[3], 7) ? chrRegister[3] : (chrLength >> 10), 0x0400);
            super.switch1kCHRbank(Tools.getbit(chrRegister[4], 7) ? chrRegister[4] : (chrLength >> 10), 0x0800);
            super.switch1kCHRbank(Tools.getbit(chrRegister[5], 7) ? chrRegister[5] : (chrLength >> 10), 0x0C00);
        }
    }
}