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

/**
 * Emulates NES APU's triangle channel that generates a pseudo-triangle wave.
 * The triangle channel contains the following: timer, length counter, linear counter, linear counter reload flag, control flag, sequencer.
 *
 * @author Parseus
 */
public class TriangleChannel extends APUChannel { 
    /**
     * Step sequencer.
     */
    private static final int[] stepSequence = {
        0x0F, 0x0E, 0x0D, 0x0C, 0x0B, 0x0A, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00,
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };
    
    private int output;
    private int step;
    private int linearCounter = 0;
    private int linearCounterReload;
    private boolean linearCounterHalt;
    private boolean channelHalt;
    
    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     */
    public TriangleChannel(nesimulare.core.Region.System system) {
        super(system);
    }
    
    /**
     * Initializes a given channel.
     */
    @Override
    public void initialize() {
        super.initialize();
        
        hardReset();
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();
        
        linearCounter = 0;
        linearCounterReload = 0;
        output = 0;
        step = 0;
        linearCounterHalt = false;
        channelHalt = false;
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
             * $4008
             * CRRR RRRR
             * Length counter halt / linear counter control (C), 
             * linear counter load (R) 
             */
            case 0:
                linearCounterHalt = lenctrHaltRequest = Tools.getbit(data, 7);
                linearCounterReload = data & 0x7F;
                break;
              
            /**
             * $400A
             * TTTT TTTT
             * Timer low (T)
             */
            case 2:
                frequency = (frequency & 0x700) | data;
                updateFrequency();
                break;
            
            /**
             * $400B
             * LLLL LTTT
             * Length counter load (L), timer high (T)
             * (also sets the linear counter reload flag)
             */    
            case 3:
                lengthCounterReload = lenctrTable[data >> 3];
                lenctrReloadRequest = true;
                channelHalt = true;
                
                frequency = (frequency & 0xFF) | ((data & 7) << 8);
                updateFrequency();
                break;
                
            default:
                break;
        }
    }
    
    /**
     * Performs an individual machine cycle.
     */
    @Override
    public void cycle() {
        if (lengthCounter > 0 && linearCounter > 0) {
            if (frequency >= 4) {
                step++;
                step &= 0x1F;
                output = stepSequence[step];
            }
        }
    }
    
    /**
     * Clocks linear counter.
     */
    public void quarterFrame() {
        if (channelHalt) {
            linearCounter = linearCounterReload;
        } else if (linearCounter != 0) {
            linearCounter--;
        }
        
        channelHalt &= linearCounterHalt;
    }
    
    /**
     * Clocks length counter.
     */
    public void halfFrame() {
        if (!lenctrHalt && lengthCounter > 0) {
            lengthCounter = (lengthCounter - 1) & 0xFF;
        }
    }
    
    /**
     * Updates a single cycle timing based on frequency.
     */
    private void updateFrequency() {
        int timer = (frequency + 1) / 2;
        
        if (timer == 0) {
            timer = 1;
        }
        
        region.singleCycle = getCycles(timer);
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