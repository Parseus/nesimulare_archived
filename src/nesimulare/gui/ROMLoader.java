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
package nesimulare.gui;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import nesimulare.core.Region;
import nesimulare.core.boards.*;
import nesimulare.core.boards.SNROM.SOROM;
import nesimulare.core.boards.SNROM.SUROM;
import nesimulare.core.boards.SNROM.SXROM;
import nesimulare.core.memory.PPUMemory;

/**
 * Class loading ROM files.
 * 
 * @author Parseus
 */
public class ROMLoader {
    private final GUIImpl gui;
    public Board board;
    private final String filename;
    private final int[] rom;
    private int[] header;
    public String sha1;

    public int prgromSize;
    public int chrromSize;
    public int prgramSize;
    public int chrramSize;
    private int prgOffset;
    private int chrOffset;

    public PPUMemory.Mirroring mirroring;
    public int mapperNumber;
    public int submapper;
    private int tvmode;
    private int vssystem;
    public int inesVersion = 1;
    private boolean hasTrainer = false;
    public boolean savesram = false;
    private boolean haschrram = false;

    /**
     * Constructor for this class.
     * 
     * @param filename      ROM filename
     * @param gui           GUI loading the ROM
     */
    public ROMLoader(String filename, GUIImpl gui) {
        this.gui = gui;
        rom = Tools.readfromfile(filename);
        this.filename = filename;
        sha1 = calculateHash(filename);
    }

    /**
     * Reads a file header.
     * 
     * @param len       Header length
     */
    private void readHeader(int len) {
        header = new int[len];
        System.arraycopy(rom, 0, header, 0, len);
    }

    /**
     * Checks if a ROM contains SRAM
     * 
     * @return      True: ROM contains SRAM
     *              False: ROM doesn't contain SRAM
     */
    public final boolean hasSRAM() {
        return savesram;
    }

    /**
     * Calculates a SHA-1 hash of ROM (without a header).
     * 
     * @param fileName      ROM filename
     * @return              SHA-1 hash
     */
    public final String calculateHash(String fileName) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA1");

            try (FileInputStream fis = new FileInputStream(fileName)) {
                fis.skip(16);
                final byte[] dataBytes = new byte[1024];

                int nread;

                while ((nread = fis.read(dataBytes)) != -1) {
                    md.update(dataBytes, 0, nread);
                }
            }

