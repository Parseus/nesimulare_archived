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
 * Emulates a IREM-74*161/161/21/138 board (mapper 77).
 * This board uses an 8 KiB SRAM to provide both 6 KiB of CHR-RAM and four-screen mirroring.
 *
 * @author Parseus
 */
public class IREM_74_161_161_21_138 extends Board {
    private final int chrram[] = new int[0x2000];
    
    /**
     * Constructor for this class.
     * 
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM
     *                  False: PCB contains CHR-ROM
     */
    public IREM_74_161_161_21_138(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    /**
     * Writes data to a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writePRG(int address, int data) {
        super.switch32kPRGbank(data & 0xF);
        super.switch2kCHRbank((data & 0xF0) >> 4, 0x0000);
    }
    
    /**
     * Reads PPU data from a given address within the range $0000-$1FFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    @Override
    public int readCHR(int address) {
        if (address < 0x800) {
            return super.readCHR(address);
        } else {
            return chrram[address - 0x800];
        }
    }
  
    /**
     * Writes PPU data to a given address within the range $0000-$1FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writeCHR(int address, int data) {
        if (address < 0x800) {
            super.writeCHR(address, data);
        } else {
            chrram[address - 0x800] = data;
        }
    }
}