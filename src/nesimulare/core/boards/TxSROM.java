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
public class TxSROM extends TxROM {
    public TxSROM(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void writePRG(int address, int data) {
        if (address != 0xA000) {
            super.writePRG(address, data);
        }
    }
    
    @Override
    public int readNametable(int address) {
        switch ((address >> 10) & 0x3) {
            case 0:
                return nes.ppuram.nmt[(chrRegister[chrMode ? 2 : 0] & 0x80) >> 7][(address & 0x03FF)];
            case 1:
                return nes.ppuram.nmt[(chrRegister[chrMode ? 3 : 0] & 0x80) >> 7][(address & 0x03FF)];
            case 2:
                return nes.ppuram.nmt[(chrRegister[chrMode ? 4 : 1] & 0x80) >> 7][(address & 0x03FF)];
            case 3:
                return nes.ppuram.nmt[(chrRegister[chrMode ? 5 : 1] & 0x80) >> 7][(address & 0x03FF)];
            default:
                return 0;
        }
    }
    
    @Override
    public void writeNametable(int address, int data) {
        switch ((address >> 10) & 0x3) {
            case 0:
                nes.ppuram.nmt[(chrRegister[chrMode ? 2 : 0] & 0x80) >> 7][(address & 0x03FF)] = data;
                break;
            case 1:
                nes.ppuram.nmt[(chrRegister[chrMode ? 3 : 0] & 0x80) >> 7][(address & 0x03FF)] = data;
                break;
            case 2:
                nes.ppuram.nmt[(chrRegister[chrMode ? 4 : 1] & 0x80) >> 7][(address & 0x03FF)] = data;
                break;
            case 3:
                nes.ppuram.nmt[(chrRegister[chrMode ? 5 : 1] & 0x80) >> 7][(address & 0x03FF)] = data;
                break;
            default:
                break;
        }
    }
}   