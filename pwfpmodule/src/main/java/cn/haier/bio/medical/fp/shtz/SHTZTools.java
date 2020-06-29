package cn.haier.bio.medical.fp.shtz;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class SHTZTools {
    public static final int FINGER_COMMAND_CLEAR = 0x05;
    public static final int FINGER_COMMAND_BREAK = 0xFE;
    public static final int FINGER_COMMAND_DELETE = 0x04;
    public static final int FINGER_COMMAND_UPLOAD = 0x41;
    public static final int FINGER_COMMAND_COMPARE = 0x0C;
    public static final int FINGER_COMMAND_DOWNLOAD = 0x31;
    public static final int FINGER_COMMAND_REGIST_FIRST = 0x01;
    public static final int FINGER_COMMAND_REGIST_THIRD = 0x03;
    public static final int FINGER_COMMAND_REGIST_SECOND = 0x02;
    public static final int FINGER_COMMAND_SEARCH_FINGER = 0x2B;
    public static final int FINGER_COMMAND_REGIST_REFUSE_REPEAT= 0x2D;
    public static final int FINGER_COMMAND_REGIST_HAND_DETECTION = 0x3F;

    public static final int FINGER_STATE_DISABLED = 0;
    public static final int FINGER_STATE_REGIST = 1;
    public static final int FINGER_STATE_UPLOAD = 2;
    public static final int FINGER_STATE_COMPARE = 3;
    public static final int FINGER_STATE_DOWNLOAD = 4;
    public static final int FINGER_STATE_REGIST_MODEL = 5;


    public static boolean checkFrame(byte[] data) {
        if ((data[0] & 0xff) != 0xF5) {
            return false;
        }
        if ((data[data.length - 1] & 0xff) != 0xF5) {
            return false;
        }
        byte check = computeXORCode(data, 1, data.length - 3);
        return (check == data[data.length - 2]);
    }

    public static byte[] packageCommand(int type, int param) {
        ByteBuf buffer = Unpooled.buffer(8);
        buffer.writeByte(0xF5);
        buffer.writeByte(type);
        switch (type) {
            case FINGER_COMMAND_CLEAR:
            case FINGER_COMMAND_BREAK:
            case FINGER_COMMAND_COMPARE:
            case FINGER_COMMAND_SEARCH_FINGER:
                buffer.writeByte(0x00);
                buffer.writeByte(0x00);
                buffer.writeByte(0x00);
                buffer.writeByte(0x00);
                break;
            case FINGER_COMMAND_DELETE:
            case FINGER_COMMAND_UPLOAD:
            case FINGER_COMMAND_DOWNLOAD:
                buffer.writeShort(param);
                buffer.writeByte(0x00);
                buffer.writeByte(0x00);
                break;
            case FINGER_COMMAND_REGIST_FIRST:
            case FINGER_COMMAND_REGIST_SECOND:
            case FINGER_COMMAND_REGIST_THIRD:
                buffer.writeByte(0x00);
                buffer.writeByte(0x00);
                buffer.writeByte(0x01);
                buffer.writeByte(0x00);
                break;
            case FINGER_COMMAND_REGIST_REFUSE_REPEAT:
                buffer.writeByte(0x00);
                buffer.writeByte(0x01);
                buffer.writeByte(0x00);
                buffer.writeByte(0x00);
                break;
            case FINGER_COMMAND_REGIST_HAND_DETECTION:
                buffer.writeByte(0x00);
                buffer.writeByte(0x02);
                buffer.writeByte(0x01);
                buffer.writeByte(0x00);
                break;
        }
        byte[] data = new byte[8];
        buffer.readBytes(data, 0, 6);
        data[6] = computeXORCode(data, 1, 5);
        data[7] = (byte) 0xF5;
        buffer.release();
        return data;
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean writeFile(String path, byte[] bytes, int length) {
        RandomAccessFile raf = null;
        try {
            File file = new File(path);
            raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(bytes, 0, length);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != raf) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String bytes2HexString(byte[] data) {
        return bytes2HexString(data, false);
    }

    public static String bytes2HexString(byte[] data, boolean hexFlag) {
        return bytes2HexString(data, hexFlag, null);
    }

    public static String bytes2HexString(byte[] data, boolean hexFlag, String separator) {
        if (data == null) {
            throw new IllegalArgumentException("The data can not be blank");
        }
        return bytes2HexString(data, 0, data.length, hexFlag, separator);
    }

    public static String bytes2HexString(byte[] data, int offset, int len) {
        return bytes2HexString(data, offset, len, false);
    }

    public static String bytes2HexString(byte[] data, int offset, int len, boolean hexFlag) {
        return bytes2HexString(data, offset, len, hexFlag, null);
    }

    public static String bytes2HexString(byte[] data, int offset, int len, boolean hexFlag, String separator) {
        if (data == null) {
            throw new IllegalArgumentException("The data can not be blank");
        }
        if (offset < 0 || offset > data.length - 1) {
            throw new IllegalArgumentException("The offset index out of bounds");
        }
        if (len < 0 || offset + len > data.length) {
            throw new IllegalArgumentException("The len can not be < 0 or (offset + len) index out of bounds");
        }
        String format = "%02X";
        if (hexFlag) {
            format = "0x%02X";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = offset; i < offset + len; i++) {
            buffer.append(String.format(format, data[i]));
            if (separator == null) {
                continue;
            }
            if (i != (offset + len - 1)) {
                buffer.append(separator);
            }
        }
        return buffer.toString();
    }

    public static byte computeXORCode(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("The data can not be blank");
        }
        return computeXORCode(data, 0, data.length);
    }

    public static byte computeXORCode(byte[] data, int offset, int len) {
        if (data == null) {
            throw new IllegalArgumentException("The data can not be blank");
        }
        if (offset < 0 || offset > data.length - 1) {
            throw new IllegalArgumentException("The offset index out of bounds");
        }
        if (len < 0 || offset + len > data.length) {
            throw new IllegalArgumentException("The len can not be < 0 or (offset + len) index out of bounds");
        }
        byte temp = data[offset];
        for (int i = offset + 1; i < offset + len; i++) {
            temp ^= data[i];
        }
        return temp;
    }
}
