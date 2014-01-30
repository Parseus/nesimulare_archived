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

package nesimulare.core;

/**
 * The interface class for processors and APU channels.
 *
 * @author Parseus
 */
public class ProcessorBase {
    public Region region = new Region();
    public Region.System system;
    
    /**
     * Constructor for this class. Connects an emulated region with a given processor/channel.
     * 
     * @param system 
     */
    public ProcessorBase(Region.System system) {
        this.system = system;
    }
    
    /**
     * Initializes a given processor/channel.
     */
    public void initialize() { 
        //Nothing to see here, move along
    }
    
    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    public void softReset() { 
        //Nothing to see here, move along
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    public void hardReset() { 
        //Nothing to see here, move along
    }
    
    /**
     * Performs an individual machine cycle.
     */
    public void cycle() { 
        //Nothing to see here, move along
    }
    
    /**
     * Performs a given number of machine cycles.
     * 
     * @param cycles        Number of machine cycles.
     */
    public void cycle(int cycles) {
        while(region.cycles < cycles) {
            region.cycles += region.singleCycle;
            
            cycle();
        }
        
        region.cycles -= cycles;
    }
}