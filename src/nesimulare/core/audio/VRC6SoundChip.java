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
 * Emulates VRC6 sound chip, which consists of two pulse wave channels and a sawtooth channel.
 *
 * @see VRC6PulseSoundChannel
 * @see VRC6SawtoothSoundChannel
 *
 * @author Parseus
 */
public class VRC6SoundChip implements ExpansionSoundChip {
    public VRC6PulseSoundChannel pulse1, pulse2;
    public VRC6SawtoothSoundChannel sawtooth;
    
    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     */
    public VRC6SoundChip(Region.System system) {
        pulse1 = new VRC6PulseSoundChannel(system);
        pulse2 = new VRC6PulseSoundChannel(system);
        sawtooth = new VRC6SawtoothSoundChannel(system);
    }

    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    @Override
    public int getOutput() {
        int output = 384 * pulse1.getOutput();
        output += pulse2.getOutput();
        output += (sawtooth.getOutput() >> 3);
        
        return output;
    }

    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        pulse1.hardReset();
        pulse2.hardReset();
        sawtooth.hardReset();
    }

    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    @Override
    public void softReset() {
        pulse1.softReset();
        pulse2.softReset();
        sawtooth.softReset();
    }

    /**
     * Clocks envelopes for both pulse wave channels.
     */
    @Override
    public void quarterFrame() {
        //Nothing to see here, move along
    }

    /**
     * Clocks length counters for both pulse wave channels.
     * Also clocks envelopes.
     */
    @Override
    public void halfFrame() {
        //Nothing to see here, move along
    }

    /**
     * Clocks channels depending on clocking length.
     */
    @Override
    public void clockChannel(boolean clockingLength) {
        //Nothing to see here, move along
    }

    /**
     * Performs a given number of machine cycles.
     * 
     * @param cycles        Number of machine cycles.
     */
    @Override
    public void cycle(int cycles) {
        pulse1.cycle(cycles);
        pulse2.cycle(cycles);
        sawtooth.cycle(cycles);
    }
}