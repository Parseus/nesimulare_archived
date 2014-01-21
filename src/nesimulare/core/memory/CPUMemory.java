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

package nesimulare.core.memory;

import java.util.Arrays;
import nesimulare.core.NES;

public class CPUMemory extends Memory  {
    public NES nes;
    
    private int[] wram = new int[2048];
    
    public CPUMemory(NES nes) {
        super(0x10000);
        this.nes = nes;
    }
    
    @Override
    public void initialize() {
        hardReset();
    }
    
    @Override
    public int read(final int address) {  
        if (address >= 0x8000) {
            return nes.board.readPRG(address);      //Optimization - PRG reads are the most common reads on NES
        } else if (address < 0x0800) {
            return wram[address];
        } else if (address < 0x2000) {
            return wram[address & 0x7FF];
        } else if (address < 0x4000) {
            return nes.ppu.read(address);
        } else if (address < 0x4020) {
            return nes.apu.read(address);
        } else if (address < 0x6000) {
            return nes.board.readEXP(address);
        } else if (address < 0x8000) {
            return nes.board.readSRAM(address);
        } else {
            return address >> 8;        //Open bus
        }
    }
    
    @Override
    public void write(final int address, final int data) {
        if (address < 0x0800) {
            wram[address] = data;
        } else if (address < 0x2000) {
            wram[address & 0x7FF] = data;
        } else if (address < 0x4000 || address == 0x4014) {
            nes.ppu.write(address, data);
        } else if (address < 0x4020) {
            nes.apu.write(address, data);
        } else if (address < 0x6000) {
            nes.board.writeEXP(address, data);
        } else if (address < 0x8000) {
            nes.board.writeSRAM(address, data);
        } else {
            nes.board.writePRG(address, data);
        }
    }
    
    @Override
    public final void hardReset() {
        wram = new int[2048];
        
        for (int i = 0; i < 0x800; i++) {
            if ((i & 4) != 0) {
                wram[i] = 0xFF;
            }
        }
    }
}