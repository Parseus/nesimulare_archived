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

import nesimulare.core.audio.VRC6SoundChip;
import nesimulare.core.cpu.CPU;
import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class VRC6 extends Board {
    private VRC6SoundChip soundChip;
    private int irqCounter = 0;
    private int irqPrescaler = 0;
    private int irqReload = 0;
    private boolean irqEnabled = false;
    private boolean irqEnabledOnAcknowledge = false;
    private boolean irqMode = false;
    private boolean vrc6a;
    
    public VRC6(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        soundChip = new VRC6SoundChip(nes.region);
        nes.apu.addExpansionSoundChip(soundChip);
        vrc6a = (nes.loader.mappertype == 24);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address) {
            case 0x8000: case 0x8001: case 0x8002: case 0x8003:
                super.switch16kPRGbank(data, 0x8000);
                break;
                
            case 0x9000:
                soundChip.pulse1.write(0, data);
                break;
            case 0x9001:
                if (vrc6a) {
                    soundChip.pulse1.write(1, data);
                } else {
                    soundChip.pulse1.write(2, data);
                }
                break;
            case 0x9002:
                if (vrc6a) {
                    soundChip.pulse1.write(2, data);
                } else {
                    soundChip.pulse1.write(1, data);
                }
                break;
            case 0x9003:
                break;
                
            case 0xA000:
                soundChip.pulse2.write(0, data);
                break;
            case 0xA001:
                if (vrc6a) {
                    soundChip.pulse2.write(1, data);
                } else {
                    soundChip.pulse2.write(2, data);
                }
                break;
            case 0xA002:
                if (vrc6a) {
                    soundChip.pulse2.write(2, data);
                } else {
                    soundChip.pulse2.write(1, data);
                }
                break;
            case 0xA003:
                break;    
            
            case 0xB000:
                soundChip.sawtooth.write(0, data);
                break;
            case 0xB001:
                if (vrc6a) {
                    soundChip.sawtooth.write(1, data);
                } else {
                    soundChip.sawtooth.write(2, data);
                }
                break;
            case 0xB002:
                if (vrc6a) {
                    soundChip.sawtooth.write(2, data);
                } else {
                    soundChip.sawtooth.write(1, data);
                }
                break;
            case 0xB003:
                switch ((data & 0xC) >> 2) {
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
                
            case 0xC000: case 0xC001: case 0xC002: case 0xC003:
                super.switch8kPRGbank(data, 0xC000);
                break;
                
            case 0xD000:
                super.switch1kCHRbank(data, 0x0000);
                break;
            case 0xD001:
                if (vrc6a) {
                    super.switch1kCHRbank(data, 0x0400);
                } else {
                    super.switch1kCHRbank(data, 0x0800);
                }
                break;
            case 0xD002:
                if (vrc6a) {
                    super.switch1kCHRbank(data, 0x0800);
                } else {
                    super.switch1kCHRbank(data, 0x0400);
                }
                break;
            case 0xD003:
                super.switch1kCHRbank(data, 0x0C00);
                break;
                
            case 0xE000:
                super.switch1kCHRbank(data, 0x1000);
                break;
            case 0xE001:
                if (vrc6a) {
                    super.switch1kCHRbank(data, 0x1400);
                } else {
                    super.switch1kCHRbank(data, 0x1800);
                }
                break;
            case 0xE002:
                if (vrc6a) {
                    super.switch1kCHRbank(data, 0x1800);
                } else {
                    super.switch1kCHRbank(data, 0x1400);
                }
                break;
            case 0xE003:
                super.switch1kCHRbank(data, 0x1C00);
                break;
                
            case 0xF000:
                irqReload = data;
                break;
            case 0xF001:
                if (vrc6a) {
                    irqMode = Tools.getbit(data, 3);
                    irqEnabled = Tools.getbit(data, 2);
                    irqEnabledOnAcknowledge = Tools.getbit(data, 1);
                    
                    if (irqEnabled) {
                        irqCounter = irqReload;
                        irqPrescaler = 341;
                    }
                    
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                } else {
                    irqEnabled = irqEnabledOnAcknowledge;
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                }
                break;
            case 0xF002:
                if (vrc6a) {
                    irqEnabled = irqEnabledOnAcknowledge;
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                } else {
                    irqMode = Tools.getbit(data, 3);
                    irqEnabled = Tools.getbit(data, 2);
                    irqEnabledOnAcknowledge = Tools.getbit(data, 1);
                    
                    if (irqEnabled) {
                        irqCounter = irqReload;
                        irqPrescaler = 341;
                    }
                    
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                }
                break;
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (irqEnabled) {
            if (irqMode) {
                irqCounter++;
                
                if (irqCounter == 0xFF) {
                    irqCounter = irqReload;
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
                }
            } else {
                if (irqPrescaler > 0) {
                    irqPrescaler -= 3;
                } else {
                    irqPrescaler = 341;
                    irqCounter++;
                    
                    if (irqCounter == 0xFF) {
                        irqCounter = irqReload;
                        nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
                    }
                }
            }
        }
    }
}