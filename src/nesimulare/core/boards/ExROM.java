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
package nesimulare.core.boards;

import nesimulare.core.audio.MMC5SoundChip;
import nesimulare.core.cpu.CPU;
import nesimulare.gui.Tools;

/**
 * Emulates a ExROM (MMC5) boardset (mapper 5).
 *
 * @author Parseus
 */
public class ExROM extends Board {
    /* MMC5 external sound */
    private MMC5SoundChip soundChip;

    /* SRAM */
    //TODO: Implement PRG-RAM bankswitching
    private int sramPage = 0;
    private int sramProtectionA = 0;
    private int sramProtectionB = 0;
    private boolean sramWritable = true;

    /* CHR, PRG and ExRAM */
    private int exramMode = 0;
    private int chrSelectMode = 3;
    private int chrBackgroundPage[] = new int[8];
    private int exCHRBank[] = new int[8];
    private int chrSwitchHigh = 0;
    private int prgSelectMode = 3;

    /* IRQ */
    private int irqScanline = 0;
    private int irqLine = 0;
    private int irqClear = 0;
    private int irqStatus = 0;
    private boolean irqEnabled = false;

    /* Multiplier */
    private int multiplierA = 0;
    private int multiplierB = 0;

    /* Split-screen */
    //TODO: Implement split-screen
    private int splitScroll = 0;
    private int splitControl = 0;
    private int splitPage = 0;
    private int lastAccessedVRAM = 0;

    /**
     * Constructor for this class.
     *
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM False: PCB contains CHR-ROM
     */
    public ExROM(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }

