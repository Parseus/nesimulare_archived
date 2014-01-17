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

package nesimulare.gui;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import nesimulare.core.Region;
import nesimulare.core.boards.*;
import nesimulare.core.boards.SxROM.SOROM;
import nesimulare.core.memory.PPUMemory;

public class ROMLoader {
    public GUIImpl gui;
    public Board board;
    public String name;
    public int prgsize;
    public int chrsize;
    public PPUMemory.Mirroring mirroring;
    public int mappertype;
    public int submapper;
    private int prgoff;
    private int chroff;
    public int prgram;
    public int chrram;
    private int tvmode;
    private int vssystem;
    private boolean hasTrainer = false;
    public boolean savesram = false;
    public boolean haschrram = false;
    private final int[] rom;
    private int[] header;
    public String sha1;

    public ROMLoader(String filename, GUIImpl gui) {
        this.gui = gui;
        rom = Tools.readfromfile(filename);
        name = filename;
        sha1 = calculateHash(filename);
    }
    
    private void readHeader(int len) {
        header = new int[len];
        System.arraycopy(rom, 0, header, 0, len);
    }

    public void parseInesHeader() {
        readHeader(16);
        // decode iNES 1.0 headers
        // 1st 4 bytes : $4E $45 $53 $1A
        if (header[0] != 0x4E || header[1] != 0x45 || header[2] != 0x53 || header[3] != 0x1A) {
            
            // not a valid file
            if (header[0] == 'U') {
                gui.messageBox("This is a UNIF file with the wrong extension");
                return;
            }
            
            gui.messageBox("iNES Header Invalid");
        }
        
        prgsize = 16384 * header[4];
        chrsize = 8192 * header[5];
        hasTrainer = Tools.getbit(header[6], 2);
        mappertype = (header[6] >> 4);
        if (header[11] + header[12] + header[13] + header[14]
                    + header[15] == 0) {// fix for DiskDude
                mappertype += ((header[7] >> 4) << 4);
            }
        savesram = Tools.getbit(header[6], 1);
        mirroring = Tools.getbit(header[6], 3) ? PPUMemory.Mirroring.FOURSCREEN : 
                (Tools.getbit(header[6], 0) ? PPUMemory.Mirroring.VERTICAL : PPUMemory.Mirroring.HORIZONTAL);
//        if ((header[7] & 0x0C) == 0x08) {
//            //iNES 2.0            
//            mappertype |= (header[8] & 0x0F) << 8;
//            submapper = (header[8] & 0xF0) << 4;
//            prgsize |= (header[9] & 0x0F) << 8;
//            chrsize |= (header[9] & 0xF0) << 4;
//            prgram |= header[10];
//            chrram |= header[11];
//            tvmode |= header[12];
//            vssystem |= header[13];
//            
//            gui.nes.setRegion(tvmode == 0 ? Region.NTSC : Region.PAL);
//            
//            if (((prgram & 0xF) == 0xF) || ((prgram & 0xF0) == 0xF0)) {
//                //throw new Exception("Invalid PRG RAM size specified!");
//            }
//            if (((chrram & 0xF) == 0xF) || ((chrram & 0xF0) == 0xF0)) {
//                //throw new Exception("Invalid CHR RAM size specified!");
//            }
//            if (((chrram & 0xF0) != 0)) {
//               //throw new Exception("TODO: Implement battery-backed CHR RAM");
//            }
//        } else {
////            for (int i = 8; i < 16; i++) {
////                gui.messageBox("Byte " + i + " contains invalid data!");
////                break;
////            }
//        } 
        
        prgoff = 0;
        chroff = prgsize;
    }

    public final boolean hasSRAM() {
        return savesram;
    }
    
    public final String calculateHash(String fileName) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA1");
            final FileInputStream fis = new FileInputStream(fileName);
            fis.skip(16);
            final byte[] dataBytes = new byte[1024];
            
            int nread;
            
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            
            fis.close();
            
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
    
