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
 *
 * @author Parseus
 */
public class Region {
    public static class System {
        public String name;
        public int serial;
        public int master;
        public int cpu;
        public int ppu;
        public int apu;
        
        public System(String name, int serial, int master, int cpu, int ppu, int apu) {
            this.name = name;
            this.serial = serial;
            this.master = master;
            this.cpu = cpu;
            this.ppu = ppu;
            this.apu = apu;
        }
    }

    public static final System NTSC = new System("NTSC", 0, 236250000, 132, 44, 264);
    public static final System PAL = new System("PAL", 1, 212813696, 128, 40, 256);
    public static final System DENDY = new System("Dendy", 2, 228774792, 129, 43, 258);
    
    public int cycles;
    public int period;
    public int singleCycle;
}