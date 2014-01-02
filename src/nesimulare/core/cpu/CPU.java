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

package nesimulare.core.cpu;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import nesimulare.core.NES;
import nesimulare.core.ProcessorBase;
import nesimulare.core.memory.CPUMemory;
import nesimulare.gui.Tools;

/**
 * Emulates the CPU component of Ricoh 2A03.
 * 
 * @author Parseus
 */
public class CPU extends ProcessorBase implements Opcodes {

    NES nes;
    
    /* Process status register mnemonics */
    private static final int P_CARRY       = 0x01;
    private static final int P_ZERO        = 0x02;
    private static final int P_IRQ_DISABLE = 0x04;
    private static final int P_DECIMAL     = 0x08;
    // Bit 4 is only pushed onto the stack with BRK and PHP
    // Bit 5 is always '1'
    private static final int P_OVERFLOW    = 0x40;
    private static final int P_NEGATIVE    = 0x80;

    // NMI vector
    private static final int NMI_VECTOR_L = 0xfffa;
    private static final int NMI_VECTOR_H = 0xfffb;
    // Reset vector
    private static final int RST_VECTOR_L = 0xfffc;
    private static final int RST_VECTOR_H = 0xfffd;
    // IRQ vector
    private static final int IRQ_VECTOR_L = 0xfffe;
    private static final int IRQ_VECTOR_H = 0xffff;

    /* Logging (for debugging) */
    private static final boolean LOGGING = false;
    public FileWriter fw; //debug log writer
    
    /* The delay in microseconds between steps. */
    private static final int CLOCK_IN_NS = 1000;

    /* DMA types */
    public enum DMATypes { DMA, OAM };
    
    /* Interrupts */
    public enum InterruptTypes {
        PPU(0), APU(1), DMC(2), BOARD(4);
        
        public final int irq;
        
        InterruptTypes(final int irq) {
            this.irq = irq;
        }   
    }
    
    /* Interrupt requests */
    private boolean interruptRequest;
    private int irqRequestFlags;
    
    /* NMI */
    private boolean nmi;
    
    /* The Memory */
    public CPUMemory ram;

    /* The CPU state */
    private static final CPUState state = new CPUState();

    /* Scratch space for addressing mode and effective address
     * calculations */
    private static AddressMode irAddressMode; // Addresing mode of the instruction
    private static int effectiveAddress;

    /* Internal scratch space */
    private int lo = 0, hi = 0;  // Used in address calculation
    private int tmp; // Temporary storage

    /* Last readnand write by CPU */
    public static int lastRead;
    private static int lastWrite;
    
    /* Checks the proper offset for branches and some addressing modes */
    private boolean inc;
    
    /* DMA cycles */
    private int dmcDMACycles = 0;
    private int oamDMACycles = 0;
    
    /**
     * Binds CPU to the emulation core.
     * 
     * @param system
     * @param nes       Emulation core
     */
    public CPU(nesimulare.core.Region.System system, NES nes) {
        super(system);
        region.singleCycle = system.cpu;
        
        this.nes = nes;
        
        if (NES.LOGGING) {
            try {
                fw = new FileWriter(new File("cpulog.txt"));
            } catch (IOException ioe) {
                nes.messageBox("Logging error: " + ioe.getMessage());
            }
        }
    }
    
    /**
     * Initializes CPU.
     */
    public final void initialize() {
        setMemory(nes.cpuram);
        hardReset();
    }
    
    /**
     * Set the ram reference for this CPU.
     * @param ram       RAM reference for CPU
     */
    public void setMemory(CPUMemory ram) {
        this.ram = ram;
    }

    /**
     * Return the Memory that this CPU is associated with.
     * @return ram      Returned memory
     */
    public CPUMemory getMemory() {
        return ram;
    }

    /**
     * Synchronizes CPU with APU and PPU (and optionally with MMC).
     */
    private void dispatch() {
        nes.apu.cycle(region.singleCycle);
        nes.ppu.cycle(region.singleCycle);
        nes.board.clockCycle();
    }
    
    /**
     * Check DMC and OAM DMA at the given address
     * 
     * @param address       Address to check DMC and OAM DMA for
     */
    private void checkDmcOamDma(int address) {
        if (dmcDMACycles > 0) {
            int _dmcDMACycles = dmcDMACycles;
            dmcDMACycles = 0;
            
            if ((address == 0x4016) || (address == 0x4017)) {
                // The 2A03 DMC gets fetch by pulling RDY low internally. 
                // This causes the CPU to pause during the next read cycle, until RDY goes high again.
                // The DMC unit holds RDY low for 4 cycles.
                if (_dmcDMACycles-- > 0) {
                    read(address);
                }
                
                // Consecutive controller port reads from this are treated as one...
                while (_dmcDMACycles-- > 0) {
                    dispatch();
                }
            } else {
                // ...but other addresses see multiple reads as expected
                while (_dmcDMACycles-- > 0) {
                    read(address);
                }
            }
            
            nes.apu.dmcFetch();
        }
        
        if (oamDMACycles > 0) {
            int _oamDMACycles = oamDMACycles;
            oamDMACycles = 0;
            
            if ((address == 0x4016) || (address == 0x4017)) {
                // The 2A03 DMC gets fetch by pulling RDY low internally. 
                // This causes the CPU to pause during the next read cycle, until RDY goes high again.
                // The DMC unit holds RDY low for 4 cycles.
                if (_oamDMACycles-- > 0) {
                    read(address);
                }
                
                // Consecutive controller port reads from this are treated as one...
                while (--_oamDMACycles > 0) {
                    dispatch();
                }
            } else {
                // ...but other addresses see multiple reads as expected
                while (--_oamDMACycles > 0) {
                    read(address);
                }
            }
            
            nes.ppu.oamTransfer();
        }
    }

    /**
     * Clocks down DMC and OAM DMA.
     */
    private void checkRDY() {
        if (dmcDMACycles > 0) {
            dmcDMACycles--;
        }
        
        if (oamDMACycles > 0) {
            oamDMACycles--;
        }
    }
    
    /**
     * Holds down CPU with DMC or OAM DMA.
     * 
     * @param type      DMC or OAM DMA to hold CPU with
     */
    public void RDY(final DMATypes type) {
        switch (type) {
            case DMA:
                dmcDMACycles += 4;
                break;
            case OAM:
                oamDMACycles += 3;
                break;
            default:
                break;
        }
    }
    
    /**
     * Reads data from the internal RAM.
     * 
     * @param address       Address to read data from
     * @return              Read value from a given address
     */
    public int read(final int address) {
        checkDmcOamDma(address);
        dispatch();
        
        lastRead = ram.read(address);
        
        return lastRead;
    }
    
    /**
     * Writes data to a given address.
     * 
     * @param address       Address to write data to
     * @param data          Value written to a given address
     */
    public void write(final int address, final int data) {
        checkRDY();
        dispatch();
        
        ram.write(address, lastWrite = data);
    }
    
    /**
     * Hard reset the CPU to known initial values.
     */
    @Override
    public void hardReset() {
        // Registers
        state.sp = 0xfd;
        state.a = 0;
        state.x = 0;
        state.y = 0;

        // Set the PC to the address stored in the reset vector
        state.pc = address(ram.read(RST_VECTOR_L), ram.read(RST_VECTOR_H));

        // Clear instruction register.
        state.ir = 0;

        // Clear status register bits.
        state.carryFlag = false;
        state.zeroFlag = false;
        state.irqDisableFlag = true;
        state.decimalModeFlag = false;
        state.overflowFlag = false;
        state.negativeFlag = false;

        // Reset step counter.
        state.stepCounter = 0L;

        // Reset interrupt requests.
        nmi = false;
        interruptRequest = false;
        irqRequestFlags = 0;
    }
    
