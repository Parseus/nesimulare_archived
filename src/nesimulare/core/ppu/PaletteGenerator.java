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
 
package nesimulare.core.ppu;
 
import nesimulare.gui.Tools;
 
/**
 *
 * @author Parseus
 */
public class PaletteGenerator {
    static float saturation = 1.0F;
    static float hueTweak = 0.0F;
    static float contrast = 1.0F;
    static float brightness = 1.0F;
    static float gamma = 1.8F;
    
    static final float BLACK = 0.518F;
    static final float WHITE = 1.962F;
    static final float ATTENTUATION = 0.746F;
    static final float WB = WHITE - BLACK;
    static final double PI = Math.PI / 6.0;
    static float bright;

    static float cos[] = new float[12];
    static float sin[] = new float[12];
   
    static final float levels[] =
        { 0.350F, 0.518F, 0.962F, 1.550F,       //Signal low
          1.094F, 1.506F, 1.962F, 1.962F        //Signal high
        };
   
    final static int wave(final int p, final int color) {
        return ((color + p + 8) % 12 < 6) ? 1 : 0;
    }
   
    final static float gammafix(final float f, final float gamma) {
        return (float)(f < 0.0f ? 0.0f : Math.pow(f, 2.2f / gamma));
    }
   
    final static int clamp(final float v) {
        return (int)(v < 0 ? 0 : v > 255 ? 255 : v);
    }
   
    private static int generateRGBColor(int pixel) {
        final int color = (pixel & 0xF);
        final int level = color < 0xE ? (pixel >> 4) & 0x3 : 0x1;
       
        final float low_high[] = { levels[level + ((color == 0x0) ? 0x4 : 0x0)], levels[level + ((color <= 0xC) ? 4 : 0)] };
       
        float y = 0.0f, i = 0.0f, q = 0.0f;
       
        for (int p = 0; p < 12; p++) {
            float spot = low_high[wave(p, color)];
           
            if (Tools.getbit(pixel, 6) && wave(p, 0xC) == 1 ||
                Tools.getbit(pixel, 7) && wave(p, 0x4) == 1 ||
                Tools.getbit(pixel, 8) && wave(p, 0x8) == 1) {
                spot *= ATTENTUATION;
            }
           
            float v = (spot - BLACK) / WB;
            v = (v - 0.5f) * contrast + 0.5f;
            v *= bright;
            y += v;
            i += (v * cos[p]);
            q += (v * sin[p]);
        }
       
        i *= saturation;
        q *= saturation;
       
        return 0x10000 * clamp(255.95F * gammafix(y + 0.946882F * i + 0.623557F * q, gamma)) +
               0x00100 * clamp(255.95F * gammafix(y - 0.274788F * i - 0.635691F * q, gamma)) +
               0x00001 * clamp(255.95F * gammafix(y - 1.108545F * i + 1.709007F * q, gamma));
    }
   
    public static int[] generattePalette() {
        int palette[] = new int[512];
        
        bright = brightness / 12.0f;
        
        for (int p = 0; p < 12; p++) {
            cos[p] = (float) Math.cos(PI * (p + hueTweak));
            sin[p] = (float) Math.sin(PI * (p + hueTweak));
        }
       
        for (int i = 0; i < 512; i++) {
            palette[i] = generateRGBColor(i) | (0xFF << 24);
        }
       
        return palette;
    }
}