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
public class IremG101 extends Board {
    private int prgRegister[] = new int[2];
    private boolean prgMode = false;
    private boolean unk_if_13 = false;
    
    public IremG101(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        unk_if_13 = "7E4180432726A433C46BA2206D9E13B32761C11E".equals(nes.loader.sha1);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
        prgRegister = new int[2];
        prgMode = false;
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xF007) {
            case 0x8000: case 0x8001: case 0x8002: case 0x8003:
            case 0x8004: case 0x8005: case 0x8006: case 0x8007:
                prgRegister[0] = data;
                setupPRG();
                break;
            case 0x9000: case 0x9001: case 0x9002: case 0x9003:
            case 0x9004: case 0x9005: case 0x9006: case 0x9007:
                if (!unk_if_13) {
                    prgMode = Tools.getbit(data, 1);
                    nes.ppuram.setMirroring(Tools.getbit(data, 0) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
                }
                break;
            case 0xA000: case 0xA001: case 0xA002: case 0xA003:
            case 0xA004: case 0xA005: case 0xA006: case 0xA007:
                prgRegister[1] = data;
                setupPRG();
                break;
            case 0xB000:
                super.switch1kCHRbank(data, 0x0000);
                break;
            case 0xB001:
                super.switch1kCHRbank(data, 0x0400);
                break;
            case 0xB002:
                super.switch1kCHRbank(data, 0x0800);
                break;
            case 0xB003:
                super.switch1kCHRbank(data, 0x0C00);
                break;
            case 0xB004:
                super.switch1kCHRbank(data, 0x1000);
                break;
            case 0xB005:
                super.switch1kCHRbank(data, 0x1400);
                break;
            case 0xB006:
                super.switch1kCHRbank(data, 0x1800);
                break;
            case 0xB007:
                super.switch1kCHRbank(data, 0x1C00);
                break;
            default:
                break;
        }
    }
    
    private void setupPRG() {
        if (prgMode) {
            super.switch8kPRGbank(prgRegister[0], 0x8000);
            super.switch8kPRGbank(prgRegister[1], 0xA000);
            super.switch8kPRGbank((prg.length - 0x4000) >> 13, 0xC000);
            super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
        } else {
            super.switch8kPRGbank((prg.length - 0x4000) >> 13, 0x8000);
            super.switch8kPRGbank(prgRegister[1], 0xA000);
            super.switch8kPRGbank(prgRegister[0], 0x8000);
            super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
        }
    }
}