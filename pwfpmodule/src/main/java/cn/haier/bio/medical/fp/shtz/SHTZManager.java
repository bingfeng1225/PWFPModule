package cn.haier.bio.medical.fp.shtz;

import java.util.List;

import cn.qd.peiwen.pwtools.EmptyUtils;

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
        if(EmptyUtils.isEmpty(this.serialPort)){
            this.serialPort = new SHTZSerialPort();
            this.serialPort.init(path);
        }
    }

    public void enable() {
        if(EmptyUtils.isNotEmpty(this.serialPort)){
            this.serialPort.enable();
        }
    }

    public void disable() {
        if(EmptyUtils.isNotEmpty(this.serialPort)){
            this.serialPort.disable();
        }
    }

    public void release() {
        if(EmptyUtils.isNotEmpty(this.serialPort)){
            this.serialPort.release();
            this.serialPort = null;
        }
    }

    public void regist() {
        if(EmptyUtils.isNotEmpty(this.serialPort)){
            this.serialPort.regist();
        }
    }

    public void uplaod(List<String> fileList) {
        if(EmptyUtils.isNotEmpty(this.serialPort)){
            this.serialPort.uplaod(fileList);
        }
    }

    public void download(String filePath) {
        if(EmptyUtils.isNotEmpty(this.serialPort)){
            this.serialPort.download(filePath);
        }
    }

    public boolean isBusy() {
        if(EmptyUtils.isNotEmpty(this.serialPort)){
            this.serialPort.isBusy();
        }
        return false;
    }

    public void changeListener(ISHTZListener listener) {
        if (EmptyUtils.isNotEmpty(this.serialPort)) {
            this.serialPort.changeListener(listener);
        }
    }
}

