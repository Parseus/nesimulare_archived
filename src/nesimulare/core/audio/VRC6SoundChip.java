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
 *
 * @author Parseus
 */
public class VRC6SoundChip implements ExpansionSoundChip {
    public VRC6PulseSoundChannel pulse1, pulse2;
    public VRC6SawtoothSoundChannel sawtooth;
    
    public VRC6SoundChip(Region.System system) {
        pulse1 = new VRC6PulseSoundChannel(system);
        pulse2 = new VRC6PulseSoundChannel(system);
        sawtooth = new VRC6SawtoothSoundChannel(system);
    }

    @Override
    public short mix() {
        short output = (short)pulse1.getOutput();
        output += (short)pulse2.getOutput();
        output += (short)sawtooth.getOutput();
        
        return output;
    }

    @Override
    public void hardReset() {
        pulse1.hardReset();
        pulse2.hardReset();
        sawtooth.hardReset();
    }

    @Override
    public void softReset() {
        pulse1.softReset();
        pulse2.softReset();
        sawtooth.softReset();
    }

    @Override
    public void quarterFrame() {
        //Nothing to see here, move along
    }

    @Override
    public void halfFrame() {
        //Nothing to see here, move along
    }

    @Override
    public void clockChannel(boolean clockingLength) {
        //Nothing to see here, move along
    }

    @Override
    public void cycle(int cycles) {
        pulse1.cycle(cycles);
        pulse2.cycle(cycles);
        sawtooth.cycle(cycles);
    }
}