    public Board loadROM() {
        parseInesHeader();
        
        haschrram = (chrsize == 0);
        
        if (haschrram) {
            if (chrram == 0) {
                chrsize = 0x2000;
            } else {
                chrsize = chrram;
            }
        }
        
        final int[] prg = new int[prgsize];
        final int[] chr = new int[chrsize];
        int[] trainer = null;
        
        if (hasTrainer) {
            trainer = new int[512];
            System.arraycopy(rom, 16, trainer, 0, 512);
            System.arraycopy(rom, 16 + 512 + prgoff, prg, 0, prgsize);
            if (!haschrram) {
                System.arraycopy(rom, 16 + 512 + chroff, chr, 0, chrsize);
            }
        } else {
            System.arraycopy(rom, 16 + prgoff, prg, 0, prgsize);
            if (!haschrram) {
                System.arraycopy(rom, 16 + chroff, chr, 0, chrsize);
            }
        }

        switch (mappertype) {
            case 0:
                return new NROM(prg, chr, trainer, haschrram);
            case 1:
                if (header[4] < 16) {
                    return new SxROM(prg, chr, trainer, haschrram);
                } else if (header[4] >= 16) {
                    return new SOROM(prg, chr, trainer, haschrram);
                }
            case 2:
                return new UxROM(prg, chr, trainer, haschrram);
            case 3:
                return new CNROM(prg, chr, trainer, haschrram);
            case 4:
                return new TxROM(prg, chr, trainer, haschrram);
            case 5:
                return new ExROM(prg, chr, trainer, haschrram);
            case 7:
                return new AxROM(prg, chr, trainer, haschrram);
            case 9:
                return new PxROM(prg, chr, trainer, haschrram);
            case 10:
                return new FxROM(prg, chr, trainer, haschrram);
            case 11:
                return new ColorDreams(prg, chr, trainer, haschrram);
            case 13:
                return new CPROM(prg, chr, trainer, haschrram);
            case 15:
            case 169:
                return new Mapper015(prg, chr, trainer, haschrram);
            case 18:
                return new JalecoSS88006(prg, chr, trainer, haschrram);
            case 19:
                return new Namco163(prg, chr, trainer, haschrram);
            case 22:
            case 23:
                return new VRC2(prg, chr, trainer, haschrram);
            case 24:
            case 26:
                return new VRC6(prg, chr, trainer, haschrram);
            case 32:
                return new IremG101(prg, chr, trainer, haschrram);
            case 33:
            case 48:
                return new Taito_TC0190FMC(prg, chr, trainer, haschrram);
            case 34:
                if (chrsize <= 8192) {
                    return new BxROM(prg, chr, trainer, haschrram);
                } else {
                    return new AVE_NINA_01(prg, chr, trainer, haschrram);
                }
            case 46:
                return new Mapper046(prg, chr, trainer, haschrram);
            case 58:
                return new Mapper058(prg, chr, trainer, haschrram);
            case 60:
                return new Mapper060(prg, chr, trainer, haschrram);
            case 64:
                return new Tengen_800032(prg, chr, trainer, haschrram);
            case 65:
                return new IremH3001(prg, chr, trainer, haschrram);
            case 66:
                return new GxROM(prg, chr, trainer, haschrram);
            case 67:
                return new Sunsoft3(prg, chr, trainer, haschrram);
            case 68:
                return new Sunsoft4(prg, chr, trainer, haschrram);
            case 69:
                return new Sunsoft_FME7(prg, chr, trainer, haschrram);
            case 70:
                return new BANDAI_74_161_161_32(prg, chr, trainer, haschrram);
            case 71:
                return new Camerica(prg, chr, trainer, haschrram);
            case 72:
                return new Jaleco_JF_17(prg, chr, trainer, haschrram);
            case 73:
                return new VRC3(prg, chr, trainer, haschrram);
            case 75:
                return new VRC1(prg, chr, trainer, haschrram);
            case 76:
                return new Namco_3446(prg, chr, trainer, haschrram);
            case 77:
                return new IREM_74_161_161_21_138(prg, chr, trainer, haschrram);
            case 80:
                return new Taito_X1_005(prg, chr, trainer, haschrram);
            case 82:
                return new Taito_X1_017(prg, chr, trainer, haschrram);
            case 86:
                return new Jaleco_JF_13(prg, chr, trainer, haschrram);
            case 87:
                return new Jaleco_JF_0x_10(prg, chr, trainer, haschrram);
            case 92:
                return new Jaleco_JF_19(prg, chr, trainer, haschrram);
            case 93:
                return new Sunsoft_2_3R(prg, chr, trainer, haschrram);
            case 94:
                return new UN1ROM(prg, chr, trainer, haschrram);
            case 96:
                return new BANDAI_74_161_02_74(prg, chr, trainer, haschrram);
            case 97:
                return new Irem_TAM_S1(prg, chr, trainer, haschrram);
            case 118:
                return new TxSROM(prg, chr, trainer, haschrram);
            case 119:
                return new TQROM(prg, chr, trainer, haschrram);
            case 140:
                return new Jaleco_JF_11_14(prg, chr, trainer, haschrram);
            case 152:
                return new TAITO_74_161_161_32(prg, chr, trainer, haschrram);
            case 174:
                return new Mapper174(prg, chr, trainer, haschrram);
            case 180:
                return new HVC_UNROM_74HC08(prg, chr, trainer, haschrram);
            case 184:
                return new Sunsoft1(prg, chr, trainer, haschrram);
            case 185:
                return new Mapper185(prg, chr, trainer, haschrram);
            case 191:
                return new Mapper191(prg, chr, trainer, haschrram);
            case 201:
                return new Mapper201(prg, chr, trainer, haschrram);
            case 202:
                return new Mapper202(prg, chr, trainer, haschrram);
            case 203:
                return new Mapper203(prg, chr, trainer, haschrram);
            case 204:
                return new Mapper204(prg, chr, trainer, haschrram);
            case 205:
                return new Mapper205(prg, chr, trainer, haschrram);
            case 207:
                return new Mapper207(prg, chr, trainer, haschrram);
            case 213:
                return new Mapper213(prg, chr, trainer, haschrram);
            case 228:
                return new MLT_Action52(prg, chr, trainer, haschrram);
            case 232:
                return new CamericaQuattro(prg, chr, trainer, haschrram);
            case 240:
                return new Mapper240(prg, chr, trainer, haschrram);
            case 242:
                return new Mapper242(prg, chr, trainer, haschrram);
            case 246:
                return new Mapper246(prg, chr, trainer, haschrram);
            case 255:
                return new Mapper255(prg, chr, trainer, haschrram);
            default:
                gui.messageBox("Couldn't load the ROM file!\nUnsupported mapper: " + mappertype);
                return null;
        }
    }
    
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
    
    public String getrominfo() {
        return ("ROM info:\n"
                + "Filename:     " + name + "\n"
                + "Mapper:       " + mappertype + getBoardName() + "\n"
                + "PRG-ROM Size:     " + prgsize / 1024 + " kB\n"
                + "CHR-ROM Size:     " + (haschrram ? 0 : chrsize / 1024) + " kB\n"
                + "Mirroring:    " + mirroring.toString() + "\n"
                + "Battery Save: " + ((savesram) ? "Yes\n" : "No\n"))
                + "SHA-1: " + sha1;
    }
}