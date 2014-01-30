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
 * Emulates NES APU's delta modulation channel (DMC), which can output 1-bit delta-encoded samples 
 * or can have its 7-bit counter directly loaded, allowing flexible manual sample playback.
 * The DMC channel contains the following: memory reader, interrupt flag, sample buffer, timer, output unit, 7-bit output level with up and down counter.
 *
 * @author Parseus
 */
public class DMCChannel extends APUChannel {

    APU apu;

    public int[] dpcmFrequency;

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

    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     * @param apu Emulated APU
     */
    public DMCChannel(nesimulare.core.Region.System system, final APU apu) {
        super(system);

        this.apu = apu;
    }

    /**
     * Initializes a delta modulation channel.
     */
    @Override
    public void initialize() {
        hardReset();
        super.initialize();
    }

    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
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

    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    @Override
    public void softReset() {
        super.softReset();

        irqFlag = false;
        apu.cpu.interrupt(CPU.InterruptTypes.DMC, false);
    }

    /**
     * Writes data to a given register
     *
     * @param register Register to write data to
     * @param data Written data
     */
    public void write(final int register, final int data) {
        switch (register) {
            /**
             * $4010 
             * IL-- RRRR IRQ enable (I), loop (L), frequency (R)
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
             * $4011 
             * -DDD DDDD Load counter (D)
             */
            case 1:
                output = data & 0x7F;
                break;

            /**
             * $4012 
             * AAAA AAAA Sample address (A)
             */
            case 2:
                sampleAddress = (data << 6) | 0xC000;
                break;

            /**
             * $4013 
             * LLLL LLLL Sample length (L)
             */
            case 3:
                sampleLength = (data << 4) | 0x0001;
                break;

            default:
                break;
        }
    }

    /**
     * Returns the status of channel.
     *
     * @return True: Length counter > 0 False: Lencth counter = 0
     */
    @Override
    public boolean getStatus() {
        return (dmaSize > 0);
    }

    /**
     * Sets/clears reloading length counter. If reloading is cleared, also clears the length counter.
     *
     * @param status Channel status
     */
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

    /**
     * Clocks a channel depending on clocking length.
     */
    @Override
    public void clockChannel(boolean clockLength) {
        if (rdyRise && !fullBuffer && dmaSize > 0) {
            rdyRise = false;
            apu.cpu.RDY(CPU.DMATypes.DMA);
        }
    }

    /**
     * Performs an individual machine cycle.
     */
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

    /**
     * Fetches sample data for DMC channel.
     */
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

    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    public final int getOutput() {
        return output;
    }
}