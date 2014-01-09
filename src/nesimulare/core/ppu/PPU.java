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

import nesimulare.core.NES;
import nesimulare.core.ProcessorBase;
import nesimulare.gui.Tools;
import nesimulare.core.cpu.CPU;
import nesimulare.core.memory.PPUMemory;
import nesimulare.core.ppu.PPUTypes.*;

/**
 *
 * @author Parseus
 */
public class PPU extends ProcessorBase {
    
    NES nes;
    CPU cpu;
    PPUMemory ppuram;
    
    /* Lookup table filled with CHR values reversed */
    private static final int[] reverseCHRLookup =
        {
            0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0, 0x10, 0x90, 0x50, 0xD0, 0x30, 0xB0, 0x70, 0xF0,
            0x08, 0x88, 0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8, 0x18, 0x98, 0x58, 0xD8, 0x38, 0xB8, 0x78, 0xF8,
            0x04, 0x84, 0x44, 0xC4, 0x24, 0xA4, 0x64, 0xE4, 0x14, 0x94, 0x54, 0xD4, 0x34, 0xB4, 0x74, 0xF4,
            0x0C, 0x8C, 0x4C, 0xCC, 0x2C, 0xAC, 0x6C, 0xEC, 0x1C, 0x9C, 0x5C, 0xDC, 0x3C, 0xBC, 0x7C, 0xFC,
            0x02, 0x82, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2, 0x12, 0x92, 0x52, 0xD2, 0x32, 0xB2, 0x72, 0xF2,
            0x0A, 0x8A, 0x4A, 0xCA, 0x2A, 0xAA, 0x6A, 0xEA, 0x1A, 0x9A, 0x5A, 0xDA, 0x3A, 0xBA, 0x7A, 0xFA,
            0x06, 0x86, 0x46, 0xC6, 0x26, 0xA6, 0x66, 0xE6, 0x16, 0x96, 0x56, 0xD6, 0x36, 0xB6, 0x76, 0xF6,
            0x0E, 0x8E, 0x4E, 0xCE, 0x2E, 0xAE, 0x6E, 0xEE, 0x1E, 0x9E, 0x5E, 0xDE, 0x3E, 0xBE, 0x7E, 0xFE,
            0x01, 0x81, 0x41, 0xC1, 0x21, 0xA1, 0x61, 0xE1, 0x11, 0x91, 0x51, 0xD1, 0x31, 0xB1, 0x71, 0xF1,
            0x09, 0x89, 0x49, 0xC9, 0x29, 0xA9, 0x69, 0xE9, 0x19, 0x99, 0x59, 0xD9, 0x39, 0xB9, 0x79, 0xF9,
            0x05, 0x85, 0x45, 0xC5, 0x25, 0xA5, 0x65, 0xE5, 0x15, 0x95, 0x55, 0xD5, 0x35, 0xB5, 0x75, 0xF5,
            0x0D, 0x8D, 0x4D, 0xCD, 0x2D, 0xAD, 0x6D, 0xED, 0x1D, 0x9D, 0x5D, 0xDD, 0x3D, 0xBD, 0x7D, 0xFD,
            0x03, 0x83, 0x43, 0xC3, 0x23, 0xA3, 0x63, 0xE3, 0x13, 0x93, 0x53, 0xD3, 0x33, 0xB3, 0x73, 0xF3,
            0x0B, 0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB, 0x1B, 0x9B, 0x5B, 0xDB, 0x3B, 0xBB, 0x7B, 0xFB,
            0x07, 0x87, 0x47, 0xC7, 0x27, 0xA7, 0x67, 0xE7, 0x17, 0x97, 0x57, 0xD7, 0x37, 0xB7, 0x77, 0xF7,
            0x0F, 0x8F, 0x4F, 0xCF, 0x2F, 0xAF, 0x6F, 0xEF, 0x1F, 0x9F, 0x5F, 0xDF, 0x3F, 0xBF, 0x7F, 0xFF,
        };
    
