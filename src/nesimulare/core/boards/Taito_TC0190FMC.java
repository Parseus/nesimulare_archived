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
public class Taito_TC0190FMC extends Board {
    protected int irqReload = 0xFF;
    protected int irqCounter = 0;
    protected boolean irqEnabled = false;
    protected boolean irqClear = false;
    protected int oldA12;
    protected int newA12;
    protected int timer;
    private boolean pal16r4 = false;
    
    public Taito_TC0190FMC(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        pal16r4 = (nes.loader.mapperNumber == 48);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
    }
    
    @Override
    public void writePRG(int address, int data) {
        if (pal16r4) {      //Mapper 48
            switch (address & 0xE003) {
                case 0x8000:
                    super.switch8kPRGbank(data, 0x8000);
                    break;
                case 0x8001:
                    super.switch8kPRGbank(data, 0xA000);
                    break;
                case 0x8002:
                    super.switch2kCHRbank(data, 0x0000);
                    break;
                case 0x8003:
                    super.switch2kCHRbank(data, 0x0800);
                    break;
                case 0xA000:
                    super.switch1kCHRbank(data, 0x1000);
                    break;
                case 0xA001:
                    super.switch1kCHRbank(data, 0x1400);
                    break;
                case 0xA002:
                    super.switch1kCHRbank(data, 0x1800);
                    break;
                case 0xA003:
                    super.switch1kCHRbank(data, 0x1C00);
                    break;
                case 0xC000:
                    irqReload = (data ^ 0xFF);
                    break;
                case 0xC001:
                    irqClear = true;
                    irqCounter = 0;
                    break;
                case 0xC002:
                    irqEnabled = true;
                    break;
                case 0xC003:
                    irqEnabled = false;
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                    break;
                case 0xE000:
                    nes.ppuram.setMirroring(Tools.getbit(data, 6) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
                    break;
                default:
                    break;
            }
        } else {        //Mapper 33
            switch (address & 0xA003) {
                case 0x8000:
                    super.switch8kPRGbank(data & 0x3F, 0x8000);
                    nes.ppuram.setMirroring(Tools.getbit(data, 6) ? PPUMemory.Mirroring.HORIZONTAL : PPUMemory.Mirroring.VERTICAL);
                    break;
                case 0x8001:
                    super.switch8kPRGbank(data & 0x3F, 0xA000);
                    break;
                case 0x8002:
                    super.switch2kCHRbank(data, 0x0000);
                    break;
                case 0x8003:
                    super.switch2kCHRbank(data, 0x0800);
                    break;
                case 0xA000:
                    super.switch1kCHRbank(data, 0x1000);
                    break;
                case 0xA001:
                    super.switch1kCHRbank(data, 0x1400);
                    break;
                case 0xA002:
                    super.switch1kCHRbank(data, 0x1800);
                    break;
                case 0xA003:
                    super.switch1kCHRbank(data, 0x1C00);
                    break;
                default:
                    break;
            }
        }
    }
    
    @Override
    public void clockPPUCycle() {
        if (pal16r4) {
            timer++;
        }
    }
    
    @Override
    public void updateAddressLines(int address) {
        if (pal16r4) {
            oldA12 = newA12;
            newA12 = address & 0x1000;
            
            if (oldA12 < newA12) {
                if (timer > 8) {
                    final int oldCounter = irqCounter;
                    
                    if (irqCounter == 0 || irqClear) {
                        irqCounter = irqReload;
                    } else {
                        irqCounter = (irqCounter - 1) & 0xFF;
                    }
                    
                    if ((oldCounter != 0 || irqClear) && irqCounter == 0 && irqEnabled) {
                        nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
                    }
                    
                    irqClear = false;
                }
                
                timer = 0;
            }
        }
    }
}