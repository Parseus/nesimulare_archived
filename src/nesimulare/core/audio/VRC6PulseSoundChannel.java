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
 *
 * @author Parseus
 */
public class VRC6PulseSoundChannel extends APUChannel {
    private int output;
    private int volume;
    private int dutyLength, dutyCycle;
    private boolean enabled = true;
    private boolean mode = false;
    
    public VRC6PulseSoundChannel(Region.System system) {
        super(system);
    }
   
    @Override
    public void hardReset() {
        super.hardReset();
        
        output = 0;
        volume = 0;
        dutyLength = 0;
        dutyCycle = 0xF;
        enabled = true;
        mode = false;
    }
    
    public void write(final int register, final int data) {
        switch (register) {
            case 0:
                mode = Tools.getbit(data, 7);
                dutyLength = ((data & 0x70) >> 4) + 1;
                volume = data & 0xF;
                break;

            case 1:
                frequency = (frequency & 0x0F00) | data;
                updateFrequency();
                break;
                 
            case 2:
                frequency = (frequency & 0x00FF) | ((data & 0xF) << 8);
                updateFrequency();
                break;

            default:
                break;
        }
    }
    
    @Override
    public void cycle() {
        if (enabled) {
            if (mode) {
                output = volume;
            } else {
                dutyCycle = (dutyCycle + 1) & 0xF;
                
                if (dutyCycle >= dutyLength) {
                    output = volume;
                } else {
                    output = 0;
                }
            }
        } else {
            output = 0;
        }
    }
    
    private void updateFrequency() {
        region.singleCycle = getCycles(frequency + 1);
    }
    
    public final int getOutput() {
        if (frequency > 0x4) {
            return output;
        } else {
            return 0;
        }
    }
}