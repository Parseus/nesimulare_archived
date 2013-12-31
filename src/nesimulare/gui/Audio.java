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

package nesimulare.gui;

import nesimulare.core.NES;
import javax.sound.sampled.*;
import nesimulare.core.Region;

/**
 *
 * @author Parseus
 */
public class Audio implements AudioInterface {

    public boolean soundEnable;
    private SourceDataLine sdl;
    private byte[] audiobuf;
    private int bufptr = 0;
    private final float outputvol;

    public Audio(final NES nes, final int samplerate) {
        soundEnable = PrefsSingleton.get().getBoolean("soundEnable", true);
        outputvol = (float) (PrefsSingleton.get().getInt("outputvol", 13107) / 16384.);
        if (soundEnable) {
            final int samplesperframe = (int) Math.ceil((samplerate * 2) / (nes.region == Region.NTSC ? 60. : 50.));
            audiobuf = new byte[samplesperframe * 2];
            try {
                AudioFormat af = new AudioFormat(
                        samplerate,
                        16,//bit
                        1,//channel
                        true,//signed
                        false //little endian
                        //(works everywhere, afaict, but macs need 44100 sample rate)
                        );
                sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af, samplesperframe * 8); //create 4 frame audio buffer
                sdl.start();
            } catch (LineUnavailableException | IllegalArgumentException a) {
                nes.messageBox("Unable to inintialize sound: " + a.getMessage());
                soundEnable = false;
            }
        }
    }
    
    @Override
    public final void flushFrame(final boolean waitIfBufferFull) {
        if (soundEnable) {
            if (sdl.available() < bufptr) {
                if (waitIfBufferFull) {
                    //write to audio buffer and don't worry if it blocks
                    sdl.write(audiobuf, 0, bufptr);
                }
                //else don't bother to write if the buffer is full
            } else {
                sdl.write(audiobuf, 0, bufptr);
            }
        }
        bufptr = 0;

    }

    @Override
    public final void outputSample(int sample) {
        if (soundEnable) {
            sample *= outputvol;
            if (sample < -32768) {
                sample = -32768;
                //System.err.println("clip");
            }
            if (sample > 32767) {
                sample = 32767;
                //System.err.println("clop");
            }
            audiobuf[bufptr] = (byte) (sample & 0xff);
            audiobuf[bufptr + 1] = (byte) ((sample >> 8) & 0xff);
            bufptr += 2;
        }
    }

    @Override
    public void pause() {
        if (soundEnable) {
            sdl.flush();
            sdl.stop();
        }
    }

    @Override
    public void resume() {
        if (soundEnable) {
            sdl.start();
        }
    }

    @Override
    public final void destroy() {
        if (soundEnable) {
            sdl.stop();
            sdl.close();
        }
    }

    @Override
    public final boolean bufferHasLessThan(final int samples) {
        //returns true if the audio buffer has less than the specified amt of samples remaining in it
        return (sdl == null) ? false : ((sdl.getBufferSize() - sdl.available()) <= samples);
    }    
}