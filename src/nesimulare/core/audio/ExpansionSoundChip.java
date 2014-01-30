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

/**
 * An interface class for additional sound chips (for Famicom).
 *
 * @author Parseus
 */
public interface ExpansionSoundChip {
    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    int getOutput();
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    void hardReset();
    
    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    void softReset();
    
    /**
     * Clocks length counters and sweep units.
     */
    void quarterFrame();
    
    /**
     * Clocks envelopes and triangle's linear counter.
     * Also clocks length counters and sweep units.
     */
    void halfFrame();
    
    /**
     * Clocks a channel depending on clocking length.
     */
    void clockChannel(boolean clockingLength);
    
    /**
     * Performs a given number of machine cycles.
     * 
     * @param cycles        Number of machine cycles.
     */
    void cycle(int cycles);
}