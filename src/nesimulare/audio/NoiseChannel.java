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
public class NoiseChannel {
           
    static int[] lenctrTable =  {
            0x0A, 0xFE, 0x14, 0x02, 0x28, 0x04, 0x50, 0x06, 0xA0, 0x08, 0x3C, 0x0A, 0x0E, 0x0C, 0x1A, 0x0E,
            0x0C, 0x10, 0x18, 0x12, 0x30, 0x14, 0x60, 0x16, 0xC0, 0x18, 0x48, 0x1A, 0x10, 0x1C, 0x20, 0x1E
    };
    
    public int[] noiseFrequency;
    
    private int cycles;
    private int output;
    private int frequency;
    private int shiftRegister;
    public int lenctr;
    private int volume;
    private int envelopeCount, envelopeTimer, envelopeSpeed;
    private boolean modeFlag;
    private boolean lenctrLoop;
    private boolean enabled;
    private boolean envelopeEnabled, envelopeReload;
    
    public void hardReset() {
        cycles = 1;
        frequency = 0;
        output = 0;
        shiftRegister = 1;
        lenctr = 0;
        volume = 0;
        envelopeCount = envelopeTimer = envelopeSpeed = 0;
        modeFlag = false;
        enabled = false;
        lenctrLoop = false;
        envelopeEnabled = envelopeReload = false;
    }
    
    public void softReset() {
        lenctr = 0;
    }
    
    public void write(final int register, final int data) {
        switch (register) {
            /**
             * --LC VVVV
             * Envelope loop / length counter halt (L), 
             * constant volume (C), volume/envelope (V)
             */
            case 0:
                envelopeSpeed = data & 0xF;
                envelopeEnabled = Tools.getbit(data, 4);
                lenctrLoop = Tools.getbit(data, 5);
                volume = envelopeEnabled ? envelopeSpeed : envelopeCount;
                
                if (lenctr > 0) {
                    output = ((shiftRegister & 0x4000) != 0 ? -2 : 2) * volume;
                }
                break;
              
            /**
             * L--- PPPP
             * Loop noise (L), noise period (P)
             */
            case 2:
                frequency = data & 0xF;
                modeFlag = Tools.getbit(data, 7);
                break;
            
            /**
             * LLLL L---
             * Length counter load (L)
             */    
            case 3:
                if (enabled) {
                    lenctr = lenctrTable[(data >> 3) & 0x1F];
                }
                
                envelopeReload = true;
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
    }
    
    public void cycle() {
        if (--cycles == 0) {
            cycles = noiseFrequency[frequency];
            
            if (modeFlag) {
                shiftRegister = (shiftRegister << 1) | (((shiftRegister >> 14) ^ (shiftRegister >> 8)) & 1);
            } else {
                shiftRegister = (shiftRegister << 1) | (((shiftRegister >> 14) ^ (shiftRegister >> 13)) & 1);
            }
            
            if (lenctr > 0) {
                output = ((shiftRegister & 0x4000) != 0 ? -2 : 2) * volume;
            }
        }
    }
    
    public void quarterFrame() {
        if (envelopeReload) {
            envelopeReload = false;
            envelopeCount = 0xF;
            envelopeTimer = envelopeSpeed;
        } else if (envelopeTimer == 0) {
            envelopeTimer = envelopeSpeed;
            
            if (envelopeCount != 0) {
                envelopeCount--;
            } else {
                envelopeCount = lenctrLoop ? 0xF : 0x0;
            }
        }
        
        volume = envelopeEnabled ? envelopeSpeed : envelopeCount;
        
        if (lenctr > 0) {
            output = ((shiftRegister & 0x4000) != 0 ? -2 : 2) * volume;
        }
    }
    
    public void halfFrame() {
        if (lenctr > 0 && !lenctrLoop) {
            lenctr--;
        }  
    }
    
    public final int getOutput() {
        return output;
    }
}