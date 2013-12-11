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

import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class NoiseChannel extends APUChannel {
    public int[] noiseFrequency;
    
    private int cycles;
    private int output;
    private int shiftRegister;
    private int volume;
    private int envelopeCount, envelopeTimer, envelopeSpeed;
    private boolean modeFlag;
    private boolean enabled;
    private boolean envelopeEnabled, envelopeReload;
    
    public NoiseChannel(nesimulare.core.Region.System system) {
        super(system);
    }
    
    @Override
    public void hardReset() {
        super.hardReset();
        
        cycles = 1;
        output = 0;
        shiftRegister = 1;
        volume = 0;
        envelopeCount = envelopeTimer = envelopeSpeed = 0;
        modeFlag = false;
        enabled = false;
        envelopeEnabled = envelopeReload = false;
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
                updateFrequency();
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
    
    @Override
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
    
    private void updateFrequency() {
        region.singleCycle = getCycles(frequency + 1);
    }
    
    public final int getOutput() {
        return output;
    }
}