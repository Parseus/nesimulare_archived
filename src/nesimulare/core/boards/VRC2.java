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

/**
 *
 * @author Parseus
 */
public class VRC2 extends Board {
    private final int chrRegister[] = new int[8];
    private boolean vrc2a;
    
    public VRC2(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        vrc2a = (nes.loader.mappertype == 22);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address) {
            case 0x8000: case 0x8001: case 0x8002: case 0x8003:
                super.switch8kPRGbank(data & 0xF, 0x8000);
                break;
                
            case 0x9000: case 0x9001: case 0x9002: case 0x9003:
                switch (data & 3) {
                    case 0:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.VERTICAL);
                        break;
                    case 1:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.HORIZONTAL);
                        break;
                    case 2:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENA);
                        break;
                    case 3:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENB);
                        break;
                    default:
                        break;
                }
                break;
                
            case 0xA000: case 0xA001: case 0xA002: case 0xA003:
                super.switch8kPRGbank(data & 0xF, 0xA000);
                break;
                
            case 0xB000:
                chrRegister[0] = (chrRegister[0] & 0xF0) | (data & 0xF);
                
                if (vrc2a) {
                    super.switch1kCHRbank(chrRegister[0] >> 1, 0x0000);
                } else {
                    super.switch1kCHRbank(chrRegister[0], 0x0000);
                }
                break;
            case 0xB001:
                if (vrc2a) {
                    chrRegister[1] = (chrRegister[1] & 0xF0) | (data & 0xF);
                    super.switch1kCHRbank(chrRegister[1] >> 1, 0x0400);
                } else {
                    chrRegister[0] = (chrRegister[0] & 0x0F) | (data & 0xF) << 4;
                    super.switch1kCHRbank(chrRegister[0], 0x0000);
                }
                break;
            case 0xB002:
                if (vrc2a) {
                    chrRegister[0] = (chrRegister[0] & 0x0F) | (data & 0xF) << 4;
                    super.switch1kCHRbank(chrRegister[0] >> 1, 0x0000);
                } else {
                    chrRegister[1] = (chrRegister[1] & 0xF0) | (data & 0xF);
                    super.switch1kCHRbank(chrRegister[1], 0x0400);
                }
                break;
            case 0xB003:
                chrRegister[1] = (chrRegister[1] & 0x0F) | (data & 0xF) << 4;
                
                if (vrc2a) {
                    super.switch1kCHRbank(chrRegister[1] >> 1, 0x0400);
                } else {
                    super.switch1kCHRbank(chrRegister[1], 0x0400);
                }
                break;
                
            case 0xC000:
                chrRegister[2] = (chrRegister[2] & 0xF0) | (data & 0xF);
                
                if (vrc2a) {
                    super.switch1kCHRbank(chrRegister[2] >> 1, 0x0800);
                } else {
                    super.switch1kCHRbank(chrRegister[2], 0x0800);
                }
                break;
            case 0xC001:
                if (vrc2a) {
                    chrRegister[3] = (chrRegister[3] & 0xF0) | (data & 0xF);
                    super.switch1kCHRbank(chrRegister[3] >> 1, 0x0C00);
                } else {
                    chrRegister[2] = (chrRegister[2] & 0x0F) | (data & 0xF) << 4;
                    super.switch1kCHRbank(chrRegister[2], 0x0800);
                }
                break;
            case 0xC002:
                if (vrc2a) {
                    chrRegister[2] = (chrRegister[2] & 0x0F) | (data & 0xF) << 4;
                    super.switch1kCHRbank(chrRegister[2] >> 1, 0x0800);
                } else {
                    chrRegister[3] = (chrRegister[1] & 0xF0) | (data & 0xF);
                    super.switch1kCHRbank(chrRegister[3], 0x0C00);
                }
                break;
            case 0xC003:
                chrRegister[3] = (chrRegister[3] & 0x0F) | (data & 0xF) << 4;
                
                if (vrc2a) {
                    super.switch1kCHRbank(chrRegister[3] >> 1, 0x0400);
                } else {
                    super.switch1kCHRbank(chrRegister[3], 0x0400);
                }
                break;
                
            case 0xD000:
                chrRegister[4] = (chrRegister[4] & 0xF0) | (data & 0xF);
                
                if (vrc2a) {
                    super.switch1kCHRbank(chrRegister[4] >> 1, 0x1000);
                } else {
                    super.switch1kCHRbank(chrRegister[4], 0x1000);
                }
                break;
            case 0xD001:
                if (vrc2a) {
                    chrRegister[5] = (chrRegister[5] & 0xF0) | (data & 0xF);
                    super.switch1kCHRbank(chrRegister[5] >> 1, 0x1400);
                } else {
                    chrRegister[4] = (chrRegister[4] & 0x0F) | (data & 0xF) << 4;
                    super.switch1kCHRbank(chrRegister[4], 0x1000);
                }
                break;
            case 0xD002:
                if (vrc2a) {
                    chrRegister[4] = (chrRegister[4] & 0x0F) | (data & 0xF) << 4;
                    super.switch1kCHRbank(chrRegister[4] >> 1, 0x1000);
                } else {
                    chrRegister[5] = (chrRegister[5] & 0xF0) | (data & 0xF);
                    super.switch1kCHRbank(chrRegister[5], 0x1400);
                }
                break;
            case 0xD003:
                chrRegister[5] = (chrRegister[5] & 0x0F) | (data & 0xF) << 4;
                
                if (vrc2a) {
                    super.switch1kCHRbank(chrRegister[5] >> 1, 0x1400);
                } else {
                    super.switch1kCHRbank(chrRegister[5], 0x1400);
                }
                break;
                
            case 0xE000:
                chrRegister[6] = (chrRegister[6] & 0xF0) | (data & 0xF);
                
                if (vrc2a) {
                    super.switch1kCHRbank(chrRegister[6] >> 1, 0x1800);
                } else {
                    super.switch1kCHRbank(chrRegister[6], 0x1800);
                }
                break;
            case 0xE001:
                if (vrc2a) {
                    chrRegister[7] = (chrRegister[7] & 0xF0) | (data & 0xF);
                    super.switch1kCHRbank(chrRegister[7] >> 1, 0x1C00);
                } else {
                    chrRegister[6] = (chrRegister[6] & 0x0F) | (data & 0xF) << 4;
                    super.switch1kCHRbank(chrRegister[6], 0x1800);
                }
                break;
            case 0xE002:
                if (vrc2a) {
                    chrRegister[6] = (chrRegister[6] & 0x0F) | (data & 0xF) << 4;
                    super.switch1kCHRbank(chrRegister[6] >> 1, 0x1800);
                } else {
                    chrRegister[7] = (chrRegister[7] & 0xF0) | (data & 0xF);
                    super.switch1kCHRbank(chrRegister[7], 0x1C00);
                }
                break;
            case 0xE003:
                chrRegister[7] = (chrRegister[7] & 0x0F) | (data & 0xF) << 4;
                
                if (vrc2a) {
                    super.switch1kCHRbank(chrRegister[7] >> 1, 0x1C00);
                } else {
                    super.switch1kCHRbank(chrRegister[7], 0x1C00);
                }
                break;
                
            default:
                break;
        }
    }
}