    /* Palette indexes - currently used only for the stock NES/Famicom */
    private final int[] paletteIndexes =
        {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
            0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
            0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
            0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
            0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F,
            0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F,
        };
    
    private static Fetch fetch = new Fetch();
    private static Fetch spriteFetch = new Fetch();
    private static Scroll scroll = new Scroll();
    private static Sprite[] buffer = new Sprite[8];
    private static Unit background = new Unit(272);
    private static Unit sprites = new Unit(256);
    private int spriteState = 0;
    private boolean oddFrame;
    private boolean toggle;
    //Least significant bits previously written into a PPU register
    private int latch;
    private int chr;
    private int grayScale;
    private int emphasis;
    private int[] colors;
    public int[][] screen;
    private boolean oddSwap;
    private boolean sprite0hit, spriteOverflow;
    private boolean spriteReset;
    
    //NMI and VBlank
    private boolean nmiOutput;
    private boolean nmiRequest;
    private boolean suppressVBlank;
    
    //Timing
    private int startNMI;
    private int endNMI;
    private int endFrame;
    public int hclock = 0;
    public int vclock = 0;
    
    //OAM
    private int oamAddress;
    private int[] oam = new int[256];
    private int oamDMAAddress = 0;
    private int oamData;
    private int oamCount = 0;
    private int oamSlot = 0;
    
    public PPU(nesimulare.core.Region.System system, final NES nes, final CPU cpu, final PPUMemory ppuram) {
        super(system);
        region.singleCycle = system.ppu;
        
        this.cpu = cpu;
        this.ppuram = ppuram;
        this.nes = nes;
    }
    
    @Override
    public final void initialize() {
        hardReset();
    }
    
    @Override
    public void hardReset() {
        setRegion();
        
        oddFrame = (system.serial == 0);
        screen = new int[240][];
        for (int i = 0; i < 240; i++) {
            screen[i] = new int[256];
        }
        
        resetEvaluation();
        
        fetch = new Fetch();
        spriteFetch = new Fetch();
        scroll = new Scroll();
        buffer = new Sprite[8];
        for (int i = 0; i < 8; i++) {
            buffer[i] = new Sprite();
        }
        background = new Unit(272);
        sprites = new Unit(256);
        
        chr = 0;
        grayScale = 0xF3;
        emphasis = 0;
        oddSwap = false;
        sprite0hit = spriteOverflow = false;
        nmiOutput = false;
        nmiRequest = false;
        suppressVBlank = false;
        toggle = true;
        
        hclock = 0;
        vclock = 0;
        
        oamAddress = 0;
        oam = new int[256];
        oamData = 0;
        oamCount = 0;
        oamSlot = 0;
    }
    
    private void setRegion() {
        switch (system.serial) {
            case 0:
                startNMI = 241;
                endNMI = 261;
                endFrame = 262;
                break;
            case 1:
                startNMI = 241;
                endNMI = 311;
                endFrame = 312;
                break;
            case 2:
                startNMI = 291;
                endNMI = 311;
                endFrame = 312;
                break;
            default:
                System.err.println("Error, defaulting to NTSC!");
                startNMI = 241;
                endNMI = 261;
                endFrame = 262;
                break;
        }
    }
    
    private void fetchNametable_0() {
        fetch.address = 0x2000 | (scroll.address & 0xFFF);
        nes.board.addressBus(fetch.address);
    }
    
    private void fetchNametable_1() {
        fetch.nametable = ppuram.read(fetch.address);
    }
    
    private void fetchAttribute_0() {
        fetch.address = 0x23C0 | (scroll.address & 0xC00) | (scroll.address >> 4 & 0x38) | (scroll.address >> 2 & 0x7);
        nes.board.addressBus(fetch.address);
    }
    
    private void fetchAttribute_1() {
        fetch.attribute = (ppuram.read(fetch.address)) >> ((scroll.address >> 4 & 0x04) | (scroll.address & 0x02));
    }
    
