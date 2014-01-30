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
 * Emulates a wavetable channel that is a part of Namco 163 sound chip.
 *
 * @author Parseus
 */
public class Namco163SoundChannel extends APUChannel {
    private final Namco163SoundChip soundChip;
    private int linearVolume = 0;
    private int output = 0;
    private int step = 0;
    private int waveformAddress = 0;
    private int waveformLength = 256;
    private int waveformBuffer[] = new int[256];
    public boolean enabled = false;
    private boolean freeze = true;
    
    /**
     * Constructor for this class. Connects an emulated region with a given channel.
     *
     * @param system Emulated region
     * @param soundChip Main sound chip class
     */
    public Namco163SoundChannel(Region.System system, Namco163SoundChip soundChip) {
        super(system);
        
        this.soundChip = soundChip;
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();
        
        linearVolume = 0;
        output = 0;
        step = 0;
        waveformAddress = 0;
        waveformLength = 256;
        waveformBuffer = new int[256];
        enabled = false;
        freeze = true;
    }
    
    /**
     * Given data, sets a low byte of frequency.
     * 
     * @param data Given data
     */
    protected void lowFrequency(int data) {
        frequency = (frequency & 0x03FF00) | data;
        updateFrequency();
    }
    
    /**
     * Given data, sets a mid byte of frequency.
     * 
     * @param data Given data
     */
    protected void midFrequency(int data) {
        frequency = (frequency & 0x0300FF) | (data << 8);
        updateFrequency();
    }
    
    /**
     * Given data, sets a high byte of frequency.
     * Also sets up a waveform buffer.
     * 
     * @param data Given data
     */
    protected void highFrequency(int data) {
        frequency = (frequency & 0x00FFFF) | ((data & 0x3) << 16);
        updateFrequency();
        waveformLength = (64 - ((data >> 2) & 0x3F)) << 2;
        setupWaveformBuffer();
    }
    
    /**
     * Sets address of a waveform.
     * 
     * @param data 
     */
    protected void waveAddress(int data) {
        waveformAddress = data;
        setupWaveformBuffer();
    }
    
    /**
     * Sets linear volume.
     * 
     * @param data 
     */
    protected void setVolume(int data) {
        linearVolume = data & 0xF;
    }
    
    /**
     * Performs an individual machine cycle.
     */
    @Override
    public void cycle() {
        if (freeze) {
            return;
        }
        
        if (waveformBuffer.length <= waveformLength) {
            step = (step + 1) & (waveformLength - 1);
            output = (waveformBuffer[step] * linearVolume) & 0xFF;
        }
    }
    
    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    public int getOutput() {
        if (enabled) {
            return output;
        }
        
        return 0;
    }
    
    /**
     * Updates a single cycle timing based on frequency.
     */
    private void updateFrequency() {
        freeze = (frequency == 0);
        
        if (frequency > 0) {
            region.singleCycle = system.cpu * (0xF0000 / frequency);
        }
    }
    
    /**
     * Sets up a waveform buffer.
     */
    private void setupWaveformBuffer() {
        waveformBuffer = new int[waveformLength];
        int raddress = waveformAddress >> 1;
        int sampleAddress = 0;
        boolean highLow = Tools.getbit(waveformAddress, 0);
        
        while (sampleAddress < waveformLength) {
            final int sample = highLow ? ((soundChip.exram[raddress] & 0xF0) >> 4) : (soundChip.exram[raddress] & 0xF);
            waveformBuffer[sampleAddress] = sample & 0xFF;
            
            if (highLow) {
                raddress++;
            }
                
            sampleAddress++;    
            highLow ^= true;    
        }
            
        step = 0;
    }
}