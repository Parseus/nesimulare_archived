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
public class Namco3446 extends Board {
    private int command = 0;
    
    public Namco3446(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch8kPRGbank(0xFE, 0xC000);
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
    }
    
    @Override
    public void writePRG(int address, int data) {
        if (address == 0x8000) {
            command = data & 0x7;
        } else if (address == 0x8001) {
            switch (command) {
                case 2:
                    super.switch2kCHRbank(data, 0x0000);
                    break;
                case 3:
                    super.switch2kCHRbank(data, 0x0800);
                    break;
                case 4:
                    super.switch2kCHRbank(data, 0x1000);
                    break;
                case 5:
                    super.switch2kCHRbank(data, 0x1800);
                    break;
                case 6:
                    super.switch8kPRGbank(data, 0x8000);
                    break;
                case 7:
                    super.switch8kPRGbank(data, 0xA000);
                    break;
                default:
                    break;
            }
        }
    }
}