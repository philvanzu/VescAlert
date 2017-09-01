package philvanzu.vescalert;

import android.os.Environment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by philippe on 23/08/2017.
 */

public class BtSnoopLogReader {
    private static final char[] crc16_tab = { 0x0000, 0x1021, 0x2042, 0x3063, 0x4084,
            0x50a5, 0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad,
            0xe1ce, 0xf1ef, 0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7,
            0x62d6, 0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
            0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485, 0xa56a,
            0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d, 0x3653, 0x2672,
            0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4, 0xb75b, 0xa77a, 0x9719,
            0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc, 0x48c4, 0x58e5, 0x6886, 0x78a7,
            0x0840, 0x1861, 0x2802, 0x3823, 0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948,
            0x9969, 0xa90a, 0xb92b, 0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50,
            0x3a33, 0x2a12, 0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b,
            0xab1a, 0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41,
            0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49, 0x7e97,
            0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70, 0xff9f, 0xefbe,
            0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78, 0x9188, 0x81a9, 0xb1ca,
            0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f, 0x1080, 0x00a1, 0x30c2, 0x20e3,
            0x5004, 0x4025, 0x7046, 0x6067, 0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d,
            0xd31c, 0xe37f, 0xf35e, 0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214,
            0x6277, 0x7256, 0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c,
            0xc50d, 0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
            0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c, 0x26d3,
            0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634, 0xd94c, 0xc96d,
            0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab, 0x5844, 0x4865, 0x7806,
            0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3, 0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e,
            0x8bf9, 0x9bd8, 0xabbb, 0xbb9a, 0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1,
            0x1ad0, 0x2ab3, 0x3a92, 0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b,
            0x9de8, 0x8dc9, 0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0,
            0x0cc1, 0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8,
            0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0 };

    private static final long btsnoopEpochDelta = 0x00dcddb30f2f8000L;
    private static final int headerLength = 24; //btsnoop frame header length
    private static final int packetSize = 0x38; //COMM_GET_VALUES payload size

    public long lastUpdateTimeStamp = 0;
    public boolean running = false;
    public boolean opened = false;
    public int updatecount = 0;
    public VescStatus currentVescStatus;
    public long currentTimeStamp;

    private File btSnoop_hciFile;
    private FileInputStream fileStream;
    private ByteArrayInputStream memoryStream;
    private byte[] unparsed;

    private byte[] fileIdPattern = new byte[8];
    private byte[] fileVersion= new byte[4];
    private byte[] fileDataLinkType = new byte[4];

    private byte[] currentPacket = new byte[packetSize];
    private int currentIdx = 0;
    private enum parsingStates {searching, processing_1, processing_2, processing_3};
    private parsingStates currentState =  parsingStates.searching;

    public int errorCode  = 0;

