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

/**
 *
 * @author Parseus
 */
public class Mapper246 extends Board {
    public Mapper246(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch16kPRGbank(0xFF, 0xE000);
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        if (address >= 0x6800) {
            super.writeSRAM(address, data);
        } else {
            switch (address & 0x7) {
                case 0:
                    super.switch8kPRGbank(data, 0x8000);
                    break;
                case 1:
                    super.switch8kPRGbank(data, 0xA000);
                    break;
                case 2:
                    super.switch8kPRGbank(data, 0xC000);
                    break;
                case 3:
                    super.switch8kPRGbank(data, 0xE000);
                    break;
                case 4:
                    super.switch2kCHRbank(data, 0x0000);
                    break;
                case 5:
                    super.switch2kCHRbank(data, 0x0800);
                    break;
                case 6:
                    super.switch2kCHRbank(data, 0x1000);
                    break;
                case 7:
                    super.switch2kCHRbank(data, 0x1800);
                    break;
                default:
                    break;
            }
        }
    }
}