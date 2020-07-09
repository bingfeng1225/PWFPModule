package cn.qd.peiwen.demo.tools;

import cn.qd.peiwen.pwtools.ThreadUtils;
import cn.qd.peiwen.serialport.PWSerialPort;

public class SHTZTools {
    //智能电子指纹模块重置
    private static final String FINGER_RESET_ON = "1";
    private static final String FINGER_RESET_OFF = "0";
    private static final String FINGER_RESET_USB = "/sys/kernel/finger_set/usb_value";
    private static final String FINGER_RESET_TARGET = "/sys/kernel/finger_set/finger_value";

    public static void resetFingerPrint() {
        PWSerialPort.writeFile(FINGER_RESET_TARGET, FINGER_RESET_OFF);
        ThreadUtils.sleep(50L);
        PWSerialPort.writeFile(FINGER_RESET_USB, FINGER_RESET_OFF);
        ThreadUtils.sleep(50L);
        PWSerialPort.writeFile(FINGER_RESET_TARGET, FINGER_RESET_ON);
        ThreadUtils.sleep(50L);
        PWSerialPort.writeFile(FINGER_RESET_USB, FINGER_RESET_ON);
    }
}