    /**
     * Initializes the board.
     */
    @Override
    public void initialize() {
        super.initialize();

        soundChip = new MMC5SoundChip(nes.region);
        nes.apu.addExpansionSoundChip(soundChip);
    }

    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();

        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0x8000);
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xA000);
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xC000);
        super.switch8kPRGbank((prg.length - 0x2000) >> 13, 0xE000);
        chrBackgroundPage = new int[8];
        exCHRBank = new int[8];
        prgSelectMode = 3;
        chrSelectMode = 3;
        chrSwitchHigh = 0;
        switch8kBGCHRbank(0);
        exramMode = 0;

        sram = new int[0x10000];
        sramPage = 0;
        sramWritable = true;
        sramProtectionA = 0;
        sramProtectionB = 0;

        irqStatus = 0;
        irqScanline = 0;
        irqLine = 0;
        irqClear = 0;
        irqEnabled = false;

        multiplierA = 0;
        multiplierB = 0;

        splitScroll = 0;
        splitControl = 0;
        splitPage = 0;
        lastAccessedVRAM = 0;
    }

    /**
     * Reads data from a given address within the range $4020-$5FFF.
     *
     * @param address Address to read data from
     * @return Read data
     */
    @Override
    public int readEXP(int address) {
        int tmp = 0;

        if (address < 0x5C00) {
            switch (address) {
                /**
                 * $5015: Status
                 */
                case 0x5015:
                    return soundChip.getStatus();

                /**
                 * $5204: IRQ Status 7 bit 0 ---- ---- SVxx xxxx || |+-------- "In Frame" signal +--------- IRQ Pending flag
                 */
                case 0x5204:
                    tmp = irqStatus;
                    irqStatus &= ~0x80;
                    nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                    break;

                /**
                 * $5205: Multiplier (low byte)
                 */
                case 0x5205:
                    return (multiplierA * multiplierB) & 0xFF;

                /**
                 * $5206: Multiplier (high byte)
                 */
                case 0x5206:
                    return ((multiplierA * multiplierB) >> 8) & 0xFF;
                default:
                    break;
            }
        } else if (address >= 0x5C00 && address <= 0x5FFF) {
            //Expansion RAM
            if (exramMode >= 2) {
                return nes.ppuram.nmt[2][address & 0x3FF];
            } else {
                return (address >> 8 & 0xe0);
            }
        }

        return tmp;
    }

    /**
     * Writes data to a given address within the range $4020-$5FFF.
     *
     * @param address Address to write data to
     * @param data Written data
     */
    @Override
    public void writeEXP(int address, int data) {
        switch (address) {
            /**
             * $5000-$5007, $5010-$5011, $5015: Sound control
             * @see MMC5SoundChip
             */
            case 0x5000:
            case 0x5001:
            case 0x5002:
            case 0x5003:
            case 0x5004:
            case 0x5005:
            case 0x5006:
            case 0x5007:
            case 0x5010:
            case 0x5011:
            case 0x5015:
                soundChip.write(address, data);
                break;

            /**
             * $5100: PRG mode
             * 7  bit  0
             * ---- ----
             * xxxx xxPP
             *        ||
             *        ++- Select PRG banking mode
             */
            case 0x5100:
                prgSelectMode = (data & 0x3);
                break;
                
            /**
             * $5101: CHR mode
             * 7  bit  0
             * ---- ----
             * xxxx xxPP
             *        ||
             *        ++- Select PRG banking mode
             */
            case 0x5101:
                chrSelectMode = (data & 0x3);
                break;
            
            /**
             * $5102: PRG RAM Protect 1
             * 7  bit  0
             * ---- ----
             * xxxx xxWW
             *        ||
             *        ++- RAM protect 1
             */
            case 0x5102:
                sramProtectionA = (data & 0x3);
                sramWritable = (sramProtectionA == 0x2 && sramProtectionB == 0x1);
                break;
                
            /**
             * $5103: PRG RAM Protect 2
             * 7  bit  0
             * ---- ----
             * xxxx xxWW
             *        ||
             *        ++- RAM protect 1
             */
            case 0x5103:
                sramProtectionB = (data & 0x3);
                sramWritable = (sramProtectionA == 0x2 && sramProtectionB == 0x1);
                break;
                
            /**
             * $5104: Extended RAM mode
             * 7  bit  0
             * ---- ----
             * xxxx xxXX
             *        ||
             *        ++- Specify extended RAM usage
             */
            case 0x5104:
                exramMode = (data & 0x3);
                break;
                
            /**
             * $5105: Nametable mapping
             * 7  bit  0
             * ---- ----
             * DDCC BBAA
             * |||| ||||
             * |||| ||++- Select nametable at PPU $2000-$23FF
             * |||| ++--- Select nametable at PPU $2400-$27FF
             * ||++------ Select nametable at PPU $2800-$2BFF
             * ++-------- Select nametable at PPU $2C00-$2FFF
             */
            case 0x5105:
                nes.ppuram.setNametable(data);
                break;
                
            /**
             * $5106: Fill-mode tile
             */    
            case 0x5106:
                for (int i = 0; i < 0x3C0; i++) {
                    nes.ppuram.nmt[3][i] = data;
                }
                break;
                
            /**
             * $5107: Fill-mode color
             * 7  bit  0
             * ---- ----
             * xxxx xxAA
             *        ||
             *        ++- Specify attribute bits to use for fill-mode nametable
             */
            case 0x5107:
                for (int i = 0x3C0; i < 0x400; i++) {
                    int value = (2 << (data & 0x3)) | (data & 0x3);
                    value |= ((value & 0xF) << 4);
                    nes.ppuram.nmt[3][i] = data;
                }
                break;

            /**
             * $5113: PRG RAM bank
             * 7  bit  0
             * ---- ----
             * xxxx xCBB
             *       |||
             *       |++- Select 8KB PRG RAM bank at $6000-$7FFF
             *       +--- Select PRG RAM chip
             */
            case 0x5113:
                sramPage = (data & 0x7);
                break;
                
            /**
             * $5114: PRG bank 0
             * 7  bit  0
             * ---- ----
             * RBBB BBBB
             * |||| ||||
             * |+++-++++- Bank number
             * +--------- RAM/ROM toggle (0: RAM; 1: ROM)
             */
            case 0x5114:
                if (prgSelectMode == 3) {
                    if (Tools.getbit(data, 7)) {
                        super.switch8kPRGbank(data & 0x7F, 0x8000);
                    } else {
                        //TODO
                    }
                }
                break;
                
            /**
             * $5115: PRG bank 1
             * 7  bit  0
             * ---- ----
             * RBBB BBBB
             * |||| ||||
             * |+++-++++- Bank number
             * +--------- RAM/ROM toggle (0: RAM; 1: ROM)
             */
            case 0x5115:
                if (prgSelectMode == 1 || prgSelectMode == 2) {
                    if (Tools.getbit(data, 7)) {
                        super.switch8kPRGbank((data & 0x7E) >> 1, 0x8000);
                    } else {
                        //TODO
                    }
                } else if (prgSelectMode == 3) {
                    if (Tools.getbit(data, 7)) {
                        super.switch8kPRGbank(data & 0x7F, 0xA000);
                    } else {
                        //TODO
                    }
                }
                break;
             
            /**
             * $5116: PRG bank 2
             * 7  bit  0
             * ---- ----
             * RBBB BBBB
             * |||| ||||
             * |+++-++++- Bank number
             * +--------- RAM/ROM toggle (0: RAM; 1: ROM)
             */
            case 0x5116:
                if (prgSelectMode == 2 || prgSelectMode == 3) {
                    if (Tools.getbit(data, 7)) {
                        super.switch8kPRGbank(data & 0x7F, 0xC000);
                    } else {
                        //TODO
                    }
                }
                break;
                
            /**
             * $5117: PRG bank 3
             * 7  bit  0
             * ---- ----
             * xBBB BBBB
             *  ||| ||||
             *  +++-++++- Bank number
             */
            case 0x5117:
                switch (prgSelectMode) {
                    case 0:
                        super.switch32kPRGbank((data & 0x7C) >> 2);
                        break;
                    case 1:
                        super.switch16kPRGbank((data & 0x7E) >> 1, 0xC000);
                        break;
                    case 2:
                    case 3:
                        super.switch8kPRGbank(data, 0xE000);
                        break;
                    default:
                        break;
                }
                break;

            /**
             * $5120: CHR bank 1
             */
            case 0x5120:
                if (chrSelectMode == 3) {
                    super.switch1kCHRbank(data, 0x0000);
                }
                break;
                
            /**
             * $5121: CHR bank 1
             */
            case 0x5121:
                if (chrSelectMode == 2) {
                    super.switch2kCHRbank(data, 0x0000);
                } else if (chrSelectMode == 3) {
                    super.switch1kCHRbank(data, 0x0400);
                }
                break;
                
            /**
             * $5122: CHR bank 2
             */
            case 0x5122:
                if (chrSelectMode == 3) {
                    super.switch1kCHRbank(data, 0x0800);
                }
                break;
             
            /**
             * $5123: CHR bank 3
             */
            case 0x5123:
                if (chrSelectMode == 1) {
                    super.switch4kCHRbank(data, 0x0000);
                } else if (chrSelectMode == 2) {
                    super.switch2kCHRbank(data, 0x0800);
                } else if (chrSelectMode == 3) {
                    super.switch1kCHRbank(data, 0x0C00);
                }
                break;
                
            /**
             * $5124: CHR bank 4
             */
            case 0x5124:
                if (chrSelectMode == 3) {
                    super.switch1kCHRbank(data, 0x1000);
                }
                break;
                
            /**
             * $5125: CHR bank 5
             */
            case 0x5125:
                if (chrSelectMode == 2) {
                    super.switch2kCHRbank(data, 0x1000);
                } else if (chrSelectMode == 3) {
                    super.switch1kCHRbank(data, 0x1400);
                }
                break;
                
            /**
             * $5126: CHR bank 6
             */
            case 0x5126:
                if (chrSelectMode == 3) {
                    super.switch1kCHRbank(data, 0x1800);
                }
                break;
             
            /**
             * $5127: CHR bank 7
             */
            case 0x5127:
                if (chrSelectMode == 0) {
                    super.switch8kCHRbank(data);
                } else if (chrSelectMode == 1) {
                    super.switch4kCHRbank(data, 0x1000);
                } else if (chrSelectMode == 2) {
                    super.switch2kCHRbank(data, 0x1800);
                } else if (chrSelectMode == 3) {
                    super.switch1kCHRbank(data, 0x1C00);
                }
                break;

            /**
             * $5128: CHR background bank 1
             */
            case 0x5128:
                if (chrSelectMode == 3) {
                    switch1kBGCHRbank(data, 0x0000);
                    switch1kBGCHRbank(data, 0x1000);
                }
                break;
                
            /**
             * $5129: CHR background bank 2
             */
            case 0x5129:
                if (chrSelectMode == 2) {
                    switch2kBGCHRbank(data, 0x0000);
                    switch2kBGCHRbank(data, 0x1000);
                } else if (chrSelectMode == 3) {
                    switch1kBGCHRbank(data, 0x0400);
                    switch1kBGCHRbank(data, 0x1400);
                }
                break;
                
            /**
             * $512A: CHR background bank 3
             */
            case 0x512A:
                if (chrSelectMode == 3) {
                    switch1kBGCHRbank(data, 0x0800);
                    switch1kBGCHRbank(data, 0x1800);
                }
                break;
                
            /**
             * $512B: CHR background bank 4
             */
            case 0x512B:
                if (chrSelectMode == 0) {
                    switch8kBGCHRbank(data);
                } else if (chrSelectMode == 1) {
                    switch4kBGCHRbank(data, 0x0000);
                    switch4kBGCHRbank(data, 0x1000);
                } else if (chrSelectMode == 2) {
                    switch2kBGCHRbank(data, 0x0800);
                    switch2kBGCHRbank(data, 0x1800);
                } else if (chrSelectMode == 3) {
                    switch1kBGCHRbank(data, 0x0C00);
                    switch1kBGCHRbank(data, 0x1C00);
                }
                break;
                
            /**
             * $5130: Upper CHR Bank bits
             * 7  bit  0
             * ---- ----
             * xxxx xxBB
             *        ||
             *        ++- Upper bits for subsequent CHR bank writes
             */
            case 0x5130:
                chrSwitchHigh = data & 0x3; 
                break;

            /**
             * $5200: Vertical Split Mode
             * 7  bit  0
             * ---- ----
             * ESxW WWWW
             * || | ||||
             * || +-++++- Specify vertical split start/stop tile
             * |+-------- Specify vertical split screen side (0:left; 1:right)
             * +--------- Enable vertical split mode
             */
            case 0x5200:
                splitControl = data;
                break;
                
            /**
             * $5201: Vertical Split Scroll
             */
            case 0x5201:
                splitScroll = data;
                break;
                
            /**
             * $5202: Vertical Split Bank
             */
            case 0x5202:
                splitPage = data;
                break;

            /**
             * $5203: IRQ Counter
             */
            case 0x5203:
                irqLine = data;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
                
            /**
             * $5204: IRQ Status
             * 7  bit  0
             * ---- ----
             * Exxx xxxx
             * |
             * +--------- IRQ Enable flag (1=IRQs enabled)
             */
            case 0x5204:
                irqEnabled = Tools.getbit(data, 7);
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;

            /**
             * $5205: Multiplier (low byte)
             */
            case 0x5205:
                multiplierA = data;
                break;
                
            /**
             * $5206: Multiplier (high byte)
             */
            case 0x5206:
                multiplierB = data;
                break;

            /**
             * Expansion RAM
             */
            default:
                if (address >= 0x5C00 && address <= 0x5FFF) {
                    if (exramMode == 2) {
                        nes.ppuram.nmt[2][(address & 0x3FF)] = data;
                    } else if (exramMode != 3) {
                        /**
                         * 7  bit  0
                         * ---- ----
                         * AACC CCCC
                         * |||| ||||
                         * ||++-++++- Select 4 KB CHR bank to use with specified tile
                         * ++-------- Select palette to use with specified tile
                         */
                        
                        if (nes.ppu.isRendering()) {
                            nes.ppuram.nmt[2][(address & 0x3FF)] = data;
                        } else {
                            nes.ppuram.nmt[2][(address & 0x3FF)] = 0;
                        }
                    }
                }
                break;
        }
    }

    /**
     * Reads data from a given address within the range $6000-$7FFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    @Override
    public int readSRAM(int address) {
        return sram[(address - 0x6000) | sramPage];
    }

    /**
     * Writes data to a given address within the range $6000-$7FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writeSRAM(int address, int data) {
        if (sramWritable) {
            sram[(address - 0x6000) | sramPage] = data;
        }
    }

    /**
     * Reads PPU data from a given address within the range $0000-$1FFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    @Override
    public int readCHR(int address) {
        if (exramMode == 1) {
            if (nes.ppu.isBackgroundFetching()) {
                final int tileNumber = nes.ppuram.nmt[2][lastAccessedVRAM] & 0x3F;
                switch4kEXCHRbank(tileNumber, address & 0x1000);

                return chr[((address & 0x03FF) | exCHRBank[address >> 10 & 0x07])];
            } else {
                return super.readCHR(address);
            }
        } else {
            if (nes.ppu.isBackgroundFetching() && nes.ppu.isOAMSize()) {
                return chr[((address & 0x03FF) | chrBackgroundPage[address >> 10 & 0x07])];
            } else {
                return super.readCHR(address);
            }
        }
    }

    /**
     * Writes PPU data to a given address within the range $0000-$1FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writeCHR(int address, int data) {
        if (nes.ppu.isBackgroundFetching() && nes.ppu.isOAMSize()) {
            chr[((address & 0x03FF) | chrBackgroundPage[address >> 10 & 0x07])] = data;
        } else {
            super.writeCHR(address, data);
        }
    }

    /**
     * Reads nametable data from a given address within the range $2000-$3EFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    @Override
    public int readNametable(int address) {
        if (exramMode == 1) {
            if ((address & 0x3FF) <= 0x3BF) {
                lastAccessedVRAM = address & 0x3FF;
            } else {
                final int paletteNumber = nes.ppuram.nmt[2][lastAccessedVRAM] & 0xC0;
                final int shift = ((lastAccessedVRAM >> 4 & 0x4) | (lastAccessedVRAM & 0x2));

                switch (shift) {
                    case 0:
                        return (paletteNumber >> 6) & 0xFF;
                    case 2:
                        return (paletteNumber >> 4) & 0xFF;
                    case 4:
                        return (paletteNumber >> 2) & 0xFF;
                    case 6:
                        return paletteNumber;
                    default:
                        break;
                }
            }
        }

        return nes.ppuram.nmt[nes.ppuram.nmtBank[address >> 10 & 0x3]][address & 0x3FF];
    }

    /**
     * Writes nametable data to a given address within the range $2000-$3EFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writeNametable(int address, int data) {
        if (exramMode == 1) {
            if ((address & 0x3FF) <= 0x3BF) {
                lastAccessedVRAM = address & 0x3FF;
            }
        }

        nes.ppuram.nmt[nes.ppuram.nmtBank[address >> 10 & 0x3]][address & 0x3FF] = data;
    }

    /**
     * Clocks IRQ every scanline tick.
     */
    @Override
    public void scanlineTick() {
        if (nes.ppu.isRendering()) {
            if (nes.ppu.isRenderingScanline()) {
                irqScanline++;
                irqStatus |= 0x40;
                irqClear = 0;
            }
        }

        if (irqScanline == irqLine) {
            irqStatus |= 0x80;
        }

        if (++irqClear > 2) {
            irqScanline = 0;
            irqStatus &= ~0x80;
            irqStatus &= ~0x40;

            nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
        }

        if (irqEnabled && Tools.getbit(irqStatus, 7) && Tools.getbit(irqStatus, 6)) {
            nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
        }
    }

    /**
     * Switches a 1 kB background CHR bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     * @param addr      Address to switch a memory bank at
     */
    private void switch1kBGCHRbank(int data, int addr) {
        chrBackgroundPage[(addr >> 10) & 7] = data << 10;
    }

    /**
     * Switches a 2 kB background CHR bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     * @param addr      Address to switch a memory bank at
     */
    private void switch2kBGCHRbank(int data, int addr) {
        int area = (addr >> 10) & 7;
        int bank = data << 11;

        for (int i = 0; i < 2; i++) {
            chrBackgroundPage[area] = bank;
            area++;
            bank += 0x400;
        }
    }

    /**
     * Switches a 4 kB background CHR bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     * @param addr      Address to switch a memory bank at
     */
    private void switch4kBGCHRbank(int data, int addr) {
        int area = (addr >> 10) & 7;
        int bank = data << 12;

        for (int i = 0; i < 4; i++) {
            chrBackgroundPage[area] = bank;
            area++;
            bank += 0x400;
        }
    }

    /**
     * Switches a 8 kB background CHR bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     */
    private void switch8kBGCHRbank(int data) {
        int bank = data << 13;

        for (int i = 0; i < 8; i++) {
            chrBackgroundPage[i] = bank;
            bank += 0x400;
        }
    }

    /**
     * Switches a 4 kB ExRAM CHR bank at a given address.
     *
     * @param data      Data to compute a memory bank from
     * @param addr      Address to switch a memory bank at
     */
    private void switch4kEXCHRbank(int data, int addr) {
        int area = (addr >> 10) & 7;
        int bank = data << 12;

        for (int i = 0; i < 4; i++) {
            exCHRBank[area] = bank;
            area++;
            bank += 0x400;
        }
    }
}