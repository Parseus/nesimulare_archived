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
 * Emulates a PCM channel that is a part of MMC5 sound chip.
 *
 * @author Parseus
 */
public class MMC5PCMChannel extends APUChannel {
    private int output = 0;
    private boolean readMode = false;
    
    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     */
    public MMC5PCMChannel(Region.System system) {
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
        
        output = 0;
        readMode = false;
    }
    
    /**
     * Clocks a channel depending on clocking length.
     */
    @Override
    public void clockChannel(boolean clockLength) { 
        //Override a method in order not to clock length counter which doesn't exist on a PCM channel
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
             * $5010
             * 7  bit  0
             * ---- ----
             * Ixxx xxxM
             * |       |
             * |       +- Mode select (0 = write mode. 1 = read mode.)
             * +--------- PCM IRQ enable (1 = enabled.)
             */
            case 0:
                //TODO: Emulate PCM IRQ
                readMode = Tools.getbit(data, 0);
                break;
                
            /**
             * $5011
             * 7  bit  0
             * ---- ----
             * WWWW WWWW
             * |||| ||||
             * ++++-++++- 8-bit PCM data
             */
            case 1:
                //Writes are ignored in PCM read mode
                if (!readMode) {
                    output = data;
                }
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
    public final int getOutput() {
        if (readMode) {
            return output;
        }
        
        return 0;
    }
}