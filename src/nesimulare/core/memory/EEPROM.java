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
package nesimulare.core.memory;

import nesimulare.gui.Tools;

/**
 *
 * @author Parseus
 */
public class EEPROM extends Memory {
    private enum eepromDevice {
        X24C01, X24C02
    };

    private enum eepromMode {
        OFF, SELECT, IGNORE, ADDRESS, READ, WRITE
    }

    private final eepromDevice device;
    private eepromMode mode = eepromMode.OFF;

    public final int[] rom;
    private int address = 0;
    private int bitsLeft = 0;
    private int data = 0;

    private boolean outEnable;
    private boolean pullDown;
    private boolean SCL;
    private boolean SDA;

    public EEPROM(int size) {
        super(size);

        rom = new int[size];
        device = (size == 256) ? eepromDevice.X24C02 : eepromDevice.X24C01;
    }

    @Override
    public void hardReset() {
        mode = eepromMode.OFF;

        address = 0;
        bitsLeft = 0;
        data = 0;

        outEnable = false;
        pullDown = false;
        SCL = false;
        SDA = false;
    }

    public boolean read(boolean deadBit) {
        if (!outEnable) {
            return deadBit;
        } else if (!SDA) {
            return false;
        }

        return !pullDown;
    }

    public void write(int data) {
        outEnable = Tools.getbit(data, 7);
        final boolean newSDA = Tools.getbit(data, 6);
        final boolean newSCL = Tools.getbit(data, 5);
        
        if (!newSCL) {
            pullDown = false;
        } else {
            if (!SCL) {
                clockBit(newSDA);
            } else {
                if (!SDA && newSDA) {
                    stop();
                } else if (SDA && !newSDA) {
                    start();
                }
            }
        }
        
        SCL = newSCL;
        SDA = newSDA;
    }

    private void start() {
        mode = eepromMode.SELECT;
        bitsLeft = 8;
    }

    private void stop() {
        mode = eepromMode.OFF;
        pullDown = false;
    }

    private void clockBit(boolean bit) {
        switch (mode) {
            case OFF:
            case IGNORE:
                break;
            case ADDRESS:
            case SELECT:
            case WRITE:
                if (bitsLeft > 0) {
                    bitsLeft--;

                    if (bit) {
                        data |= (1 << bitsLeft);
                    } else {
                        data &= (1 << bitsLeft);
                    }
                } else {
                    clockWrite();
                }
                break;
            case READ:
                if (bitsLeft > 0) {
                    bitsLeft--;
                    pullDown = !Tools.getbit(data, 0);
                } else {
                    if (bit) {
                        pullDown = false;
                    }
                    
                    address = (address + 1) & mask;
                    rom[address] = data;
                    bitsLeft = 8;
                }
                break;
            default:
                break;
        }
    }

    private void clockWrite() {
        if (mode == eepromMode.WRITE) {
            pullDown = true;
            rom[address] = data;
            address = (address + 1) & mask;
            bitsLeft = 8;
        } else if (mode == eepromMode.SELECT) {
            if (device == eepromDevice.X24C02) {
                if ((data & 0xFE) != 0xA0) {
                    mode = eepromMode.IGNORE;
                } else {
                    if (Tools.getbit(data, 0)) {
                        pullDown = true;
                        mode = eepromMode.READ;
                        bitsLeft = 8;
                        data = rom[address];
                    } else {
                        pullDown = true;
                        mode = eepromMode.ADDRESS;
                        bitsLeft = 8;
                    }
                }
            } else {
                address = (data >> 1) & 0xFF;

                if (Tools.getbit(data, 0)) {
                    pullDown = true;
                    mode = eepromMode.READ;
                    bitsLeft = 8;
                    data = rom[address];
                } else {
                    pullDown = true;
                    mode = eepromMode.WRITE;
                    bitsLeft = 8;
                }
            }
        } else if (mode == eepromMode.ADDRESS) {
            address = data;
            pullDown = true;
            mode = eepromMode.WRITE;
            bitsLeft = 8;
        }
    }
}