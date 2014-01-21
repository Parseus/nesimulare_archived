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

/**
 *
 * @author Parseus
 */
public interface Opcodes {
    
    /**
     * Adressing modes.
     */
    public enum AddressMode {       
        ABS_A {
            @Override
            public String toString() {
                return "Absolute";
            }
        },
        
        ABS_LC {
            @Override
            public String toString() {
                return "Absolute";
            }
        },
        
        ABX_R {
            @Override
            public String toString() {
                return "Absolute, X-indexed";
            }
        },
        
        ABX_W {
            @Override
            public String toString() {
                return "Absolute, X-indexed";
            }
        },
        
        ABY_R {
            @Override
            public String toString() {
                return "Absolute, Y-indexed";
            }
        },
        
        ABY_W {
            @Override
            public String toString() {
                return "Absolute, Y-indexed";
            }
        },
        
        IMM_A {
            @Override
            public String toString(){
                return "Immediate";
            }
        },
        
        IMP_A {
            @Override
            public String toString(){
                return "Implied";
            }
        },
        
        IMP_LC {
            @Override
            public String toString(){
                return "Accumulator";
            }
        },
        
        IND_A {
            @Override
            public String toString(){
                return "Indirect";
            }
        },
        
        INX_A {
            @Override
            public String toString(){
                return "Indirect, X-indexed";
            }
        },
        
        INY_R {
            @Override
            public String toString(){
                return "Indirect, Y-indexed";
            }
        },
        
        INY_W {
            @Override
            public String toString(){
                return "Indirect, Y-indexed";
            }
        },
        
        REL_A {
            @Override
            public String toString(){
                return "Relative";
            }
        },
        
        ZPG_A {
            @Override
            public String toString(){
                return "Zeropage";
            }
        },
        
        ZPX_A {
            @Override
            public String toString(){
                return "Zeropage, X-indexed";
            }
        },
        
        ZPY_A {
            @Override
            public String toString(){
                return "Zeropage, Y-indexed";
            }
        },
    }
    
    /**
     * Instruction opcode names.
     */
    public static final String[] opcodeNames =  {
        "BRK", "ORA", "KIL", "SLO", "NOP", "ORA", "ASL", "SLO",
        "PHP", "ORA", "ASL", "ANC", "NOP", "ORA", "ASL", "SLO",
        "BPL", "ORA", "KIL", "SLO", "NOP", "ORA", "ASL", "SLO",
        "CLC", "ORA", "NOP", "SLO", "NOP", "ORA", "ASL", "SLO",
        "JSR", "AND", "KIL", "RLA", "BIT", "AND", "ROL", "RLA",
        "PLP", "AND", "ROL", "ANC", "BIT", "AND", "ROL", "RLA",
        "BMI", "AND", "KIL", "RLA", "NOP", "AND", "ROL", "RLA",
        "SEC", "AND", "NOP", "RLA", "NOP", "AND", "ROL", "RLA",
        "RTI", "EOR", "KIL", "SRE", "NOP", "EOR", "LSR", "SRE",
        "PHA", "EOR", "LSR", "ALR", "JMP", "EOR", "LSR", "SRE",
        "BVC", "EOR", "KIL", "SRE", "NOP", "EOR", "LSR", "SRE",
        "CLI", "EOR", "NOP", "SRE", "NOP", "EOR", "LSR", "SRE",
        "RTS", "ADC", "KIL", "RRA", "NOP", "ADC", "ROR", "RRA",
        "PLA", "ADC", "ROR", "ARR", "JMP", "ADC", "ROR", "RRA",
        "BVS", "ADC", "KIL", "RRA", "NOP", "ADC", "ROR", "RRA",
        "SEI", "ADC", "NOP", "RRA", "NOP", "ADC", "ROR", "RRA",
        "NOP", "STA", "NOP", "SAX", "STY", "STA", "STX", "SAX",
        "DEY", "NOP", "TXA", "XAA", "STY", "STA", "STX", "SAX",
        "BCC", "STA", "KIL", "AHX", "STY", "STA", "STX", "SAX",
        "TYA", "STA", "TXS", "TAS", "SHY", "STA", "SHX", "AHX",
        "LDY", "LDA", "LDX", "LAX", "LDY", "LDA", "LDX", "LAX",
        "TAY", "LDA", "TAX", "LAX", "LDY", "LDA", "LDX", "LAX",
        "BCS", "LDA", "KIL", "LAX", "LDY", "LDA", "LDX", "LAX",
        "CLV", "LDA", "TSX", "LAS", "LDY", "LDA", "LDX", "LAX",
        "CPY", "CMP", "NOP", "DCP", "CPY", "CMP", "DEC", "DCP",
        "INY", "CMP", "DEX", "AXS", "CPY", "CMP", "DEC", "DCP",
        "BNE", "CMP", "KIL", "DCP", "NOP", "CMP", "DEC", "DCP",
        "CLD", "CMP", "NOP", "DCP", "NOP", "CMP", "DEC", "DCP",
        "CPX", "SBC", "NOP", "ISC", "CPX", "SBC", "INC", "ISC",
        "INX", "SBC", "NOP", "SBC", "CPX", "SBC", "INC", "ISC",
        "BEQ", "SBC", "KIL", "ISC", "NOP", "SBC", "INC", "ISC",
        "SED", "SBC", "NOP", "ISC", "NOP", "SBC", "INC", "ISC"
    };
    
