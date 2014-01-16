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

import nesimulare.core.Region;
import nesimulare.gui.Tools;

/**
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
    
    public Namco163SoundChannel(Region.System system, Namco163SoundChip soundChip) {
        super(system);
        
        this.soundChip = soundChip;
    }
    
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
    
    protected void lowFrequency(int data) {
        frequency = (frequency & 0x03FF00) | data;
        updateFrequency();
    }
    
    protected void midFrequency(int data) {
        frequency = (frequency & 0x0300FF) | (data << 8);
        updateFrequency();
    }
    
    protected void highFrequency(int data) {
        frequency = (frequency & 0x00FFFF) | ((data & 0x3) << 16);
        updateFrequency();
        waveformLength = (64 - ((data >> 2) & 0x3F)) << 2;
        setupWaveformBuffer();
    }
    
    protected void waveAddress(int data) {
        waveformAddress = data;
        setupWaveformBuffer();
    }
    
    protected void setVolume(int data) {
        linearVolume = data & 0xF;
    }
    
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
    
    public int getOutput() {
        if (enabled) {
            return output;
        }
        
        return 0;
    }
    
    private void updateFrequency() {
        freeze = (frequency == 0);
        
        if (frequency > 0) {
            region.singleCycle = system.cpu * (0xF0000 / frequency);
        }
    }
    
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