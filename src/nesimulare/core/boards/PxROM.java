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

import java.util.Arrays;
import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class PxROM extends Board {
    private int leftLatch = 0xFE;
    private int rightLatch = 0xFE;
    private final int register[] = new int[4];
    
    public PxROM(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        Arrays.fill(register, 0);
        leftLatch = rightLatch = 0xFE;
        
        super.switch8kPRGbank((prg.length - 0x6000) >> 13, 0xA000);
        super.switch8kPRGbank((prg.length - 0x4000) >> 13, 0xC000);
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF000) {
            case 0xA000:
                super.switch8kPRGbank(data, 0x8000);
                break;
            case 0xB000:
                register[0] = data;
                
                if (leftLatch == 0xFD) {
                    super.switch4kCHRbank(register[0], 0x0000);
                }
                break;
            case 0xC000:
                register[1] = data;
                
                if (leftLatch == 0xFE) {
                    super.switch4kCHRbank(register[1], 0x0000);
                }
                break;
            case 0xD000:
                register[2] = data;
                
                if (rightLatch == 0xFD) {
                    super.switch4kCHRbank(register[2], 0x1000);
                }
                break;
           case 0xE000:
                register[3] = data;
                
                if (rightLatch == 0xFE) {
                    super.switch4kCHRbank(register[3], 0x1000);
                }
                break;
           case 0xF000:
               if (Tools.getbit(data, 0)) {
                   nes.ppuram.setMirroring(PPUMemory.Mirroring.HORIZONTAL);
               } else {
                   nes.ppuram.setMirroring(PPUMemory.Mirroring.VERTICAL);
               }
               break;
           default:
               break;
        }
    }
    
    @Override
    public int readCHR(int address) {
        final int data = super.readCHR(address);
        chrLatch(address);
        
        return data;
    }
    
    private void chrLatch(int address) {
        if ((address & 0x1FF0) == 0x0FD0 && leftLatch != 0xFD) {
            leftLatch = 0xFD;
            super.switch4kCHRbank(register[0], 0x0000);
        } else if ((address & 0x1FF0) == 0x0FE0 && leftLatch != 0xFE) {
            leftLatch = 0xFE;
            super.switch4kCHRbank(register[1], 0x0000);
        } else if ((address & 0x1FF0) == 0x1FD0 && rightLatch != 0xFD) {
            rightLatch = 0xFD;
            super.switch4kCHRbank(register[2], 0x1000);
        } else if ((address & 0x1FF0) == 0x1FE0 && rightLatch != 0xFE) {
            rightLatch = 0xFE;
            super.switch4kCHRbank(register[3], 0x1000);
        }
    }
}