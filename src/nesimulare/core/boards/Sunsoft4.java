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

import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class Sunsoft4 extends Board {
    private int nametableAbank = 0;
    private int nametableBbank = 0;
    private boolean nametableMode = false;
    
    public Sunsoft4(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
        nametableAbank = 0;
        nametableBbank = 0;
        nametableMode = false;
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF000) {
            case 0x8000:
                super.switch2kCHRbank(data, 0x0000);
                break;
            case 0x9000:
                super.switch2kCHRbank(data, 0x0800);
                break;
            case 0xA000:
                super.switch2kCHRbank(data, 0x1000);
                break;
            case 0xB000:
                super.switch2kCHRbank(data, 0x1800);
                break;
            case 0xC000:
                nametableAbank = (data & 0x7F) | 0x80;
                break;
            case 0xD800:
                nametableBbank = (data & 0x7F) | 0x80;
                break;
            case 0xE000:
                nametableMode = Tools.getbit(data, 4);
                nes.ppuram.setMirroring(Tools.getbit(data, 0) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
                break;
            case 0xF000:
                super.switch16kPRGbank(data, 0x8000);
                break;
            default:
                break;
        }
    }
    
    @Override
    public int readNametable(int address) {
        if (nametableMode) {
            if (nes.ppuram.nmt[nes.ppuram.nmtBank[address >> 10 & 0x3]][address & 0x3FF] == 0) {
                return chr[(nametableAbank << 10) | (address & 0x3FF)];
            } else if (nes.ppuram.nmt[nes.ppuram.nmtBank[address >> 10 & 0x3]][address & 0x3FF] == 1) {
                return chr[(nametableBbank << 10) | (address & 0x3FF)];
            }
        } else {
            return nes.ppuram.nmt[nes.ppuram.nmtBank[address >> 10 & 0x3]][address & 0x3FF];
        }
        
        return nes.ppuram.nmt[nes.ppuram.nmtBank[address >> 10 & 0x3]][address & 0x3FF];
    }
    
    @Override
    public void writeNametable(int address, int data) {
        nes.ppuram.nmt[nes.ppuram.nmtBank[(address >> 10) & 3]][address & 0x03FF] = data;
    }
}