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

import nesimulare.core.cpu.CPU;
import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class Mapper182 extends TxROM {
    public Mapper182(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xE001) {
            case 0x8001:
                if (nes.getLoader().mirroring != PPUMemory.Mirroring.FOURSCREEN) {
                    if (Tools.getbit(data, 0)) {
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.HORIZONTAL);
                    } else {
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.VERTICAL);
                    }
                }
                break;
            case 0xA000:
                chrMode = Tools.getbit(data, 7);
                prgMode = Tools.getbit(data, 6);
                register = data & 0x7;
                setupPRG();
                setupCHR();
                break;
            case 0xC000:
                switch (register) {
                    case 0:
                        chrRegister[0] = data;
                        setupCHR();
                        break;
                    case 1:
                        chrRegister[3] = data;
                        setupCHR();
                        break;
                    case 2:
                        chrRegister[1] = data;
                        setupCHR();
                        break;
                    case 3:
                        chrRegister[5] = data;
                        setupCHR();
                        break;
                    case 4:
                        prgRegister[0] = data & 0x3F;
                        setupPRG();
                        break;
                    case 5:
                        prgRegister[1] = data & 0x3F;
                        setupPRG();
                        break;
                    case 6:
                        chrRegister[2] = data;
                        setupCHR();
                        break;
                    case 7:
                        chrRegister[4] = data;
                        break;
                    default:
                        break;
                }
                break;
            case 0xC001:
                irqReload = data;
                irqClear = true;
                irqCounter = 0;
                break;
            case 0xE000:
                irqEnable = false;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xE001:
                irqEnable = true;
                break;
            default:
                break;
        }
    }
}