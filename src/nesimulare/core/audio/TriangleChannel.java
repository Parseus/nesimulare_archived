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

import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class TriangleChannel extends APUChannel { 
    private static final int[] stepSequence = {
        0x0F, 0x0E, 0x0D, 0x0C, 0x0B, 0x0A, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00,
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };
    
    private int output;
    private int step;
    private int counter = 0;
    private int counterReload;
    private boolean counterHalt;
    private boolean halt;
    
    public TriangleChannel(nesimulare.core.Region.System system) {
        super(system);
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        hardReset();
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        counter = 0;
        counterReload = 0;
        output = 0;
        step = 0;
        counterHalt = false;
        halt = false;
    }
    
    public void write(final int register, final int data) {
        switch (register) {
            /**
             * CRRR RRRR
             * Length counter halt / linear counter control (C), 
             * linear counter load (R) 
             */
            case 0:
                counterHalt = lenctrHaltRequest = Tools.getbit(data, 7);
                counterReload = data & 0x7F;
                break;
              
            /**
             * TTTT TTTT
             * Timer low (T)
             */
            case 2:
                frequency = (frequency & 0x700) | data;
                updateFrequency();
                break;
            
            /**
             * LLLL LTTT
             * Length counter load (L), timer high (T)
             */    
            case 3:
                lenctrReload = lenctrTable[data >> 3];
                lenctrReloadRequest = true;
                halt = true;
                
                frequency = (frequency & 0xFF) | ((data & 7) << 8);
                updateFrequency();
                break;
                
            default:
                break;
        }
    }
    
    @Override
    public void cycle() {
        if (lenctr > 0 && counter > 0 && frequency < 4) {
            step++;
            step &= 0x1F;
            output = stepSequence[step];
        }
    }
    
    public void quarterFrame() {
        if (halt) {
            counter = counterReload;
        } else if (counter != 0) {
            counter--;
        }
        
        halt &= counterHalt;
    }
    
    public void halfFrame() {
        if (!lenctrHalt && lenctr > 0) {
            lenctr = (lenctr - 1) & 0xFF;
        }
    }
    
    private void updateFrequency() {
        int time = (frequency + 1) / 2;
        
        if (time == 0) {
            time = 1;
        }
        
        region.singleCycle = getCycles(time);
    }
    
    public final int getOutput() {
        return output & 0xFF;
    }
}