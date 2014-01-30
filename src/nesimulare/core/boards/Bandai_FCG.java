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

import nesimulare.core.cpu.CPU;
import nesimulare.core.memory.EEPROM;
import nesimulare.core.memory.PPUMemory;
import nesimulare.gui.Tools;

/**
 * Emulates following boards (mapper 16):
 * - Bandai FCG-1 (Registers are at $6000-$7FFF. No persistent writable storage);
 * - Bandai FCG-2 (Registers are at $6000-$7FFF. No persistent writable storage);
 * - Bandai LZ93D50 (Registers are at $8000-$FFFF. No persistent writable storage);
 * - Bandai LZ93D50 with 24C02 (Registers are at $8000-$FFFF. 24C02 256-byte serial EEPROM is attached to $6000-$7FFF).
 *
 * @author Parseus
 */
public class Bandai_FCG extends Board {
    protected EEPROM eeprom = new EEPROM(256);
    private int irqCounter = 0;
    private boolean irqEnabled = false;
    
    /**
     * Constructor for this class.
     * 
     * @param prg PRG-ROM
     * @param chr CHR-ROM (or CHR-RAM)
     * @param trainer Trainer
     * @param haschrram True: PCB contains CHR-RAM
     *                  False: PCB contains CHR-ROM
     */
    public Bandai_FCG(int[] prg, int[] chr, int[] trainer, boolean haschrram) {
        super(prg, chr, trainer, haschrram);
    }
    
    /**
     * Initializes the board.
     */
    @Override
    public void initialize() {
        super.initialize();
        
        eeprom = new EEPROM(256);
    }
    
    /**
     * Performs a hard reset (turning console off and after about 30 minutes turning it back on).
     */
    @Override
    public void hardReset() {
        super.hardReset();
        eeprom.hardReset();
        
        super.switch16kPRGbank((prg.length - 0x4000) >> 14, 0xC000);
        irqCounter = 0;
        irqEnabled = false;
    }
    
    /**
     * Reads data from a given address within the range $6000-$7FFF.
     * 
     * @param address       Address to read data from
     * @return              Read data
     */
    @Override
    public int readSRAM(int address) {
        int result = CPU.lastRead & 0xEF;
        
        if (eeprom != null && eeprom.read(Tools.getbit(CPU.lastRead, 4))) {
            result = (result | 0x10) & 0xFF;
        }
        
        return result;
    }
    
    /**
     * Writes data to a given address within the range $6000-$7FFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writeSRAM(int address, int data) {
        writeReg(address & 0xF, data);
    }
    
    /**
     * Writes data to a given address within the range $8000-$FFFF.
     * 
     * @param address       Address to write data to
     * @param data          Written data
     */
    @Override
    public void writePRG(int address, int data) {
        writeReg(address & 0xF, data);
    }
    
    /**
     * Writes data to a given register
     *
     * @param register Register to write data to
     * @param data Written data
     */
    private void writeReg(int register, int data) {
        switch (register) {
            case 0x0: // CHR bank select 0 (PPU $0000-$03FF)
                super.switch1kCHRbank(data, 0x0000);
                break;
            case 0x1: // CHR bank select 1 (PPU $0400-$07FF)
                super.switch1kCHRbank(data, 0x0400);
                break;
            case 0x2: // CHR bank select 2 (PPU $0800-$0BFF)
                super.switch1kCHRbank(data, 0x0800);
                break;
            case 0x3: // CHR bank select 3 (PPU $0C00-$0FFF)
                super.switch1kCHRbank(data, 0x0C00);
                break;
            case 0x4: // CHR bank select 4 (PPU $1000-$13FF)
                super.switch1kCHRbank(data, 0x1000);
                break;
            case 0x5: // CHR bank select 5 (PPU $1400-$17FF)
                super.switch1kCHRbank(data, 0x1400);
                break;
            case 0x6: // CHR bank select 6 (PPU $1800-$1BFF)
                super.switch1kCHRbank(data, 0x1800);
                break;
            case 0x7: // CHR bank select 7 (PPU $1C00-$1FFF)
                super.switch1kCHRbank(data, 0x1C00);
                break;
            case 0x8: // PRG Bank select (CPU $8000-$BFFF)
                super.switch16kPRGbank(data, 0x8000);
                break;
            case 0x9: // Mirroring Control
                switch (data & 3) {
                    case 0:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.VERTICAL);
                        break;
                    case 1:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.HORIZONTAL);
                        break;
                    case 2:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENA);
                        break;
                    case 3:
                        nes.ppuram.setMirroring(PPUMemory.Mirroring.ONESCREENB);
                        break;
                    default:
                        break;
                }
                break;
            case 0xA: // IRQ Control
                irqEnabled = Tools.getbit(data, 0);
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, false);
                break;
            case 0xB: // IRQ Counter
                irqCounter = (irqCounter & 0xFF00) | data;
                break;
            case 0xC: // IRQ Counter
                irqCounter = (irqCounter & 0x00FF) | (data << 8);
                break;
            case 0xD: // EEPROM Control
                eeprom.write(register, data);
                break;
            default:
                break;
        }
    }
    
    /**
     * Clocks IRQ every CPU cycle.
     */
    @Override
    public void clockCPUCycle() {
        if (irqEnabled) {
            if (irqCounter > 0) {
                irqCounter--;
            }
            
            if (irqCounter == 0) {
                irqEnabled = false;
                irqCounter = 0xFFFF;
                nes.cpu.interrupt(CPU.InterruptTypes.BOARD, true);
            }
        }
    }
    
    /**
     * Returns save data on EEPROM.
     * 
     * @return      Save data on EEPROM
     */
    @Override
    public int[] getSRAM() {
        return eeprom.rom;
    }
}