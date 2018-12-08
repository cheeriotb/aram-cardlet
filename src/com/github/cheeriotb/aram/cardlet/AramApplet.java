/*
 *  Copyright (C) 2018 cheeriotb <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301, USA.
 */

package com.github.cheeriotb.aram.cardlet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class AramApplet extends Applet {
    private static final short DATA_BUFFER_SIZE = 0x100;

    private static final byte TEMPLATE_FCI         = 0x00;
    private static final byte TEMPLATE_FCP         = 0x04;
    private static final byte TEMPLATE_FMD         = 0x08;
    private static final byte TEMPLATE_PROPRIETARY = 0x0C;

    private static final byte[] SELECT_RESPONSE_FCI = {
            (byte) 0x6F, (byte) 0x0A, (byte) 0x64, (byte) 0x03, (byte) 0x53, (byte) 0x01,
            (byte) 0x01, (byte) 0x62, (byte) 0x03, (byte) 0x85, (byte) 0x01, (byte) 0x01
    };

    private static final byte[] SELECT_RESPONSE_FCP = {
            (byte) 0x62, (byte) 0x1A, (byte) 0x82, (byte) 0x02, (byte) 0x38, (byte) 0x21,
            (byte) 0x83, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x8A, (byte) 0x01,
            (byte) 0x00, (byte) 0x8C, (byte) 0x02, (byte) 0x20, (byte) 0x00, (byte) 0xC6,
            (byte) 0x09, (byte) 0x90, (byte) 0x01, (byte) 0x00, (byte) 0x83, (byte) 0x01,
            (byte) 0x00, (byte) 0x83, (byte) 0x01, (byte) 0x00
    };

    private static final byte[] SELECT_RESPONSE_FMD = {
            (byte) 0x64, (byte) 0x07, (byte) 0x53, (byte) 0x05, (byte) 0x01, (byte) 0x02,
            (byte) 0x03, (byte) 0x04, (byte) 0x05
    };

    private static final byte INS_GET_RESPONSE = (byte) 0xC0;

    private byte mCurrentClass = 0x00;
    private byte[] mOutgoingData = null;
    private short mDataOffset = 0;

    private AramApplet() {
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        AramApplet applet = new AramApplet();
        applet.register();
    }

    public void process(APDU apdu) throws ISOException {
        byte[] buffer = apdu.getBuffer();
        byte cla = buffer[ISO7816.OFFSET_CLA];
        byte ins = buffer[ISO7816.OFFSET_INS];
        byte p2 = buffer[ISO7816.OFFSET_P2];

        if (selectingApplet()) {
            byte[] response;

            switch (p2 & 0x0C) {
                case TEMPLATE_FCI:
                    response = SELECT_RESPONSE_FCI;
                    break;

                case TEMPLATE_FCP:
                    response = SELECT_RESPONSE_FCP;
                    break;

                case TEMPLATE_FMD:
                    response = SELECT_RESPONSE_FMD;
                    break;

                case TEMPLATE_PROPRIETARY:
                    return;

                default:
                    ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                    return;
            }

            initiateOutgoingCase4(cla, response);
            return;
        }

        if (ins != INS_GET_RESPONSE) {
            clearOutgoingData();
        }

        switch (ins) {
            case INS_GET_RESPONSE:
                if (cla != mCurrentClass) {
                    ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
                }
                if (mOutgoingData != null) {
                    processOutgoingCase2(apdu);
                }
                break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    private void processOutgoingCase2(APDU apdu) throws ISOException {
        short remaining = (short) (mOutgoingData.length - mDataOffset);
        short available = (DATA_BUFFER_SIZE < remaining) ? DATA_BUFFER_SIZE : remaining;

        byte[] command = apdu.getBuffer();
        short expected = command[ISO7816.OFFSET_LC];
        if (expected == 0x00) {
            expected = DATA_BUFFER_SIZE;
        }

        if (expected > available) {
            // Return SW 6Cxx if Le is bigger than the the actual outgoing data.
            ISOException.throwIt((short) (ISO7816.SW_CORRECT_LENGTH_00
                    + ((available < DATA_BUFFER_SIZE) ? available : 0x00)));
        }

        apdu.setOutgoing();
        apdu.setOutgoingLength(available);
        apdu.sendBytesLong(mOutgoingData, mDataOffset, available);

        if ((remaining -= available) > 0) {
            mDataOffset += available;
            // Return SW 61xx if remaining outgoing data exists after sending outgoing data.
            ISOException.throwIt((short) (ISO7816.SW_BYTES_REMAINING_00
                    + ((remaining < DATA_BUFFER_SIZE) ? remaining : (short) 0x00)));
        } else {
            clearOutgoingData();
        }
    }

    private void initiateOutgoingCase4(byte cla, byte[] data)
            throws ISOException {
        mCurrentClass = cla;
        mOutgoingData = data;
        mDataOffset = 0;

        short sw2 = (mOutgoingData.length < DATA_BUFFER_SIZE) ? (short) mOutgoingData.length : 0x00;
        // Return SW 61xx for now as Le is unknown.
        ISOException.throwIt((short) (ISO7816.SW_BYTES_REMAINING_00 + sw2));
    }

    private void clearOutgoingData() {
        if (mCurrentClass != 0x00) {
            mCurrentClass = 0x00;
        }
        if (mOutgoingData != null) {
            mOutgoingData = null;
        }
        if (mDataOffset != 0) {
            mDataOffset = 0;
        }
    }
}

