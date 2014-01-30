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
import static nesimulare.core.audio.APUChannel.lenctrTable;
import nesimulare.gui.Tools;

/**
 * Emulates a pulse wave channel that is a part of MMC5 sound chip.
 * It functions almost identically to the stock 2A03 pulse wave channel
 * except it doesn't have a sweep unit and an equivalent frame counter.
 * @see PulseChannel
 *
 * @author Parseus
 */
public class MMC5PulseChannel extends APUChannel {
    /**
     * Duty cycle sequences
     */
    static final int[][] dutySequences = { 
        new int[] { 0, 1, 0, 0, 0, 0, 0, 0 },       //12.5%
        new int[] { 0, 1, 1, 0, 0, 0, 0, 0 },       //25%
        new int[] { 0, 1, 1, 1, 1, 0, 0, 0 },       //50%
        new int[] { 1, 0, 0, 1, 1, 1, 1, 1 }        //25% negated
    };
    
    private int output;
    private int dutyLength, dutyCycle;
    private int envelopeCount, envelopeTimer, envelopeSound;
    private int envelopeDelay, envelopeVolume;
    private boolean envelopeEnabled, envelopeLoop, envelopeReload;
    
    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     */
    public MMC5PulseChannel(Region.System system) {
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
        envelopeCount = envelopeTimer = envelopeSound = 0;
        dutyLength = dutyCycle = 0;
        envelopeDelay = envelopeVolume = 0;
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
             * $5000/$5004
             * DDLC VVVV
             * Duty (D), envelope loop / length counter halt (L), 
             * constant volume (C), volume/envelope (V)
             */
            case 0:
                lenctrHaltRequest = Tools.getbit(data, 5);
                envelopeLoop = Tools.getbit(data, 5);
                envelopeEnabled = Tools.getbit(data, 4);
                envelopeDelay = data & 0xF;
                dutyLength = (data & 0xC0) >> 6;
                envelopeVolume = envelopeEnabled ? envelopeDelay : envelopeCount;
                break;
              
            /**
             * $5002/$5006
             * TTTT TTTT
             * Timer low (T)
             */
            case 2:
                frequency = (frequency & 0x700) | data;
                updateFrequency();
                break;
            
            /**
             * $5003/$5007
             * LLLL LTTT
             * Length counter load (L), timer high (T)
             */    
            case 3:
                lengthCounterReload = lenctrTable[data >> 3];
                lenctrReloadRequest = true;
                envelopeReload = true;
                
                frequency = (frequency & 0x00FF) | ((data & 7) << 8);
                dutyCycle = 0;
               
                updateFrequency();
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
        envelopeSound = envelopeEnabled ? envelopeVolume : envelopeCount;
        
        dutyCycle = (dutyCycle + 1) & 7;
            
        if (lengthCounter > 0 && validFreq()) {
            output = (dutySequences[dutyLength][dutyCycle] * envelopeSound) & 0xFF;
        } else {
            output = 0;
        }
    }
    
    /**
     * Checks if a frequency is within a valid range.
     * 
     * @return      True: Frequency is within a valid range
     *              False: Frequency is outside a valid range
     */
    private boolean validFreq() {
        return (frequency >= 0x8) && (frequency <= 0x7FF);
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
        if (!lenctrHalt && lengthCounter > 0) {
            lengthCounter = (lengthCounter - 1) & 0xFF;
        }

        updateFrequency();
    }
    
    /**
     * Updates a single cycle timing based on frequency.
     */
    private void updateFrequency() {
        region.singleCycle = getCycles(frequency + 1);
    }
    
    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    public final int getOutput() {
        if (lengthCounter > 0 && validFreq()) {
            return output;
        } else {
            return output = 0;
        }
    }
}