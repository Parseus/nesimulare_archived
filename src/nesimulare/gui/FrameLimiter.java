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
import nesimulare.core.Region;

/**
 *
 * @author Parseus
 */

public class FrameLimiter {
    NES nes;
    public double fps;
    private double framePeriod = (1.0 / 60.0988);
    public double currentFrameTime;
    public double lastFrameTime;

    public FrameLimiter(NES nes) {
        this.nes = nes;
        
        hardReset();
    }
    
    public final void hardReset() {
        fps = (nes.region == Region.NTSC) ? 60.0988 : 50.0070;
        framePeriod = 1.0 / fps;
    }

    public void sleep() {
        //Frame Limiter
        double immediateFrameTime = currentFrameTime = System.nanoTime() - lastFrameTime;
        
        while (immediateFrameTime < framePeriod) {
            immediateFrameTime = System.nanoTime() - lastFrameTime;
        }
        
        lastFrameTime = System.nanoTime();
    }

    public void sleepFixed() {
        try {
            //sleep for 16 ms
            Thread.sleep(16);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}