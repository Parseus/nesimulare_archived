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
 * Emulates NES APU's noise channel which generates pseudo-random 1-bit noise at 16 different frequencies.
 * The noise channel contains the following: envelope generator, timer, Linear Feedback Shift Register, length counter.
 *
 * @author Parseus
 */
public class NoiseChannel extends APUChannel {
    public int[] noiseFrequency;
    
    private int shiftRegister;
    private int envelopeCount, envelopeTimer, envelopeSound;
    private int envelopeDelay, envelopeVolume;
    private boolean modeFlag;
    private boolean envelopeEnabled, envelopeLoop, envelopeReload;
    
    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     */
    public NoiseChannel(nesimulare.core.Region.System system) {
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

        shiftRegister = 1;
        envelopeCount = envelopeTimer = envelopeSound = 0;
        envelopeDelay = envelopeVolume = 0;
        modeFlag = false;
        envelopeEnabled = envelopeLoop = envelopeReload = false;
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
             * $400C
             * --LC VVVV
             * Envelope loop / length counter halt (L), 
             * constant volume (C), volume/envelope (V)
             */
            case 0:
                lenctrHaltRequest = Tools.getbit(data, 5);
                envelopeLoop = Tools.getbit(data, 5);
                envelopeEnabled = Tools.getbit(data, 4);
                envelopeDelay = data & 0xF;
                envelopeVolume = envelopeEnabled ? envelopeDelay : envelopeCount;
                break;
              
            /**
             * $400E
             * M--- PPPP
             * Mode flag (M), noise period (P)
             */
            case 2:
                region.singleCycle = getCycles(noiseFrequency[data & 0xF]);
                modeFlag = Tools.getbit(data, 7);
                break;
            
            /**
             * $400F
             * LLLL L---
             * Length counter load (L)
             */    
            case 3:
                lengthCounterReload = lenctrTable[data >> 3];
                lenctrReloadRequest = true;
                envelopeReload = true;
                break;
                
            default:
                break;
        }
    }
    
    /**
     * Performs an individual machine cycle.
     */
    @Override
    public void cycle() {
        if (modeFlag) {
            shiftRegister = (shiftRegister << 1) | (((shiftRegister >> 14) ^ (shiftRegister >> 8)) & 1);
        } else {
            shiftRegister = (shiftRegister << 1) | (((shiftRegister >> 14) ^ (shiftRegister >> 13)) & 1);
        }
    }
    
    /**
     * Clocks envelope.
     */
    public void quarterFrame() {
        if (envelopeReload) {
            envelopeReload = false;
            envelopeCount = 0xF;
            envelopeTimer = envelopeDelay;
        } else {
            if (envelopeTimer != 0) {
                envelopeTimer--;
            } else {
                envelopeTimer = envelopeDelay;
                
                if (envelopeLoop || envelopeCount != 0) {
                    envelopeCount = (envelopeCount - 1) & 0xF;
                }
            }
        }
    }
    
    /**
     * Clocks length counter.
     */
    public void halfFrame() {
        if (!lenctrHalt) {
            if (lengthCounter > 0) {
                lengthCounter = (lengthCounter - 1) & 0xFF;
            }
        }  
    }
    
    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    public final int getOutput() {
        envelopeSound = envelopeEnabled ? envelopeVolume : envelopeCount;
        
        if (lengthCounter > 0 && !Tools.getbit(shiftRegister, 0)) {
            return envelopeSound;
        }
        
        return 0;
    }
}