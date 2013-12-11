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

package nesimulare.core.ppu;

public class PPUTypes {
    public static class Fetch {
        public int address;
        public int attribute;
        public int bit0;
        public int bit1;
        public int name;
    }
    
    public static class Scroll {
        public boolean swap;
        public int address;
        public int fine;
        public int step = 1;
        public int temp;
        
        public void clockX() {
            if ((address & 0x001F) == 0x001F) {
                address ^= 0x041F;
            } else {
                address++;
            }
        }
        
        public void clockY() {
            if ((address & 0x7000) != 0x7000) {
                address += 0x1000;
            } else {
                address ^= 0x7000;
                
                switch (address & 0x3E0) {
                    case 0x3A0:
                        address ^= 0xBA0;
                        break;
                    case 0x3E0:
                        address ^= 0x3E0;
                        break;
                    default:
                        address += 0x20;
                        break;
                }
            }
        }
        
        public void resetX() {
            address = (address & ~0x41F) | (temp & 0x41F);
        }
        
        public void resetY() {
            address = temp;
        }
    }
    
    public static class Sprite {
        public int y;
        public int name;
        public int attribute;
        public int x;
        public boolean zero;
    }
    
    public static class Unit {
        public boolean clipped;
        public boolean enabled;
        public int address;
        public int rasters = 8;
        public int[] pixels;
        
        public Unit(int capacity) {
            this.pixels = new int[capacity];
        }
        
        public final int getPixel(int hclock, int offset) {
            if (!enabled || (clipped && hclock < 8)) {
                return 0;
            }
            
            return pixels[hclock + offset];
        }
    }
}