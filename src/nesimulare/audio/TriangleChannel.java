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

package nesimulare.audio;

import nesimulare.Tools;

/**
 *
 * @author Parseus
 */
public class TriangleChannel {
           
    static int[] lenctrTable =  {
            0x0A, 0xFE, 0x14, 0x02, 0x28, 0x04, 0x50, 0x06, 0xA0, 0x08, 0x3C, 0x0A, 0x0E, 0x0C, 0x1A, 0x0E,
            0x0C, 0x10, 0x18, 0x12, 0x30, 0x14, 0x60, 0x16, 0xC0, 0x18, 0x48, 0x1A, 0x10, 0x1C, 0x20, 0x1E
    };
    
    static int[] stepSequence = {
        0x0F, 0x0E, 0x0D, 0x0C, 0x0B, 0x0A, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00,
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };
    
    private int cycles;
    private int output;
    private int frequency;
    private int step;
    public int lenctr;
    private int linearCount, linearCountReload;
    private boolean active, enabled;
    private boolean linearLoop, linearHalt;
    
    public void hardReset() {
        cycles = 1;
        frequency = 0;
        output = 0;
        step = 0;
        lenctr = 0;
        active = enabled = false;
        linearLoop = linearHalt = false; 
    }
    
    public void softReset() {
        lenctr = 0;
    }
    
    public void write(final int register, final int data) {
        switch (register) {
            /**
             * CRRR RRRR
             * Length counter halt / linear counter control (C), 
             * linear counter load (R) 
             */
            case 0:
                linearLoop = Tools.getbit(data, 7);
                linearCountReload = data & 0x7F;
                break;
              
            /**
             * TTTT TTTT
             * Timer low (T)
             */
            case 2:
                frequency = (frequency & 0x700) | data;
                break;
            
            /**
             * LLLL LTTT
             * Length counter load (L), timer high (T)
             */    
            case 3:
                if (enabled) {
                    lenctr = lenctrTable[(data >> 3) & 0x1F];
                }
                
                frequency = (frequency & 0xFF) | ((data & 7) << 8);
                linearHalt = true;
                linearLoop = true;
                break;
            
            case 4:
                enabled = (data != 0);
                
                if (!enabled) {
                    lenctr = 0;
                }
                break;
                
            default:
                break;
        }
        
        checkActive();
    }
    
    public void cycle() {
        if (--cycles == 0) {
            cycles = frequency;
            
            if (active) {
                step = (step + 1) & 0x1F;
                
                if (frequency < 4) {
                    output = 0;
                } else {
                    output = stepSequence[step] << 3;
                }
            }
        }
    }
    
    private void checkActive() {
        active = (lenctr > 0 && linearCount > 0);
        
        if (frequency < 4) {
            output = 0;
        } else {
            output = stepSequence[step] << 3;
        }
    }
    
    public void quarterFrame() {
        if (linearHalt) {
            linearCount = linearCountReload;
        } else if (linearCount > 0) {
            linearCount--;
        }
        
        if (!linearLoop) {
            linearHalt = false;
        }
        
        checkActive();
    }
    
    public void halfFrame() {
        if (lenctr > 0 && !linearLoop) {
            lenctr--;
        }
        
        checkActive();
    }
    
    public final int getOutput() {
        return output;
    }
}