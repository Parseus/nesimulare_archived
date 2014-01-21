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
    
    private int output;
    private int sampleAddress, dmaAddress;
    private int sampleLength;
    private int shiftRegister;
    private int dmaSize;
    private int buffer, outbits;
    private boolean rdyRise = false;
    public boolean fetching;
    private boolean dmaLoop;
    private boolean dmaEnabled;
    private boolean fullBuffer;
    private boolean irqEnabled;
    public boolean irqFlag;
    
    public DMCChannel(nesimulare.core.Region.System system, final APU apu) {
        super(system);
        
        this.apu = apu;
    }
    
    @Override
    public void initialize() {
        hardReset();
        super.initialize();
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        outbits = 1;
        shiftRegister = 1;
        dmaAddress = 0;
        sampleAddress = sampleLength = 0xC000;
        dmaSize = 0;
        output = 0;
        buffer = 0;
        dmaLoop = false;
        dmaEnabled = false;
        rdyRise = true;
        fetching = false;
        fullBuffer = false; 
        irqEnabled = irqFlag = false;
    }
    
    @Override
    public void softReset() {
        super.softReset();
        
        irqFlag = false;
        apu.cpu.interrupt(CPU.InterruptTypes.DMC, false);
    }
    
    public void write(final int register, final int data) {
        switch (register) {
            /**
             * IL-- RRRR
             * IRQ enable (I), loop (L), frequency (R)
             */
            case 0:
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
                output = data & 0x7F;
                break;
                
            /**
             * AAAA AAAA
             * Sample address (A)
             */
            case 2:
                sampleAddress = (data << 6) | 0xC000;
                break;
            
            /**
             * LLLL LLLL
             * Sample length (L)
             */    
            case 3:
                sampleLength = (data << 4) | 0x0001;
                break;
                
            default:
                break;
        }
    }
    
    
    @Override
    public boolean getStatus() {
        return (dmaSize > 0);
    }
    
    @Override
    public void setStatus(boolean status) {
        if (status) {
            if (dmaSize == 0) {
                dmaSize = sampleLength;
                dmaAddress = sampleAddress;
            }
        } else {
            dmaSize = 0;
        }
        
        irqFlag = false;
        apu.cpu.interrupt(CPU.InterruptTypes.DMC, false);
    }
    
    @Override
    public void clockChannel(boolean clockLength) {
        if (rdyRise && !fullBuffer && dmaSize > 0) {
            rdyRise = false;
            apu.cpu.RDY(CPU.DMATypes.DMA);
        }
    }
    
    @Override
    public void cycle() {
        if (dmaEnabled) {
            if (Tools.getbit(shiftRegister, 0)) {
                if (output <= 0x7D) {
                    output += 2;
                }
            } else {
                if (output >= 0x02) {
                    output -= 2;
                }
            }
            
            shiftRegister >>= 1;
        }
        
        outbits--;
        
        if (outbits == 0) {
            outbits = 8;
            
            if (fullBuffer) {
                fullBuffer = false;
                dmaEnabled = true;
                rdyRise = true;
                shiftRegister = buffer;
                
                if (dmaSize > 0) {
                    rdyRise = false;
                    apu.cpu.RDY(CPU.DMATypes.DMA);
                }
            } else {
                dmaEnabled = false;
            }
        }
    }
    
    public void fetch() {
        buffer = apu.cpu.read(dmaAddress);
        fullBuffer = true;
        rdyRise = true;
        
        if (++dmaAddress == 0x10000) {
            dmaAddress = 0x8000;
        }
        
        if (dmaSize > 0) {
            dmaSize--;
        }
        
        if (dmaSize == 0) {
            if (dmaLoop) {
                dmaAddress = sampleAddress;
                dmaSize = sampleLength;
            } else if (irqEnabled) {
                irqFlag = true;
                apu.cpu.interrupt(CPU.InterruptTypes.DMC, true);
            }
        }
    }
    
    public final int getOutput() {
        return output & 0xFF;
    }
}