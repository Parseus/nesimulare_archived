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

package nesimulare.core.audio;

import nesimulare.gui.Tools;
import nesimulare.core.cpu.CPU;

/**
 *
 * @author Parseus
 */
public class DMCChannel extends APUChannel {
 
    public int[] dpcmFrequency;
    
    APU apu;
    
    private int cycles;
    private int output;
    private int address, dmaAddress;
    private int length;
    private int shiftRegister;
    private int pcmData;
    private int buffer, outbits;
    private boolean rdyRise = false;
    public boolean fetching;
    private boolean dmaLoop;
    private boolean emptyBuffer;
    private boolean irqEnabled;
    public boolean irqFlag;
    private boolean silenced, enabled;
    
    public DMCChannel(nesimulare.core.Region.System system, final APU apu) {
        super(system);
        
        this.apu = apu;
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        cycles = 1;
        outbits = 8;
        shiftRegister = 0;
        dmaAddress = address = length = 0;
        output = 0;
        pcmData = 0;
        buffer = 0;
        fetching = false;
        silenced = emptyBuffer = true; 
        irqEnabled = irqFlag = false;
        enabled = false;
    }
    
    @Override
    public void softReset() {
        super.softReset();
        
        irqEnabled = irqFlag = false;
        apu.cpu.interrupt(CPU.InterruptTypes.DMC, false);
    }
    
    public void write(final int register, final int data) {
        switch (register) {
            /**
             * IL-- RRRR
             * IRQ enable (I), loop (L), frequency (R)
             */
            case 0:
                frequency = data & 0xF;
                dmaLoop = Tools.getbit(data, 6);
                irqEnabled = Tools.getbit(data, 7);
                
                if (!irqEnabled) {
                    irqFlag = false;
                    apu.cpu.interrupt(CPU.InterruptTypes.DMC, false);
                }
                
                region.singleCycle = getCycles(dpcmFrequency[data & 0xF]);
                break;
             
            /**
             * -DDD DDDD
             * Load counter (D)
             */
            case 1:
                pcmData = data & 0x7F;
                output = (pcmData - 0x40) * 3;
                break;
                
            /**
             * AAAA AAAA
             * Sample address (A)
             */
            case 2:
                address = data;
                break;
            
            /**
             * LLLL LLLL
             * Sample length (L)
             */    
            case 3:
                length = data;
                break;
            
            case 4:
                enabled = (data != 0);
                
                if (enabled) {
                    if (lenctr == 0) {
                        dmaAddress = 0xC000 | (address << 6);
                        lenctr = (length << 4) + 1;
                    }
                } else {
                    lenctr = 0;
                }
                
                apu.cpu.interrupt(CPU.InterruptTypes.DMC, false);
                break;
                
            default:
                break;
        }
    }
    
    @Override
    public void clockChannel(boolean clockLength) {
        if (emptyBuffer && lenctr > 0) {
            apu.cpu.RDY(CPU.DMATypes.DMA);
        }
    }
    
    @Override
    public void cycle() {
        if (--cycles == 0) {
            cycles = dpcmFrequency[frequency];
            
            if (silenced) {
                if (Tools.getbit(shiftRegister, 0)) {
                    if (pcmData <= 0x7D) {
                        pcmData += 2;
                    } 
                } else {
                    if (pcmData >= 0x02) {
                        pcmData -= 2;
                    }
                }
                
                shiftRegister >>= 1;
                output = (pcmData - 0x40) * 3;
            } 
            
            if (--outbits == 0) {
                if (!emptyBuffer) {
                    shiftRegister = buffer;
                    emptyBuffer = true;
                    silenced = false;
                } else {
                    silenced = true;
                }
            }
        }
        
        if (emptyBuffer && !fetching && (lenctr > 0)) {
            fetching = true;
            apu.cpu.RDY(CPU.DMATypes.DMA);
            lenctr--;
        }
    }
    
    public void fetch() {
        buffer = apu.cpu.read(dmaAddress);
        emptyBuffer = false;
        fetching = false;
        
        if (++dmaAddress == 0x10000) {
            dmaAddress = 0x8000;
        }
        
        if (lenctr == 0) {
            if (dmaLoop) {
                dmaAddress = 0xC000 | (address << 6);
                lenctr = (length << 4) + 1;
            } else if (irqEnabled) {
                irqFlag = true;
                apu.cpu.interrupt(CPU.InterruptTypes.DMC, true);
            }
        }
    }
    
    public final int getOutput() {
        return output;
    }
}