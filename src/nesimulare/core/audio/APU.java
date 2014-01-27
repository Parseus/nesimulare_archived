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

import java.util.ArrayList;
import nesimulare.core.NES;
import nesimulare.core.ProcessorBase;
import nesimulare.core.cpu.CPU;
import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class APU extends ProcessorBase {
    CPU cpu;
    NES nes;

    PulseChannel pulse1, pulse2;
    TriangleChannel triangle;
    NoiseChannel noise;
    DMCChannel dmc;

    private final int[] noiseFreqNTSC = {
        0x002, 0x004, 0x008, 0x010, 0x020, 0x030, 0x040, 0x050,
        0x065, 0x07F, 0x0BE, 0x0FE, 0x17D, 0x1FC, 0x3F9, 0x7F2
    };
    private final int[] noiseFreqPAL = {
        0x002, 0x003, 0x007, 0x00F, 0x01E, 0x02C, 0x03B, 0x04A,
        0x05E, 0x076, 0x0B1, 0x0EC, 0x162, 0x1D8, 0x3B1, 0x761
    };
    private final int[] dpcmFreqNTSC = {
        0xD6, 0xBE, 0xAA, 0xA0, 0x8F, 0x7F, 0x71, 0x6B,
        0x5F, 0x50, 0x47, 0x40, 0x35, 0x2A, 0x24, 0x1B
    };
    private final int[] dpcmFreqPAL = {
        0xC7, 0xB1, 0x9E, 0x95, 0x8A, 0x76, 0x69, 0x63,
        0x58, 0x4A, 0x42, 0x3B, 0x31, 0x27, 0x21, 0x19,};
    private final int[][] SequenceMode0 = {
        new int[]{7459, 7456, 7458, 7457, 1, 1, 7457}, // NTSC
        new int[]{8315, 8314, 8312, 8313, 1, 1, 8313}, // PAL
        new int[]{7459, 7456, 7458, 7457, 1, 1, 7457}, // DENDY
    };
    private final int[][] SequenceMode1
            = {
                new int[]{1, 7458, 7456, 7458, 14910}, // NTSC
                new int[]{1, 8314, 8314, 8312, 16626}, // PAL
                new int[]{1, 7458, 7456, 7458, 14910}, // DENDY
            };

    private int apuCycles = 0;
    private int currentSequencer;
    private boolean expansionSound = false;
    private boolean clockLength = false;
    private boolean sequencerMode = false;
    private boolean oddCycle = false;
    private boolean frameIRQEnabled;
    private boolean frameIRQFlag;
    private final ArrayList<ExpansionSoundChip> expnSndChip = new ArrayList<>();

    private int sampleCycles = 0;
    private int samplePeriod = 3125;
    private int sampleSingle = 77;
    private int lpaccum = 0, dckiller = 0;
    private final int[] tnd_lookup, square_lookup;

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

        square_lookup = new int[31];
        for (int i = 0; i < 31; ++i) {
            square_lookup[i] = (int) ((95.52 / (8128.0 / i + 100)) * 49151);
        }
        tnd_lookup = new int[203];
        for (int i = 0; i < 203; ++i) {
            tnd_lookup[i] = (int) ((163.67 / (24329.0 / i + 100)) * 49151);
        }
    }

    @Override
    public final void initialize() {
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

    @Override
    public void hardReset() {
        setRegion();

        apuCycles = SequenceMode0[system.serial][0] - 10;

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
        
        lpaccum = 0; 
        dckiller = 0;

        if (expansionSound) {
            for (ExpansionSoundChip esc : expnSndChip) {
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
            for (ExpansionSoundChip esc : expnSndChip) {
                esc.softReset();
            }
        }

        apuCycles = SequenceMode0[system.serial][0] - 10;

        oddCycle = false;
        frameIRQEnabled = true;
        frameIRQFlag = false;
        cpu.interrupt(CPU.InterruptTypes.APU, false);
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
                result = (pulse1.getStatus() ? 0x1 : 0x0)
                        | (pulse2.getStatus() ? 0x2 : 0x0)
                        | (triangle.getStatus() ? 0x4 : 0x0)
                        | (noise.getStatus() ? 0x8 : 0x0)
                        | (dmc.getStatus() ? 0x10 : 0x0)
                        | ((frameIRQFlag) ? 0x40 : 0x0)
                        | ((dmc.irqFlag) ? 0x80 : 0x0);
                frameIRQFlag = false;
                cpu.interrupt(CPU.InterruptTypes.APU, false);
                break;

            case 0x4016:
                result = (CPU.lastRead & 0xC0);
                result |= nes.controllers.read(address);
                break;

            case 0x4017:
                result = (CPU.lastRead & 0xC0);
                result |= nes.controllers.read(address);
                break;

            default:
                result = (address >> 8 & 0xe0);
                break;
        }

        return result;
    }

    public void write(final int address, final int data) {
        switch (address) {
            case 0x4000:
            case 0x4001:
            case 0x4002:
            case 0x4003:
                pulse1.write(address & 3, data);
                break;
            case 0x4004:
            case 0x4005:
            case 0x4006:
            case 0x4007:
                pulse2.write(address & 3, data);
                break;
            case 0x4008:
            case 0x400A:
            case 0x400B:
                triangle.write(address & 3, data);
                break;
            case 0x400C:
            case 0x400E:
            case 0x400F:
                noise.write(address & 3, data);
                break;
            case 0x4010:
            case 0x4011:
            case 0x4012:
            case 0x4013:
                dmc.write(address & 3, data);
                break;
            case 0x4015:
                pulse1.setStatus(Tools.getbit(data, 0));
                pulse2.setStatus(Tools.getbit(data, 1));
                triangle.setStatus(Tools.getbit(data, 2));
                noise.setStatus(Tools.getbit(data, 3));
                dmc.setStatus(Tools.getbit(data, 4));
                break;
            case 0x4016:
                nes.controllers.write(data);
                break;
            case 0x4017:
                sequencerMode = Tools.getbit(data, 7);
                frameIRQEnabled = !Tools.getbit(data, 6);

                currentSequencer = 0;

                if (!sequencerMode) {
                    apuCycles = SequenceMode0[system.serial][0];
                } else {
                    apuCycles = SequenceMode1[system.serial][0];
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
            for (ExpansionSoundChip esc : expnSndChip) {
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
            for (ExpansionSoundChip esc : expnSndChip) {
                esc.halfFrame();
            }
        }
    }

    @Override
    public void cycle(int cycles) {
        clockLength = false;
        oddCycle ^= true;
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
                        clockLength = true;
                        break;
                    case 1:
                        quarterFrame();
                        break;
                    case 2:
                        halfFrame();
                        clockLength = true;
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
        updatePlayback();
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
            for (ExpansionSoundChip esc : expnSndChip) {
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
            for (ExpansionSoundChip esc : expnSndChip) {
                esc.clockChannel(clockLength);
            }
        }

        clockLength = false;
    }

    public void dmcFetch() {
        dmc.fetch();
    }

    public void setupPlayback() {
        samplePeriod = system.master;
        sampleSingle = system.cpu * nes.sampleRate;
        
        int sample[] = { samplePeriod, sampleSingle };
        sample = Tools.reduce(sample);
        
        samplePeriod = sample[0];
        sampleSingle = sample[1];
    }
    
    private void updatePlayback() {
        sampleCycles += sampleSingle;
        
        if (sampleCycles >= samplePeriod) {
            sampleCycles -= samplePeriod;
            nes.audio.outputSample(lowpass_filter(highpass_filter(outputSample())));
        }
    }
    
    private int outputSample() {
        int vol;
        vol = square_lookup[pulse1.getOutput() + pulse2.getOutput()];
        vol += tnd_lookup[3 * triangle.getOutput() + 2 * noise.getOutput() + dmc.getOutput()];
        
        if (expansionSound) {
            vol *= 0.8;
            
            for (ExpansionSoundChip c : expnSndChip) {
                vol += c.mix();
            }
        }

        return vol;
    }

    private int highpass_filter(int sample) {
        //for killing the dc in the signal
        sample += dckiller;
        dckiller -= sample >> 8;//the actual high pass part
        dckiller += (sample > 0 ? -1 : 1);//guarantees the signal decays to exactly zero
        
        return sample;
    }

    private int lowpass_filter(int sample) {
        sample += lpaccum;
        lpaccum -= sample * 0.9;
        
        return lpaccum;
    }

    public void finishFrame() {
        nes.audio.flushFrame(nes.frameLimiter.enabled);
    }
}