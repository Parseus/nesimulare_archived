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

import nesimulare.core.Region;

/**
 *
 * @author Parseus
 */
public class Sunsoft5BSoundChip implements ExpansionSoundChip {
    public Sunsoft5BSquareChannel square0, square1, square2;
    
    public Sunsoft5BSoundChip(Region.System system) {
        square0 = new Sunsoft5BSquareChannel(system);
        square1 = new Sunsoft5BSquareChannel(system);
        square2 = new Sunsoft5BSquareChannel(system);
    }
    
    @Override
    public short mix() {
        short output = (short)square0.getOutput();
        output += (short)square1.getOutput();
        output += (short)square2.getOutput();
        
        return output;
    }

    @Override
    public void hardReset() {
        square0.hardReset();
        square1.hardReset();
        square2.hardReset();
    }

    @Override
    public void softReset() {
        square0.softReset();
        square1.softReset();
        square2.softReset();
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
        square0.cycle(cycles);
        square1.cycle(cycles);
        square2.cycle(cycles);
    }   
}