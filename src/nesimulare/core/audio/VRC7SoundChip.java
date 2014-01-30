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

import java.util.Arrays;
import nesimulare.gui.Tools;

/**
 * Emulates VRC7 sound chip, which produces 6 channels of 2-operator FM Synthesis Audio.
 * It is a derivative of the Yamaha YM2413 OPLL, implementing a subset of its features and containing a custom fixed patch set.
 *
 * @author Parseus
 */
public class VRC7SoundChip implements ExpansionSoundChip {
    private final static double[] attack_tbl = {0, 0, 0, 0,
        0.00147964, 0.001827788, 0.002219467, 0.002589363,
        0.00295653, 0.003280789, 0.004438896, 0.005178727,
        0.005918528, 0.007147843, 0.009131117, 0.010357663,
        0.011837056, 0.014622722, 0.017718715, 0.020728745,
        0.023675206, 0.029243774, 0.035121416, 0.041430652,
        0.046655732, 0.058487549, 0.071032186, 0.082847896,
        0.094709582, 0.121904762, 0.142064373, 0.165695793,
        0.189349112, 0.234003656, 0.284128746, 0.331606218,
        0.378698225, 0.468007313, 0.567627494, 0.663212435,
        0.775757576, 0.934306569, 1.137777778, 1.32642487,
        1.514792899, 1.868613139, 2.265486726, 2.639175258,
        3.047619048, 3.657142857, 4.266666667, 4.740740741,
        5.12, 6.095238095, 7.529411765, 8.533333333,
        9.142857143, 11.63636364, 14.22222222, 18.28571429,
        511, 511, 511, 511,
        511, 511, 511, 511,
        511, 511, 511, 511,
        511, 511, 511, 511,
        511, 511, 511, 511,
        511, 511, 511, 511,};
    
    private final static double[] decay_tbl = {0, 0, 0, 0,
        0.000122332, 0.000152316, 0.000175261, 0.000211944,
        0.000244665, 0.000304632, 0.000365559, 0.000425651,
        0.00048933, 0.000609264, 0.000731117, 0.000851302,
        0.000978661, 0.001173833, 0.00146223, 0.001702603,
        0.001957321, 0.002437051, 0.002924478, 0.003405206,
        0.003914672, 0.004874148, 0.005848888, 0.006808873,
        0.007829225, 0.009748296, 0.011698044, 0.013620644,
        0.01565845, 0.01949585, 0.023396088, 0.027242737,
        0.031318816, 0.038994669, 0.046792177, 0.054479677,
        0.063888196, 0.07992507, 0.093567251, 0.108982546,
        0.125244618, 0.156002438, 0.188235294, 0.21787234,
        0.250489237, 0.31181486, 0.374269006, 0.436115843,
        0.500978474, 0.624390244, 0.748538012, 0.870748299,
        1.003921569, 1.248780488, 1.497076023, 1.741496599,
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        //last lines duplicated to account for key scaling
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        2.015748031, 2.015748031, 2.015748031, 2.015748031};

    private static enum adsr {
        CUTOFF, ATTACK, DECAY, SUSTAIN, SUSTRELEASE, RELEASE;
    }
    
