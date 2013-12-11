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

/**
 *
 * @author Parseus
 */


public class Colors {
    private final static double ATTENTUATION = 0.7;
    public final static int[][] colorsTable = GetNESColors();

    private static int[][] GetNESColors() {
        int[] colorarray = {
            0x606060, 0x09268e, 0x1a11bd, 0x3409b6, 0x5e0982, 0x790939, 0x6f0c09, 0x511f09,
            0x293709, 0x0d4809, 0x094e09, 0x094b17, 0x093a5a, 0x000000, 0x000000, 0x000000,
            0xb1b1b1, 0x1658f7, 0x4433ff, 0x7d20ff, 0xb515d8, 0xcb1d73, 0xc62922, 0x954f09,
            0x5f7209, 0x28ac09, 0x099c09, 0x099032, 0x0976a2, 0x090909, 0x000000, 0x000000,
            0xffffff, 0x5dadff, 0x9d84ff, 0xd76aff, 0xff5dff, 0xff63c6, 0xff8150, 0xffa50d,
            0xccc409, 0x74f009, 0x54fc1c, 0x33f881, 0x3fd4ff, 0x494949, 0x000000, 0x000000,
            0xffffff, 0xc8eaff, 0xe1d8ff, 0xffccff, 0xffc6ff, 0xffcbfb, 0xffd7c2, 0xffe999,
            0xf0f986, 0xd6ff90, 0xbdffaf, 0xb3ffd7, 0xb3ffff, 0xbcbcbc, 0x000000, 0x000000};
        
        for (int i = 0; i < colorarray.length; ++i) {
            colorarray[i] |= 0xff000000;
        }
        
        int[][] colors = new int[8][colorarray.length];
        
        for (int j = 0; j < colorarray.length; ++j) {
            final int color = colorarray[j];
            final int r = r(color);
            final int b = b(color);
            final int g = g(color);
            
            colors[0][j] = color;
            //emphasize red
            colors[1][j] = compose_col(r, g * ATTENTUATION, b * ATTENTUATION);
            //emphasize green
            colors[2][j] = compose_col(r * ATTENTUATION, g, b * ATTENTUATION);
            //emphasize yellow
            colors[3][j] = compose_col(r, g, b * ATTENTUATION);
            //emphasize blue
            colors[4][j] = compose_col(r * ATTENTUATION, g * ATTENTUATION, b);
            //emphasize purple
            colors[5][j] = compose_col(r, g * ATTENTUATION, b);
            //emphasize cyan?
            colors[6][j] = compose_col(r * ATTENTUATION, g, b);
            //de-emph all 3 colors
            colors[7][j] = compose_col(r * ATTENTUATION, g * ATTENTUATION, b * ATTENTUATION);

        }
        
        return colors;
    }

    static final int r(int color) {
        return (color >> 16) & 0xff;
    }

    static final int g(int color) {
        return (color >> 8) & 0xff;
    }

    static final int b(int color) {
        return color & 0xff;
    }

    static final int compose_col(double r, double g, double b) {
        return (((int) r & 0xff) << 16) + (((int) g & 0xff) << 8) + ((int) b & 0xff) + 0xff000000;
    }
}