            final byte[] mdbytes = md.digest();
            final StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException ex) {
            gui.messageBox(ex.getMessage());
            return null;
        }
    }

    /**
     * Loads ROM.
     * 
     * @return      Mapper used by ROM
     */
    public Board loadROM() {
        board = loadINESFile();
        
        return board;
    }
    
    /**
     * Parses and loads a file with an iNES header.
     * 
     * @return 
     */
    private Board loadINESFile() {
        new INESParser();

        if (inesVersion == 2) {
            haschrram = (header[11] != 0);
        } else {
            haschrram = (chrromSize == 0);
            
            if (haschrram) {
                chrramSize = chrromSize = 0x2000;
            }
        }

        final int[] prgrom = new int[prgromSize];
        final int[] chrrom = new int[chrromSize];
        int[] trainer = null;

        if (hasTrainer) {
            trainer = new int[512];
            System.arraycopy(rom, 16, trainer, 0, 512);
            System.arraycopy(rom, 16 + 512 + prgOffset, prgrom, 0, prgromSize);
            if (!haschrram) {
                System.arraycopy(rom, 16 + 512 + chrOffset, chrrom, 0, chrromSize);
            }
        } else {
            System.arraycopy(rom, 16 + prgOffset, prgrom, 0, prgromSize);
            if (!haschrram) {
                System.arraycopy(rom, 16 + chrOffset, chrrom, 0, chrromSize);
            }
        }

        switch (mapperNumber) {
            case 0:
                return new NROM(prgrom, chrrom, trainer, haschrram);
            case 1:
                if (chrromSize <= 8192) {
                    return new SXROM(prgrom, chrrom, trainer, haschrram);
                } else if (header[4] >= 32) {
                    return new SUROM(prgrom, chrrom, trainer, haschrram);
                } else if (header[4] >= 16) {
                    return new SOROM(prgrom, chrrom, trainer, haschrram);
                } else {
                    return new SNROM(prgrom, chrrom, trainer, haschrram);
                }
            case 2:
                return new UxROM(prgrom, chrrom, trainer, haschrram);
            case 3:
                return new CNROM(prgrom, chrrom, trainer, haschrram);
            case 4:
                return new TxROM(prgrom, chrrom, trainer, haschrram);
            case 5:
                return new ExROM(prgrom, chrrom, trainer, haschrram);
            case 7:
                return new AxROM(prgrom, chrrom, trainer, haschrram);
            case 9:
                return new PxROM(prgrom, chrrom, trainer, haschrram);
            case 10:
                return new FxROM(prgrom, chrrom, trainer, haschrram);
            case 11:
                return new ColorDreams(prgrom, chrrom, trainer, haschrram);
            case 13:
                return new CPROM(prgrom, chrrom, trainer, haschrram);
            case 15:
            case 169:
                return new Mapper015(prgrom, chrrom, trainer, haschrram);
            case 16:
                return new Bandai_FCG(prgrom, chrrom, trainer, haschrram);
            case 18:
                return new JalecoSS88006(prgrom, chrrom, trainer, haschrram);
            case 19:
                return new Namco163(prgrom, chrrom, trainer, haschrram);
            case 22:
            case 23:
                return new VRC2(prgrom, chrrom, trainer, haschrram);
            case 24:
            case 26:
                return new VRC6(prgrom, chrrom, trainer, haschrram);
            case 29:
                return new Mapper029(prgrom, chrrom, trainer, haschrram);
            case 32:
                return new IremG101(prgrom, chrrom, trainer, haschrram);
            case 33:
            case 48:
                return new Taito_TC0190FMC(prgrom, chrrom, trainer, haschrram);
            case 34:
                if (haschrram && chrromSize <= 0x2000) {
                    return new BxROM(prgrom, chrrom, trainer, haschrram);
                } else {
                    return new AVE_NINA_01(prgrom, chrrom, trainer, haschrram);
                }
            case 37:
                return new NES_ZZ(prgrom, chrrom, trainer, haschrram);
            case 41:
                return new MLT_Caltron(prgrom, chrrom, trainer, haschrram);
            case 46:
                return new Mapper046(prgrom, chrrom, trainer, haschrram);
            case 47:
                return new NES_QJ(prgrom, chrrom, trainer, haschrram);
            case 50:
                return new Mapper050(prgrom, chrrom, trainer, haschrram);
            case 52:
                return new Mapper052(prgrom, chrrom, trainer, haschrram);
            case 58:
                return new Mapper058(prgrom, chrrom, trainer, haschrram);
            case 60:
                return new Mapper060(prgrom, chrrom, trainer, haschrram);
            case 61:
                return new Mapper061(prgrom, chrrom, trainer, haschrram);
            case 62:
                return new Mapper062(prgrom, chrrom, trainer, haschrram);
            case 64:
                return new Tengen_800032(prgrom, chrrom, trainer, haschrram);
            case 65:
                return new IremH3001(prgrom, chrrom, trainer, haschrram);
            case 66:
                return new GxROM(prgrom, chrrom, trainer, haschrram);
            case 67:
                return new Sunsoft3(prgrom, chrrom, trainer, haschrram);
            case 68:
                return new Sunsoft4(prgrom, chrrom, trainer, haschrram);
            case 69:
                return new Sunsoft_FME7(prgrom, chrrom, trainer, haschrram);
            case 70:
                return new BANDAI_74_161_161_32(prgrom, chrrom, trainer, haschrram);
            case 71:
                return new Camerica(prgrom, chrrom, trainer, haschrram);
            case 72:
                return new Jaleco_JF_17(prgrom, chrrom, trainer, haschrram);
            case 73:
                return new VRC3(prgrom, chrrom, trainer, haschrram);
            case 75:
                return new VRC1(prgrom, chrrom, trainer, haschrram);
            case 76:
                return new Namco3446(prgrom, chrrom, trainer, haschrram);
            case 77:
                return new IREM_74_161_161_21_138(prgrom, chrrom, trainer, haschrram);
            case 78:
                return new Jaleco_JF_16(prgrom, chrrom, trainer, haschrram);
            case 79:
            case 113:
                return new AVE_NINA_03_06(prgrom, chrrom, trainer, haschrram);
            case 80:
                return new Taito_X1_005(prgrom, chrrom, trainer, haschrram);
            case 82:
                return new Taito_X1_017(prgrom, chrrom, trainer, haschrram);
            case 85:
                return new VRC7(prgrom, chrrom, trainer, haschrram);
            case 86:
                return new Jaleco_JF_13(prgrom, chrrom, trainer, haschrram);
            case 87:
                return new Jaleco_JF_0x_10(prgrom, chrrom, trainer, haschrram);
            case 88:
                return new Namco3433_3443(prgrom, chrrom, trainer, haschrram);
            case 89:
                return new Sunsoft_2_3(prgrom, chrrom, trainer, haschrram);
            case 91:
                return new Mapper091(prgrom, chrrom, trainer, haschrram);
            case 92:
                return new Jaleco_JF_19(prgrom, chrrom, trainer, haschrram);
            case 93:
                return new Sunsoft_2_3R(prgrom, chrrom, trainer, haschrram);
            case 94:
                return new UN1ROM(prgrom, chrrom, trainer, haschrram);
            case 95:
                return new Namco3425(prgrom, chrrom, trainer, haschrram);
            case 96:
                return new BANDAI_74_161_02_74(prgrom, chrrom, trainer, haschrram);
            case 97:
                return new Irem_TAM_S1(prgrom, chrrom, trainer, haschrram);
            case 105:
                return new NES_EVENT(prgrom, chrrom, trainer, haschrram);
            case 112:
                return new Mapper112(prgrom, chrrom, trainer, haschrram);
            case 118:
                return new TxSROM(prgrom, chrrom, trainer, haschrram);
            case 119:
                return new TQROM(prgrom, chrrom, trainer, haschrram);
            case 133:
                return new Mapper133(prgrom, chrrom, trainer, haschrram);
            case 140:
                return new Jaleco_JF_11_14(prgrom, chrrom, trainer, haschrram);
            case 142:
                return new Mapper142(prgrom, chrrom, trainer, haschrram);
            case 143:
                return new Mapper143(prgrom, chrrom, trainer, haschrram);
            case 145:
                return new Mapper145(prgrom, chrrom, trainer, haschrram);
            case 146:
                return new Mapper146(prgrom, chrrom, trainer, haschrram);
            case 147:
                return new Mapper147(prgrom, chrrom, trainer, haschrram);
            case 148:
                return new Mapper148(prgrom, chrrom, trainer, haschrram);
            case 149:
                return new Mapper149(prgrom, chrrom, trainer, haschrram);
            case 151:
                return new Mapper151(prgrom, chrrom, trainer, haschrram);
            case 152:
                return new TAITO_74_161_161_32(prgrom, chrrom, trainer, haschrram);
            case 153:
                return new Mapper153(prgrom, chrrom, trainer, haschrram);
            case 154:
                return new Namco3453(prgrom, chrrom, trainer, haschrram);
            case 159:
                return new Mapper159(prgrom, chrrom, trainer, haschrram);
            case 172:
                return new Mapper172(prgrom, chrrom, trainer, haschrram);
            case 173:
                return new Mapper173(prgrom, chrrom, trainer, haschrram);
            case 174:
                return new Mapper174(prgrom, chrrom, trainer, haschrram);
            case 180:
                return new HVC_UNROM_74HC08(prgrom, chrrom, trainer, haschrram);
            case 182:
                return new Mapper182(prgrom, chrrom, trainer, haschrram);
            case 184:
                return new Sunsoft1(prgrom, chrrom, trainer, haschrram);
            case 185:
                return new Mapper185(prgrom, chrrom, trainer, haschrram);
            case 188:
                return new Mapper188(prgrom, chrrom, trainer, haschrram);
            case 189:
                return new Mapper189(prgrom, chrrom, trainer, haschrram);
            case 191:
                return new Mapper191(prgrom, chrrom, trainer, haschrram);
            case 193:
                return new Mapper193(prgrom, chrrom, trainer, haschrram);
            case 200:
                return new Mapper200(prgrom, chrrom, trainer, haschrram);
            case 201:
                return new Mapper201(prgrom, chrrom, trainer, haschrram);
            case 202:
                return new Mapper202(prgrom, chrrom, trainer, haschrram);
            case 203:
                return new Mapper203(prgrom, chrrom, trainer, haschrram);
            case 204:
                return new Mapper204(prgrom, chrrom, trainer, haschrram);
            case 205:
                return new Mapper205(prgrom, chrrom, trainer, haschrram);
            case 206:
                return new Namco118(prgrom, chrrom, trainer, haschrram);
            case 207:
                return new Mapper207(prgrom, chrrom, trainer, haschrram);
            case 212:
                return new Mapper212(prgrom, chrrom, trainer, haschrram);
            case 213:
                return new Mapper213(prgrom, chrrom, trainer, haschrram);
            case 214:
                return new Mapper214(prgrom, chrrom, trainer, haschrram);
            case 216:
                return new Mapper216(prgrom, chrrom, trainer, haschrram);
            case 228:
                return new MLT_Action52(prgrom, chrrom, trainer, haschrram);
            case 232:
                return new CamericaQuattro(prgrom, chrrom, trainer, haschrram);
            case 234:
                return new MLT_Maxi15(prgrom, chrrom, trainer, haschrram);
            case 240:
                return new Mapper240(prgrom, chrrom, trainer, haschrram);
            case 241:
                return new Mapper241(prgrom, chrrom, trainer, haschrram);
            case 242:
                return new Mapper242(prgrom, chrrom, trainer, haschrram);
            case 243:
                return new Mapper243(prgrom, chrrom, trainer, haschrram);
            case 244:
                return new Mapper244(prgrom, chrrom, trainer, haschrram);
            case 245:
                return new Mapper245(prgrom, chrrom, trainer, haschrram);
            case 246:
                return new Mapper246(prgrom, chrrom, trainer, haschrram);
            case 255:
                return new Mapper255(prgrom, chrrom, trainer, haschrram);
            default:
                gui.messageBox("Couldn't load the ROM file!\nUnsupported mapper: " + mapperNumber);
                return null;
        }
    }

    /**
     * Returns a board name.
     * 
     * @return      Board name
     */
    public String getBoardName() {
        String boardname;
        final StringBuilder sb = new StringBuilder();

        if (gui.nes.board != null) {
            boardname = gui.nes.board.getClass().getSimpleName();

            if (boardname.startsWith("Mapper")) {
                boardname = "";
            }

            sb.append("".equals(boardname) ? "" : " (" + gui.nes.board.getClass().getSimpleName() + ")");
        }

        return sb.toString();
    }

    /**
     * Returns a ROM info.
     * 
     * @return      ROM info.
     */
    public String getrominfo() {
        return ("ROM info:\n"
                + "Filename:     " + filename + "\n"
                + "Mapper:       " + mapperNumber + getBoardName() + "\n"
                + "PRG-ROM Size:     " + prgromSize / 1024 + " kB\n"
                + (savesram ? "PRG-RAM Size:     " + (gui.nes.board.sram.length / 1024) + " kB\n" : "")
                + (haschrram ? "" : "CHR-ROM Size:     " + (chrromSize / 1024) + " kB\n")
                + (!haschrram ? "" : "CHR-RAM Size:     " + (chrramSize / 1024) + " kB\n")
                + "Mirroring:    " + mirroring.toString() + "\n"
                + "SHA-1: " + sha1);
    }

    /**
     * iNES and NES 2.0 parser.
     */
    class INESParser {
        /**
         * Initializes header parsing.
         */
        INESParser() {
            parseInesHeader();
        }   
        
        /**
         * Parses iNES or NES 2.0 header
         */
        public void parseInesHeader() {
            readHeader(16);
        // decode iNES 1.0 headers
            // 1st 4 bytes : $4E $45 $53 $1A
            if (header[0] != 0x4E || header[1] != 0x45 || header[2] != 0x53 || header[3] != 0x1A) {

                // not a valid file
                if (header[0] == 'U') {
                    gui.messageBox("This is a UNIF file with the wrong extension!");
                    return;
                }

                gui.messageBox("Invalid iNES header!");
                return;
            }

            prgromSize = 16384 * header[4];
            chrromSize = 8192 * header[5];
            hasTrainer = Tools.getbit(header[6], 2);
            mapperNumber = ((header[6] & 0xF0) >> 4) | (header[7] & 0xF0);

            savesram = Tools.getbit(header[6], 1);

            switch (header[6] & 0x9) {
                case 0x0:
                    mirroring = PPUMemory.Mirroring.HORIZONTAL;
                    break;
                case 0x1:
                    mirroring = PPUMemory.Mirroring.VERTICAL;
                    break;
                case 0x8:
                case 0x9:
                    mirroring = PPUMemory.Mirroring.FOURSCREEN;
                    break;
                default:
                    break;
            }

            prgramSize = (header[8] == 0) ? 8192 : 8192 * header[8];

            if ((header[7] & 0x0C) == 0x08) {
                //iNES 2.0
                inesVersion = 2;

                mapperNumber |= (header[8] & 0x0F) << 8;
                submapper = (header[8] & 0xF0) << 4;
                prgromSize |= (header[9] & 0x0F) << 8;
                chrromSize |= (header[9] & 0xF0) << 4;
                prgramSize = header[10];
                chrramSize = header[11];
                tvmode = header[12];
                vssystem = header[13];

                gui.nes.setRegion(tvmode == 0 ? Region.NTSC : Region.PAL);

                if (((prgramSize & 0xF) == 0xF) || ((prgramSize & 0xF0) == 0xF0)) {
                    gui.messageBox("Invalid PRG RAM size specified!");
                    return;
                }

                if (((chrramSize & 0xF) == 0xF) || ((chrramSize & 0xF0) == 0xF0)) {
                    gui.messageBox("Invalid CHR RAM size specified!");
                    return;
                }

                if (((chrramSize & 0xF0) != 0)) {
                    gui.messageBox("TODO: Implement battery-backed CHR RAM");
                }

                if (header[14] != 0) {
                    gui.messageBox("Unrecognized data found at header offset 14!");
                    return;
                }

                if (header[15] != 0) {
                    gui.messageBox("Unrecognized data found at header offset 15!");
                    return;
                }
            } else {
                StringBuilder sb = new StringBuilder();
                
                for (int i = 9; i < 16; i++) {
                    if (header[i] != 0) {
                        sb.append(header[i]).append(" ");
                        break;
                    }
                }
                
                if (!sb.toString().isEmpty()) {
                    gui.messageBox("Bytes: " + sb.toString() + "contain invalid data!");
                }
            }

            prgOffset = 0;
            chrOffset = prgromSize;
        }
    }
}