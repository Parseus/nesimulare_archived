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

import nesimulare.core.NES;

/**
 * Main class for board/mapper emulation.
 *
 * @author Parseus
 */
public class Board {
    NES nes;
    
    protected int[] prg;
    protected int[] prgpage;
    protected int[] chr;
    protected int[] chrpage;
    protected int[] trainer;
    public int[] sram;
    protected boolean haschrram;
    protected int prgmask;
    protected int chrmask;
    protected String[] filename;
    private static final boolean LOGGING = false;
    
    /**
     * Constructor for this class.
     * 
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM
     *                  False: PCB contains CHR-ROM
     */
    public Board(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        this.prg = prg.clone();
        this.chr = chr.clone();
        this.trainer = trainer;
        this.haschrram = haschrram;
        
        this.prgpage = new int[4];
        this.chrpage = new int[8];
        
        this.prgmask = prg.length - 1;
        this.chrmask = chr.length - 1;
    }
    
    /**
     * Initializes the board.
     */
    public void initialize() {
        hardReset();
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    public void hardReset() {
        sram = new int[nes.loader.prgramSize];
        switch8kCHRbank(0);
        switch32kPRGbank(0);
    }
    
    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    public void softReset() {
        //placeholder
    }

    /**
     * Switches a 8 kB PRG bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     * @param addr      Address to switch a memory bank at
     */
    protected void switch8kPRGbank(int data, int addr) {
        final int bank = data << 13;
        
        switch (addr) {
            case 0x8000: prgpage[0] = bank; break;
            case 0xA000: prgpage[1] = bank; break;
            case 0xC000: prgpage[2] = bank; break;
            case 0xE000: prgpage[3] = bank; break;
            default: break;
        }
    }
    
    /**
     * Switches a 16 kB PRG bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     * @param addr      Address to switch a memory bank at
     */
    protected void switch16kPRGbank(int data, int addr) {
        int bank = data << 14;
        
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
     * Switches a 32 kB PRG bank.
     *
     * @param data      Data to compute a memory bank from
     */
    protected void switch32kPRGbank(int data) {
        int bank = data << 15;
        
        for (int i = 0; i < 4; i++) {
            prgpage[i] = bank;
            bank += 0x2000;
        }
    }
    
    /**
     * Switches a 1 kB CHR bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     * @param addr      Address to switch a memory bank at
     */
    protected void switch1kCHRbank(int data, int addr) {
        chrpage[(addr >> 10) & 7] = data << 10;
    }
    
    /**
     * Switches a 2 kB CHR bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     * @param addr      Address to switch a memory bank at
     */
    protected void switch2kCHRbank(int data, int addr) {
        int area = (addr >> 10) & 7;
        int bank = data << 11;
        
        for (int i = 0; i < 2; i++) {
            chrpage[area] = bank;
            area++;
            bank += 0x400;
        }
    }
    
    /**
     * Switches a 4 kB CHR bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     * @param addr      Address to switch a memory bank at
     */
    protected void switch4kCHRbank(int data, int addr) {
        int area = (addr >> 10) & 7;
        int bank = data << 12;
        
        for (int i = 0; i < 4; i++) {
            chrpage[area] = bank;
            area++;
            bank += 0x400;
        }
    }
    
    /**
     * Switches a 8 kB CHR bank.
     *
     * @param data      Data to compute a memory bank from
     */
    protected void switch8kCHRbank(int data) {
        int bank = data << 13;
        
        for (int i = 0; i < 8; i++) {
            chrpage[i] = bank;
            bank += 0x400;
        }
    }
    
    /**
     * Decodes a PRG address.
     * 
     * @param address       Address to decode
     * @return              Decoded address
     */
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
    
    /**
     * Decodes a CHR address.
     * 
     * @param address       Address to decode
     * @return              Decoded address
     */
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
    
    /**
     * Returns saved data on the SRAM.
     * 
     * @return      Saved data on the SRAM
     */
    public int[] getSRAM() {
        return sram.clone();
    }
    
    /**
     * Loads saved data to the SRAM.
     * 
     * @param sram  Data from the SRAM loaded from a file
     */
    public void setSRAM(int[] sram) {
        this.sram = sram.clone();
    }
    
    /**
     * Returns a data from a given address, considering bus conflicts.
     * 
     * @param address       Address to read data from
     * @param data          Data to pass through an address bus
     * @return              Data passed through an address bus
     */
    protected int getBusData(final int address, final int data) {
        return data & readPRG(address);
    }
    
    /**
     * Reads data from a given address within the range $4020-$5FFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    public int readEXP(final int address) {
        return (address >> 8 & 0xe0);
    }
    
    /**
     * Reads data from a given address within the range $6000-$7FFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    public int readSRAM(int address) {
        return sram[address - 0x6000];
    }
    
    /**
     * Reads data from a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    public int readPRG(final int address) {
        return prg[decodePRGAddress(address) & prgmask];
    }
    
    /**
     * Reads PPU data from a given address within the range $0000-$1FFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    public int readCHR(int address) {
        return chr[decodeCHRAddress(address) & chrmask];
    }
    
    /**
     * Writes data to a given address within the range $4020-$5FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    public void writeEXP(final int address, final int data) {
        //Placeholder
    }
    
    /**
     * Writes data to a given address within the range $6000-$7FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    public void writeSRAM(int address, int data) {
        sram[address & 0x1FFF] = data;
        
        if (LOGGING) {
            int c, addr = 0x6004;
            final StringBuilder testResults = new StringBuilder();
            
            while ((c = nes.cpuram.read(addr++)) != 0) {
                testResults.append((char)c);
            }
            
            System.err.println(testResults.toString());
            //nes.messageBox("Test results: " + testResults.toString());
        }
    }
        
    /**
     * Writes data to a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    public void writePRG(final int address, final int data) {
        //Placeholder
    }
    
    /**
     * Writes PPU data to a given address within the range $0000-$1FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    public void writeCHR(final int address, final int data) {
        if (haschrram) {
            chr[decodeCHRAddress(address) & chrmask] = data;
        }
    }
    
    /**
     * Reads nametable data from a given address within the range $2000-$3EFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    public int readNametable(int address) {
        return (address >> 8 & 0xe0);
    }
    
    /**
     * Writes nametable data to a given address within the range $2000-$3EFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    public void writeNametable(final int address, final int data) {
        //Placeholder
    }
    
    /**
     * Clocks IRQ every CPU cycle.
     */
    public void clockCPUCycle() {
        //Placeholder
    }
    
    /**
     * Clocks IRQ every PPU cycle.
     */
    public void clockPPUCycle() {
        //Placeholder
    }
    
    /**
     * Clocks IRQ every scanline tick.
     */
    public void scanlineTick() {
        //Placeholder
    }
    
    /**
     * Updates PPU on a given address while rising A12 address line.
     * 
     * @param address       Address to update PPU
     */
    public void updateAddressLines(final int address) {
        //Placeholder
    }
    
    /**
     * Connects board to the emulation core.
     * 
     * @param nes       Emulation core
     */
    public void setCore(NES nes) {
        this.nes = nes;
    }
}