    private void fetchBit0_0() {
        fetch.address = background.address | (fetch.nametable << 4) | (scroll.address >> 12 & 0x7);
        nes.board.addressBus(fetch.address);
    }
    
    private void fetchBit0_1() {
        fetch.bit0 = ppuram.read(fetch.address);
    }
    
    private void fetchBit1_0() {
        fetch.address = background.address | (fetch.nametable << 4) | 8 | (scroll.address >> 12 & 0x7);
        nes.board.addressBus(fetch.address);
    }
    
    private void fetchBit1_1() {
        fetch.bit1 = ppuram.read(fetch.address);
    }
    
    private void spriteFetchBit0_0() {
        final int index = hclock >> 3 & 7;
        final int comparator = (vclock - buffer[index].y) ^ (Tools.getbit(buffer[index].attribute, 7) ? 0x0F : 0x00);
        
        if (sprites.rasters == 0x10) {
            spriteFetch.address = (buffer[index].nametable << 0x0C & 0x1000) | (buffer[index].nametable << 0x04 & 0x0FE0) |
                    (comparator << 0x01 & 0x0010) | (comparator & 0x7);
        } else {
            spriteFetch.address = sprites.address | (buffer[index].nametable << 0x4) | (comparator & 0x7);
        }
        
        nes.board.addressBus(fetch.address);
    }
    
    private void spriteFetchBit0_1() {
        spriteFetch.bit0 = ppuram.read(spriteFetch.address);
        
        if (Tools.getbit(buffer[hclock >> 3 & 0x7].attribute, 6)) {
            spriteFetch.bit0 = reverseCHRLookup[spriteFetch.bit0];
        }

    }
    
    private void spriteFetchBit1_0() {
        spriteFetch.address = spriteFetch.address | 0x8;
        nes.board.addressBus(fetch.address);
    }
    
    private void spriteFetchBit1_1() {
        spriteFetch.bit1 = ppuram.read(spriteFetch.address);
        
        if (Tools.getbit(buffer[hclock >> 3 & 0x7].attribute, 6)) {
            spriteFetch.bit1 = reverseCHRLookup[spriteFetch.bit1];
        }
        
        spriteFetch.attribute = buffer[hclock >> 3 & 7].attribute;
    }
    
    public final int read(final int address) {
        switch (address & 7) {
            
            //PPUSTATUS
            case 2:
                if (vclock == startNMI) {
                    if (hclock == 0) {
                        suppressVBlank = true;
                        cpu.interrupt(CPU.InterruptTypes.PPU, false);
                    } else if (hclock < 3) {
                        cpu.interrupt(CPU.InterruptTypes.PPU, false);
                    }
                }
                
                int data = (latch & 0x1F);
                
                if (nmiRequest) {
                    data |= 0x80;
                }
                if (sprite0hit) {
                    data |= 0x40;
                }
                if (spriteOverflow) {
                    data |= 0x20;
                }
                
                nmiRequest = false;
                toggle = true;
                
                return latch = data;
            
            //OAMDATA
            case 4:
                return latch = oam[oamAddress & 0xFF];
                
            //PPUDATA
            case 7:
                int tmp;
                
                if ((scroll.address & 0x3F00) == 0x3F00) {
                    tmp = ppuram.read(scroll.address);
                    chr = ppuram.read(scroll.address & 0x2FFF) & 0xFF;
                } else {
                    tmp = chr;
                    chr = ppuram.read(scroll.address) & 0xFF;
                }
                
                if (isRendering()) {
                    scroll.clockY();
                } else {
                    scroll.address = (scroll.address + scroll.step) & 0x7FFF;
                }
                
                nes.board.addressBus(scroll.address);
                
                return latch = tmp;
            
            default:
                return latch;
        }
    }
    