    /**
     * Soft reset the CPU to known inital values.
     */
    @Override
    public void softReset() {
        state.irqDisableFlag = true;
        state.sp = (state.sp - 3) & 0xFF;
        state.pc = address(ram.read(RST_VECTOR_L), ram.read(RST_VECTOR_H));
    }
    
    /**
     * Performs an individual machine cycle.
     */
    @Override
    public void cycle() {
        // Store the address from which the IR was read (for debugging)
        state.lastPc = state.pc;

        // Fetch memory location for this instruction.
        state.ir = read(state.pc);
        irAddressMode = instructionModes[state.ir];

        // Increment PC
        incrementPC();

        // Decode the instruction and datas
        state.instSize = CPU.instructionSizes[state.ir];
        for (int i = 0; i < state.instSize - 1; i++) {
            state.args[i] = read(state.pc);
            // Increment PC after reading
            incrementPC();
        }

        // Increment step counter
        state.stepCounter++;

        // Get the data from the effective address (if any)
        effectiveAddress = 0;

        switch (irAddressMode) {
            case ABS_A:     // Absolute
                effectiveAddress = address(state.args[0], state.args[1]);
                break;
            case ABS_LC:    // Absolute with an additional read
                checkInterrupts();
                effectiveAddress = address(state.args[0], state.args[1]);
                break;      
            case ABX_R:     // Absolute,X (dummy read - on carry)
                lo = state.args[0];
                hi = state.args[1];
                inc = (lo + state.x) >= 0x100;
                
                if (inc) {
                    read(xAddress(lo, hi));
                    hi = (hi + 1) & 0xff;
                }

                effectiveAddress = xAddress(lo, hi);
                break;
            case ABX_W:     // Absolute,X (dummy read - always)
                lo = state.args[0];
                hi = state.args[1];
                inc = (lo + state.x) >= 0x100;
                read(xAddress(lo, hi));
                
                if (inc) {                 
                    hi = (hi + 1) & 0xff;
                }

                effectiveAddress = xAddress(lo, hi);
                break;
            case ABY_R:     // Absolute,Y (dummy read - on carry)
                lo = state.args[0];
                hi = state.args[1];
                inc = (lo + state.y) >= 0x100;
                lo = (lo + state.y) & 0xff;
                
                if (inc) {
                    read(address(lo, hi));
                    hi = (hi + 1) & 0xff;
                }

                effectiveAddress = address(lo, hi);
                break;
            case ABY_W:     // Absolute,Y (dummy read - always)
                lo = state.args[0];
                hi = state.args[1];
                inc = (lo + state.y) >= 0x100;
                lo = (lo + state.y) & 0xff;
                read(address(lo, hi));
                
                if (inc) {                 
                    hi = (hi + 1) & 0xff;
                }

                effectiveAddress = address(lo, hi);
                break;
            case IMM_A:     // #Immediate
            case REL_A:     // Relative
                break;
            case IMP_A:     // Implied
                read(state.pc);
                break;
            case IMP_LC:    // Implied with an additional check
                checkInterrupts();
                read(state.pc);
                break;
            case IND_A:     // Indirect (for buggy JMP)
                effectiveAddress = address(state.args[0], state.args[1]);
                checkInterrupts();
                break;
            case INX_A:     // (Zero Page,X)
                tmp = read(state.args[0]);
                tmp = (tmp + state.x) & 0xff;
                
                lo = read(tmp);
                hi = read((tmp + 1) & 0xff);
                
                effectiveAddress = address(lo, hi);
                break;
            case INY_R:     // (Zero Page),Y (dummy read - on carry)
                lo = read(state.args[0]);
                hi = read((state.args[0] + 1) & 0xff);
                inc = (lo + state.y) >= 0x100;
                lo = (lo + state.y) & 0xff;
                
                if (inc) {
                    read(address(lo, hi));
                    hi = (hi + 1) & 0xff;
                }
                
                effectiveAddress = address(lo, hi);
                break;
            case INY_W:     // (Zero Page),Y (dummy read - always)
                lo = read(state.args[0]);
                hi = read((state.args[0] + 1) & 0xff);
                inc = (lo + state.y) >= 0x100;
                lo = (lo + state.y) & 0xff;
                read(address(lo, hi));
                
                if (inc) {
                    hi = (hi + 1) & 0xff;
                }
                
                effectiveAddress = address(lo, hi);
                break;
            case ZPG_A:     // Zero Page
                effectiveAddress = state.args[0];
                break;
            case ZPX_A:     // Zero Page,X
                effectiveAddress = zpxAddress(state.args[0]);
                read(state.pc);
                break;
            case ZPY_A:     // Zero Page,Y
                effectiveAddress = zpyAddress(state.args[0]);
                read(state.pc);
                break;
            default:
                break;
        }
        
        // Logging (debugging only)
        if (LOGGING) {
            try {
                //Note: This *might* trigger side effects if logging while executing
                //code from I/O registers. So don't do that.
                fw.write(getCPUState().toTraceEvent() + " CYC:" + nes.ppu.hclock + " SL:" + nes.ppu.vclock + "\n");
                
                if (state.stepCounter == 0) {
                    fw.flush();
                }
            } catch (IOException ioe) {
                nes.messageBox("Cannot write to debug log: " + ioe.getMessage());
            }
        }

        // Execute
        switch (state.ir) {
            case 0x00: // BRK - BReaK - Implied
                // Push program counter onto the stack
                stackPush((state.pc >> 8) & 0xff); // PC high byte
                stackPush(state.pc & 0xff);        // PC low byte
                // Push status flag register (and bit 4) onto the stack
                stackPush(state.getStatusFlag() | 0x10);
                // Set the Interrupt Disabled flag.  RTI will clear it.
                setIrqDisableFlag();
                    
                // Write to debug writer about the interrupt (debugging only)
                if (LOGGING) {
                    try {
                        fw.write("**BREAK INTERRUPT**\n");
                    } catch (IOException ioe) {
                        nes.messageBox("Cannot write to debug log: " + ioe.getMessage());
                    }
                }
                
                // Load interrupt vector address into PC (with possible NMI hijacking)
                if (nmi) {
                    nmi = false;
                    state.pc = address(read(NMI_VECTOR_L), read(NMI_VECTOR_H));
                } else {
                    state.pc = address(read(IRQ_VECTOR_L), read(IRQ_VECTOR_H));
                }
                break;
            case 0x08: // PHP - PusH Processor status - Implied
                // Bit 4 is always set in the stack value.
                checkInterrupts();
                stackPush(state.getStatusFlag() | 0x10); 
                break;
            case 0x10: // BPL - Branch on PLus - Relative
                branch(!getNegativeFlag());
                break;
            case 0x18: // CLC - CLear Carry - Implied
                clearCarryFlag();
                break;
            case 0x20: // JSR - Jump to SubRoutine - Implied
                state.pc--;
                stackPush((state.pc >> 8) & 0xff); // PC high byte
                stackPush(state.pc & 0xff);        // PC low byte
                checkInterrupts();
                dispatch();
                state.pc = address(state.args[0], state.args[1]);
                break;
            case 0x28: // PLP - PuLl Processor status - Implied
                dispatch();
                checkInterrupts();
                setProcessorStatus(stackPop());
                break;
            case 0x30: // BMI - Branch on MInus - Relative
                branch(getNegativeFlag());
                break;
            case 0x38: // SEC - SEt Carry flag - Implied
                setCarryFlag();
                break;
            case 0x40: // RTI - ReTurn from Interrupt - Implied
                setProcessorStatus(stackPop());
                lo = stackPop();
                checkInterrupts();
                hi = stackPop();
                setProgramCounter(address(lo, hi));
                break;
            case 0x48: // PHA - PusH Accumulator - Implied
                checkInterrupts();
                stackPush(state.a);
                break;
            case 0x50: // BVC - Branch on oVerflow Clear - Relative
                branch(!getOverflowFlag());
                break;
            case 0x58: // CLI - CLear Interrupt - Implied
                clearIrqDisableFlag();
                break;
            case 0x60: // RTS - ReTurn from Subroutine - Implied
                lo = stackPop();
                hi = stackPop();
                setProgramCounter(address(lo, hi) + 1);
                checkInterrupts();
                break;
            case 0x68: // PLA - PuLl Accumulator - Implied
                dispatch();
                checkInterrupts();
                state.a = stackPop();
                setArithmeticFlags(state.a);
                break;
            case 0x70: // BVS - Branch on oVerflow Set - Relative
                branch(getOverflowFlag());
                break;
            case 0x78: // SEI - Set Interrupt - Implied
                setIrqDisableFlag();
                break;
            case 0x88: // DEY - DEcrement Y - Implied
                state.y = (state.y - 1) & 0xff;
                setArithmeticFlags(state.y);
                break;
            case 0x8a: // TXA - Transfer X to A - Implied
                state.a = state.x;
                setArithmeticFlags(state.a);
                break;
            case 0x90: // BCC - Branch on Carry Clear - Relative
                branch(!getCarryFlag());
                break;
            case 0x98: // TYA - Transfer Y to A - Implied
                state.a = state.y;
                setArithmeticFlags(state.a);
                break;
            case 0x9a: // TXS - Transfer X to Stack pointer - Implied
                setStackPointer(state.x);
                break;
            case 0xa8: // TAY - Transfer A to Y - Implied
                state.y = state.a;
                setArithmeticFlags(state.y);
                break;
            case 0xaa: // TAX - Transfer A to X - Implied
                state.x = state.a;
                setArithmeticFlags(state.x);
                break;
            case 0xb0: // BCS - Branch on Carry Set - Relative
                branch(getCarryFlag());
                break;
            case 0xb8: // CLV - CLear oVerflow - Implied
                clearOverflowFlag();
                break;
            case 0xba: // TSX - Transfer Stack pointer to X - Implied
                state.x = getStackPointer();
                setArithmeticFlags(state.x);
                break;
            case 0xc8: // INY - INcrement Y - Implied
                state.y = (state.y + 1) & 0xff;
                setArithmeticFlags(state.y);
                break;
            case 0xca: // DEX - DEcrement X - Implied
                state.x = (state.x - 1) & 0xff;
                setArithmeticFlags(state.x);
                break;
            case 0xd0: // BNE - Branch on Not Equal - Relative
                branch(!getZeroFlag());
                break;
            case 0xd8: // CLD - CLear Decimal - Implied
                clearDecimalModeFlag();
                break;
            case 0xe8: // INX - INcrement X - Implied
                state.x = (state.x + 1) & 0xff;
                setArithmeticFlags(state.x);
                break;
            case 0x04: case 0x14: case 0x34: case 0x44: case 0x54: case 0x64: case 0x74: //DOP - Double-byte NOP
                checkInterrupts();
                dispatch();
                break;
            case 0x80: case 0x82: case 0x89: case 0xC2: case 0xD4: case 0xE2: case 0xF4: //DOP - Double-byte NOP
                checkInterrupts();
                dispatch();
                break;
            case 0x0C: case 0x1C: case 0x3C: case 0x5C: case 0x7C: case 0xDC: case 0xFC: //TOP - Triple-byte TOP
                checkInterrupts();
                dispatch();
                break;
            case 0x1A: case 0x3A: case 0x5A: case 0x7A: case 0xDA: case 0xEA: case 0xFA: // NOP - No OPeration
                // Do nothing.
                break;
            case 0xf0: // BEQ - Branch on EQual - Relative
                branch(getZeroFlag());
                break;
            case 0xf8: // SED - SEt Decimal - Implied
                setDecimalModeFlag();
                break;

            /** JMP *****************************************************************/
            case 0x4c: // JMP - JuMP - Absolute
                state.pc = address(state.args[0], state.args[1]);
                break;
            case 0x6c: // JMP - JuMP - Indirect
                lo = address(state.args[0], state.args[1]); // Address of low byte

                //Indirect adressing modes are not able to fetch 
                //an adress which crosses the page boundary
                if (state.args[0] == 0xff) {
                    hi = address(0x00, state.args[1]);
                } else {
                    hi = lo + 1;
                }

                state.pc = address(read(lo), read(hi));
                break;


            /** ORA - Logical Inclusive Or ******************************************/
            case 0x09: // #Immediate
                checkInterrupts();
                state.a |= state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0x01: // (Zero Page,X)
            case 0x05: // Zero Page
            case 0x0d: // Absolute
            case 0x11: // (Zero Page),Y
            case 0x15: // Zero Page,X
            case 0x19: // Absolute,Y
            case 0x1d: // Absolute,X
                checkInterrupts();
                state.a |= read(effectiveAddress);
                setArithmeticFlags(state.a);
                break;


            /** ASL - Arithmetic Shift Left *****************************************/
            case 0x0a: // Accumulator
                state.a = asl(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x06: // Zero Page
            case 0x0e: // Absolute
            case 0x16: // Zero Page,X
            case 0x1e: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = asl(tmp);
                checkInterrupts();
                write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** BIT - Bit Test ******************************************************/
            case 0x24: // Zero Page
            case 0x2c: // Absolute
                checkInterrupts();
                tmp = read(effectiveAddress);
                setZeroFlag((state.a & tmp) == 0);
                setNegativeFlag(Tools.getbit(tmp, 7));
                setOverflowFlag(Tools.getbit(tmp, 6));
                break;


            /** AND - Logical AND ***************************************************/
            case 0x29: // #Immediate
                checkInterrupts();
                state.a &= state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0x21: // (Zero Page,X)
            case 0x25: // Zero Page
            case 0x2d: // Absolute
            case 0x31: // (Zero Page),Y
            case 0x35: // Zero Page,X
            case 0x39: // Absolute,Y
            case 0x3d: // Absolute,X
                checkInterrupts();
                state.a &= read(effectiveAddress);
                setArithmeticFlags(state.a);
                break;


            /** ROL - Rotate Left ***************************************************/
            case 0x2a: // Accumulator
                state.a = rol(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x26: // Zero Page
            case 0x2e: // Absolute
            case 0x36: // Zero Page,X
            case 0x3e: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = rol(tmp);
                checkInterrupts();
                write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** EOR - Exclusive OR **************************************************/
            case 0x49: // #Immediate
                checkInterrupts();
                state.a ^= state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0x41: // (Zero Page,X)
            case 0x45: // Zero Page
            case 0x4d: // Absolute
            case 0x51: // (Zero Page,Y)
            case 0x55: // Zero Page,X
            case 0x59: // Absolute,Y
            case 0x5d: // Absolute,X
                checkInterrupts();
                state.a ^= read(effectiveAddress);
                setArithmeticFlags(state.a);
                break; 


            /** LSR - Logical Shift Right *******************************************/
            case 0x4a: // Accumulator
                state.a = lsr(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x46: // Zero Page
            case 0x4e: // Absolute
            case 0x56: // Zero Page,X
            case 0x5e: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = lsr(tmp);
                checkInterrupts();
                write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** ADC - Add with Carry ************************************************/
            case 0x69: // #Immediate
                state.a = adc(state.a, state.args[0]);
                break;
            case 0x61: // (Zero Page,X)
            case 0x65: // Zero Page
            case 0x6d: // Absolute
            case 0x71: // (Zero Page),Y
            case 0x75: // Zero Page,X
            case 0x79: // Absolute,Y
            case 0x7d: // Absolute,X
                state.a = adc(state.a, read(effectiveAddress));
                break;


            /** ROR - Rotate Right **************************************************/
            case 0x6a: // Accumulator
                state.a = ror(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x66: // Zero Page
            case 0x6e: // Absolute
            case 0x76: // Zero Page,X
            case 0x7e: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = ror(tmp);
                checkInterrupts();
                write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** STA - Store Accumulator *********************************************/
            case 0x81: // (Zero Page,X)
            case 0x85: // Zero Page
            case 0x8d: // Absolute
            case 0x91: // (Zero Page),Y
            case 0x95: // Zero Page,X
            case 0x99: // Absolute,Y
            case 0x9d: // Absolute,X
                checkInterrupts();
                write(effectiveAddress, state.a);
                break;


            /** STY - Store Y Register **********************************************/
            case 0x84: // Zero Page
            case 0x8c: // Absolute
            case 0x94: // Zero Page,X
                checkInterrupts();
                write(effectiveAddress, state.y);
                break;


            /** STX - Store X Register **********************************************/
            case 0x86: // Zero Page
            case 0x8e: // Absolute
            case 0x96: // Zero Page,Y
                checkInterrupts();
                write(effectiveAddress, state.x);
                break;


            /** LDY - Load Y Register ***********************************************/
            case 0xa0: // #Immediate
                checkInterrupts();
                state.y = state.args[0];
                setArithmeticFlags(state.y);
                break;
            case 0xa4: // Zero Page
            case 0xac: // Absolute
            case 0xb4: // Zero Page,X
            case 0xbc: // Absolute,X
                checkInterrupts();
                state.y = read(effectiveAddress);
                setArithmeticFlags(state.y);
                break;


            /** LDX - Load X Register ***********************************************/
            case 0xa2: // #Immediate
                state.x = state.args[0];
                setArithmeticFlags(state.x);
                break;
            case 0xa6: // Zero Page
            case 0xae: // Absolute
            case 0xb6: // Zero Page,Y
            case 0xbe: // Absolute,Y
                checkInterrupts();
                state.x = read(effectiveAddress);
                setArithmeticFlags(state.x);
                break;


            /** LDA - Load Accumulator **********************************************/
            case 0xa9: // #Immediate
                checkInterrupts();
                state.a = state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0xa1: // (Zero Page,X)
            case 0xa5: // Zero Page
            case 0xad: // Absolute
            case 0xb1: // (Zero Page),Y
            case 0xb5: // Zero Page,X
            case 0xb9: // Absolute,Y
            case 0xbd: // Absolute,X
                checkInterrupts();
                state.a = read(effectiveAddress);
                setArithmeticFlags(state.a);
                break;


            /** CPY - Compare Y Register ********************************************/
            case 0xc0: // #Immediate
                cmp(state.y, state.args[0]);
                break;
            case 0xc4: // Zero Page
            case 0xcc: // Absolute
                cmp(state.y, read(effectiveAddress));
                break;


            /** CMP - Compare Accumulator *******************************************/
            case 0xc9: // #Immediate
                cmp(state.a, state.args[0]);
                break;
            case 0xc1: // (Zero Page,X)
            case 0xc5: // Zero Page
            case 0xcd: // Absolute
            case 0xd1: // (Zero Page),Y
            case 0xd5: // Zero Page,X
            case 0xd9: // Absolute,Y
            case 0xdd: // Absolute,X
                cmp(state.a, read(effectiveAddress));
                break;


            /** DEC - Decrement Memory **********************************************/
            case 0xc6: // Zero Page
            case 0xce: // Absolute
            case 0xd6: // Zero Page,X
            case 0xde: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = (tmp - 1) & 0xff;
                checkInterrupts();
                write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** CPX - Compare X Register ********************************************/
            case 0xe0: // #Immediate
                cmp(state.x, state.args[0]);
                break;
            case 0xe4: // Zero Page
            case 0xec: // Absolute
                cmp(state.x, read(effectiveAddress));
                break;


            /** SBC - Subtract with Carry (Borrow) **********************************/
            case 0xe9: // #Immediate
            case 0xeb: // #Immediate
                state.a = sbc(state.a, state.args[0]);
                break;
            case 0xe1: // (Zero Page,X)
            case 0xe5: // Zero Page
            case 0xed: // Absolute
            case 0xf1: // (Zero Page),Y
            case 0xf5: // Zero Page,X
            case 0xf9: // Absolute,Y
            case 0xfd: // Absolute,X
                state.a = sbc(state.a, read(effectiveAddress));
                break;


            /** INC - Increment Memory **********************************************/
            case 0xe6: // Zero Page
            case 0xee: // Absolute
            case 0xf6: // Zero Page,X
            case 0xfe: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = (tmp + 1) & 0xff;
                checkInterrupts();
                write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;
                
            /** SLO - ASL + ORA **********************************************/
            case 0x03: // (Zero Page,X)
            case 0x07: // Zero Page
            case 0x0F: // Absolute
            case 0x13: // (Zero Page),Y
            case 0x17: // Zero Page,X
            case 0x1B: // Absolute,Y
            case 0x1F: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                setCarryFlag(Tools.getbit(tmp, 7));
                tmp = asl(tmp);
                checkInterrupts();
                state.a |= tmp;
                setArithmeticFlags(state.a);
                write(effectiveAddress, tmp);
                break;
                
            /** ANC - AND byte and set carry***********************************/
            case 0x0B: // #Immediate
            case 0x2B: // #Immediate
                checkInterrupts();
                state.a &= state.args[0];
                setCarryFlag(Tools.getbit(state.a, 7));
                setArithmeticFlags(state.a);
                break;
           
            /** RLA - AND + ROL **********************************************/
            case 0x23: // (Zero Page,X)
            case 0x27: // Zero Page
            case 0x2F: // Absolute
            case 0x33: // (Zero Page),Y
            case 0x37: // Zero Page,X
            case 0x3B: // Absolute,Y
            case 0x3F: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                final boolean carry = Tools.getbit(tmp, 7);
                tmp = rol(tmp);
                setCarryFlag(carry);
                checkInterrupts();
                state.a &= tmp;
                setArithmeticFlags(state.a);
                write(effectiveAddress, tmp);
                break;
                
             /** SRE - LSR + EOR **********************************************/
            case 0x43: // (Zero Page,X)
            case 0x47: // Zero Page
            case 0x4F: // Absolute
            case 0x53: // (Zero Page),Y
            case 0x57: // Zero Page,X
            case 0x5B: // Absolute,Y
            case 0x5F: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = lsr(tmp);
                checkInterrupts();
                state.a ^= tmp;
                write(effectiveAddress, tmp);
                setArithmeticFlags(state.a);
                break;
                
             /** ALR - AND + LSR **********************************************/
            case 0x4B: // #Immediate
                checkInterrupts();
                state.a &= state.args[0];
                state.a = lsr(state.a);
                setArithmeticFlags(state.a);
                break;
                
             /** RRA - ROR + ADC **********************************************/
            case 0x63: // (Zero Page,X)
            case 0x67: // Zero Page
            case 0x6F: // Absolute
            case 0x73: // (Zero Page),Y
            case 0x77: // Zero Page,X
            case 0x7B: // Absolute,Y
            case 0x7F: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = ror(tmp);
                checkInterrupts();
                write(effectiveAddress, tmp);
                state.a = adc(state.a, tmp);
                break;
            
            /** ARR - AND + LSR **********************************************/
            case 0x6B: // #Immediate
                checkInterrupts();
                state.a &= state.args[0];
                final boolean carryTemp = Tools.getbit(state.a, 0);
                state.a = ((state.a >> 1) | (getCarryFlag() ? 0x80 : 0x00));
                setCarryFlag(carryTemp);
                
                if (Tools.getbit(state.a, 5)) {
                    if (Tools.getbit(state.a, 6)) {
                        setCarryFlag();
                        clearOverflowFlag();
                    } else {
                        clearCarryFlag();
                        setOverflowFlag();
                    }
                } else if (Tools.getbit(state.a, 6)) {
                    setCarryFlag();
                    setOverflowFlag();
                } else {
                    clearCarryFlag();
                    clearOverflowFlag();
                }

                setArithmeticFlags(state.a);
                break;
                
            /** SAX - Store A & X *********************************************/
            case 0x83: // (Zero Page,X)
            case 0x87: // Zero Page
            case 0x8F: // Absolute
            case 0x97: // Zero Page,Y
                checkInterrupts();
                write(effectiveAddress, state.a & state.x);
                break;
                
           /** XAA - TXA + AND ************************************************/
            case 0x8B: // #Immediate
                checkInterrupts();
                state.a = state.x;
                state.a &= state.args[0];
                setArithmeticFlags(state.a);
                break;
                
            /** AHX - Store A & X & high byte of the address in memory*********/    
            case 0x93: // (Zero Page),Y
            case 0x9F: // Absolute,Y
                lo = effectiveAddress & 0xFF;
                hi = (effectiveAddress >> 8) & 0xFF;
                tmp = (state.a & state.x & (hi + 1)) & 0xFF;
                
                read(effectiveAddress);
                lo = (lo + state.y) & 0xFF;
                
                if (lo < state.y) {
                    hi = tmp;
                }
                
                checkInterrupts();
                write(address(lo, hi), tmp);
                break;
                
             /** TAS - Store A & X & high byte of the address in SP ***********/    
            case 0x9B: // Absolute,Y
                state.sp = state.a & state.x;                
                lo = effectiveAddress & 0xFF;
                hi = (effectiveAddress >> 8) & 0xFF;
                tmp = (state.sp & (hi + 1)) & 0xFF;
                
                read(effectiveAddress);
                lo = (lo + state.y) & 0xFF;
                
                if (lo < state.y) {
                    hi = tmp;
                }
                
                checkInterrupts();
                write(address(lo, hi), tmp);
                break;
                
             /** SHY - Store Y & high byte of the address in memory ***********/    
            case 0x9C: // Absolute,X
                lo = effectiveAddress & 0xFF;
                hi = (effectiveAddress >> 8) & 0xFF;
                tmp = (state.y & (hi + 1)) & 0xFF;
                
                read(effectiveAddress);
                lo = (lo + state.x) & 0xFF;
                
                if (lo < state.x) {
                    hi = tmp;
                }
                
                checkInterrupts();
                write(address(lo, hi), tmp);
                break;
                
             /** SHX - Store X & high byte of the address in memory ***********/    
            case 0x9E: // Absolute,Y
                lo = effectiveAddress & 0xFF;
                hi = (effectiveAddress >> 8) & 0xFF;
                tmp = (state.x & (hi + 1)) & 0xFF;
                
                read(effectiveAddress);
                lo = (lo + state.y) & 0xFF;
                
                if (lo < state.y) {
                    hi = tmp;
                }
                
                checkInterrupts();
                write(address(lo, hi), tmp);
                break;
            
             /** LAX - Load Accumulator and X *********************************/ 
            case 0xAB: // #Immediate
                checkInterrupts();
                state.a = state.x = state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0xA3: // (Zero Page,X)
            case 0xA7: // Zero Page
            case 0xAF: // Absolute
            case 0xB3: // (Zero Page),Y
            case 0xB7: // Zero Page,Y
            case 0xBF: // Absolute,Y
                checkInterrupts();
                state.a = state.x = read(effectiveAddress);
                setArithmeticFlags(state.a);
                break;    
           
            /** LAS - Load Accumulator, X and ANDed SP ******************************/    
            case 0xBB: // Absolute,Y
                checkInterrupts();
                state.sp &= read(effectiveAddress);
                state.a = state.x = state.sp;
                setArithmeticFlags(state.sp);
                break;
                
            /** DCP - DEC + CMP **********************************************/
            case 0xC3: // (Zero Page,X)
            case 0xC7: // Zero Page
            case 0xCF: // Absolute
            case 0xD3: // (Zero Page),Y
            case 0xD7: // Zero Page,X
            case 0xDB: // Absolute,Y
            case 0xDF: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = (tmp - 1) & 0xff;
                checkInterrupts();
                cmp(state.a, tmp);
                write(effectiveAddress, tmp);
                break;
            
             /** AXS - Stores (A & X - #imm) into X **************************/    
            case 0xCB: // #Immediate
                checkInterrupts();
                state.x = (state.a & state.x) - state.args[0];
                setArithmeticFlags(state.x);
                setCarryFlag(state.x >= 0);
                break;
            
            /** ISC - INC + SBC **********************************************/
            case 0xE3: // (Zero Page,X)
            case 0xE7: // Zero Page
            case 0xEF: // Absolute
            case 0xF3: // (Zero Page),Y
            case 0xF7: // Zero Page,X
            case 0xFB: // Absolute,Y
            case 0xFF: // Absolute,X
                tmp = read(effectiveAddress);
                write(effectiveAddress, tmp);
                tmp = (tmp + 1) & 0xff;
                checkInterrupts();
                state.a = sbc(state.a, tmp);
                write(effectiveAddress, tmp);
                break;
           
            /** KIL - Jams the CPU - Implied *********************************/
            case 0x02: case 0x12: case 0x22: case 0x32: case 0x42: case 0x52:
            case 0x62: case 0x72: case 0x92: case 0xB2: case 0xD2: case 0xF2:
                checkInterrupts();
                dispatch();
                state.pc--;
                break;
              
            default:
                break;
        }

        delayLoop(state.ir);
        
        //Interrupts stuff
        if (interruptRequest) {
            final boolean oldNMI = nmi;
            read(state.pc);
            read(state.pc);
            stackPush((state.pc >> 8) & 0xff); // PC high byte
            stackPush(state.pc & 0xff);        // PC low byte
            stackPush(state.getStatusFlag());      // Status register
            setIrqDisableFlag();
            
            if (LOGGING) {
                try {
                    fw.write("**INTERRUPT: ");
                } catch (IOException ioe) {
                    nes.messageBox("Cannot write to debug log: " + ioe.getMessage());
                }
            }
            
            if (oldNMI) {
                //Disable NMI only if it occured before the 4th cycle
                nmi = false;
            
                //If NMI is requested, hijack the IRQ request
                state.pc = address(read(NMI_VECTOR_L), read(NMI_VECTOR_H));
                
                if (LOGGING) {
                    try {
                        fw.write("NMI**\n");
                    } catch (IOException ioe) {
                        nes.messageBox("Cannot write to debug log: " + ioe.getMessage());
                    }
                }
            } else {
                state.pc = address(read(IRQ_VECTOR_L), read(IRQ_VECTOR_H));
                
                if (LOGGING) {
                    try {
                        fw.write("IRQ**\n");
                    } catch (IOException ioe) {
                        nes.messageBox("Cannot write to debug log: " + ioe.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 
     * @param flag 
     */
    private void branch(boolean flag) {
        checkInterrupts();
   
        if (flag) {
            final int offset = state.args[0];
            read(state.pc);

            final int low = (state.pc + offset) & 0xFF;
            int high = (state.pc >> 8) & 0xFF;
            
            if (offset >= 0x80) {
                if (low >= offset) {
                    checkInterrupts();
                    read(state.pc);
                    high = (high - 1) & 0xFF;
                } 
            } else {
                if (low < offset) {
                    checkInterrupts();
                    read(state.pc);
                    high = (high + 1) & 0xFF;
                }
            }
            
            // If an interrupt occurs during the final cycle of a non-pagecrossing branch
            // then it will be ignored until the next instruction completes
            state.pc = address(low, high);
        }
    }
    
    /**
     * Add with Carry, used by all addressing mode implementations of ADC.
     * As a side effect, this will set the overflow and carry flags as
     * needed.
     *
     * @param addr     The current value of the addrumulator
     * @param data     The data
     * @return
     */
    private int adc(int addr, int data) {
        checkInterrupts();
        int result = (data & 0xff) + (addr & 0xff) + getCarryBit();
        final int carry6 = (data & 0x7f) + (addr & 0x7f) + getCarryBit();
        setCarryFlag(Tools.getbit(result, 8));
        setOverflowFlag(state.carryFlag ^ Tools.getbit(carry6, 7));
        result &= 0xff;
        setArithmeticFlags(result);
        
        return result;
    }

    /**
     * Code for Subtract with Carry.  Just calls ADC of the
     * one's complement of the data.  This lets the N, V, C, and Z
     * flags work out nicely without any additional logic.
     */
    private int sbc(int addr, int data) {
        checkInterrupts();
        final int result = adc(addr, ~data);
        setArithmeticFlags(result);
        
        return result;
    }

    /**
     * Compare two values, and set carry, zero, and negative flags
     * appropriately.
     */
    private void cmp(int reg, int data) {
        checkInterrupts();
        final int temp = reg - data;
        setCarryFlag(temp >= 0);
        setArithmeticFlags(temp);
    }

    /**
     * Checks if there is an interrupt request from either IRQ or NMI.
     */
    private void checkInterrupts() {
        interruptRequest = (!state.irqDisableFlag && (irqRequestFlags != 0)) | nmi;
    }
    
    /**
     * Updates flags for interrupt requests.
     * @param type          Source of an interrupt request
     * @param asserted      Indicates whether an interrupt request was asserted or not
     */
    public void interrupt(final InterruptTypes type, final boolean asserted) {
        switch (type) {
            case PPU:
                nmi = asserted;
                break;
            case APU:
            case DMC:
            case BOARD:
                if (asserted) {
                    irqRequestFlags |= (int)type.irq;
                } else { 
                   irqRequestFlags &= ~(int)type.irq;
                }
            default:
                break;
        }
    } 
    
    /**
     * Set the Negative and Zero flags based on the current value of the
     * register data.
     */
    private void setArithmeticFlags(int reg) {
        state.zeroFlag = (reg == 0);
        state.negativeFlag = Tools.getbit(reg, 7);
    }

    /**
     * Shifts the given value left by one bit, and sets the carry
     * flag to the high bit of the initial value.
     *
     * @param m The value to shift left.
     * @return the left shifted value (m * 2).
     */
    private int asl(int m) {
        setCarryFlag(Tools.getbit(m, 7));
        
        return (m << 1) & 0xff;
    }

    /**
     * Shifts the given value right by one bit, filling with zeros,
     * and sets the carry flag to the low bit of the initial value.
     */
    private int lsr(int m) {
        setCarryFlag(Tools.getbit(m, 0));
        
        return (m >> 1) & 0xff;
    }

    /**
     * Rotates the given value left by one bit, setting bit 0 to the value
     * of the carry flag, and setting the carry flag to the original value
     * of bit 7.
     */
    private int rol(int m) {
        final int result = ((m << 1) | getCarryBit()) & 0xff;
        setCarryFlag(Tools.getbit(m, 7));
        
        return result;
    }

    /**
     * Rotates the given value right by one bit, setting bit 7 to the value
     * of the carry flag, and setting the carry flag to the original value
     * of bit 1.
     */
    private int ror(int m) {
        final int result = ((m >> 1) | (getCarryBit() << 7)) & 0xff;
        setCarryFlag(Tools.getbit(m, 0));
        
        return result;
    }

    /**
     * Return the current CPU State.
     *
     * @return the current CPU State.
     */
    public CPUState getCPUState() {
        return state;
    }

    /**
     * @return the negative flag
     */
    private boolean getNegativeFlag() {
        return state.negativeFlag;
    }

    /**
     * @param negativeFlag the negative flag to set
     */
    private void setNegativeFlag(boolean negativeFlag) {
        state.negativeFlag = negativeFlag;
    }

    /**
     * @return the carry flag
     */
    private boolean getCarryFlag() {
        return state.carryFlag;
    }

    /**
     * @return 1 if the carry flag is set, 0 if it is clear.
     */
    private int getCarryBit() {
        return (state.carryFlag ? 1 : 0);
    }

    /**
     * @param carryFlag the carry flag to set
     */
    private void setCarryFlag(boolean carryFlag) {
        state.carryFlag = carryFlag;
    }

    /**
     * Sets the Carry Flag
     */
    private void setCarryFlag() {
        state.carryFlag = true;
    }

    /**
     * Clears the Carry Flag
     */
    private void clearCarryFlag() {
        state.carryFlag = false;
    }

    /**
     * @return the zero flag
     */
    private boolean getZeroFlag() {
        return state.zeroFlag;
    }

    /**
     * @param zeroFlag the zero flag to set
     */
    private void setZeroFlag(boolean zeroFlag) {
        state.zeroFlag = zeroFlag;
    }

    /**
     * Set the IRQ disable flag
     */
    private void setIrqDisableFlag() {
        state.irqDisableFlag = true;
    }
    
    /**
     * @param irqDisableFlag The IRQ disable flag to set
     */
    private void setIrqDisableFlag(boolean irqDisableFlag) {
        state.irqDisableFlag = irqDisableFlag;
    }

    /**
     * Clears the IRQ disable flag
     */
    private void clearIrqDisableFlag() {
        state.irqDisableFlag = false;
    }

    /**
     * Sets the Decimal Mode Flag to true.
     */
    private void setDecimalModeFlag() {
        state.decimalModeFlag = true;
    }

    /**
     * @param decimalModeFlag The IRQ disable flag to set
     */
    private void setDecimalModeFlag(boolean decimalModeFlag) {
        state.decimalModeFlag = decimalModeFlag;
    }
    
    /**
     * Clears the Decimal Mode Flag.
     */
    private void clearDecimalModeFlag() {
        state.decimalModeFlag = false;
    }

    /**
     * @return the overflow flag
     */
    private boolean getOverflowFlag() {
        return state.overflowFlag;
    }

    /**
     * @param overflowFlag the overflow flag to set
     */
    private void setOverflowFlag(boolean overflowFlag) {
        state.overflowFlag = overflowFlag;
    }

    /**
     * Sets the Overflow Flag
     */
    private void setOverflowFlag() {
        state.overflowFlag = true;
    }

    /**
     * Clears the Overflow Flag
     */
    private void clearOverflowFlag() {
        state.overflowFlag = false;
    }

    /**
     * 
     * @param addr 
     */
    private void setProgramCounter(int addr) {
        state.pc = addr;
    }

    /**
     * 
     * @return 
     */
    private int getStackPointer() {
        return state.sp;
    }

    /**
     * 
     * @param offset 
     */
    private void setStackPointer(int offset) {
        state.sp = offset;
    }

    /**
     * @value The value of the Process Status Register bits to be set.
     */
    private void setProcessorStatus(int value) {
        setCarryFlag(Tools.getbit(value, 0));
        setZeroFlag(Tools.getbit(value, 1));
        setIrqDisableFlag(Tools.getbit(value, 2));
        setDecimalModeFlag(Tools.getbit(value, 3));
        setOverflowFlag(Tools.getbit(value, 6));
        setNegativeFlag(Tools.getbit(value, 7));
    }

    /**
     * 
     * @return 
     */
    public String getAccumulatorStatus() {
        return "$" + Tools.byteToHex(state.a);
    }

    /**
     * 
     * @return 
     */
    public String getXRegisterStatus() {
        return "$" + Tools.byteToHex(state.x);
    }

    /**
     * 
     * @return 
     */
    public String getYRegisterStatus() {
        return "$" + Tools.byteToHex(state.y);
    }

    /**
     * 
     * @return 
     */
    public String getProgramCounterStatus() {
        return "$" + Tools.wordToHex(state.pc);
    }

    /**
     * 
     * @return 
     */
    public String getStackPointerStatus() {
        return "$" + Tools.byteToHex(state.sp);
    }

    /**
     * 
     * @return 
     */
    public int getProcessorStatus() {
        return state.getStatusFlag();
    }

    /**
     * Push an item onto the stack, and decrement the stack counter.
     * Will wrap-around if already at the bottom of the stack (This
     * is the same behavior as the real 6502)
     */
    private void stackPush(int data) {
        write(0x100 + state.sp, data);
        
        if (state.sp == 0) {
            state.sp = 0xff;
        } else {
            --state.sp;
        }
    }

    /**
     * Pre-increment the stack pointer, and return the top of the stack.
     * Will wrap-around if already at the top of the stack (This
     * is the same behavior as the real 6502)
     */
    private int stackPop() {
        if (state.sp == 0xff) {
            state.sp = 0x00;
        } else {
            ++state.sp;
        }

        return read(0x100 + state.sp);
    }

    /*
    * Increment the PC, rolling over if necessary.
    */
    private void incrementPC() {
        if (state.pc == 0xffff) {
            state.pc = 0;
        } else {
            ++state.pc;
        }
    }

    /**
     * Given two bytes, return an address.
     */
    final int address(int lowByte, int hiByte) {
        return (lowByte | (hiByte << 8)) & 0xffff;
    }

    /**
     * Given a hi byte and a low byte, return the Absolute,X
     * offset address.
     */
        final int xAddress(int lowByte, int hiByte) {
        return (address((lowByte + state.x) & 0xff, hiByte)) & 0xffff;
    }

    /**
     * Given a hi byte and a low byte, return the Absolute,Y
     * offset address.
     */
    final int yAddress(int lowByte, int hiByte) {
        return (address((lowByte + state.y) & 0xff, hiByte)) & 0xffff;
    }

    /**
     * Given a single byte, compute the Zero Page,X offset address.
     */
    final int zpxAddress(int zp) {
        return (zp + state.x) & 0xff;
    }

    /**
     * Given a single byte, compute the Zero Page,Y offset address.
     */
    final int zpyAddress(int zp) {
        return (zp + state.y) & 0xff;
    }

    /*
     * Perform a ramy-loop for CLOCK_IN_NS nanoseconds
     */
    @SuppressWarnings("empty-statement")
    private void delayLoop(int opcode) {
        final int clockSteps = CPU.instructionClocks[opcode];
        final long startTime = System.nanoTime();
        final long stopTime = startTime + (CLOCK_IN_NS * clockSteps);
        
        // Memory loop
        while (System.nanoTime() < stopTime) {
            ;
        }
    }


    /**
     * A compact, struct-like representation of CPU state.
     */
    public static class CPUState {
        /**
         * Accumulator
         */
        public int a;
        /**
         * X index regsiter
         */
        public int x;
        /**
         * Y index register
         */
        public int y;
        /**
         * Stack Pointer
         */
        public int sp;
        /**
         * Program Counter
         */
        public int pc;
        /**
         * Instruction Register
         */
        public int ir;
        public int lastPc;
        public int[] args = new int[2];
        public int instSize;

        /* Status Flag Register bits */
        public boolean carryFlag;
        public boolean negativeFlag;
        public boolean zeroFlag;
        public boolean irqDisableFlag;
        public boolean decimalModeFlag;
        public boolean overflowFlag;
        /**
         * Step counter
         */
        public long stepCounter = 0L;

        /**
         * Create an empty CPU State.
         */
        public CPUState() {
            //Nothing to see here, move along
        }

        /**
         * Snapshot a copy of the CPUState.
         *
         * @param s The CPUState to copy.
         */
        public CPUState(CPUState s) {
            this.a = s.a;
            this.x = s.x;
            this.y = s.y;
            this.sp = s.sp;
            this.pc = s.pc;
            this.ir = s.ir;
            this.lastPc = s.lastPc;
            this.args[0] = s.args[0];
            this.args[1] = s.args[1];
            this.instSize = s.instSize;
            this.carryFlag = s.carryFlag;
            this.negativeFlag = s.negativeFlag;
            this.zeroFlag = s.zeroFlag;
            this.irqDisableFlag = s.irqDisableFlag;
            this.decimalModeFlag = s.decimalModeFlag;
            this.overflowFlag = s.overflowFlag;
            this.stepCounter = s.stepCounter;
        }

        /**
         * Returns a string formatted for the trace log.
         *
         * @return a string formatted for the trace log.
         */
        public String toTraceEvent() {
            final String opcode = disassembleOp();
            final StringBuilder sb = new StringBuilder(getInstructionByteStatus());
            
            sb.append("  ");
            sb.append(String.format("%-24s", opcode)).append(" ");
            sb.append("A:").append(Tools.byteToHex(a)).append(" ");
            sb.append("X:").append(Tools.byteToHex(x)).append(" ");
            sb.append("Y:").append(Tools.byteToHex(y)).append(" ");
            sb.append("P:").append(Tools.byteToHex(getStatusFlag())).append(" ");
            sb.append("S:").append(Tools.byteToHex(sp)).append(" ");
            sb.append(getProcessorStatusString());
            
            return sb.toString();
        }

        /**
         * @return The value of the Process Status Register, as a byte.
         */
        public int getStatusFlag() {
            int status = 0x20;
            
            if (carryFlag) {
                status |= P_CARRY;
            }
            if (zeroFlag) {
                status |= P_ZERO;
            }
            if (irqDisableFlag) {
                status |= P_IRQ_DISABLE;
            }
            if (decimalModeFlag) {
                status |= P_DECIMAL;
            }
            if (overflowFlag) {
                status |= P_OVERFLOW;
            }
            if (negativeFlag) {
                status |= P_NEGATIVE;
            }
            
            return status;
        }

        /**
         * 
         * @return 
         */
        public String getInstructionByteStatus() {
            switch (CPU.instructionSizes[ir]) {
                case 1:
                    return Tools.wordToHex(lastPc) + "  " +
                           Tools.byteToHex(ir) + "      ";
                case 2:
                    return Tools.wordToHex(lastPc) + "  " +
                           Tools.byteToHex(ir) + " " +
                           Tools.byteToHex(args[0]) + "   ";
                case 3:
                    if (ir == 0x60) {
                        return Tools.wordToHex(lastPc) + "  " +
                           Tools.byteToHex(ir) + "      ";
                    }
                    
                    return Tools.wordToHex(lastPc) + "  " +
                           Tools.byteToHex(ir) + " " +
                           Tools.byteToHex(args[0]) + " " +
                           Tools.byteToHex(args[1]);
                default:
                    return null;
            }
        }
        
        /**
         * Given an opcode and its datas, return a formatted name.
         *
         * @return A string representing the mnemonic and datas of the instruction
         */
        public String disassembleOp() {
            final String mnemonic = opcodeNames[ir];
            final StringBuilder sb = new StringBuilder(mnemonic);

            switch (instructionModes[ir]) {
                case ABS_A:
                    switch (mnemonic) {
                        case "LDA":
                        case "LDX":
                        case "LDY":
                            sb.append(" $").append(Tools.wordToHex(address(args[0], args[1]))).append(" = ").append(Tools.byteToHex(lastRead));
                            break;
                        case "JSR":
                            sb.append(" $").append(Tools.wordToHex(address(args[0], args[1])));
                            break;
                        default:
                            sb.append(" $").append(Tools.wordToHex(address(args[0], args[1]))).append(" = ").append(Tools.byteToHex(lastWrite));
                            break;
                    }
                    break;
                case ABS_LC:
                    sb.append(" $").append(Tools.wordToHex(address(args[0], args[1])));
                    break;
                case ABX_R:
                case ABX_W:
                    if ("LDA".equals(mnemonic) || "LDX".equals(mnemonic) || "LDY".equals(mnemonic)) {
                        sb.append(" $").append(Tools.wordToHex(address(args[0], args[1]))).append(",X @ ")
                            .append(Tools.wordToHex(effectiveAddress)).append(" = ").append(Tools.byteToHex(lastRead));
                    } else {
                        sb.append(" $").append(Tools.wordToHex(address(args[0], args[1]))).append(",X @ ")
                            .append(Tools.wordToHex(effectiveAddress)).append(" = ").append(Tools.byteToHex(lastWrite));
                    }
                    break;
                case ABY_R:
                case ABY_W:
                    if ("LDA".equals(mnemonic) || "LDX".equals(mnemonic) || "LDY".equals(mnemonic)) {
                        sb.append(" $").append(Tools.wordToHex(address(args[0], args[1]))).append(",Y @ ")
                            .append(Tools.wordToHex(effectiveAddress)).append(" = ").append(Tools.byteToHex(lastRead));
                    } else {
                        sb.append(" $").append(Tools.wordToHex(address(args[0], args[1]))).append(",Y @ ")
                            .append(Tools.wordToHex(effectiveAddress)).append(" = ").append(Tools.byteToHex(lastWrite));
                    }
                    break;
                case IMM_A:
                    sb.append(" #$").append(Tools.byteToHex(args[0]));
                    break;
                case IND_A:
                    sb.append(" ($").append(Tools.wordToHex(address(args[0], args[1]))).append(")");
                    break;
                case INX_A:
                    if ("LDA".equals(mnemonic) || "LDX".equals(mnemonic) || "LDY".equals(mnemonic)) {
                        sb.append(" ($").append(Tools.byteToHex(args[0])).append(",X) @ ").append(Tools.wordToHex(effectiveAddress))
                                .append(" = ").append(Tools.byteToHex(lastRead));
                    } else {
                        sb.append(" ($").append(Tools.byteToHex(args[0])).append(",X) @ ").append(Tools.wordToHex(effectiveAddress))
                                .append(" = ").append(Tools.byteToHex(lastWrite));
                    }
                    break;
                case INY_R:
                case INY_W:
                    if ("LDA".equals(mnemonic) || "LDX".equals(mnemonic) || "LDY".equals(mnemonic)) {
                        sb.append(" ($").append(Tools.byteToHex(args[0])).append("),Y @ ").append(Tools.wordToHex(effectiveAddress))
                                .append(" = ").append(Tools.byteToHex(lastRead));
                    } else {
                        sb.append(" ($").append(Tools.byteToHex(args[0])).append("),Y @ ").append(Tools.wordToHex(effectiveAddress))
                                .append(" = ").append(Tools.byteToHex(lastWrite));
                    }
                    break;
                case IMP_A:
                    break;
                case IMP_LC:
                    if (ir == 0x0a || ir == 0x2a || ir == 0x4a || ir == 0x6a) {
                        sb.append(" A");
                    }
                    break;
                case REL_A:
                    sb.append(" $").append(Tools.wordToHex(state.pc));
                    break;
                case ZPG_A:
                    sb.append(" $").append(Tools.byteToHex(args[0])).append(" = ").append(Tools.byteToHex(lastWrite));
                    break;
                case ZPX_A:
                    if ("LDA".equals(mnemonic) || "LDX".equals(mnemonic) || "LDY".equals(mnemonic)) {
                        sb.append(" $").append(Tools.byteToHex(args[0])).append(",X @ ").append(Tools.byteToHex(effectiveAddress))
                                .append(" = ").append(Tools.byteToHex(lastRead));
                    } else {
                        sb.append(" $").append(Tools.byteToHex(args[0])).append(",X @ ").append(Tools.byteToHex(effectiveAddress))
                                .append(" = ").append(Tools.byteToHex(lastWrite));
                    }
                    break;
                case ZPY_A:
                    sb.append(" $").append(Tools.byteToHex(args[0])).append(",Y");
                    break;
                default:
                    break;
            }

            return sb.toString();
        }

        /**
         * Given two bytes, return an address.
         */
        private int address(int lowByte, int hiByte) {
            return ((hiByte << 8) | lowByte) & 0xffff;
        }


        /**
         * @return A string representing the current status register state.
         */
        public String getProcessorStatusString() {
            final StringBuilder sb = new StringBuilder("[");
            
            sb.append(negativeFlag ? 'N' : '.');    // Bit 7
            sb.append(overflowFlag ? 'V' : '.');    // Bit 6
            sb.append("-");                         // Bit 5 (always 1)
            sb.append(".");                         // Bit 4
            sb.append(decimalModeFlag ? 'D' : '.'); // Bit 3
            sb.append(irqDisableFlag ? 'I' : '.');  // Bit 2
            sb.append(zeroFlag ? 'Z' : '.');        // Bit 1
            sb.append(carryFlag ? 'C' : '.');       // Bit 0
            sb.append("]");
            
            return sb.toString();
        }
    }
}