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

import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 * Emulates Jaleco JF-16 and Irem Holy Diver boards (mapper 78).
 *
 * @author Parseus
 */
public class Jaleco_JF_16 extends Board {
    private boolean jf16 = false;
    
    /**
     * Constructor for this class.
     *
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM False: PCB contains CHR-ROM
     */
    public Jaleco_JF_16(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    /**
     * Initializes the board.
     */
    @Override
    public void initialize() {
        super.initialize();
        
        jf16 = "BC6F5A884FD31FE6B4439E83AD6C2A29D038E545".equals(nes.loader.sha1);
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
    }
    
    /**
     * Writes data to a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writePRG(int address, int data) {
        if (jf16) {
            nes.ppuram.setMirroring(Tools.getbit(data, 3) ? PPUMemory.Mirroring.ONESCREENA : PPUMemory.Mirroring.ONESCREENB);
        } else {
            nes.ppuram.setMirroring(Tools.getbit(data, 3) ? PPUMemory.Mirroring.VERTICAL : PPUMemory.Mirroring.HORIZONTAL);
        }
        
        //For homebrew mirroring behaviour is set by default to Holy Diver board.
        
        super.switch16kPRGbank(data & 0x7, 0x8000);
        super.switch8kCHRbank((data & 0xF0) >> 4);
    }
}