    public final void write(final int address, final int data) {       
        //OAM DMA
        if (address == 0x4014) {
            oamDMAAddress = (data << 8);
            cpu.RDY(CPU.DMATypes.OAM);
        }
        else {
            switch (address & 7) {

                //PPUCTRL
                case 0:
                    scroll.temp        = (scroll.temp & ~0x0C00) | (data << 10 & 0x0C00);
                    scroll.step        = Tools.getbit(data, 2) ? 0x20 : 0x1;
                    sprites.address    = Tools.getbit(data, 3) ? 0x1000 : 0x0000;
                    background.address = Tools.getbit(data, 4) ? 0x1000 : 0x0000;
                    sprites.rasters    = Tools.getbit(data, 5) ? 0x10 : 0x8;

                    final boolean oldNMI = nmiOutput;
                    nmiOutput = Tools.getbit(data, 7);

                    if (vclock == startNMI && hclock < 3) {
                        cpu.interrupt(CPU.InterruptTypes.PPU, nmiOutput && nmiRequest);
                    }

                    if (!oldNMI && nmiOutput && nmiRequest) {
                        cpu.interrupt(CPU.InterruptTypes.PPU, true);
                    }
                    break;

                //PPUMASK
                case 1:
                    grayScale = Tools.getbit(data, 0) ? 0x30 : 0x3F;
                    emphasis = ((data & 0xE0) << 1) & 0xFF;

                    background.clipped = !Tools.getbit(data, 1);
                    sprites.clipped = !Tools.getbit(data, 2);
                    background.enabled = Tools.getbit(data, 3);
                    sprites.enabled = Tools.getbit(data, 4);
                    break;

                //OAMADDR
                case 3:
                    oamAddress = data;
                    break;

                //OAMDATA
                case 4:  
                    if ((oamAddress & 0x03) == 0x02) {
                        oam[oamAddress++] = (data & 0xE3);
                    } else {
                        oam[oamAddress++] = data;
                    }
                    
                    oamAddress &= 0xFF;
                    break;

                //PPUSCROLL
                case 5:
                    if (toggle) {
                        scroll.temp = (scroll.temp & ~0x001F) | (data >> 3 & 0x001F);
                        scroll.fine = (data & 0x7);
                    } else {
                        scroll.temp = (scroll.temp & ~0x73E0) | (data << 2 & 0x03E0) | (data << 12 & 0x7000);
                    }
                    
                    toggle ^= true;
                    break;

                //PPUADDR
                case 6:
                    if (toggle) {
                        scroll.temp = (scroll.temp & ~0xFF00) | (data << 8 & 0x3F00);
                    } else {
                        scroll.temp = (scroll.temp & ~0x00FF) | (data & 0x00FF);
                        scroll.address = scroll.temp;
                        nes.board.addressBus(scroll.address);
                    }
                    
                    toggle ^= true;
                    break;

                //PPUDATA
                case 7:
                    ppuram.write(scroll.address, data);
                    
                    if (isRendering()) {
                        scroll.clockY();
                    } else {
                        scroll.address = (scroll.address + scroll.step) & 0x7FFF;
                    }
                    
                    nes.board.addressBus(scroll.address);
                    break;

                default:
                    break;
            }
        }

        latch = data;
    }
    
    private void synthesizeBackgroundPixels()  {
        int position = (hclock + 9) % 336;
        
        for (int i = 0; i < 8 && position < 272; i++, position++, fetch.bit0 <<= 1, fetch.bit1 <<= 1) {
            background.pixels[position] = (fetch.attribute << 2 & 12) | (fetch.bit0 >> 7 & 1) | (fetch.bit1 >> 6 & 2);
        }
    }    
    
    private void synthesizeSpritePixels() {
        final int index = hclock >> 3 & 7;
        
        if (buffer[index].x == 255) {
            return;
        }
        
        int position = buffer[index].x;
        final int object0 = buffer[index].zero ? 0x4000 : 0x0000;
        final int infront = Tools.getbit(buffer[index].attribute, 5) ? 0x0000 : 0x8000;
        
        for (int i = 0; i < 8 && position < 256; i++, position++, spriteFetch.bit0 <<= 1, spriteFetch.bit1 <<= 1) {
            if (position > 255) {
                return;
            }
            
            final int pixel = (spriteFetch.attribute << 2 & 12) | (spriteFetch.bit0 >> 7 & 1) | (spriteFetch.bit1 >> 6 & 2) | object0 | infront;
            
            if ((sprites.pixels[position] & 0x3) == 0 && (pixel & 0x3) != 0) {
                sprites.pixels[position] = pixel;
            }
        }
    }
    
