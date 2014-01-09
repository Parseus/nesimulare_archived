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
public class Mapper174 extends Board {
    public Mapper174(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void writePRG(int address, int data) {
        final boolean prgMode = Tools.getbit(address, 7);
        
        if (prgMode) {
            super.switch16kPRGbank((address & 0x70) >> 4, 0x8000);
            super.switch16kPRGbank((address & 0x70) >> 4, 0xC000);
        } else {
            super.switch32kPRGbank((address & 0x70) >> 4);
        }
        
        super.switch8kCHRbank((address & 0x14) >> 1);
        nes.ppuram.setMirroring(Tools.getbit(address, 0) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
    }
}