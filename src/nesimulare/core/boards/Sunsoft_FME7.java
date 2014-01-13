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

import nesimulare.core.audio.Sunsoft5BSoundChip;
import nesimulare.core.cpu.CPU;
import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class Sunsoft_FME7 extends Board {
    private Sunsoft5BSoundChip soundChip;
    private int register;
    private int soundRegister;
    private int sramAddress = 0;
    private boolean wramEnabled = false;
    private boolean ramSelected = false;
    
    private int irqCounter = 0;
    private boolean irqCounterEnabled = false;
    private boolean irqEnabled = false;

    public Sunsoft_FME7(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        soundChip = new Sunsoft5BSoundChip(nes.region);
        nes.apu.addExpansionSoundChip(soundChip);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
        register = 0;
        soundRegister = 0;
        sramAddress = 0;
        wramEnabled = false;
        ramSelected = false;
        
        irqCounter = 0;
        irqCounterEnabled = false;
        irqEnabled = false;
    }
    
    @Override
    public int readSRAM(int address) {
        if (ramSelected) {
            if (wramEnabled) {
                return super.readSRAM(address);
            }
        } else {
            return prg[(sramAddress << 13) | (address & 0x1FFF)];
        }
        
        return 0;
    }
    
    @Override
    public void writeSRAM(int address, int data) {
        if (ramSelected && wramEnabled) {
            super.writeSRAM(address, data);
        }
    }
    
    @Override
    public void writePRG(int address, int data) {
        switch (address & 0xE000) {
            case 0x8000:
                register = (data & 0xF);
                break;
            case 0xA000:
                switch (register) {
                    case 0x0:
                        super.switch1kCHRbank(data, 0x0000);
                        break;
                    case 0x1:
                        super.switch1kCHRbank(data, 0x0400);
                        break;
                    case 0x2:
                        super.switch1kCHRbank(data, 0x0800);
                        break;
                    case 0x3:
                        super.switch1kCHRbank(data, 0x0C00);
                        break;
                    case 0x4:
                        super.switch1kCHRbank(data, 0x1000);
                        break;
                    case 0x5:
                        super.switch1kCHRbank(data, 0x1400);
                        break;
                    case 0x6:
                        super.switch1kCHRbank(data, 0x1800);
                        break;
                    case 0x7:
                        super.switch1kCHRbank(data, 0x1C00);
                        break;
                    case 0x8:
                        wramEnabled = Tools.getbit(data, 7);
                        ramSelected = Tools.getbit(data, 6);
                        sramAddress = (data & 0x3F);
                        break;
                    case 0x9:
                        super.switch8kPRGbank(data, 0x8000);
                        break;
                    case 0xA:
                        super.switch8kPRGbank(data, 0xA000);
                        break;
                    case 0xB:
                        super.switch8kPRGbank(data, 0xC000);
                        break;
                    case 0xC:
                        switch (data & 0x3) {
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
                    case 0xD:
                        irqCounterEnabled = Tools.getbit(data, 7);
                        irqEnabled = Tools.getbit(data, 0);
                        nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                        break;
                    case 0xE:
                        irqCounter = (irqCounter & 0xFF00) | data;
                        break;
                    case 0xF:
                        irqCounter = (irqCounter & 0x00FF) | (data << 8);
                        break;
                    default:
                        break;
                }
                break;
            case 0xC000:
                soundRegister = (data & 0xF);
                break;
            case 0xE000:
                switch (soundRegister) {    //TODO: Emulate noise and envelope generators that are not used in any NES game
                    case 0x0:
                        soundChip.square0.write(0, data);
                        break;
                    case 0x1:
                        soundChip.square0.write(1, data);
                        break;
                    case 0x2:
                        soundChip.square1.write(0, data);
                        break;
                    case 0x3:
                        soundChip.square1.write(1, data);
                        break;
                    case 0x4:
                        soundChip.square2.write(0, data);
                        break;
                    case 0x5:
                        soundChip.square2.write(1, data);
                        break;
                    case 0x7:
                        soundChip.square0.disabled = Tools.getbit(data, 0);
                        soundChip.square1.disabled = Tools.getbit(data, 1);
                        soundChip.square2.disabled = Tools.getbit(data, 2);
                        break;
                    case 0x8:
                        soundChip.square0.write(2, data);
                        break;
                    case 0x9:
                        soundChip.square1.write(2, data);
                        break;
                    case 0xA:
                        soundChip.square2.write(2, data);
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }
    
    @Override
    public void clockCPUCycle() {
        if (irqCounterEnabled) {
            irqCounter--;
            
            if (irqCounter == 0) {
                irqCounter = 0xFFFF;
                
                if (irqEnabled) {
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
                }
            }
        }
    }
}