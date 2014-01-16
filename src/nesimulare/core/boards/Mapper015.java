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
public class Mapper015 extends Board {
    public Mapper015(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0x3) {
            case 0:
                super.switch16kPRGbank(data, 0x8000);
                super.switch16kPRGbank(data ^ 1, 0xC000);
                break;
            case 1:
                super.switch16kPRGbank(data, 0x8000);
                super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
                break;
            case 2:
                int value = (data << 1) & 0xFF;
                value = ((value & 0x7E) | ((data >> 7) & 1)) & 0xFF;
                super.switch8kPRGbank(value, 0x8000);
                super.switch8kPRGbank(value, 0xA000);
                super.switch8kPRGbank(value, 0xC000);
                super.switch8kPRGbank(value, 0xE000);
                break;
            case 3:
                super.switch16kPRGbank(data, 0x8000);
                super.switch16kPRGbank(data, 0xC000);
                break;
            default:
                break;
        }
        
        nes.ppuram.setMirroring(Tools.getbit(data, 6) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
    }
}