    private final adsr[] modenv_state = new adsr[6], carenv_state = new adsr[6];
    private final int[] vol = new int[6], freq = new int[6],
            octave = new int[6], instrument = new int[6],
            mod = new int[6],
            oldmodout = new int[6], out = new int[6];
    private final boolean[] key = new boolean[6], sust = new boolean[6];
    private int fmctr = 0, amctr = 0; //free running counter for indices
    private final double[] wave = new double[6], modenv_vol = new double[6], carenv_vol = new double[6];
    private final int[][] instdata = { //instrument parameters
        {00, 00, 00, 00, 00, 00, 00, 00}, //user tone register
        //here's the latest one from rainwarrior aug.2012
        {0x03, 0x21, 0x05, 0x06, 0xB8, 0x82, 0x42, 0x27},
        {0x13, 0x41, 0x13, 0x0D, 0xD8, 0xD6, 0x23, 0x12},
        {0x31, 0x11, 0x08, 0x08, 0xFA, 0x9A, 0x22, 0x02},
        {0x31, 0x61, 0x18, 0x07, 0x78, 0x64, 0x30, 0x27},
        {0x22, 0x21, 0x1E, 0x06, 0xF0, 0x76, 0x08, 0x28},
        {0x02, 0x01, 0x06, 0x00, 0xF0, 0xF2, 0x03, 0xF5},
        {0x21, 0x61, 0x1D, 0x07, 0x82, 0x81, 0x16, 0x07},
        {0x23, 0x21, 0x1A, 0x17, 0xCF, 0x72, 0x25, 0x17},
        {0x15, 0x11, 0x25, 0x00, 0x4F, 0x71, 0x00, 0x11},
        {0x85, 0x01, 0x12, 0x0F, 0x99, 0xA2, 0x40, 0x02},
        {0x07, 0xC1, 0x69, 0x07, 0xF3, 0xF5, 0xA7, 0x12},
        {0x71, 0x23, 0x0D, 0x06, 0x66, 0x75, 0x23, 0x16},
        {0x01, 0x02, 0xD3, 0x05, 0xA3, 0x92, 0xF7, 0x52},
        {0x61, 0x63, 0x0C, 0x00, 0x94, 0xAF, 0x34, 0x06},
        {0x21, 0x62, 0x0D, 0x00, 0xB1, 0xA0, 0x54, 0x17}
    };
    
    private static int[] logsin, exp, am;
    private final static double[] multbl = {0.5, 1, 2, 3, 4, 5,
        6, 7, 8, 9, 10, 10, 12, 12, 15, 15}, vib = genvibtbl();
    private final static int[] keyscaletbl = {0, 1536, 2048, 2368, 2560,
        2752, 2880, 3008, 3072, 3200, 3264, 3328, 3392, 3456, 3520, 3584
    };
    
    final private static int zerovol = 511;
    final private static int maxvol = 0;
    static final double pi = Math.PI;
    private int lpaccum = 0;
    private int ch = 0;
    private int s; // sign flag

