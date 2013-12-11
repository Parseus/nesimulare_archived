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

import java.awt.image.BufferedImage;

/**
 *
 * @author Parseus
 */
public class RGBRenderer extends Renderer {

    @Override
    public BufferedImage render(int[][] nespixels) {
        final int colors[] = new int[240 * 256];
        
        for (int y = 0; y < 240; y++) {
            System.arraycopy(nespixels[y], 0, colors, y * 256, 256);
        }
        
        for (int i = 0; i < colors.length; ++i) {
            colors[i] = Colors.colorsTable[(colors[i] & 0x1c0) >> 6][colors[i] & 0x3f];
        }
        
        return getImageFromArray(colors, 256 * 8, 256, 224);
    }
}