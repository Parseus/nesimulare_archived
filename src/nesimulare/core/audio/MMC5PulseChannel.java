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
    private int volume;
    private int dutyLength, dutyCycle;
    private int envelopeCount, envelopeTimer, envelopeSpeed;
    private boolean envelopeEnabled, envelopeLoop, envelopeReload;
    
    public MMC5PulseChannel(Region.System system) {
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
        
        output = 0;
        volume = 0;
        envelopeCount = envelopeTimer = envelopeSpeed = 0;
        dutyLength = dutyCycle = 0;
        envelopeEnabled = envelopeLoop = envelopeReload = false;
    }
    
    public void write(final int register, final int data) {
        switch (register) {
            /**
             * DDLC VVVV
             * Duty (D), envelope loop / length counter halt (L), 
             * constant volume (C), volume/envelope (V)
             */
            case 0:
                lenctrHaltRequest = Tools.getbit(data, 5);
                envelopeLoop = Tools.getbit(data, 5);
                envelopeEnabled = Tools.getbit(data, 4);
                envelopeSpeed = data & 0xF;
                dutyLength = (data & 0xC0) >> 6;
                volume = envelopeEnabled ? envelopeSpeed : envelopeCount;
                break;
                
            /**
             * EPPP NSSS
             * Sweep unit: enabled (E), period (P), negate (N), shift (S)
             */    
            case 1:
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
                envelopeReload = true;
                
                frequency = (frequency & 0x00FF) | ((data & 7) << 8);
                dutyCycle = 0;
               
                updateFrequency();
                break;
                
            default:
                break;
        }
    }
    
    @Override
    public void cycle() {
        dutyCycle = (dutyCycle + 1) & 7;
            
        if (lenctr > 0 && validFreq()) {
            output = (dutySequences[dutyLength][dutyCycle] * volume) & 0xFF;
        } else {
            output = 0;
        }
    }
    
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
            envelopeTimer = envelopeSpeed;
        } else {
            if (envelopeTimer != 0) {
                envelopeTimer--;
            } else {
                envelopeTimer = envelopeSpeed;
                
                if (envelopeLoop || envelopeCount != 0) {
                    envelopeCount = (envelopeCount - 1) & 0xF;
                }
            }
        }
    }
    
    /**
     * Clocks sweep.
     */
    public void halfFrame() {
        if (!lenctrHalt && lenctr > 0) {
            lenctr = (lenctr - 1) & 0xFF;
        }

        updateFrequency();
    }
    
    private void updateFrequency() {
        region.singleCycle = getCycles(frequency + 1);
    }
    
    public final int getOutput() {
        if (lenctr > 0 && validFreq()) {
            return output = (dutySequences[dutyLength][dutyCycle] * volume) & 0xFF;
        } else {
            return output = 0;
        }
    }
}