    /**
     * Constructor for this class.
     */
    public VRC7SoundChip() {
        Arrays.fill(modenv_vol, 511);
        Arrays.fill(modenv_state, adsr.CUTOFF);
        Arrays.fill(carenv_state, adsr.CUTOFF);
        logsin = genlogsintbl();
        exp = genexptbl();
        am = genamtbl();
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() { 
        lpaccum = 0;
        s = 0;
        ch = 0;
    }

    /**
     * Performs a soft reset (pressing Reset button on a console).
     */
    @Override
    public void softReset() { 
        //Nothing to see here, move along
    }

    /**
     * Clocks envelopes for both pulse wave channels.
     */
    @Override
    public void quarterFrame() { 
        //Nothing to see here, move along
    }

    /**
     * Clocks length counters. Also clocks envelopes.
     */
    @Override
    public void halfFrame() { 
        //Nothing to see here, move along
    }

    /**
     * Clocks channels depending on clocking length.
     */
    @Override
    public void clockChannel(boolean clockingLength) { 
        //Nothing to see here, move along
    }

    /**
     * Generates a lookup table for vibrato.
     * 
     * @return Lookup table for vibrato
     */
    private static double[] genvibtbl() {
        final double l = 1789773 / 6.;
        final double f = 6.4;
        final int depth = 10;
        double[] tbl = new double[(int) Math.ceil(l / f)];
        
        for (int x = 0; x < tbl.length; ++x) {
            tbl[x] = (depth * tri(2 * pi * f * x / l));
        }
        
        return tbl;
    }

    /**
     * Generates a lookup table for amplitude modulation.
     * 
     * @return Lookup table for amplitude modulation
     */
    private static int[] genamtbl() {
        final double l = 1789773 / 6.;
        final double f = 3.7;
        final int depth = 128;
        int[] tbl = new int[(int) Math.ceil(l / f)];//one full cycle of wave
        
        for (int x = 0; x < tbl.length; ++x) {
            tbl[x] = (int) (depth * tri(2 * pi * f * x / l) + depth);
        }
        
        return tbl;
    }

    /**
     * Given a parameter, computes a triangle wave function.
     * 
     * @param x     Paramater for the function
     * @return      Computed value
     */
    private static double tri(double x) {
        //triangle wave function.
        x %= 2 * pi;
        if (x < (pi / 2)) {
            return x / (pi);
        } else if (x < (3 * pi) / 2) {
            return 1 - (x / (pi));
        } else {
            return x / (pi) - 2;
        }
    }

    /**
     * Generates a lookup table for logartihm of sine.
     * 
     * @return Lookup table for logarithm of sine
     */
    private static int[] genlogsintbl() {
        int[] tbl = new int[256];
        
        for (int i = 0; i < tbl.length; ++i) {
            tbl[i] = (int) Math.round(-Math.log(Math.sin((i + 0.5) * pi / 256 / 2)) / Math.log(2) * 256);
        }
        
        return tbl;
    }

    /**
     * Generates a lookup table for an exponentia function.
     * 
     * @return Lookup table for an exponentia function
     */
    private static int[] genexptbl() {
        int[] tbl = new int[256];
        
        for (int i = 0; i < tbl.length; ++i) {
            tbl[i] = (int) Math.round((Math.pow(2, i / 256.) - 1) * 1024.);
        }
        
        return tbl;
    }

    /**
     * Writes data to a given register
     *
     * @param register Register to write data to
     * @param data Written data
     */
    public final void write(int register, int data) {
        switch (register) {
            case 0: // TVSK MMMM	 Modulator tremolo (T), vibrato (V), sustain (S), key rate scaling (K), multiplier (M)
            case 1: // TVSK MMMM	 Carrier tremolo (T), vibrato (V), sustain (S), key rate scaling (K), multiplier (M)
            case 2: // KKOO OOOO	 Modulator key level scaling (K), output level (O)
            case 3: // KK-Q WFFF	 Carrier key level scaling (K), unused (-), carrier waveform (Q), modulator waveform (W), feedback (F)
            case 4: // AAAA DDDD	 Modulator attack (A), decay (D)
            case 5: // AAAA DDDD	 Carrier attack (A), decay (D)
            case 6: // SSSS RRRR	 Modulator sustain (S), release (R)
            case 7: // SSSS RRRR	 Carrier sustain (S), release (R)
                //parameters for instrument 0
                instdata[0][register & 7] = data;
                break;
            case 0x10:
            case 0x11:
            case 0x12:
            case 0x13:
            case 0x14:
            case 0x15: //frequency registers for ch. 0-5
                final int n = register - 0x10;
                freq[n] = (freq[n] & 0xf00) | data;
                break;
            case 0x20:
            case 0x21:
            case 0x22:
            case 0x23:
            case 0x24:
            case 0x25:  // ???tooof
                        //f: Upper bit of frequency
                        //o: Octave Select 
                        //t: Channel keying on/off (key on = note starts, key off: note decays).
                        //?: bit 5 is sustain, 6 and 7 unused
                final int m = register - 0x20;
                octave[m] = (data >> 1) & 7;
                freq[m] = (freq[m] & 0xff) | ((data & 1) << 8);
                if (Tools.getbit(data, 4) && !key[m]) {
                    //when note is keyed on
                    carenv_state[m] = adsr.CUTOFF;
                    modenv_state[m] = adsr.CUTOFF;
                }
                key[m] = Tools.getbit(data, 4);
                sust[m] = Tools.getbit(data, 5);
                break;
            case 0x30:
            case 0x31:
            case 0x32:
            case 0x33:
            case 0x34:
            case 0x35: //top 4 bits instrument number, bottom 4 volume
                final int j = register - 0x30;
                vol[j] = data & 0xf;
                instrument[j] = (data >> 4) & 0xf;
                break;
            default:
                break;
        }
    }

    /**
     * Performs a given number of machine cycles.
     * 
     * @param cycles        Number of machine cycles.
     */
    @Override
    public final void cycle(int cycles) {
        //chip runs at 3.58 mhz, but this operates at 1.789
        //because i do the modulator and carrier in a single cycle
        //as opposed to doing them alternate cycles like the real one
        //actual chip on the nes runs at 3.6 mhz with a separate cycle
        for (int i = 0; i < cycles; ++i) {
            ch = ++ch % (6 * 6);
            
            if (ch < 6) {
                wave[ch] += (1 / (256. * 2.)) * (freq[ch] << (octave[ch]));
                //Tuned this with audacity so it's definitely ok this time.
                final int[] inst = instdata[instrument[ch]];
                
                //envelopes
                for (int fi = 0; fi < 6; ++fi) {
                    setenvelope(inst, modenv_state, modenv_vol, ch, false);
                    setenvelope(inst, carenv_state, carenv_vol, ch, true);
                }
                
                int keyscale = keyscaletbl[freq[ch] >> 5] - 512 * (7 - octave[ch]);
                
                if (keyscale < 0) {
                    keyscale = 0;
                }
                
                int modks = inst[2] >> 6;
                modks = (modks == 0) ? 0 : (keyscale >> (3 - modks));
                int carks = (inst[3] >> 6);
                carks = (carks == 0) ? 0 : (keyscale >> (3 - carks));
                final int fb = (~inst[3] & 7);
                
                //now the operator cells
                //invaluable info: http://gendev.spritesmind.net/forum/viewtopic.php?t=386
                int mod_f = (int) ((wave[ch]
                        + ((mod[ch] + oldmodout[ch]) >> (6 + fb)))
                        * multbl[inst[0] & 0xf]//modulator base freq and multiplier
                        + (Tools.getbit(inst[0], 6) ? vib[fmctr] * (1 << octave[ch]) : 0))//modulator vibrato
                        ;

                mod[ch] = operator(mod_f,
                        (int) ((inst[2] & 0x3f) * 32//modulator vol
                        + (((int) modenv_vol[ch]) << 2)
                        + modks //key scaling
                        + (Tools.getbit(inst[0], 7) ? am[amctr] : 0)),
                        Tools.getbit(inst[3], 3))//modulator rectify
                        << 2;
                out[ch] = operator((mod[ch] + oldmodout[ch]) / 2
                        + (int) ((Tools.getbit(inst[1], 6) ? vib[fmctr] * (freq[ch] << octave[ch]) / 512. : 0)//carrier vibrato
                        + wave[ch] * multbl[inst[1] & 0xf]//carrier freq multiplier
                        ),
                        (int) (vol[ch] * 128
                        + (Tools.getbit(inst[1], 7) ? am[amctr] : 0)
                        + carks//key scaling
                        + (((int) carenv_vol[ch]) << 2)//carrier volume
                        ),
                        Tools.getbit(inst[3], 4))//carrier rectify
                        << 3;
                
                wave[ch] %= 1024;
                oldmodout[ch] = mod[ch];
                
                outputSample();
                fmctr = ++fmctr % vib.length;
                amctr = ++amctr % am.length;
            }
        }
    }

    /**
     * Sets an operator waveform.
     * 
     * @param phase     Current phase
     * @param gain      Cuurent gain
     * @param rectify   True: Sine
     *                  False: Half-wave rectified sine (sine values less than 0 are clipped to 0)
     * @return 
     */
    private int operator(final int phase, final int gain, final boolean rectify) {
        return exp((logsin(phase, rectify) + gain));
    }

    /**
     * Computes an exponential function.
     * 
     * @param val       Function paramater
     * @return          Computed value
     */
    private int exp(int val) {
        if (val > (1 << 13) - 1) {
            val = (1 << 13) - 1;
        }

        final int mantissa = exp[(-val & 0xff)];
        final int exponent = (-val) >> 8;
        final int b = ((mantissa + 1024) >> (-exponent)) * s;

        return b;
    }

    /**
     * Returns a value from a logarithm sine lookup table.
     * 
     * @param x             Function paramter
     * @param rectify       True: Sine
     *                      False: Half-wave rectified sine (sine values less than 0 are clipped to 0)
     * @return              Value from a logarithm sine lookup table
     */
    private int logsin(final int x, final boolean rectify) {
        //s stores sign, in actual hw the sign bypasses everything else and
        //goes directly to the dac.
        switch ((x >> 8) & 3) {
            case 0:
                s = 1;
                return logsin[(x & 0xff)];
            case 1:
                s = 1;
                return logsin[255 - (x & 0xff)];
            case 2:
                s = rectify ? 0 : -1;
                return logsin[(x & 0xff)];
            case 3:
            default:
                s = rectify ? 0 : -1;
                return logsin[255 - (x & 0xff)];
        }
    }

    /**
     * Generates an audio sample and passes it through a low-pass filter.
     */
    private void outputSample() {
        int sample = (out[0] + out[1] + out[2] + out[3] + out[4] + out[5]);
        sample += lpaccum;
        lpaccum -= sample >> 2;
    }

    /**
     * Generates an audio sample for use with an audio renderer.
     *
     * @return Audio sample for use with an audio renderer.
     */
    @Override
    public int getOutput() {
        return lpaccum;
    }

    /**
     * Sets an envelope for a given channel.
     * 
     * @param instrument        Current instrument
     * @param state             Current state
     * @param vol               Channel volume
     * @param ch                Channel number
     * @param isCarrier         True:  Generates an envelope with a carrier
     *                          False: Generates an envelope with a modulator
     */
    private void setenvelope(final int[] instrument, final adsr[] state, final double[] vol, final int ch, final boolean isCarrier) {
        //from docs on the OPL3: envelope starts at 511 and counts down to zero (no attenuation)
        switch (state[ch]) {
            default:
            case CUTOFF:
                if (vol[ch] < zerovol) {
                    vol[ch] += 2; //the programmer's manual suggests that sound has to
                    //decay back to zero volume when keyed on, but other references don't say this
                } else {
                    vol[ch] = zerovol;
                    
                    if (key[ch]) {
                        state[ch] = adsr.ATTACK;
                        wave[ch] = 0;
                    }
                }
                break;
            case ATTACK:
                if (vol[ch] > maxvol + 0.01) {
                    vol[ch] -= ((vol[ch] + 17) / 272) * attack_tbl[
                            (instrument[(isCarrier ? 5 : 4)] >> 4) * 4
                            + (Tools.getbit(instrument[(isCarrier ? 1 : 0)], 4) ? octave[ch] << 1 : octave[ch] >> 1)];
                } else {
                    state[ch] = adsr.DECAY;
                }
                
                if (!key[ch]) {
                    state[ch] = adsr.RELEASE;
                }
                break;
            case DECAY:
                if (vol[ch] < ((instrument[(isCarrier ? 7 : 6)] >> 4)) * 32) {
                    vol[ch] += decay_tbl[
                            (instrument[(isCarrier ? 5 : 4)] & 0xf) * 4
                            + (Tools.getbit(instrument[(isCarrier ? 1 : 0)], 4) ? octave[ch] << 1 : octave[ch] >> 1)];
                } else {
                    state[ch] = adsr.RELEASE;
                }
                
                if (!key[ch]) {
                    state[ch] = adsr.RELEASE;
                }
                break;
            case RELEASE:
                //release at std rate if key is off
                if (!key[ch] && vol[ch] < zerovol) {
                    if (sust[ch]) {
                        vol[ch] += 0.001;
                    } else {
                        vol[ch] += .005;
                    }
                } else if (vol[ch] < zerovol) {
                    if (Tools.getbit(instrument[isCarrier ? 1 : 0], 5)) {
                        //sustain on, don't decay until keyed
                        if (!key[ch]) {
                            state[ch] = adsr.SUSTRELEASE;
                        }
                    } else {
                        //decay immediately
                        vol[ch] += decay_tbl[(instrument[(isCarrier ? 7 : 6)] & 0xf) * 4
                                + (Tools.getbit(instrument[(isCarrier ? 1 : 0)], 4) ? octave[ch] << 1 : octave[ch] >> 1)];
                    }

                }
                break;
            case SUSTRELEASE:
                if (vol[ch] < zerovol) {
                    if (sust[ch]) {
                        vol[ch] += 0.0001;
                    } else {
                        vol[ch] += decay_tbl[(instrument[(isCarrier ? 7 : 6)] & 0xf) * 4
                                + (Tools.getbit(instrument[(isCarrier ? 1 : 0)], 4) ? octave[ch] << 1 : octave[ch] >> 1)];
                    }
                }
                break;
        }
        
        if (vol[ch] < maxvol) {
            vol[ch] = maxvol;
        }
        
        if (vol[ch] > zerovol) {
            vol[ch] = zerovol;
        }
    }
}