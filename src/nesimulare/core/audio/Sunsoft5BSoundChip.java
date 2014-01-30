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
 * Emulates Sunsoft 5B sound chip, which is a type of Yamaha YM2149F.
 * It consists of three channels that output a square wave tone.
 * In addition there is one noise generator, and one envelope generator, both of which may be shared by any of the three channels
 * (currently not emulated).
 * @see Sunsoft5BSquareChannel
 *
 * @author Parseus
 */
public class Sunsoft5BSoundChip implements ExpansionSoundChip {
    //TODO: Emulate envelope and noise generators.
    public Sunsoft5BSquareChannel square0, square1, square2;
    
    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     */
    public Sunsoft5BSoundChip(Region.System system) {
        square0 = new Sunsoft5BSquareChannel(system);
        square1 = new Sunsoft5BSquareChannel(system);
        square2 = new Sunsoft5BSquareChannel(system);
    }
    
    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    @Override
    public int getOutput() {
        int output = square0.getOutput();
        output += square1.getOutput();
        output += square2.getOutput();
        
        return output;
    }

    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        square0.hardReset();
        square1.hardReset();
        square2.hardReset();
    }

    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    @Override
    public void softReset() {
        square0.softReset();
        square1.softReset();
        square2.softReset();
    }

    /**
     * Clocks envelopes.
     */
    @Override
    public void quarterFrame() {
        //Nothing to see here, move along
    }

    /**
     * Clocks length counters. Also clocks envelopes.
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
        square0.cycle(cycles);
        square1.cycle(cycles);
        square2.cycle(cycles);
    }   
}