    private void evaluateFetch() {
        oamData = oam[oamAddress];
    }
    
    private void evaluateReset() {
        oamData = 0xFF;
    }
    
    private void oamFetch() {
        if (spriteReset) {
            evaluateReset();
        } else {
            evaluateFetch();
        }
    }
    
    private void beginEvaluation() {
        spriteReset = false;
        spriteState = 1;
        oamSlot = 0;
        oamCount = 0;
    }
    
    private void resetEvaluation() {
        spriteReset = true;
        spriteState = 0;
        oamSlot = 0;
        oamAddress = 0;
        oamCount = 0;
        
        sprites.pixels = new int[256];
    }

    private void evaluateSprites() {   
        int comparator;
        
        switch (spriteState) {
            case 0:
                if (hclock <= 64) {
                    switch (hclock >> 1 & 0x3) {
                        case 0:
                            buffer[hclock >> 3].y = 0xFF;
                            break;
                        case 1:
                            buffer[hclock >> 3].nametable = 0xFF;
                            break;
                        case 2:
                            buffer[hclock >> 3].attribute = 0xFF;
                            break;
                        case 3:
                            buffer[hclock >> 3].x = 0xFF;
                            buffer[hclock >> 3].zero = false;
                            break;
                        default:
                            break;
                    }
                }
                break;
                
            case 1:
                if (vclock == endNMI) {
                    return;
                }
                
                oamCount++;
                comparator = (vclock - oamData) & Integer.MAX_VALUE;
                
                if (comparator >= sprites.rasters) {
                    if (oamCount != 64) {
                        oamAddress = (oamCount != 2 ? (oamAddress + 4) : 8) & 0xFF;
                    } else {
                        oamAddress = 0;
                        spriteState = 9;
                    }
                } else {
                    oamAddress = (oamAddress + 1) & 0xFF;
                    spriteState = 2;
                    buffer[oamSlot].y = oamData & 0xFF;
                    buffer[oamSlot].zero = (oamCount == 1);
                }
                break;
                
            case 2:
                oamAddress = (oamAddress + 1) & 0xFF;
                spriteState = 3;
                buffer[oamSlot].nametable = oamData & 0xFF;    
                break;
                
            case 3:
                oamAddress = (oamAddress + 1) & 0xFF;
                spriteState = 4;
                buffer[oamSlot].attribute = oamData & 0xFF;
                break;
                
            case 4:
                buffer[oamSlot].x = oamData & 0xFF;
                oamSlot++;
                
                if (oamCount != 64) {
                    spriteState = (oamSlot == 8) ? 5 : 1;
                    
                    if (oamCount != 2) {
                        oamAddress = (oamAddress + 1) & 0xFF;
                    } else {
                        oamAddress = 8;
                    }
                } else {
                    oamAddress = 0;
                    spriteState = 9;
                }
                break;
                
            case 5:
                if (vclock == endNMI) {
                    return;
                }
                
                oamCount++;
                comparator = (vclock - oamData) & Integer.MAX_VALUE;
                
                if (comparator >= sprites.rasters) {
                    oamAddress = (((oamAddress + 4) & 0xFC) + ((oamAddress + 1) & 0x3)) & 0xFF;
                    
                    if (oamAddress <= 5) {
                        spriteState = 9;
                        oamAddress &= 0xFC;
                    }
                } else {
                    spriteState = 6;
                    oamAddress = (oamAddress + 1) & 0xFF;
                    spriteOverflow = true;
                }
                break;
                
            case 6:
                spriteState = 7;
                oamAddress = (oamAddress + 1) & 0xFF;
                break;
                
            case 7:
                spriteState = 8;
                oamAddress = (oamAddress + 1) & 0xFF;
                break;
                
            case 8:
                spriteState = 9;
                oamAddress++;
                
                if ((oamAddress & 0x3) == 0x3) {
                    oamAddress++;
                }
                
                oamAddress &= 0xFC;
                break;
                
            case 9:
                oamAddress = (oamAddress + 4) & 0xFF;
                break;
                
            default:
                break;
        }
    }
    
