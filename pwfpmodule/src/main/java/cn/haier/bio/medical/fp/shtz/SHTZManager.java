package cn.haier.bio.medical.fp.shtz;

import java.util.List;

/***
 * 上海图正指纹模块
 *
 */
public class SHTZManager {
    private SHTZSerialPort serialPort;
    private static SHTZManager manager;

    public static SHTZManager getInstance() {
        if (manager == null) {
            synchronized (SHTZManager.class) {
                if (manager == null)
                    manager = new SHTZManager();
            }
        }
        return manager;
    }

    private SHTZManager() {

    }

    public void init(String path) {
        if(this.serialPort == null){
            this.serialPort = new SHTZSerialPort();
            this.serialPort.init(path);
        }
    }

    public void enable() {
        if(null != this.serialPort){
            this.serialPort.enable();
        }
    }

    public void disable() {
        if(null != this.serialPort){
            this.serialPort.disable();
        }
    }

    public void release() {
        if(null != this.serialPort){
            this.serialPort.release();
            this.serialPort = null;
        }
    }

    public void regist() {
        if(null != this.serialPort){
            this.serialPort.regist();
        }
    }

    public void uplaod(List<String> fileList) {
        if(null != this.serialPort){
            this.serialPort.uplaod(fileList);
        }
    }

    public void download(String filePath) {
        if(null != this.serialPort){
            this.serialPort.download(filePath);
        }
    }

    public boolean isBusy() {
        if(null != this.serialPort){
            this.serialPort.isBusy();
        }
        return false;
    }

    public void changeListener(ISHTZListener listener) {
        if(null != this.serialPort){
            this.serialPort.changeListener(listener);
        }
    }
}

