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

package nesimulare.audio;

import nesimulare.NES;
import nesimulare.Tools;
import nesimulare.cpu.CPU;

/**
 *
 * @author Parseus
 */
public class APU {
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
        new int[] { 8315, 8314, 8312, 8313, 1, 1, 8313 }, // PALB
    };
    private final int[][] SequenceMode1 = 
    { 
        new int[] { 1, 7458, 7456, 7458, 14910 } , // NTSC
        new int[] { 1, 8314, 8314, 8312, 16626 } , // PALB
    };
    
    private int cycles = 0;
    private int currentSequencer;
    private boolean sequencerMode = false;
    private boolean oddCycle = false;
    private boolean frameIRQEnabled;
    private boolean frameIRQFlag;
    public enum Region {
        NTSC, PAL, DENDY;
    }
    Region region = Region.NTSC;
    //private ArrayList<ExpansionSoundChip> expnSndChip = new ArrayList<ExpansionSoundChip>();
    
    public APU(final CPU cpu, final NES nes) {
        this.cpu = cpu;
        this.nes = nes;
        
        pulse1 = new PulseChannel();
        pulse2 = new PulseChannel();
        triangle = new TriangleChannel();
        noise = new NoiseChannel();
        dmc = new DMCChannel(this);
        
        setRegion(region);
    }
    
    private void setRegion(Region region) {
        switch (region) {
            case NTSC:
            case DENDY:
                noise.noiseFrequency = noiseFreqNTSC;
                dmc.dpcmFrequency = dpcmFreqNTSC;
                break;
            case PAL:
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
    
    public void hardReset() {
        switch (region) {
            case NTSC:
            case DENDY:
                cycles = SequenceMode0[0][0] - 10;
                break;
            case PAL:
                cycles = SequenceMode0[1][0] - 10;
                break;
            default:
                System.err.println("Error!");
                cycles = SequenceMode0[0][0] - 10;
                break;
        }
        
        setRegion(region);
        
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
    }
    
    public void softReset() {
        pulse1.softReset();
        pulse2.softReset();
        triangle.softReset();
        noise.softReset();
        dmc.softReset();
        
        switch (region) {
            case NTSC:
            case DENDY:
                cycles = SequenceMode0[0][0] - 10;
                break;
            case PAL:
                cycles = SequenceMode0[1][0] - 10;
                break;
            default:
                System.err.println("Error!");
                cycles = SequenceMode0[0][0] - 10;
                break;
        }
        
        oddCycle = false;
        frameIRQEnabled = true;
        frameIRQFlag = false;
        sequencerMode = false;
        currentSequencer = 0;
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
                cpu.interrupt(CPU.interruptTypes.APU, false);
                
                return result;
            case 0x4016:
                nes.controllers.read(address);
                result = (cpu.lastRead & 0xC0);
                
                return result;
            case 0x4017:
                nes.controllers.read(address);
                result = (cpu.lastRead & 0xC0);
                
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
                pulse1.write(4, data);
                pulse2.write(4, data);
                triangle.write(4, data);
                noise.write(4, data);
                dmc.write(4, data);
                break;
            case 0x4016:
                nes.controllers.joypad1.output(Tools.getbit(data, 0));
                nes.controllers.joypad2.output(Tools.getbit(data, 0));
                break;
            case 0x4017:
                sequencerMode = Tools.getbit(data, 7);
                frameIRQEnabled = Tools.getbit(data, 6);
                
                currentSequencer = 0;
                
                if (!sequencerMode) {
                     switch (region) {
                        case NTSC:
                        case DENDY:
                            cycles = SequenceMode0[0][0] - 10;
                            break;
                        case PAL:
                            cycles = SequenceMode0[1][0] - 10;
                            break;
                        default:
                            System.err.println("Error!");
                            cycles = SequenceMode0[0][0] - 10;
                            break;
                    }
                } else {
                    switch (region) {
                        case NTSC:
                        case DENDY:
                            cycles = SequenceMode1[0][0] - 10;
                            break;
                        case PAL:
                            cycles = SequenceMode1[1][0] - 10;
                            break;
                        default:
                            System.err.println("Error!");
                            cycles = SequenceMode1[0][0] - 10;
                            break;
                    }
                }
                
                if (!oddCycle) {
                    cycles++;
                } else {
                    cycles += 2;
                }
                
                if (!frameIRQEnabled) {
                    frameIRQFlag = false;
                    cpu.interrupt(CPU.interruptTypes.APU, false);
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
            cpu.interrupt(CPU.interruptTypes.APU, true);
        }
    }
    
    private void quarterFrame() {
        pulse1.quarterFrame();
        pulse2.quarterFrame();
        triangle.quarterFrame();
        noise.quarterFrame();
    }
    
    private void halfFrame() {
        quarterFrame();
        
        pulse1.halfFrame();
        pulse2.halfFrame();
        triangle.halfFrame();
        noise.halfFrame();
    }
    
    public void cycle() {
        oddCycle = !oddCycle;
        
        if (--cycles == 0) {
            if (!sequencerMode) {
                switch (currentSequencer) {
                    case 0:
                        quarterFrame();
                        break;
                    case 1:
                        halfFrame();
                        break;
                    case 2:
                        quarterFrame();
                        break;
                    case 3:
                        checkInterrupt();
                        break;
                    case 4:
                        checkInterrupt();
                        halfFrame();
                        break;
                    case 5:
                        checkInterrupt();
                        break;
                    default:
                        break;
                }
                
                currentSequencer++;
                
                switch (region) {
                    case NTSC:
                    case DENDY:
                        cycles = SequenceMode0[0][currentSequencer];
                        break;
                    case PAL:
                        cycles = SequenceMode0[1][currentSequencer];
                        break;
                    default:
                        System.err.println("Error, defaulting to NTSC!");
                        cycles = SequenceMode0[0][currentSequencer];
                        break;
                }
                
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
                
                switch (region) {
                    case NTSC:
                    case DENDY:
                        cycles = SequenceMode1[0][currentSequencer];
                        break;
                    case PAL:
                        cycles = SequenceMode1[1][currentSequencer];
                        break;
                    default:
                        System.err.println("Error, defaulting to NTSC!");
                        cycles = SequenceMode1[0][currentSequencer];
                        break;
                }
                
                if (currentSequencer == 4) {
                    currentSequencer = 0;
                }
            }
            
            pulse1.cycle();
            pulse2.cycle();
            triangle.cycle();
            noise.cycle();
            dmc.cycle();
        }
    }
    
    public void dmcFetch() {
        dmc.fetch();
    }
}