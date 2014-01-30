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
import nesimulare.gui.Tools;

/**
 * Emulates a sawtooth channel that is a part of VRC6 sound chip.
 *
 * @author Parseus
 */
public class VRC6SawtoothSoundChannel extends APUChannel {
    private int accum = 0;
    private int accumRate = 0;
    private int accumStep = 0;
    private int output = 0;
    private boolean enabled = true;
    
    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     */
    public VRC6SawtoothSoundChannel(Region.System system) {
        super(system);
    }

    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();
        
        accum = 0;
        accumRate = 0;
        accumStep = 0;
        output = 0;
        enabled = true;
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
             * $B000
             * Saw volume
             * 7  bit  0
             * ---- ----
             * ..AA AAAA
             *   ++-++++- Accumulator Rate (controls volume)
             */
            case 0:
                accumRate = data & 0x3F;
                break;

            /**
             * $B001
             * Saw period low
             * 7  bit  0
             * ---- ----
             * FFFF FFFF
             * |||| ||||
             * ++++-++++- Low 8 bits of frequency
             */    
            case 1:
                frequency = (frequency & 0x0F00) | data;
                updateFrequency();
                break;
                 
            /**
             * $B002
             * Saw period high
             * 7  bit  0
             * ---- ----
             * E... FFFF
             * |    ||||
             * |    ++++- High 4 bits of frequency
             * +--------- Enable (0 = channel disabled)
             */
            case 2:
                enabled = Tools.getbit(data, 7);
                frequency = (frequency & 0x00FF) | ((data & 0xF) << 8);
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
        accumStep++;
        
        switch (++accumStep) {
            case 2: case 4:  case 6:
            case 8: case 10: case 12:
                accum += accumRate;
                break;
            case 14:
                accum = 0;
                accumStep = 0;
                break;
            default:
                break;
        }
        
        output = (accum >> 3) & 0x1F;
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
        if (enabled && frequency > 0x4) {
            return output;
        } else {
            return 0;
        }
    }
}