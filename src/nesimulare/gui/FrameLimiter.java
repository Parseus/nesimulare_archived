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
    private long sleepingtest = 0;
    public final long FRAME_NS;

    public FrameLimiter(NES nes) {
        this.nes = nes;
        FRAME_NS = nes.region == Region.NTSC ? 16638935 : 19997200;
        forceHighResolutionTimer();
    }

    public void sleep() {
        //Frame Limiter
        final long timeleft = System.nanoTime() - nes.frameStartTime;
        if (timeleft < FRAME_NS) {
            final int sleepytime = (int) (FRAME_NS - timeleft + sleepingtest);
            if (sleepytime < 0) {
                return;
                //don't sleep at all.
            }
            sleepingtest = System.nanoTime();
            try {
                //System.err.println(sleepytime/ 1000000.);
                Thread.sleep(sleepytime / 1000000);
                // sleep for rest of the time until the next frame
                } catch (InterruptedException ex) {
            }
            sleepingtest = System.nanoTime() - sleepingtest;
            //now sleeping test has how many ns the sleep *actually* was
            sleepingtest = sleepytime - sleepingtest;
            //now sleepingtest has how much the next frame needs to be delayed by to make things match
        }
    }

    public void sleepFixed() {
        try {
            //sleep for 16 ms
            Thread.sleep(16);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

    }

    public static void forceHighResolutionTimer() {
        Thread daemon;
        daemon = new Thread("ForceHighResolutionTimer") {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(99999999999L);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };
        daemon.setDaemon(true);
        daemon.start();
    }
}