    public void CreateDumpReader() {
        //Find the directory for the SD Card using the API
        //*Don't* hardcode "/sdcard"
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        btSnoop_hciFile = new File(sdcard, "btsnoop_hci.log");

        try {
            fileStream = new FileInputStream(btSnoop_hciFile);
            fileStream.read(fileIdPattern);
            fileStream.read(fileVersion);
            fileStream.read(fileDataLinkType);
            int available =  fileStream.available();
            fileStream.skip(available);
            opened = true;
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public void CloseDumpReader() {
        if(!opened) return;
        try {
            fileStream.close();
            opened = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public VescStatus ParseAvailable()
    {
        errorCode = 0;
        if(!opened)
        {
            errorCode = -2;
            return null;
        }


        int available = 0;
        byte[] availableData;

        try {
            available = fileStream.available();
            // Add available bytes to the remaining bytes of last parseAvailable run.
            // Copy all that in a byte array and pass it up to ByteArrayInputStream.
            int unparsedSize = 0;
            if(unparsed  != null)
                unparsedSize = unparsed.length;
            availableData = new byte[available + unparsedSize];
            if(unparsed != null) {
                System.arraycopy(unparsed, 0, availableData, 0, unparsedSize);
                unparsed = null;
            }
            fileStream.read(availableData, unparsedSize, available);
            memoryStream = new ByteArrayInputStream(availableData);
        } catch (IOException e) {
            e.printStackTrace();
            errorCode = -1;
            return null;
        }
        //parse all the available frames
        while(true && available >= 44)
        {
            int result = 0;
            available = memoryStream.available();
            if(available < 44) {
                // Save the last bytes which were not parsed. They will be parsed next time.
                unparsed = new byte[available];
                try {
                    memoryStream.read(unparsed);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            else
            {
                result = ParseFrame();
                if(result < 0)
                {
                    errorCode = result;
                    return null;
                }

            }
        }
        try  {
            memoryStream.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        memoryStream = null;

        lastUpdateTimeStamp = currentTimeStamp;
        updatecount++;
        return currentVescStatus;
    }

    //Ensure that at least 44 bytes are available for reading before calling this function.
    //Return values :
    // 0 no error.
    // -1 Out of memory or out of bounds parser cursor is off. should reinit parser..
    // -2 IOException. Log file unavailable. Service should quit.
    private int ParseFrame() {
        byte[] frameOriginalLength = new byte[4];
        byte[] frameIncludedLength = new byte[4];
        byte[] frameFlags = new byte[4];
        byte[] frameCumulativeDrops = new byte[4];
        byte[] frameTimeStamp = new byte[8];
        int len = 0;
        boolean rcv = false;
        boolean data = false;
        byte[] frameData;
        long time;
        long timedif;

        try {
            memoryStream.read(frameOriginalLength);
            memoryStream.read(frameIncludedLength);
            memoryStream.read(frameFlags);

            len = ByteBuffer.wrap(frameIncludedLength).getInt();
            rcv = ((frameFlags[3] << 0) & 1) == 1;
            data = ((frameFlags[3] << 1) & 1) == 0;

            memoryStream.read(frameCumulativeDrops);
            memoryStream.read(frameTimeStamp);
            time = ( ByteBuffer.wrap(frameTimeStamp).getLong() - btsnoopEpochDelta ) / 1000;
            long now = System.currentTimeMillis();
            timedif = now - time;

            try {
                frameData = new byte[len];
                if(len <= 32 && len >= 20 && rcv == true && data == true && timedif < 10000)
                    memoryStream.read(frameData);
                else
                {
                    memoryStream.skip(len);
                    return 0;
                }

            }
            catch (OutOfMemoryError e){
                e.printStackTrace();
                return -1;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return -2;
        }

        if(frameData[0] != 0x02  || frameData[10] != 0x12 || frameData[11]!= 0x00) return 0;

        int frameDataHeaderLength = 12;
        int frameValueLength = 0;

        switch (currentState)
        {
            case searching: {
                frameValueLength = 20;
                if(len-12 != frameValueLength) return 0;
                Arrays.fill(currentPacket, (byte)0);
                currentTimeStamp = time;
                byte sizeRange = frameData[frameDataHeaderLength];
                byte payloadSize = frameData[frameDataHeaderLength+1];
                byte comm_packet_id = frameData[frameDataHeaderLength + 2];              //COMM_PACKTET_ID, searching for 0x04 (COMM_GET_VALUES)
                if (sizeRange != 0x02 || payloadSize != 0x38 || comm_packet_id != 0x04 ) return 0;
                try{
                    System.arraycopy(frameData, frameDataHeaderLength+2, currentPacket, currentIdx, frameValueLength-2);
                    currentIdx += frameValueLength-2;
                    currentState = parsingStates.processing_1;
                } catch (IndexOutOfBoundsException e) {
                    packetAssemblyFailed();
                    return 0;
                }
                break;
            }

            case processing_1:{
                frameValueLength = 20;
                if(len-12 != frameValueLength) {
                    packetAssemblyFailed();
                    return 0;
                }
                try{
                    System.arraycopy(frameData, frameDataHeaderLength, currentPacket, currentIdx, frameValueLength);
                    currentIdx += frameValueLength;
                    currentState = parsingStates.processing_2;
                } catch (IndexOutOfBoundsException e) {
                    packetAssemblyFailed();
                    return 0;
                }
                break;
            }

            case processing_2:
            {
                frameValueLength = 8;
                if(len-12 != frameValueLength) {
                    packetAssemblyFailed();
                    return 0;
                }
                try{
                    System.arraycopy(frameData, frameDataHeaderLength, currentPacket, currentIdx, frameValueLength);
                    currentIdx += frameValueLength;
                    currentState = parsingStates.processing_3;
                } catch (IndexOutOfBoundsException e) {
                    packetAssemblyFailed();
                    return 0;
                }
                break;
            }

            case processing_3:
            {
                frameValueLength = 13;
                if(len-12 != frameValueLength || frameData[len-1] != 0x03) { //correct length and eop byte (0x03) present
                    packetAssemblyFailed();
                    return 0;
                }


                int crc = ByteBuffer.wrap(frameData, len-3, 2).getChar();

                try{
                    System.arraycopy(frameData, frameDataHeaderLength, currentPacket, currentIdx, frameValueLength-3);
                    currentIdx += frameValueLength-3;
                    currentState = parsingStates.searching;

                    //check crc is correct
                    int i;
                    int crc_value = crc16(currentPacket, 0, 0x38);
                    if(crc_value != crc)
                    {
                        packetAssemblyFailed();
                        return 0;
                    }
                    currentVescStatus = new VescStatus(currentPacket, currentTimeStamp);

                } catch (IndexOutOfBoundsException e) {
                    packetAssemblyFailed();
                    return 0;
                }
                break;
            }
        }
        return 0;
    }

    private void packetAssemblyFailed()
    {
        Arrays.fill(currentPacket, (byte)0);
        currentState = parsingStates.searching;
        currentIdx = 0;
    }

    private int crc16(byte[] buf, int start, int len) {
        int cksum = 0;
        for (int i = start; i < start+len; i++) {
            int r = cksum >> 8;
            r = r ^ buf[i];
            r = r & 0xff;
            int l = cksum << 8;
            cksum = (crc16_tab[r] ^ l);
            cksum = cksum & 0xffff;
        }
        return cksum ;
    }



}
