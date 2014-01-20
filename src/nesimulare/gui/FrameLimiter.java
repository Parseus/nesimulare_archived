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

public class FrameLimiter extends Thread {
    NES nes;
    public double frameTime;
    private double elapsedTime;
    private double fps;
    private double framePeriod;
    private double lastFrameTime;
    private double sleepTime;
    public boolean enabled;

    public FrameLimiter(NES nes) {
        super();
        this.nes = nes;
        this.enabled = true;
        
        hardReset();
    }
    
    public final void hardReset() {
        fps = (nes.region == Region.NTSC) ? 60.098813897440515532 : 50.006977968268290849;
        framePeriod = 1000 / fps;
    }

    public void sleep() {
        //Frame Limiter
        elapsedTime = (System.nanoTime() / 1000000.0) - lastFrameTime;
        sleepTime = (framePeriod - elapsedTime);
        
        frameTime = enabled ? fps - sleepTime : 1000 / elapsedTime;
        
        if (enabled) {
            if (sleepTime > 0) {
                try {
                    Thread.sleep((int)sleepTime);
                } catch (InterruptedException ie) {
                    nes.messageBox(ie.getMessage());
                }
            }
        }
        
        lastFrameTime = (System.nanoTime() / 1000000.0);
    }

    public void sleepFixed() {
        elapsedTime = (System.nanoTime() / 1000000.0) - lastFrameTime;
        sleepTime = framePeriod - elapsedTime;
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            nes.messageBox(ie.getMessage());
        }
        
        lastFrameTime = (System.nanoTime() / 1000000.0);
    }
}