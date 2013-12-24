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

package nesimulare.core.audio;

import java.util.ArrayList;
import nesimulare.core.NES;
import nesimulare.core.ProcessorBase;
import nesimulare.core.cpu.CPU;
import nesimulare.gui.Audio;
import nesimulare.gui.AudioInterface;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class APU extends ProcessorBase {
    public AudioInterface ai;
    
    CPU cpu;
    NES nes;
    
    PulseChannel pulse1, pulse2;
    TriangleChannel triangle;
    NoiseChannel noise;
    DMCChannel dmc;
    
    private final int[] noiseFreqNTSC = {
        0x004, 0x008, 0x010, 0x020, 0x040, 0x060, 0x080, 0x0A0,
	0x0CA, 0x0FE, 0x17C, 0x1FC, 0x2FA, 0x3F8, 0x7F2, 0xFE4
    };   
    private final int[] noiseFreqPAL = {
        0x004, 0x007, 0x00E, 0x01E, 0x03C, 0x058, 0x076, 0x094,
	0x0BC, 0x0EC, 0x162, 0x1D8, 0x2C4, 0x3B0, 0x762, 0xEC2
    };
    private final int[] dpcmFreqNTSC = {
        0x1AC, 0x17C, 0x154, 0x140, 0x11E, 0x0FE, 0x0E2, 0x0D6,
        0x0BE, 0x0A0, 0x08E, 0x080, 0x06A, 0x054, 0x048, 0x036  
    };
    private final int[] dpcmFreqPAL = {
        0x18E, 0x162, 0x13C, 0x12A, 0x114, 0x0EC, 0x0D2, 0x0C6,
        0x0B0, 0x094, 0x084, 0x076, 0x062, 0x04E, 0x042, 0x032  
    };
    private final int[][] SequenceMode0 = { 
        new int[] { 7459, 7456, 7458, 7457, 1, 1, 7457 }, // NTSC
        new int[] { 8315, 8314, 8312, 8313, 1, 1, 8313 }, // PAL
        new int[] { 7459, 7456, 7458, 7457, 1, 1, 7457 }, // DENDY
    };
    private final int[][] SequenceMode1 = 
    { 
        new int[] { 1, 7458, 7456, 7458, 14910 } , // NTSC
        new int[] { 1, 8314, 8314, 8312, 16626 } , // PAL
        new int[] { 1, 7458, 7456, 7458, 14910 } , // DENDY
    };
    
    private int apuCycles = 0;
    private int sampleRate = 44100;
    private int currentSequencer;
    private boolean expansionSound = false;
    private boolean clockLength = false;
    private boolean sequencerMode = false;
    private boolean oddCycle = false;
    private boolean frameIRQEnabled;
    private boolean frameIRQFlag;
    private final ArrayList<ExpansionSoundChip> expnSndChip = new ArrayList<>();
    
    public APU(nesimulare.core.Region.System system, final CPU cpu, final NES nes) {
        super(system);
        region.singleCycle = system.apu;
        
        this.cpu = cpu;
        this.nes = nes;
        
        pulse1 = new PulseChannel(system);
        pulse2 = new PulseChannel(system);
        triangle = new TriangleChannel(system);
        noise = new NoiseChannel(system);
        dmc = new DMCChannel(system, this);
        
        setRegion();
    }
    
    public final void initialize() {
        if (ai != null) {
            ai.destroy();
        }
        ai = new Audio(nes, sampleRate);
        
        hardReset();
    }
    
    private void setRegion() {
        switch (system.serial) {
            case 0:
            case 2:
                noise.noiseFrequency = noiseFreqNTSC;
                dmc.dpcmFrequency = dpcmFreqNTSC;
                break;
            case 1:
                noise.noiseFrequency = noiseFreqPAL;
                dmc.dpcmFrequency = dpcmFreqPAL;
                break;
            default:
                System.err.println("Error, defaulting to NTSC!");
                noise.noiseFrequency = noiseFreqNTSC;
                dmc.dpcmFrequency = dpcmFreqNTSC;
                break;
        }
    }
    
    public boolean bufferHasLessThan(int samples) {
        return ai.bufferHasLessThan(samples);
    }
    
    @Override
    public void hardReset() {
        apuCycles = SequenceMode0[system.serial][0] - 10;
        
        setRegion();
        
        clockLength = false;
        oddCycle = false;
        frameIRQFlag = false;
        frameIRQEnabled = true;
        sequencerMode = false;
        currentSequencer = 0;
        
        pulse1.hardReset();
        pulse2.hardReset();
        triangle.hardReset();
        noise.hardReset();
        dmc.hardReset();
        
        if (expansionSound) {
            for (ExpansionSoundChip esc: expnSndChip) {
                esc.hardReset();
            }
        }
    }
    
    @Override
    public void softReset() {
        pulse1.softReset();
        pulse2.softReset();
        triangle.softReset();
        noise.softReset();
        dmc.softReset();
        
        if (expansionSound) {
            for (ExpansionSoundChip esc: expnSndChip) {
                esc.softReset();
            }
        }
        
        apuCycles = SequenceMode0[system.serial][0] - 10;
        
        oddCycle = false;
        frameIRQEnabled = true;
        frameIRQFlag = false;
        sequencerMode = false;
        currentSequencer = 0;
    }
    
    public void addExpansionSoundChip(ExpansionSoundChip chip) {
        expnSndChip.add(chip);
        expansionSound = true;
    }
    
    public int read(final int address) {
        int result;
        
        switch (address) {
            case 0x4015:
                result = ((pulse1.lenctr > 0) ? 0x1 : 0x0) | 
                        ((pulse2.lenctr > 0) ? 0x2 : 0x0) |
                        ((triangle.lenctr > 0) ? 0x4 : 0x0) |
                        ((noise.lenctr > 0) ? 0x8 : 0x0) |
                        ((dmc.lenctr > 0) ? 0x10 : 0x0) |
                        ((frameIRQFlag) ? 0x40 : 0x0) |
                        ((dmc.irqFlag) ? 0x80 : 0x0);
                frameIRQFlag = false;
                cpu.interrupt(CPU.InterruptTypes.APU, false);
                
                return result;
            case 0x4016:
                nes.controllers.read(address);
                result = (CPU.lastRead & 0xC0);
                
                return result;
            case 0x4017:
                nes.controllers.read(address);
                result = (CPU.lastRead & 0xC0);
                
                return result;
            default:
                return 0x40;        //Open bus
        }
    }
    
    public void write(final int address, final int data) {
        switch (address) {
            case 0x4000: case 0x4001: case 0x4002: case 0x4003:
                pulse1.write(address & 3, data);
                break;
            case 0x4004: case 0x4005: case 0x4006: case 0x4007:
                pulse2.write(address & 3, data);
                break;
            case 0x4008: case 0x400A: case 0x400B:
                triangle.write(address & 3, data);
                break;
            case 0x400C: case 0x400E: case 0x400F:
                noise.write(address & 3, data);
                break;
            case 0x4010: case 0x4011: case 0x4012: case 0x4013:
                dmc.write(address & 3, data);
                break;
            case 0x4015:
                pulse1.write(4, data & 0x1);
                pulse2.write(4, data & 0x2);
                triangle.write(4, data & 0x4);
                noise.write(4, data & 0x8);
                dmc.write(4, data & 0x10);
                break;
            case 0x4016:
                nes.getJoypad1().output(Tools.getbit(data, 0));
                nes.getJoypad2().output(Tools.getbit(data, 0));
                break;
            case 0x4017:
                sequencerMode = Tools.getbit(data, 7);
                frameIRQEnabled = Tools.getbit(data, 6);
                
                currentSequencer = 0;
                
                if (!sequencerMode) {
                    apuCycles = SequenceMode0[system.serial][0] - 10;
                } else {
                    apuCycles = SequenceMode1[system.serial][0] - 10;
                }
                
                if (!oddCycle) {
                    apuCycles++;
                } else {
                    apuCycles += 2;
                }
                
                if (!frameIRQEnabled) {
                    frameIRQFlag = false;
                    cpu.interrupt(CPU.InterruptTypes.APU, false);
                }
                break;
            default:
                break;
        }    
    }
    
    private void checkInterrupt() {
        if (frameIRQEnabled) {
            frameIRQFlag = true;
        }
        
        if (frameIRQFlag) {
            cpu.interrupt(CPU.InterruptTypes.APU, true);
        }
    }
    
    private void quarterFrame() {
        pulse1.quarterFrame();
        pulse2.quarterFrame();
        triangle.quarterFrame();
        noise.quarterFrame();
        
        if (expansionSound) {
            for (ExpansionSoundChip esc: expnSndChip) {
                esc.quarterFrame();
            }
        }
    }
    
    private void halfFrame() {
        quarterFrame();
        
        pulse1.halfFrame();
        pulse2.halfFrame();
        triangle.halfFrame();
        noise.halfFrame();
        
        if (expansionSound) {
            for (ExpansionSoundChip esc: expnSndChip) {
                esc.halfFrame();
            }
        }
    }
    
    @Override
    public void cycle(int cycles) {
        clockLength = false;
        oddCycle = !oddCycle;
        apuCycles--;
        
        if (apuCycles == 0) {
            if (!sequencerMode) {
                switch (currentSequencer) {
                    case 0:
                        quarterFrame();
                        break;
                    case 1:
                        halfFrame();
                        clockLength = true;
                        break;
                    case 2:
                        quarterFrame();
                        break;
                    case 3:
                        checkInterrupt();
                        break;
                    case 4:
                        checkInterrupt();
                        clockLength = true;
                        halfFrame();
                        break;
                    case 5:
                        checkInterrupt();
                        break;
                    default:
                        break;
                }
                
                currentSequencer++;
                
                apuCycles += SequenceMode0[system.serial][currentSequencer];
                
                if (currentSequencer == 6) {
                    currentSequencer = 0;
                }
            } else {
                switch (currentSequencer) {
                    case 0:
                        halfFrame();
                        break;
                    case 1:
                        quarterFrame();
                        break;
                    case 2:
                        halfFrame();
                        break;
                    case 3:
                        quarterFrame();
                        break;
                    default:
                        break;
                }
                
                currentSequencer++;
                
                apuCycles = SequenceMode1[system.serial][currentSequencer];
                
                if (currentSequencer == 4) {
                    currentSequencer = 0;
                }
            }
        }
        
        clockChannels();
        super.cycle(cycles);
    }
    
    @Override
    public void cycle() {
        pulse1.cycle(region.singleCycle);
        pulse2.cycle(region.singleCycle);
        triangle.cycle(region.singleCycle);
        noise.cycle(region.singleCycle);
        dmc.cycle(region.singleCycle);
        
        if (expansionSound) {
            for (ExpansionSoundChip esc: expnSndChip) {
                esc.cycle(region.singleCycle);
            }
        }
    }
    
    private void clockChannels() {
        pulse1.clockChannel(clockLength);
        pulse2.clockChannel(clockLength);
        triangle.clockChannel(clockLength);
        noise.clockChannel(clockLength);
        dmc.clockChannel(clockLength);
        
        if (expansionSound) {
            for (ExpansionSoundChip esc: expnSndChip) {
                esc.clockChannel(clockLength);
            }
        }
        
        clockLength = false;
    }
    
    public void dmcFetch() {
        dmc.fetch();
    }
    
    private void addSample() {
        short output = mixSamples();
        
        if (expansionSound) {
            short expn = 0;
            
            for (ExpansionSoundChip esc: expnSndChip) {
                expn += esc.mix();
            }
            
            output = Mixer.mixSamples(pulse1.getOutput(), pulse2.getOutput(), triangle.getOutput(), 
                    noise.getOutput(), dmc.getOutput(), expn);
            
            if (output > 80) {
                output = 80;
            }
            
            if (output < -80) {
                output = -80;
            }
        }
    }
    
    public short mixSamples() {
        return Mixer.mixSamples(pulse1.getOutput(), pulse2.getOutput(), triangle.getOutput(), noise.getOutput(), dmc.getOutput());
    }
    
    public static class Mixer {
        private static short mix_table[][][][][];
        private static int accum;
        private static int prev_x;
        private static int prev_y;
        
        public Mixer() {
            mix_table = new short[16][][][][];
            
            for (int pulse1 = 0; pulse1 < 16; pulse1++) {
                mix_table[pulse1] = new short[16][][][];
                
                for (int pulse2 = 0; pulse2 < 16; pulse2++) {
                    mix_table[pulse1][pulse2] = new short[16][][];
                    
                    for(int triangle = 0; triangle < 16; triangle++) {
                        mix_table[pulse1][pulse2][triangle] = new short[16][];
                        
                        for (int noise = 0; noise < 16; noise++) {
                            mix_table[pulse1][pulse2][triangle][noise] = new short[128];
                            
                            for (int dmc = 0; dmc < 128; dmc++) {
                                final double pulse = (95.88 / (8128.0 / (pulse1 + pulse2) + 100));
                                final double tnd = (159.79 / (1.0 / ((triangle / 8227.0) + (noise / 12241.0) + (dmc / 22638.0)) + 100));
                                
                                mix_table[pulse1][pulse2][triangle][noise][dmc] = (short)((pulse + tnd) * 128);
                            }
                        }
                    }
                }
            }
        }
        
        private static short Filter(int value)
            {
                final int POLE = (int)(32767 * (1.0 - 0.9999));

                accum -= prev_x;
                prev_x = value << 15;
                accum += prev_x - prev_y * POLE;
                prev_y = accum >> 15;

                return (short)prev_y;
            }

            public static short mixSamples(int pulse1, int pulse2, int triangle, int noise, int dmc)
            {
                return mix_table[pulse1][pulse2][triangle][noise][dmc];
            }
            public static short mixSamples(int pulse1, int pulse2, int triangle, int noise, int dmc, short exp)
            {
                return Filter(
                    (mix_table[pulse1][pulse2][triangle][noise][dmc] >> 1) + (exp >> 1));
            }
    }
} 