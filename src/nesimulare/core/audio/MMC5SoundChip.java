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
 * Emulates MMC5 sound chip, which consists of two pulse wave channels and a PCM channel.
 *
 * @see MMC5PCMChannel
 * @see MMC5PulseChannel
 *
 * @author Parseus
 */
public class MMC5SoundChip implements ExpansionSoundChip {

    public MMC5PulseChannel pulse1, pulse2;
    public MMC5PCMChannel pcm;

    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     */
    public MMC5SoundChip(Region.System system) {
        pulse1 = new MMC5PulseChannel(system);
        pulse2 = new MMC5PulseChannel(system);
        pcm = new MMC5PCMChannel(system);
    }

    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        pulse1.hardReset();
        pulse2.hardReset();
        pcm.hardReset();
    }

    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    @Override
    public void softReset() {
        pulse1.softReset();
        pulse2.softReset();
        pcm.softReset();
    }

    /**
     * Returns the status of channel ($5015).
     *
     * @return      0: Pulse 1 - length counter = 0; Pulse 2 - length counter = 0 
     *              1: Pulse 1 - length counter > 0; Pulse 2 - length counter = 0 
     *              2: Pulse 1 - length counter = 0; Pulse 2 - length counter > 0
     *              3: Pulse 1 - length counter > 0; Pulse 2 - length counter > 0
     */
    public int getStatus() {
        final int result = (pulse1.getStatus() ? 0x1 : 0x0) | (pulse2.getStatus() ? 0x2 : 0x0);

        return result;
    }

    /**
     * Writes data to a given register
     *
     * @param address Register to write data to
     * @param data Written data
     */
    public void write(int address, int data) {
        switch (address) {
            /**
             * $5000-5003: Pulse 1
             */
            case 0x5000:
            case 0x5001:
            case 0x5002:
            case 0x5003:
                pulse1.write(address & 3, data);
                break;
                
            /**
             * $5004-5007: Pulse 2
             */
            case 0x5004:
            case 0x5005:
            case 0x5006:
            case 0x5007:
                pulse2.write(address & 3, data);
                break;
                
            /**
             * $5010-5011: PCM
             */
            case 0x5010:
            case 0x5011:
                pcm.write(address & 3, data);
                break;
                
            /**
             * $5015: Status
             */
            case 0x5015:
                pulse1.setStatus(Tools.getbit(data, 0));
                pulse2.setStatus(Tools.getbit(data, 1));
                break;
                
            default:
                break;
        }
    }

    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    @Override
    public int getOutput() {
        int output = pulse1.getOutput();
        output += pulse2.getOutput();
        output += pcm.getOutput();

        return output;
    }

    /**
     * Clocks envelopes for both pulse wave channels.
     */
    @Override
    public void quarterFrame() {
        pulse1.quarterFrame();
        pulse2.quarterFrame();
    }

    /**
     * Clocks length counters for both pulse wave channels.
     * Also clocks envelopes.
     */
    @Override
    public void halfFrame() {
        pulse1.halfFrame();
        pulse2.halfFrame();
    }

    /**
     * Clocks channels depending on clocking length.
     */
    @Override
    public void clockChannel(boolean clockingLength) {
        pulse1.clockChannel(clockingLength);
        pulse2.clockChannel(clockingLength);
        pcm.clockChannel(clockingLength);
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
        pcm.cycle(cycles);
    }
}