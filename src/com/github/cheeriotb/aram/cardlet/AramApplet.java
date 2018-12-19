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
import javacard.framework.Util;

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
    private static final byte INS_GET_DATA     = (byte) 0xCA;

    private static final short GET_DATA_ALL         = (short) 0xFF40;
    private static final short GET_DATA_NEXT        = (short) 0xFF60;
    private static final short GET_DATA_REFRESH_TAG = (short) 0xDF20;

    private static final byte[] RESPONSE_ALL_REF_AR_DO = {
        /*
           |Response-ALL-REF-AR-DO|T|FF40  |
           |                      |L|8206A8|
        */
        (byte) 0xFF, (byte) 0x40, (byte) 0x82, (byte) 0x06, (byte) 0xA8,

        /*
           REF-AR-DO for UICC Carrier Privileges

           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|1E                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|06                                      |
           |         | |      | |                  |V|FFFFFFFFFFFF                            |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|61ED377E85D386A8DFEE6B864BD85B0BFAA5AF81|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|0D                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |         | |      | |PERM-AR-DO        |T|DB                                      |
           |         | |      | |                  |L|08                                      |
           |         | |      | |                  |V|0000000000000001                        |
        */
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x1E, (byte) 0x4F, (byte) 0x06,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xC1, (byte) 0x14, (byte) 0x61, (byte) 0xED, (byte) 0x37, (byte) 0x7E,
        (byte) 0x85, (byte) 0xD3, (byte) 0x86, (byte) 0xA8, (byte) 0xDF, (byte) 0xEE,
        (byte) 0x6B, (byte) 0x86, (byte) 0x4B, (byte) 0xD8, (byte) 0x5B, (byte) 0x0B,
        (byte) 0xFA, (byte) 0xA5, (byte) 0xAF, (byte) 0x81, (byte) 0xE3, (byte) 0x0D,
        (byte) 0xD0, (byte) 0x01, (byte) 0x01, (byte) 0xDB, (byte) 0x08, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01,

        /*
           REF-AR-DO for non-specific applications and applets

           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|0B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|04                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
        */
        (byte) 0xE2, (byte) 0x0B, (byte) 0xE1, (byte) 0x04, (byte) 0x4F, (byte) 0x00,
        (byte) 0xC1, (byte) 0x00, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,

        /*
           REF-AR-DOs for the specific applets.
           Accesses from those applets are basically prohibited.

           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545340        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545341        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545342        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545343        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545344        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545345        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545346        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545347        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545348        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545349        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534A        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534B        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534C        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534D        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534E        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|1B    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|14                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534F        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|00 (Not Specified)                      |
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|00 (Never)                              |
        */
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x40, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x41, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x42, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x43, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x44, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x45, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x46, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x47, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x48, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x49, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4A, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4B, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4C, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4D, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4E, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,
        (byte) 0xE2, (byte) 0x1B, (byte) 0xE1, (byte) 0x14, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4F, (byte) 0xC1, (byte) 0x00,
        (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01, (byte) 0x00,

        /*
           REF-AR-DOs with the specific APDU fileters for CtsSecureElementAccessControlTestCases1.

           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|3E    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545340        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|12                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|00060000 / FFFF0000                     |
           |         | |      | |                  | |A0060000 / FFFF0000                     |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|36    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545341        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|0A                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|08                                      |
           |         | |      | |                  |V|94000000 / FF000000                     |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545342        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545344        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545345        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545347        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545348        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545349        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534A        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534B        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534C        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534D        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534E        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F69644354534F        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|4BBE31BEB2F753CFE71EC6BF112548687BB6C34E|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
        */
        (byte) 0xE2, (byte) 0x3E, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x40, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x12, (byte) 0xD0, (byte) 0x10,
        (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x00, (byte) 0xA0, (byte) 0x06, (byte) 0x00, (byte) 0x00,
        (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
        (byte) 0xE2, (byte) 0x36, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x41, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x0A, (byte) 0xD0, (byte) 0x08,
        (byte) 0x94, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x42, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x44, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x45, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x47, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x48, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x49, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4A, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4B, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4C, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4D, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4E, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x4F, (byte) 0xC1, (byte) 0x14,
        (byte) 0x4B, (byte) 0xBE, (byte) 0x31, (byte) 0xBE, (byte) 0xB2, (byte) 0xF7,
        (byte) 0x53, (byte) 0xCF, (byte) 0xE7, (byte) 0x1E, (byte) 0xC6, (byte) 0xBF,
        (byte) 0x11, (byte) 0x25, (byte) 0x48, (byte) 0x68, (byte) 0x7B, (byte) 0xB6,
        (byte) 0xC3, (byte) 0x4E, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,

        /*
           REF-AR-DOs with the specific APDU fileters for CtsSecureElementAccessControlTestCases2.

           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|3E    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545340        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|93B0FF2260BABD4C2A92C68AAA0039DC514D8A33|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|12                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|00060000 / FFFF0000                     |
           |         | |      | |                  | |A0060000 / FFFF0000                     |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|36    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545341        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|93B0FF2260BABD4C2A92C68AAA0039DC514D8A33|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|0A                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|08                                      |
           |         | |      | |                  |V|94000000 / FF000000                     |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545343        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|93B0FF2260BABD4C2A92C68AAA0039DC514D8A33|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545345        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|93B0FF2260BABD4C2A92C68AAA0039DC514D8A33|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545346        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|93B0FF2260BABD4C2A92C68AAA0039DC514D8A33|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
        */
        (byte) 0xE2, (byte) 0x3E, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x40, (byte) 0xC1, (byte) 0x14,
        (byte) 0x93, (byte) 0xB0, (byte) 0xFF, (byte) 0x22, (byte) 0x60, (byte) 0xBA,
        (byte) 0xBD, (byte) 0x4C, (byte) 0x2A, (byte) 0x92, (byte) 0xC6, (byte) 0x8A,
        (byte) 0xAA, (byte) 0x00, (byte) 0x39, (byte) 0xDC, (byte) 0x51, (byte) 0x4D,
        (byte) 0x8A, (byte) 0x33, (byte) 0xE3, (byte) 0x12, (byte) 0xD0, (byte) 0x10,
        (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x00, (byte) 0xA0, (byte) 0x06, (byte) 0x00, (byte) 0x00,
        (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
        (byte) 0xE2, (byte) 0x36, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x41, (byte) 0xC1, (byte) 0x14,
        (byte) 0x93, (byte) 0xB0, (byte) 0xFF, (byte) 0x22, (byte) 0x60, (byte) 0xBA,
        (byte) 0xBD, (byte) 0x4C, (byte) 0x2A, (byte) 0x92, (byte) 0xC6, (byte) 0x8A,
        (byte) 0xAA, (byte) 0x00, (byte) 0x39, (byte) 0xDC, (byte) 0x51, (byte) 0x4D,
        (byte) 0x8A, (byte) 0x33, (byte) 0xE3, (byte) 0x0A, (byte) 0xD0, (byte) 0x08,
        (byte) 0x94, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x43, (byte) 0xC1, (byte) 0x14,
        (byte) 0x93, (byte) 0xB0, (byte) 0xFF, (byte) 0x22, (byte) 0x60, (byte) 0xBA,
        (byte) 0xBD, (byte) 0x4C, (byte) 0x2A, (byte) 0x92, (byte) 0xC6, (byte) 0x8A,
        (byte) 0xAA, (byte) 0x00, (byte) 0x39, (byte) 0xDC, (byte) 0x51, (byte) 0x4D,
        (byte) 0x8A, (byte) 0x33, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x45, (byte) 0xC1, (byte) 0x14,
        (byte) 0x93, (byte) 0xB0, (byte) 0xFF, (byte) 0x22, (byte) 0x60, (byte) 0xBA,
        (byte) 0xBD, (byte) 0x4C, (byte) 0x2A, (byte) 0x92, (byte) 0xC6, (byte) 0x8A,
        (byte) 0xAA, (byte) 0x00, (byte) 0x39, (byte) 0xDC, (byte) 0x51, (byte) 0x4D,
        (byte) 0x8A, (byte) 0x33, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x46, (byte) 0xC1, (byte) 0x14,
        (byte) 0x93, (byte) 0xB0, (byte) 0xFF, (byte) 0x22, (byte) 0x60, (byte) 0xBA,
        (byte) 0xBD, (byte) 0x4C, (byte) 0x2A, (byte) 0x92, (byte) 0xC6, (byte) 0x8A,
        (byte) 0xAA, (byte) 0x00, (byte) 0x39, (byte) 0xDC, (byte) 0x51, (byte) 0x4D,
        (byte) 0x8A, (byte) 0x33, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,

        /*
           REF-AR-DOs with the specific APDU fileters for CtsSecureElementAccessControlTestCases3.

           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545340        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|5528CA826DA49D0D7329F8117481CCB27B8833AA|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|36    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545341        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|5528CA826DA49D0D7329F8117481CCB27B8833AA|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|0A                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|08                                      |
           |         | |      | |                  |V|94000000 / FF000000                     |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545345        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|5528CA826DA49D0D7329F8117481CCB27B8833AA|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
           |REF-AR-DO|T|E2    | |                  | |                                        |
           |         |L|2F    | |                  | |                                        |
           |         |V|REF-DO|T|E1                | |                                        |
           |         | |      |L|28                | |                                        |
           |         | |      |V|AID-REF-DO        |T|4F                                      |
           |         | |      | |                  |L|10                                      |
           |         | |      | |                  |V|A000000476416E64726F696443545346        |
           |         | |      | |DeviceAppID-REF-DO|T|C1                                      |
           |         | |      | |                  |L|14                                      |
           |         | |      | |                  |V|5528CA826DA49D0D7329F8117481CCB27B8833AA|
           |         | |AR-DO |T|E3                | |                                        |
           |         | |      |L|03                | |                                        |
           |         | |      |V|APDU-AR-DO        |T|D0                                      |
           |         | |      | |                  |L|01                                      |
           |         | |      | |                  |V|01 (Always)                             |
        */
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x40, (byte) 0xC1, (byte) 0x14,
        (byte) 0x55, (byte) 0x28, (byte) 0xCA, (byte) 0x82, (byte) 0x6D, (byte) 0xA4,
        (byte) 0x9D, (byte) 0x0D, (byte) 0x73, (byte) 0x29, (byte) 0xF8, (byte) 0x11,
        (byte) 0x74, (byte) 0x81, (byte) 0xCC, (byte) 0xB2, (byte) 0x7B, (byte) 0x88,
        (byte) 0x33, (byte) 0xAA, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x36, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x41, (byte) 0xC1, (byte) 0x14,
        (byte) 0x55, (byte) 0x28, (byte) 0xCA, (byte) 0x82, (byte) 0x6D, (byte) 0xA4,
        (byte) 0x9D, (byte) 0x0D, (byte) 0x73, (byte) 0x29, (byte) 0xF8, (byte) 0x11,
        (byte) 0x74, (byte) 0x81, (byte) 0xCC, (byte) 0xB2, (byte) 0x7B, (byte) 0x88,
        (byte) 0x33, (byte) 0xAA, (byte) 0xE3, (byte) 0x0A, (byte) 0xD0, (byte) 0x08,
        (byte) 0x94, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x45, (byte) 0xC1, (byte) 0x14,
        (byte) 0x55, (byte) 0x28, (byte) 0xCA, (byte) 0x82, (byte) 0x6D, (byte) 0xA4,
        (byte) 0x9D, (byte) 0x0D, (byte) 0x73, (byte) 0x29, (byte) 0xF8, (byte) 0x11,
        (byte) 0x74, (byte) 0x81, (byte) 0xCC, (byte) 0xB2, (byte) 0x7B, (byte) 0x88,
        (byte) 0x33, (byte) 0xAA, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01,
        (byte) 0xE2, (byte) 0x2F, (byte) 0xE1, (byte) 0x28, (byte) 0x4F, (byte) 0x10,
        (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x76, (byte) 0x41,
        (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69, (byte) 0x64,
        (byte) 0x43, (byte) 0x54, (byte) 0x53, (byte) 0x46, (byte) 0xC1, (byte) 0x14,
        (byte) 0x55, (byte) 0x28, (byte) 0xCA, (byte) 0x82, (byte) 0x6D, (byte) 0xA4,
        (byte) 0x9D, (byte) 0x0D, (byte) 0x73, (byte) 0x29, (byte) 0xF8, (byte) 0x11,
        (byte) 0x74, (byte) 0x81, (byte) 0xCC, (byte) 0xB2, (byte) 0x7B, (byte) 0x88,
        (byte) 0x33, (byte) 0xAA, (byte) 0xE3, (byte) 0x03, (byte) 0xD0, (byte) 0x01,
        (byte) 0x01
    };

    private static final byte[] RESPONSE_REFRESH_TAG_DO = {
        /*
           |Response-Refresh-Tag-DO|T|DF20            |
           |                       |L|08              |
           |                       |V|0123456789ABCDEF|
        */
        (byte) 0xDF, (byte) 0x20, (byte) 0x08, (byte) 0x01, (byte) 0x23, (byte) 0x45,
        (byte) 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
    };

    private byte mCurrentClass = 0x00;
    private byte[] mOutgoingData = null;
    private short mDataOffset = 0;

    private short mRemainingData = 0;

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
            byte[] response = null;
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

            case INS_GET_DATA:
                // Only Global Platform command is supported.
                if ((cla & 0x80) != 0x80) {
                    ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
                }
                switch (Util.getShort(buffer, (short) ISO7816.OFFSET_P1)) {
                    case GET_DATA_ALL:
                        mRemainingData = (short) RESPONSE_ALL_REF_AR_DO.length;
                        initiateOutgoingCase2(apdu, RESPONSE_ALL_REF_AR_DO, (short) 0);
                        break;
                    case GET_DATA_NEXT:
                        if (mRemainingData == 0) {
                            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
                        }
                        initiateOutgoingCase2(apdu, RESPONSE_ALL_REF_AR_DO,
                            (short) (RESPONSE_ALL_REF_AR_DO.length - mRemainingData));
                        break;
                    case GET_DATA_REFRESH_TAG:
                        initiateOutgoingCase2(apdu, RESPONSE_REFRESH_TAG_DO, (short) 0);
                        break;
                    default:
                        ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                        break;
                }
                break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    private void initiateOutgoingCase2(APDU apdu, byte[] data, short offset)
            throws ISOException {
        byte[] command = apdu.getBuffer();

        mCurrentClass = command[ISO7816.OFFSET_CLA];
        mOutgoingData = data;
        mDataOffset = offset;

        processOutgoingCase2(apdu);
    }

    private void processOutgoingCase2(APDU apdu) throws ISOException {
        short remaining = (short) (mOutgoingData.length - mDataOffset);
        short available = (DATA_BUFFER_SIZE < remaining) ? DATA_BUFFER_SIZE : remaining;

        byte[] command = apdu.getBuffer();
        short expected = (short) (command[ISO7816.OFFSET_LC] & 0xFF);
        if (expected == 0x00) {
            expected = DATA_BUFFER_SIZE;
        }

        if (expected > available) {
            // Return SW 6Cxx if Le is bigger than the the actual outgoing data.
            ISOException.throwIt((short) (ISO7816.SW_CORRECT_LENGTH_00
                    + ((available < DATA_BUFFER_SIZE) ? available : 0x00)));
        }

        apdu.setOutgoing();
        apdu.setOutgoingLength(expected);
        apdu.sendBytesLong(mOutgoingData, mDataOffset, expected);

        if (command[ISO7816.OFFSET_INS] != INS_GET_DATA) {
            if (((remaining -= expected) > 0) && (expected == DATA_BUFFER_SIZE)) {
                mDataOffset += expected;
                // Return SW 61xx if remaining outgoing data exists after sending outgoing data.
                ISOException.throwIt((short) (ISO7816.SW_BYTES_REMAINING_00
                        + ((remaining < DATA_BUFFER_SIZE) ? remaining : (short) 0x00)));
            } else {
                clearOutgoingData();
            }
        } else {
            mRemainingData -= expected;
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

