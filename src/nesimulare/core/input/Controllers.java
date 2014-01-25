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

package nesimulare.core.input;

import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class Controllers {
    public Joypad joypad1, joypad2;
    public Zapper zapper;
    public boolean zapperConnected = false;
    
    public Controllers() {
        zapper = new Zapper();
    }
    
    
    public int read(int address) {
        int data;
        
        switch (address) {
            case 0x4016:
                joypad1.strobe();
                data = joypad1.getbyte() | (address >> 8 & 0xE0);
                return data;
                
            case 0x4017:
                joypad2.strobe();
                data = joypad2.getbyte() | (address >> 8 & 0xE0);
                
                if (zapperConnected) {
                    data |= zapper.getTrigger() ? 0x08 : 0x00;
                    data |= zapper.getLightDetected() ? 0x10 : 0x00;
                }
                
                return data;
                
            default:
                return 0x40;
        }
    }
    
    public void write(int data) {
        joypad1.output(Tools.getbit(data, 0));
        joypad2.output(Tools.getbit(data, 0));
    }
}