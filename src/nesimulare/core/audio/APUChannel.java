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

import nesimulare.core.ProcessorBase;

/**
 *
 * @author Parseus
 */


public class APUChannel extends ProcessorBase {
    protected static int[] lenctrTable =  {
            0x0A, 0xFE, 0x14, 0x02, 0x28, 0x04, 0x50, 0x06, 0xA0, 0x08, 0x3C, 0x0A, 0x0E, 0x0C, 0x1A, 0x0E,
            0x0C, 0x10, 0x18, 0x12, 0x30, 0x14, 0x60, 0x16, 0xC0, 0x18, 0x48, 0x1A, 0x10, 0x1C, 0x20, 0x1E,
    };
    
    protected int lenctr, lenctrReload = 0;
    protected boolean lenctrLoop, lenctrHalt = false, lenctrHaltRequest = false, lenctrReloadEnabled, lenctrReloadRequest = false;
    protected int frequency;
    
    public APUChannel(nesimulare.core.Region.System system) {
        super(system);
        region.singleCycle = getCycles(frequency + 1);
    }
    
    protected final int getCycles(int cycles) {
        return cycles * system.apu;
    }

    @Override
    public void initialize() {
        hardReset();
        super.initialize();
    }
    
    @Override
    public void hardReset() {
        lenctrHalt = false;
        lenctrHaltRequest = false;
        lenctr = 0;
        lenctrReloadEnabled = false;
        lenctrReload = 0;
        lenctrReloadRequest = false;
    }
    
    @Override
    public void softReset() {
        lenctrReloadEnabled = false;
        lenctr = 0;
    }
    
    public void clockChannel(boolean clockLength) {
        lenctrHalt = lenctrHaltRequest;
        
        if (clockLength && lenctr > 0) {
            lenctrReloadRequest = false;
        }
        
        if (lenctrReloadRequest) {
            if (lenctrReloadEnabled) {
                lenctr = lenctrReload;
            }
            
            lenctrReloadRequest = false;
        }
    }
    
    public boolean getStatus() {
        return (lenctr > 0);
    }
    
    public void setStatus(boolean status) {
        lenctrReloadEnabled = status;
        
        if (!lenctrReloadEnabled) {
            lenctr = 0;
        }
    }
}