    @Override
    public void cycle() {
        nes.board.clockPPUCycle();
        
        if (vclock < 240 || vclock == endNMI) {
            if (isRendering()) {
                if (hclock < 256) {
                    //Background fetches
                    switch (hclock & 7) {
                        case 0:
                            fetchNametable_0();
                            break;
                        
                        case 1:
                            fetchNametable_1();
                            break;
                        
                        case 2:
                            fetchAttribute_0();
                            break;
                        
                        case 3:
                            fetchAttribute_1();
                            
                            if (hclock == 251) {
                                scroll.clockY();
                            } else {
                                scroll.clockX();
                            }
                            break;
                            
                        case 4:
                            fetchBit0_0();
                            break;
                         
                        case 5:
                            fetchBit0_1();
                            break;
                            
                        case 6:
                            fetchBit1_0();
                            break;
                            
                        case 7:
                            fetchBit1_1();
                            synthesizeBackgroundPixels();
                            break;
                            
                        default:
                            break;
                    }
                    
                    if (Tools.getbit(hclock, 0)) {
                        evaluateSprites();
                    } else {
                        oamFetch();
                    }
                    
                    if (vclock < 240) {
                        renderPixel();
                    }
                    
                    if (hclock == 63) {
                        beginEvaluation();
                    }
                    
                    if (hclock == 255) {
                        resetEvaluation();
                    }
                } else if (hclock < 320) {
                    if (hclock == 256) {
                        scroll.resetX();
                    }
                    
                    if (hclock == 304 && vclock == endNMI) {
                        scroll.resetY();
                    }
                    
                    //Sprite fetches
                    switch (hclock & 7) {
                        case 0:
                            fetchNametable_0();
                            break;
                        
                        case 1:
                            fetchNametable_1();
                            break;
                        
                        case 2:
                            fetchAttribute_0();
                            break;
                        
                        case 3:
                            fetchAttribute_1();
                            break;
                            
                        case 4:
                            spriteFetchBit0_0();
                            break;
                         
                        case 5:
                            spriteFetchBit0_1();
                            break;
                            
                        case 6:
                            spriteFetchBit1_0();
                            break;
                            
                        case 7:
                            spriteFetchBit1_1();
                            synthesizeSpritePixels();
                            break;
                            
                        default:
                            break;
                    }
                } else if (hclock < 336) {
                    //Background fetches
                    switch (hclock & 7) {
                        case 0:
                            fetchNametable_0();
                            break;
                        
                        case 1:
                            fetchNametable_1();
                            break;
                        
                        case 2:
                            fetchAttribute_0();
                            break;
                        
                        case 3:
                            fetchAttribute_1();
                            scroll.clockX();
                            break;
                            
                        case 4:
                            fetchBit0_0();
                            break;
                         
                        case 5:
                            fetchBit0_1();
                            break;
                            
                        case 6:
                            fetchBit1_0();
                            break;
                            
                        case 7:
                            fetchBit1_1();
                            synthesizeBackgroundPixels();
                            break;
                            
                        default:
                            break;
                    }
                } else if (hclock < 340) {
                    //Dummy fetches
                    switch (hclock & 1) {
                        case 0:
                            fetchNametable_0();
                            break;
                            
                        case 1:
                            fetchNametable_1();
                            break;
                            
                        default:
                            break;
                    }
                }
            } else {
                //Rendering is off, draw color at VRAM address if it's in range 0x3F00 - 0x3FFF
                if (hclock < 255 && vclock < 240) {
                    int pixel;
                    
                    if ((scroll.address & 0x3F00) == 0x3F00) {
                        pixel = colors[paletteIndexes[ppuram.read(scroll.address & 0x3FFF) & (grayScale | emphasis) & 0x7F]];
                    } else {
                        pixel = colors[paletteIndexes[ppuram.read(0x3F00) & (grayScale | emphasis) & 0x7F]];
                    }
                    
                    screen[vclock][hclock] = pixel;
                }
            }
        }
        
        hclock++;
        
        //Odd frame
        if (hclock == 328) {
            if (oddFrame) {
                if (vclock == endNMI) {
                    oddSwap ^= true;
                    
                    if (!oddSwap && background.enabled) {
                        hclock++;
                    }
                }
            }
        }
        
        //VBlank
        if (hclock == 1) {
            //Set VBlank
            if (vclock == startNMI) {
                if (!suppressVBlank) {
                    nmiRequest = true;
                }
                
                suppressVBlank = false;
            }
            
            //Clear VBlank
            if (vclock == endNMI) {
                sprite0hit = false;
                spriteOverflow = false;
                nmiRequest = false;
            }
        }
        
        //End of frame
        if (hclock == 341) {
            hclock = 0;
            vclock++;
            nes.board.scanlineTick();
            
            //Trigger NMI
            if (vclock == startNMI) {
                if (nmiOutput) {
                    cpu.interrupt(CPU.InterruptTypes.PPU, true);
                }
            }
            
            if (vclock == endFrame) {
                vclock = 0;
                nes.finishFrame(nes.gui);
            }
        }
    }
    
