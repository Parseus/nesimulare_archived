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

package nesimulare.core.memory;

import nesimulare.core.NES;

public final class PPUMemory extends Memory  {
    public NES nes;
    
    public enum Mirroring {
        HORIZONTAL(0x05), VERTICAL(0x11), ONESCREENA(0x00), ONESCREENB(0x55), FOURSCREEN (0x1B);
        
        public final int value;
        
        Mirroring(final int value) {
            this.value = value;
        }   
    }
    
    private int[] paletteRAM;
    public int[][] nmt;
    public int[] nmtBank;
    
    public PPUMemory(NES nes) {
        super(0x4000);
        this.nes = nes;
    }
    
    @Override
    public void initialize() {
        hardReset();
    }
    
    @Override
    public int read(int address) {
        final int addr = address & mask;
        
        if (addr >= 0x0000 && addr <= 0x1FFF) {
            return nes.board.readCHR(addr);
        } else if (addr >= 0x2000 && addr <= 0x3EFF) {
            return readNametable(addr);
        } else if (addr >= 0x3F00 && addr <= 0x3FFF) {
            return readPalette(addr);
        } else {
            //Open bus
            return addr >> 8;
        }
    }
    
    @Override
    public void write(int address, int data) {
        final int addr = address & mask;
        
        if (addr >= 0x0000 && addr <= 0x1FFF) {
            nes.board.writeCHR(addr, data);
        } else if (addr >= 0x2000 && addr <= 0x3EFF) {
            writeNametable(addr, data);
        } else if (addr >= 0x3F00 && addr <= 0x3FFF) {
            writePalette(addr, data);
        } 
    }
    
    private int readNametable(final int address) {
        return nmt[nmtBank[(address >> 10) & 3]][address & 0x03FF];
    }
    
    private int readPalette(final int address) {
        return paletteRAM[address & ((address & 3) == 0 ? 0x0C : 0x1F)];
    }
    
    private void writeNametable(final int address, final int data) {
        nmt[nmtBank[(address >> 10) & 3]][address & 0x03FF] = data;
    }
    
    private void writePalette(final int address, final int data) {
        paletteRAM[address & ((address & 3) == 0 ? 0x0C : 0x1F)] = data;
    }
    
    public void setMirroring(Mirroring mirror) {
        setMirroring(mirror.value);
    }
    
    public void setMirroring(final int data) {
        nmtBank[0] = ((data >> 6 & 0xFF) & 3);
        nmtBank[1] = ((data >> 4 & 0xFF) & 3);
        nmtBank[2] = ((data >> 2 & 0xFF) & 3);
        nmtBank[3] = (data & 3);
    }
    
    public void setNametable(final int data) {
        nmtBank[3] = ((data >> 6 & 0xFF) & 3);
        nmtBank[2] = ((data >> 4 & 0xFF) & 3);
        nmtBank[1] = ((data >> 2 & 0xFF) & 3);
        nmtBank[0] = (data & 3);
    }
    
    @Override
    public void hardReset() {
        nmtBank = new int[4];
        setMirroring(nes.getLoader().mirroring);
        
        /* Each NES has a different palette on power-on. This palette is still 
         * used as a 'canonical' palette for emulation by several emulators, though.*/
        paletteRAM = new int[] {
            0x09, 0x01, 0x00, 0x01, 0x00, 0x02, 0x02, 0x0D, 0x08, 0x10, 0x08, 0x24, 0x00, 0x00, 0x04, 0x2C,     //Background palette
            0x09, 0x01, 0x34, 0x03, 0x00, 0x04, 0x00, 0x14, 0x08, 0x3A, 0x00, 0x02, 0x00, 0x20, 0x2C, 0x08      //Sprite palette
        };
        
        nmt = new int[4][];
        nmt[0] = new int[0x400];
        nmt[1] = new int[0x400];
        nmt[2] = new int[0x400];
        nmt[3] = new int[0x400];
    }
}