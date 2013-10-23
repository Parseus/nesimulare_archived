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

package nesimulare.boards;

import nesimulare.cpu.CPU;
import nesimulare.memory.CPUMemory;
import nesimulare.memory.PPUMemory;
import nesimulare.ppu.PPU;

/**
 *
 * @author Parseus
 */
public class BoardInterface {

    CPU cpu;
    CPUMemory ram;
    PPU ppu;
    PPUMemory ppuram;
    
    protected int[] prg;
    protected int[] prgpage;
    protected int[] chr;
    protected int[] chrpage;
    protected int[] trainer;
    protected int[] sram = new int[0x2000];
    protected boolean haschrram;
    protected int prgmask;
    protected int chrmask;
    protected String[] filename;
    public static enum MirrorType {
        H_MIRROR,V_MIRROR,SS_MIRRORA, SS_MIRRORB, FOUR_SCREEN_MIRROR
    };
    
    /**
     *
     * @param prg
     * @param chr
     * @param trainer
     * @param haschrram
     */
    public BoardInterface(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        this.prg = prg;
        this.chr = chr;
        this.trainer = trainer;
        this.haschrram = haschrram;
        
        this.prgpage = new int[4];
        this.chrpage = new int[8];
        
        this.prgmask = prg.length - 1;
        this.chrmask = chr.length - 1;
    }
    
    public void hardReset() {
        sram = new int[0x2000];
        switch8kCHRbank(0, 0);
        switch32kPRGbank(0, 0);
    }
    
    /**
     *
     */
    public void softReset() {
        //placeholder
    }

    /**
     *
     * @param index
     * @param addr
     */
    protected void switch8kPRGbank(int index, int addr) {
        final int bank = index << 13;
        
        switch (addr) {
            case 0x8000: prgpage[0] = bank; break;
            case 0xA000: prgpage[1] = bank; break;
            case 0xC000: prgpage[2] = bank; break;
            case 0xE000: prgpage[3] = bank; break;
            default: break;
        }
    }
    
    /**
     *
     * @param index
     * @param addr
     */
    protected void switch16kPRGbank(int index, int addr) {
        int bank = index << 14;
        
        switch (addr) {
            case 0x8000: 
                prgpage[0] = bank;
                bank += 0x2000;
                prgpage[1] = bank;
                break;
            case 0xC000:
                prgpage[2] = bank;
                bank += 0x2000;
                prgpage[3] = bank;
                break;
            default:
                break;
        }
    }
    
    /**
     *
     * @param index
     * @param addr
     */
    protected void switch32kPRGbank(int index, int addr) {
        int bank = index << 15;
        
        for (int i = 0; i < 4; i++) {
            prgpage[i] = bank;
            bank += 0x2000;
        }
    }
    
    /**
     *
     * @param index
     * @param addr
     */
    protected void switch1kCHRbank(int index, int addr) {
        chrpage[(addr >> 10) & 7] = index << 10;
    }
    
    /**
     *
     * @param index
     * @param addr
     */
    protected void switch2kCHRbank(int index, int addr) {
        int area = (addr >> 10) & 7;
        int bank = index << 11;
        
        for (int i = 0; i < 2; i++) {
            chrpage[area] = bank;
            area++;
            bank += 0x400;
        }
    }
    
    /**
     *
     * @param index
     * @param addr
     */
    protected void switch4kCHRbank(int index, int addr) {
        int area = (addr >> 10) & 7;
        int bank = index << 12;
        
        for (int i = 0; i < 4; i++) {
            chrpage[area] = bank;
            area++;
            bank += 0x400;
        }
    }
    
    /**
     *
     * @param index
     * @param addr
     */
    protected void switch8kCHRbank(int index, int addr) {
        int area = (addr >> 10) & 7;
        int bank = index << 13;
        
        for (int i = 0; i < 8; i++) {
            chrpage[area] = bank;
            area++;
            bank += 0x400;
        }
    }
    
    protected int decodePRGAddress(int address) {
        if (address < 0xA000) {
            return (address & 0x1FFF) | prgpage[0];
        } else if (address < 0xC000) {
            return (address & 0x1FFF) | prgpage[1];
        } else if (address < 0xE000) {
            return (address & 0x1FFF) | prgpage[2];
        } else {
            return (address & 0x1FFF) | prgpage[3];
        }
    }
    
    protected int decodeCHRAddress(int address) {
        if (address < 0x0400) {
            return (address & 0x03FF) | chrpage[0];
        } else if (address < 0x0800) {
            return (address & 0x03FF) | chrpage[1];
        } else if (address < 0x0C00) {
            return (address & 0x03FF) | chrpage[2];
        } else if (address < 0x1000) {
            return (address & 0x03FF) | chrpage[3];
        } else if (address < 0x1400) {
            return (address & 0x03FF) | chrpage[4];
        } else if (address < 0x1800) {
            return (address & 0x03FF) | chrpage[5];
        } else if (address < 0x1C00) {
            return (address & 0x03FF) | chrpage[6];
        } else {
            return (address & 0x03FF) | chrpage[7];
        }
    }
    
    protected int getBusData(final int address, final int data) {
        return data & ram.read(address & 0x7FFF);
    }
    
    public int readEXP(final int address) {
        return 0xFF;
    }
    
    
    public int readPRG(final int address) {
        return prg[decodePRGAddress(address) & prgmask];
    }
    
    /**
     *
     * @param addr
     * @param data
     * @return
     */
    public int readSRAM(int addr) {
        return sram[addr - 0x6000];
    }
    
    /**
     *
     * @param addr
     * @param data
     */
    public void writeSRAM(int addr, int data) {
        sram[addr - 0x6000] = data;
    }
    
    public void writeEXP(final int address, final int data) {
        
    }
    
    public void writePRG(final int address, final int data) {
        
    }
    
    public void writeCHR(final int address, final int data) {
        ppuram.write(address, data);
    }
}