    /**
     * Instruction addressing modes.
     */
    public static final AddressMode[] instructionModes = {
        AddressMode.IMM_A,  AddressMode.INX_A, AddressMode.IMP_A,  AddressMode.INX_A,   // 0x00-0x03
        AddressMode.ZPG_A,  AddressMode.ZPG_A, AddressMode.ZPG_A,  AddressMode.ZPG_A,   // 0x04-0x07
        AddressMode.IMP_A,  AddressMode.IMM_A, AddressMode.IMP_LC, AddressMode.IMM_A,   // 0x08-0x0b
        AddressMode.ABS_A,  AddressMode.ABS_A, AddressMode.ABS_A,  AddressMode.ABS_A,   // 0x0c-0x0f
        AddressMode.REL_A,  AddressMode.INY_R, AddressMode.IMP_A,  AddressMode.INY_W,   // 0x10-0x13
        AddressMode.ZPX_A,  AddressMode.ZPX_A, AddressMode.ZPX_A,  AddressMode.ZPX_A,   // 0x14-0x17
        AddressMode.IMP_LC, AddressMode.ABY_R, AddressMode.IMP_LC, AddressMode.ABY_W,   // 0x18-0x1b
        AddressMode.ABX_R,  AddressMode.ABX_R, AddressMode.ABX_W,  AddressMode.ABX_W,   // 0x1c-0x1f
        AddressMode.ABS_A,  AddressMode.INX_A, AddressMode.IMP_A,  AddressMode.INX_A,   // 0x20-0x23
        AddressMode.ZPG_A,  AddressMode.ZPG_A, AddressMode.ZPG_A,  AddressMode.ZPG_A,   // 0x24-0x27
        AddressMode.IMP_A,  AddressMode.IMM_A, AddressMode.IMP_LC, AddressMode.IMM_A,   // 0x28-0x2b
        AddressMode.ABS_A,  AddressMode.ABS_A, AddressMode.ABS_A,  AddressMode.ABS_A,   // 0x2c-0x2f
        AddressMode.REL_A,  AddressMode.INY_R, AddressMode.IMP_A,  AddressMode.INY_W,   // 0x30-0x33
        AddressMode.ZPX_A,  AddressMode.ZPX_A, AddressMode.ZPX_A,  AddressMode.ZPX_A,   // 0x34-0x37
        AddressMode.IMP_LC, AddressMode.ABY_R, AddressMode.IMP_LC, AddressMode.ABY_W,   // 0x38-0x3b
        AddressMode.ABX_R,  AddressMode.ABX_R, AddressMode.ABX_W,  AddressMode.ABX_W,   // 0x3c-0x3f
        AddressMode.IMP_A,  AddressMode.INX_A, AddressMode.IMP_A,  AddressMode.INX_A,   // 0x40-0x43
        AddressMode.ZPG_A,  AddressMode.ZPG_A, AddressMode.ZPG_A,  AddressMode.ZPG_A,   // 0x44-0x47
        AddressMode.IMP_A,  AddressMode.IMM_A, AddressMode.IMP_LC, AddressMode.IMM_A,   // 0x48-0x4b
        AddressMode.ABS_LC, AddressMode.ABS_A, AddressMode.ABS_A,  AddressMode.ABS_A,   // 0x4c-0x4f
        AddressMode.REL_A,  AddressMode.INY_R, AddressMode.IMP_A,  AddressMode.INY_W,   // 0x50-0x53
        AddressMode.ZPX_A,  AddressMode.ZPX_A, AddressMode.ZPX_A,  AddressMode.ZPX_A,   // 0x54-0x57
        AddressMode.IMP_LC, AddressMode.ABY_R, AddressMode.IMP_LC, AddressMode.ABY_W,   // 0x58-0x5b
        AddressMode.ABX_R,  AddressMode.ABX_R, AddressMode.ABX_W,  AddressMode.ABX_W,   // 0x5c-0x5f
        AddressMode.IMP_A,  AddressMode.INX_A, AddressMode.IMP_A,  AddressMode.INX_A,   // 0x60-0x63
        AddressMode.ZPG_A,  AddressMode.ZPG_A, AddressMode.ZPG_A,  AddressMode.ZPG_A,   // 0x64-0x67
        AddressMode.IMP_A,  AddressMode.IMM_A, AddressMode.IMP_LC, AddressMode.IMM_A,   // 0x68-0x6b
        AddressMode.IND_A,  AddressMode.ABS_A, AddressMode.ABS_A,  AddressMode.ABS_A,   // 0x6c-0x6f
        AddressMode.REL_A,  AddressMode.INY_R, AddressMode.IMP_A,  AddressMode.INY_W,   // 0x70-0x73
        AddressMode.ZPX_A,  AddressMode.ZPX_A, AddressMode.ZPX_A,  AddressMode.ZPX_A,   // 0x74-0x77
        AddressMode.IMP_LC, AddressMode.ABY_R, AddressMode.IMP_LC, AddressMode.ABY_W,   // 0x78-0x7b
        AddressMode.ABX_R,  AddressMode.ABX_R, AddressMode.ABX_W,  AddressMode.ABX_W,   // 0x7c-0x7f
        AddressMode.IMM_A,  AddressMode.INX_A, AddressMode.IMM_A,  AddressMode.INX_A,   // 0x80-0x83
        AddressMode.ZPG_A,  AddressMode.ZPG_A, AddressMode.ZPG_A,  AddressMode.ZPG_A,   // 0x84-0x87
        AddressMode.IMP_LC, AddressMode.IMM_A, AddressMode.IMP_LC, AddressMode.IMM_A,   // 0x88-0x8b
        AddressMode.ABS_A,  AddressMode.ABS_A, AddressMode.ABS_A,  AddressMode.ABS_A,   // 0x8c-0x8f
        AddressMode.REL_A,  AddressMode.INY_W, AddressMode.IMP_A,  AddressMode.INY_W,   // 0x90-0x93
        AddressMode.ZPX_A,  AddressMode.ZPX_A, AddressMode.ZPY_A,  AddressMode.ZPY_A,   // 0x94-0x97
        AddressMode.IMP_LC, AddressMode.ABY_W, AddressMode.IMP_LC, AddressMode.ABY_W,   // 0x98-0x9b
        AddressMode.ABS_A,  AddressMode.ABX_W, AddressMode.ABS_A,  AddressMode.ABY_W,   // 0x9c-0x9f
        AddressMode.IMM_A,  AddressMode.INX_A, AddressMode.IMM_A,  AddressMode.INX_A,   // 0xa0-0xa3
        AddressMode.ZPG_A,  AddressMode.ZPG_A, AddressMode.ZPG_A,  AddressMode.ZPG_A,   // 0xa4-0xa7
        AddressMode.IMP_LC, AddressMode.IMM_A, AddressMode.IMP_LC, AddressMode.IMM_A,   // 0xa8-0xab
        AddressMode.ABS_A,  AddressMode.ABS_A, AddressMode.ABS_A,  AddressMode.ABS_A,   // 0xac-0xaf
        AddressMode.REL_A,  AddressMode.INY_R, AddressMode.IMP_A,  AddressMode.INY_R,   // 0xb0-0xb3
        AddressMode.ZPX_A,  AddressMode.ZPX_A, AddressMode.ZPY_A,  AddressMode.ZPY_A,   // 0xb4-0xb7
        AddressMode.IMP_LC, AddressMode.ABY_R, AddressMode.IMP_LC, AddressMode.ABY_R,   // 0xb8-0xbb
        AddressMode.ABX_R,  AddressMode.ABX_R, AddressMode.ABY_R,  AddressMode.ABY_R,   // 0xbc-0xbf
        AddressMode.IMM_A,  AddressMode.INX_A, AddressMode.IMM_A,  AddressMode.INX_A,   // 0xc0-0xc3
        AddressMode.ZPG_A,  AddressMode.ZPG_A, AddressMode.ZPG_A,  AddressMode.ZPG_A,   // 0xc4-0xc7
        AddressMode.IMP_LC, AddressMode.IMM_A, AddressMode.IMP_LC, AddressMode.IMM_A,   // 0xc8-0xcb
        AddressMode.ABS_A,  AddressMode.ABS_A, AddressMode.ABS_A,  AddressMode.ABS_A,   // 0xcc-0xcf
        AddressMode.REL_A,  AddressMode.INY_R, AddressMode.IMP_A,  AddressMode.INY_W,   // 0xd0-0xd3
        AddressMode.ZPX_A,  AddressMode.ZPX_A, AddressMode.ZPX_A,  AddressMode.ZPX_A,   // 0xd4-0xd7
        AddressMode.IMP_LC, AddressMode.ABY_R, AddressMode.IMP_LC, AddressMode.ABY_W,   // 0xd8-0xdb
        AddressMode.ABX_R,  AddressMode.ABX_R, AddressMode.ABX_W,  AddressMode.ABX_W,   // 0xdc-0xdf
        AddressMode.IMM_A,  AddressMode.INX_A, AddressMode.IMM_A,  AddressMode.INX_A,   // 0xe0-0xe3
        AddressMode.ZPG_A,  AddressMode.ZPG_A, AddressMode.ZPG_A,  AddressMode.ZPG_A,   // 0xe4-0xe7
        AddressMode.IMP_LC, AddressMode.IMM_A, AddressMode.IMP_LC, AddressMode.IMM_A,   // 0xe8-0xeb
        AddressMode.ABS_A,  AddressMode.ABS_A, AddressMode.ABS_A,  AddressMode.ABS_A,   // 0xec-0xef
        AddressMode.REL_A,  AddressMode.INY_R, AddressMode.IMP_A,  AddressMode.INY_W,   // 0xf0-0xf3
        AddressMode.ZPX_A,  AddressMode.ZPX_A, AddressMode.ZPX_A,  AddressMode.ZPX_A,   // 0xf4-0xf7
        AddressMode.IMP_LC, AddressMode.ABY_R, AddressMode.IMP_LC, AddressMode.ABY_W,   // 0xf8-0xfb
        AddressMode.ABX_R,  AddressMode.ABX_R, AddressMode.ABX_W,  AddressMode.ABX_W    // 0xfc-0xff
    };
    
    /**
     * Size, in bytes, required for each instruction.
     */
    public static final int[] instructionSizes = {
        2, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,     //0x00 - 0x0F
        2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,     //0x10 - 0x1F
        3, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,     //0x20 - 0x2F
        2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,     //0x30 - 0x3F
        2, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,     //0x40 - 0x4F
        2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,     //0x50 - 0x5F
        3, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,     //0x60 - 0x6F
        2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,     //0x70 - 0x7F
        2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,     //0x80 - 0x8F
        2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,     //0x90 - 0x9F
        2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,     //0xA0 - 0xAF 
        2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,     //0xB0 - 0xBF
        2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,     //0xC0 - 0xCF
        2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,     //0xD0 - 0xDF
        2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,     //0xE0 - 0xEF
        2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3      //0xF0 - 0xFF
    };
}