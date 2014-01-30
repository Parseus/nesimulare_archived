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

import nesimulare.core.Region;

/**
 * Emulates a square wave tone channel that is a part of Sunsoft 5B sound chip.
 * It functions similar to the stock 2A03 pulse wave channel
 * except it generates only a square wave tone.
 * @see PulseChannel
 *
 * @author Parseus
 */
public class Sunsoft5BSquareChannel extends APUChannel {
    private int dutyCycle = 0;
    private int volume = 0;
    private int output = 0;
    public boolean disabled = false;
    
    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     */
    public Sunsoft5BSquareChannel(Region.System system) {
        super(system);
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();
        
        disabled = false;
        output = volume = 0;
        dutyCycle = 0;
    }
    
    
    public void write(final int register, final int data) {
        switch (register) {
            
            /**
             * LLLL LLLL	 Channel low period
             */
            case 0:
                frequency = (frequency & 0x0F00) | data;
                updateFrequency();
                break;
                 
            /**
             * ---- HHHH	 Channel A high period
             */
            case 1:
                frequency = (frequency & 0x00FF) | ((data & 0xF) << 8);
                updateFrequency();
                break;
              
            /**
             * ---E VVVV	 Channel envelope enable (E), volume (V)
             * (currently envelope is not emulated)
             */
            case 2:
                volume = (data & 0xF);
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
        dutyCycle = (dutyCycle + 1) & 0x1F;
            
        if (dutyCycle <= 15) {
            output = volume;
        } else {
            output = 0;
        }
    }
    
    /**
     * Updates a single cycle timing based on frequency.
     */
    private void updateFrequency() {
        region.singleCycle = (frequency + 1) * system.cpu;
    }
    
    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    public final int getOutput() {
        if (disabled) {
            return 0;
        } else {
            return output;
        }
    }
}
