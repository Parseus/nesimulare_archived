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
public class Namco163SoundChip implements ExpansionSoundChip {
    private final Namco163SoundChannel channels[];
    public int exram[] = new int[128];
    private int lpaccum = 0;
    private int channelIndex = 0;
    public int enabledChannels = 0;
    private int soundRegister = 0;

    public Namco163SoundChip(Region.System system) {
        channels = new Namco163SoundChannel[8];
        exram = new int[128];
        channelIndex = 0;
        enabledChannels = 0;
        lpaccum = 0;
        soundRegister = 0;
        
        for (int i = 0; i < 8; i++) {
            channels[i] = new Namco163SoundChannel(system, this);
        }
    }
    
    public int readData(int address) {
        final int value = exram[soundRegister & 0x7F];
        
        if (Tools.getbit(soundRegister, 7)) {
            soundRegister = (((soundRegister + 1) & 0x7F) | 0x80) & 0xFF;
        }
        
        return value;
    }
    
    public void writeData(int address, int data) {
        if (soundRegister >= 0x40) {
            switch (soundRegister & 0x7F) {
                case 0x40: channels[0].lowFrequency(data); break;
                case 0x42: channels[0].midFrequency(data); break;
                case 0x44: channels[0].highFrequency(data); break;
                case 0x46: channels[0].waveAddress(data); break;
                case 0x47: channels[0].setVolume(data); break;
                case 0x48: channels[1].lowFrequency(data); break;
                case 0x4A: channels[1].midFrequency(data); break;
                case 0x4C: channels[1].highFrequency(data); break;
                case 0x4E: channels[1].waveAddress(data); break;
                case 0x4F: channels[1].setVolume(data); break;
                case 0x50: channels[2].lowFrequency(data); break;
                case 0x52: channels[2].midFrequency(data); break;
                case 0x54: channels[2].highFrequency(data); break;
                case 0x56: channels[2].waveAddress(data); break;
                case 0x57: channels[2].setVolume(data); break;
                case 0x58: channels[3].lowFrequency(data); break;
                case 0x5A: channels[3].midFrequency(data); break;
                case 0x5C: channels[3].highFrequency(data); break;
                case 0x5E: channels[3].waveAddress(data); break;
                case 0x5F: channels[3].setVolume(data); break;
                case 0x60: channels[4].lowFrequency(data); break;
                case 0x62: channels[4].midFrequency(data); break;
                case 0x64: channels[4].highFrequency(data); break;
                case 0x66: channels[4].waveAddress(data); break;
                case 0x67: channels[4].setVolume(data); break;
                case 0x68: channels[5].lowFrequency(data); break;
                case 0x6A: channels[5].midFrequency(data); break;
                case 0x6C: channels[5].highFrequency(data); break;
                case 0x6E: channels[5].waveAddress(data); break;
                case 0x6F: channels[5].setVolume(data); break;
                case 0x70: channels[6].lowFrequency(data); break;
                case 0x72: channels[6].midFrequency(data); break;
                case 0x74: channels[6].highFrequency(data); break;
                case 0x76: channels[6].waveAddress(data); break;
                case 0x77: channels[6].setVolume(data); break;
                case 0x78: channels[7].lowFrequency(data); break;
                case 0x7A: channels[7].midFrequency(data); break;
                case 0x7C: channels[7].highFrequency(data); break;
                case 0x7E: channels[7].waveAddress(data); break;
                case 0x7F: channels[7].setVolume(data); enableChannels(data); break;
                default: break;
           }
        }
    }
    
    public void writeRegister(int data) {
        soundRegister = data;
    }
    
    private void enableChannels(int data) {
        enabledChannels = ((data & 0x70) >> 4);
        channelIndex = 0;
        int enabledTemp = enabledChannels + 1;
        
        for (int i = 7; i >= 0; i--) {
            if (enabledTemp > 0) {
                channels[i].enabled = true;
                enabledTemp--;
            } else {
                break;
            }
        }
    }
    
    @Override
    public int mix() {
        int output = 0;
        
        int enabledTemp = enabledChannels + 1;
        
        for (int i = 7; i >= 0; i--) {
            if (enabledTemp > 0) {
                enabledTemp--;
                output += channels[i].getOutput();
            } else {
                break;
            }
        }
        
        output += lpaccum;
        lpaccum -= output * (1 / 16.);
        
        return lpaccum << 2;
    }

    @Override
    public void hardReset() {
        for (int i = 0; i < 8; i++) {
            channels[i].hardReset();
        }
        
        exram = new int[128];
        channelIndex = 0;
        enabledChannels = 0;
        soundRegister = 0;
    }

    @Override
    public void softReset() {
        for (int i = 0; i < 8; i++) {
            channels[i].softReset();
        }
        
        exram = new int[128];
        channelIndex = 0;
        enabledChannels = 0;
        soundRegister = 0;
    }

    @Override
    public void quarterFrame() {
        //Nothing to see here, move along
    }

    @Override
    public void halfFrame() {
        //Nothing to see here, move along
    }

    @Override
    public void clockChannel(boolean clockingLength) {
        //Nothing to see here, move along
    }

    @Override
    public void cycle(int cycles) {
        channels[7 - (channelIndex = ((channelIndex + 1) & enabledChannels))].cycle(cycles);
    }
}