    public boolean isRendering() {
        return (background.enabled || sprites.enabled);
    }
    
    public boolean isBackgroundFetching() {
        return (hclock < 256 || hclock >= 320);
    }
    
    public boolean isOAMSize() {
        return (sprites.rasters == 0x10);
    }
    
    public boolean isRenderingScanline() {
        return (vclock < 240);
    }
    
    public void oamTransfer() {
        for (int i = 0; i < 256; i++) {
            final int data = cpu.read(oamDMAAddress);
            cpu.write(0x2004, data);
            
            oamDMAAddress++;
            oamDMAAddress &= 0xFFFF;
        }
    }
    
    private void renderPixel() {
        final int bckgr = ppuram.read(0x3F00);
        screen[vclock][hclock] = colors[paletteIndexes[bckgr & (grayScale | emphasis) & 0x7F]];
        final int backgroundPixel = 0x3F00 | background.getPixel(hclock, scroll.fine);
        final int spritePixel = 0x3F10 | sprites.getPixel(hclock, 0);
        int pixel;
        
        if ((spritePixel & 0x8000) != 0) {
            pixel = spritePixel;
        } else {
            pixel = backgroundPixel;
        }
        
        if ((backgroundPixel & 0x3) == 0) {
            pixel = spritePixel;
            
            if ((pixel & 0x3) != 0) {
                screen[vclock][hclock] = colors[paletteIndexes[ppuram.read(pixel) & (grayScale | emphasis) & 0x7F]];
            }
            
            return;
        }
        
        if ((spritePixel & 0x3) == 0) {
            pixel = backgroundPixel;
            
            if ((pixel & 0x3) != 0) {
                screen[vclock][hclock] = colors[paletteIndexes[ppuram.read(pixel) & (grayScale | emphasis) & 0x7F]];
            }
            
            return;
        }
        
        if ((spritePixel & 0x4000) != 0 && hclock < 255) {
            sprite0hit = true;
        }
        
        if ((pixel & 0x3) != 0) {
            screen[vclock][hclock] = colors[paletteIndexes[ppuram.read(pixel) & (grayScale | emphasis) & 0x7F]];
        }
    }
    
    public void setupPalette(int[] colors) {
        this.colors = colors.clone();
    }
}