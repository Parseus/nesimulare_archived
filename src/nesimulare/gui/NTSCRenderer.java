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
public class NTSCRenderer extends Renderer {
    
    private final int[] frame = new int[WIDTH * 240];
    private int framePointer = 0;
    private int frameCounter;
    private int phase;
    private final static double attentuation = 0.746;
    private final static double[] levels = { -0.117f, 0.000f, 0.308f, 0.715f, 0.397f, 0.681f, 1.0f, 1.0f };
    private final static int SAMPLESPERPIXEL = 8;
    private double[] signal_levels = new double[256 * SAMPLESPERPIXEL];
    private int ntsc_buf_ptr = 0;
    private final static int WIDTH = 604;
    
    public NTSCRenderer() {
        super();
    }

    @Override
    public BufferedImage render(int[][] nespixels) {
        ++frameCounter;
        if ((frameCounter & 1) == 0) {
            phase = 0;
        } else {
            phase = 6;
        }
        for (int i = 0; i < 240; ++i) {
            final double phi = (phase + 3.9 - 0.3) % 12;
            for (int j = 0; j < 256; ++j) {
                ntsc_render(nespixels[i][j]);
            }
            ntsc_decode(phi);
            ntsc_buf_ptr = 0;
        }
        framePointer = 0;
        return getImageFromArray(frame, WIDTH * 8, WIDTH, 224);
    }

    private void ntsc_render(int pixel) {
        final int color = pixel & 0xf;
        int level = (pixel >> 4) & 3;
        final int emphasis = (pixel >> 6);
        if (color > 13) {
            level = 1;
        }
        double low = levels[level];
        double high = levels[4 + level];
        if (color == 0) {
            low = high;
        } else if (color > 12) {
            high = low;
        }
        for (int i = 0; i < SAMPLESPERPIXEL; ++i, ++phase) {
            double signal = inColorPhase(color, phase) ? high : low;
            if (emphasis != 0) {
                if ((Tools.getbit(emphasis, 0) && inColorPhase(0, phase))
                        || (Tools.getbit(emphasis, 1) && inColorPhase(4, phase))
                        || (Tools.getbit(emphasis, 2) && inColorPhase(8, phase))) {
                    signal *= attentuation;
                }
            }
            signal_levels[ntsc_buf_ptr++] = signal;
        }
    }

    private static boolean inColorPhase(final int color, final int phase) {
        return (color + phase) % 12 < 6;
    }

    private void ntsc_decode(final double phase) {
        for (int x = 0; x < WIDTH; ++x) {
            final int center = x * (256 * SAMPLESPERPIXEL) / WIDTH + 0;
            int begin = center - 6;
            if (begin < 0) {
                begin = 0;
            }
            int end = center + 6;
            if (end > 256 * SAMPLESPERPIXEL) {
                end = (256 * SAMPLESPERPIXEL);
            }
            double y = 0, i = 0, q = 0;
            for (int p = begin; p < end; ++p) {
                final double level = signal_levels[p] / 12.;
                y += level;
                i += level * Math.cos(Math.PI * (phase + p) / 6.);
                q += level * Math.sin(Math.PI * (phase + p) / 6.);
            }
            render_pixel(y, i, q);
        }
    }

    private void render_pixel(final double y, final double i, final double q) {
        final int rgb = 0xff000000
                | 0x10000 * clamp(255.95 * gammafix(y + 0.946882f * i + 0.623557f * q))
                + 0x00100 * clamp(255.95 * gammafix(y + -0.274788f * i + -0.635691f * q))
                + 0x00001 * clamp(255.95 * gammafix(y + -1.108545f * i + 1.709007f * q));
        frame[framePointer++] = rgb;
    }

    public static int clamp(final double a) {
        return (int) ((a < 0) ? 0 : ((a > 255) ? 255 : a));
    }

    public static double gammafix(double luma) {
        final float gamma = 2.0f; // Assumed display gamma
        return luma <= 0.f ? 0.f : Math.pow(luma, 2.2f / gamma);
    }
}