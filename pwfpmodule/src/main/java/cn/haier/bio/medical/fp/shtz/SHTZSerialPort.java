package cn.haier.bio.medical.fp.shtz;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.qd.peiwen.serialport.PWSerialPortHelper;
import cn.qd.peiwen.serialport.PWSerialPortListener;
import cn.qd.peiwen.serialport.PWSerialPortState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class SHTZSerialPort implements PWSerialPortListener {
    private ByteBuf buffer;
    private HandlerThread thread;
    private TZFPHandler handler;
    private PWSerialPortHelper helper;

    private int state;
    private boolean ready = false;
    private boolean enabled = false;
    private WeakReference<ISHTZListener> listener;

    private int currentIndex = 0;
    private String filePath = null;
    private List<String> fileList = null;
    private List<Integer> fingerList = null;


    public SHTZSerialPort() {
        
    }

    public void init(String path) {
        this.createHandler();
        this.createHelper(path);
        this.createBuffer();
        this.state = SHTZTools.FINGER_STATE_DISABLED;
    }

    public void enable() {
        if (this.isInitialized() && !this.enabled) {
            this.enabled = true;
            this.helper.open();
            this.state = SHTZTools.FINGER_STATE_REGIST_MODEL;
        }
    }

    public void disable() {
        if (this.enabled && this.isInitialized()) {
            this.state = SHTZTools.FINGER_STATE_DISABLED;
            this.enabled = false;
            this.helper.close();
        }
    }

    public void release() {
        this.listener = null;
        this.state = SHTZTools.FINGER_STATE_DISABLED;
        this.destoryHandler();
        this.destoryHelper();
        this.destoryBuffer();
    }

    public void regist() {
        if(null != this.listener && null != this.listener.get()){
            this.listener.get().onSHTZRegistStated();
        }
        this.changeFingerPrintState(SHTZTools.FINGER_STATE_REGIST);
    }

    public void uplaod(List<String> fileList) {
        this.currentIndex = -1;
        this.fileList = fileList;
        if(null != this.listener && null != this.listener.get()){
            this.listener.get().onSHTZUploadStated();
        }
        this.changeFingerPrintState(SHTZTools.FINGER_STATE_UPLOAD);
    }

    public void download(String filePath) {
        this.filePath = filePath;
        if(null != this.listener && null != this.listener.get()){
            this.listener.get().onSHTZDownloadStated();
        }
        this.changeFingerPrintState(SHTZTools.FINGER_STATE_DOWNLOAD);
    }

    public boolean isBusy() {
        return (this.state != SHTZTools.FINGER_STATE_COMPARE);
    }

    public void changeListener(ISHTZListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    private boolean isInitialized() {
        if (this.handler == null) {
            return false;
        }
        if (this.helper == null) {
            return false;
        }
        if (this.buffer == null) {
            return false;
        }
        return true;
    }

    private void createHelper(String path) {
        if (this.helper == null) {
            this.helper = new PWSerialPortHelper("SHTZSerialPort");
            this.helper.setTimeout(10);
            this.helper.setPath(path);
            this.helper.setBaudrate(115200);
            this.helper.init(this);
        }
    }

    private void destoryHelper() {
        if (null != this.helper) {
            this.helper.release();
            this.helper = null;
        }
    }

    private void createHandler() {
        if (this.thread == null && this.handler == null) {
            this.thread = new HandlerThread("SHTZSerialPort");
            this.thread.start();
            this.handler = new TZFPHandler(this.thread.getLooper());
        }
    }

    private void destoryHandler() {
        if (null != this.thread) {
            this.thread.quitSafely();
            this.thread = null;
            this.handler = null;
        }
    }

    private void createBuffer() {
        if (this.buffer == null) {
            this.buffer = Unpooled.buffer(4);
        }
    }

    private void destoryBuffer() {
        if (null != this.buffer) {
            this.buffer.release();
            this.buffer = null;
        }
    }

    private void write(byte[] data) {
        if (!this.isInitialized() || !this.enabled) {
            return;
        }
        this.helper.write(data);
        this.loggerPrint("SHTZSerialPort Send:" + SHTZTools.bytes2HexString(data, true, ", "));
    }

    private void sendCommand(int type) {
        this.sendCommand(type, 0);
    }

    private void sendCommand(int type, int param) {
        byte[] data = SHTZTools.packageCommand(type, param);
        this.write(data);
    }

    private void loggerPrint(String message) {
        if(null != this.listener && null != this.listener.get()){
            this.listener.get().onSHTZPrint(message);
        }
    }

    private void changeFingerPrintState(int state) {
        this.state = state;
        this.loggerPrint("SHTZSerialPort 中断模组当前操作");
        this.sendCommand(SHTZTools.FINGER_COMMAND_BREAK);
    }

    private void operationInterrupted() {
        switch (this.state) {
            case SHTZTools.FINGER_STATE_REGIST:
                this.loggerPrint("SHTZSerialPort 设置抬手检测");
                this.sendCommand(SHTZTools.FINGER_COMMAND_REGIST_HAND_DETECTION);
                break;
            case SHTZTools.FINGER_STATE_UPLOAD:
                this.loggerPrint("SHTZSerialPort 清空所有已注册指纹");
                this.sendCommand(SHTZTools.FINGER_COMMAND_CLEAR);
                break;
            case SHTZTools.FINGER_STATE_DOWNLOAD:
                this.loggerPrint("SHTZSerialPort 查询所有已注册指纹");
                this.sendCommand(SHTZTools.FINGER_COMMAND_SEARCH_FINGER);
                break;
            case SHTZTools.FINGER_STATE_REGIST_MODEL:
                this.loggerPrint("SHTZSerialPort 设置拒绝重复注册");
                this.sendCommand(SHTZTools.FINGER_COMMAND_REGIST_REFUSE_REPEAT);
                break;
            default: {
                this.loggerPrint("SHTZSerialPort 开始指纹识别");
                this.handler.sendEmptyMessageDelayed(SHTZTools.FINGER_COMMAND_COMPARE, 1000);
                break;
            }
        }
    }


    private void fireDownloadSuccessed() {
        if(null != this.listener && null != this.listener.get()){
            this.listener.get().onSHTZDownloadSuccessed();
        }
    }

    private boolean isFingerValid(int finger) {
        if(null != this.listener && null != this.listener.get()){
            return this.listener.get().isFingerValid(finger);
        }
        return false;
    }

    private void uplaodFinger() throws IOException {
        String filePath = this.fileList.get(this.currentIndex);
        this.loggerPrint("SHTZSerialPort 指纹上传开始:" + filePath);
        this.sendCommand(SHTZTools.FINGER_COMMAND_UPLOAD, 8195);
        InputStream stream = new FileInputStream(filePath);
        while (stream.available() > 0) {
            byte[] data = null;
            int readable = stream.available();
            if (readable >= 128) {
                data = new byte[128];
                stream.read(data, 0, 128);
            } else {
                data = new byte[readable];
                stream.read(data, 0, readable);
            }
            this.write(data);
            SHTZTools.sleep(30);//200
        }
        this.loggerPrint("SHTZSerialPort 指纹上传完成:" + filePath);
        stream.close();
    }

    private void processUnknownCommand() {
        this.buffer.skipBytes(8);
        this.buffer.discardReadBytes();
        this.loggerPrint("SHTZSerialPort 指令无法识别");
    }

    private void processFileList() throws IOException {
        this.currentIndex++;
        if (this.currentIndex < this.fileList.size()) {
            this.uplaodFinger();
        } else {
            this.loggerPrint("SHTZSerialPort 指纹上传完毕");
            if(null != this.listener && null != this.listener.get()){
                this.listener.get().onSHTZUploadSuccessed();
            }
            this.changeFingerPrintState(SHTZTools.FINGER_STATE_COMPARE);
        }
    }

    private void processClearCommand() throws IOException {
        this.buffer.skipBytes(8);
        this.buffer.discardReadBytes();
        this.loggerPrint("SHTZSerialPort 所有指纹已删除");
        this.currentIndex = -1;
        this.processFileList();
    }

    private void processBreakCommand() {
        this.buffer.skipBytes(8);
        this.buffer.discardReadBytes();
        this.loggerPrint("SHTZSerialPort 指纹操作中断");
        this.operationInterrupted();
    }

    private void processUploadCommand() throws IOException {
        this.buffer.skipBytes(4);
        int status = this.buffer.readByte();
        this.buffer.skipBytes(3);
        this.buffer.discardReadBytes();
        if (status == 0x00) {
            this.loggerPrint("SHTZSerialPort 指纹上传成功:" + this.fileList.get(this.currentIndex));
            this.processFileList();
        } else {
            this.loggerPrint("SHTZSerialPort 指纹上传失败");
            if(null != this.listener && null != this.listener.get()){
                this.listener.get().onSHTZUploadFailured();
            }
            this.changeFingerPrintState(SHTZTools.FINGER_STATE_COMPARE);
        }
    }

    private void processDeleteCommand() {
        this.buffer.skipBytes(8);
        this.buffer.discardReadBytes();
        this.processFingerList();
    }

    private void processCompareCommand() {
        this.buffer.skipBytes(2);
        int finger = this.buffer.readShort();
        int status = this.buffer.readByte();
        this.buffer.skipBytes(3);
        this.buffer.discardReadBytes();
        if (status == 0x08) {
            this.loggerPrint("SHTZSerialPort 指纹比对超时");
            this.changeFingerPrintState(SHTZTools.FINGER_STATE_COMPARE);
        } else if (status == 0x18) {
            this.loggerPrint("SHTZSerialPort 指纹比对中断");
            this.operationInterrupted();
        } else {
            if (finger == 0) {
                this.loggerPrint("SHTZSerialPort 指纹未注册");
                if(null != this.listener && null != this.listener.get()){
                    this.listener.get().onSHTZFingerUNRegistered();
                }
            } else {
                this.loggerPrint("SHTZSerialPort 指纹比对成功");
                if(null != this.listener && null != this.listener.get()){
                    this.listener.get().onSHTZFingerRecognized(finger);
                }
            }
            this.changeFingerPrintState(SHTZTools.FINGER_STATE_COMPARE);
        }
    }

    private void processDownloadCommand() throws IOException {
        this.buffer.markReaderIndex();
        this.buffer.skipBytes(2);
        int length = this.buffer.readShort();
        int state = this.buffer.readByte();
        this.buffer.skipBytes(3);
        if (state == 0x00) {
            if (this.buffer.readableBytes() < length + 3) {
                this.buffer.resetReaderIndex();
            } else {
                byte[] data = new byte[length + 3];
                this.buffer.readBytes(data, 0, length + 3);
                this.buffer.discardReadBytes();
                if (!SHTZTools.checkFrame(data)) {
                    throw new IOException("Finger data format error");
                }
                int finger = this.fingerList.get(this.currentIndex);
                if (!SHTZTools.writeFile(this.filePath + File.separator + finger + ".finger", data, data.length)) {
                    throw new IOException("Write finger file error");
                } else {
                    this.loggerPrint("SHTZSerialPort 指纹下载完成：" + finger);
                    this.processFingerList();
                }
            }
        } else {
            this.loggerPrint("SHTZSerialPort 指纹下载失败");
            if(null != this.listener && null != this.listener.get()){
                this.listener.get().onSHTZDownloadFailured();
            }
            this.changeFingerPrintState(SHTZTools.FINGER_STATE_COMPARE);
        }
    }

    private void processRegistFirstCommand() {
        this.buffer.skipBytes(8);
        this.buffer.discardReadBytes();
        this.loggerPrint("SHTZSerialPort 指纹注册第二步开始");
        if(null != this.listener && null != this.listener.get()){
            this.listener.get().onSHTZRegistStepChanged(2);
        }
        this.handler.sendEmptyMessageDelayed(SHTZTools.FINGER_COMMAND_REGIST_SECOND, 1000);
    }

    private void processRegistSecondCommand() {
        this.buffer.skipBytes(8);
        this.buffer.discardReadBytes();
        if(null != this.listener && null != this.listener.get()){
            this.listener.get().onSHTZRegistStepChanged(3);
        }
        this.loggerPrint("SHTZSerialPort 指纹注册第三步开始");
        this.handler.sendEmptyMessageDelayed(SHTZTools.FINGER_COMMAND_REGIST_THIRD, 1000);
    }

    private void processRegistThirdCommand() {
        this.buffer.skipBytes(2);
        int finger = this.buffer.readShort();
        int status = this.buffer.readByte();
        this.buffer.skipBytes(3);
        this.buffer.discardReadBytes();
        this.changeFingerPrintState(SHTZTools.FINGER_STATE_COMPARE);
        if (status == 0x00) {//注册成功
            this.loggerPrint("SHTZSerialPort 指纹注册成功");
            if(null != this.listener && null != this.listener.get()){
                this.listener.get().onSHTZRegistSuccessed(finger);
            }
        } else if (status == 0x08) {//超时
            this.loggerPrint("SHTZSerialPort 指纹注册超时");
            if(null != this.listener && null != this.listener.get()){
                this.listener.get().onSHTZRegistTimeout();
            }
        } else if (status == 0x07) {//重复注册
            this.loggerPrint("SHTZSerialPort 指纹注册重复");
            if(null != this.listener && null != this.listener.get()){
                this.listener.get().onSHTZFingerAlreadyExists();
            }
        } else {//注册失败
            this.loggerPrint("SHTZSerialPort 指纹注册失败");
            if(null != this.listener && null != this.listener.get()){
                this.listener.get().onSHTZRegistFailured();
            }
        }
    }

    private List<Integer> parseFingerList() {
        this.buffer.skipBytes(1);
        int length = this.buffer.readShort();
        int number = length / 3;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            int finger = this.buffer.readShort();
            int role = this.buffer.readByte();
            this.loggerPrint("SHTZSerialPort 发现已注册指纹：" + finger + "，分组：" + role);
            list.add(finger);
        }
        this.buffer.skipBytes(2);
        return list;
    }

    private void processFingerList() {
        this.currentIndex++;
        if (this.currentIndex < this.fingerList.size()) {
            int finger = this.fingerList.get(this.currentIndex);
            if (this.state == SHTZTools.FINGER_STATE_DOWNLOAD) {
                this.loggerPrint("SHTZSerialPort 指纹下载开始：" + finger);
                this.sendCommand(SHTZTools.FINGER_COMMAND_DOWNLOAD, finger);
            } else {
                if (this.isFingerValid(finger)) {
                    this.loggerPrint("SHTZSerialPort 发现已绑定用户指纹：" + finger);
                    this.processFingerList();
                } else {
                    this.loggerPrint("SHTZSerialPort 删除未绑定用户指纹：" + finger);
                    this.sendCommand(SHTZTools.FINGER_COMMAND_DELETE, finger);
                }
            }
        } else {
            if (this.state == SHTZTools.FINGER_STATE_REGIST) {
                this.loggerPrint("SHTZSerialPort 指纹注册第一步开始");
                if(null != this.listener && null != this.listener.get()){
                    this.listener.get().onSHTZRegistStepChanged(1);
                }
                this.sendCommand(SHTZTools.FINGER_COMMAND_REGIST_FIRST);
            } else {
                this.loggerPrint("SHTZSerialPort 指纹下载完毕");
                this.fireDownloadSuccessed();
                this.changeFingerPrintState(SHTZTools.FINGER_STATE_COMPARE);
            }
        }
    }

    private void processSearchFingerCommand() {
        this.buffer.markReaderIndex();
        this.buffer.skipBytes(2);
        int length = this.buffer.readShort();
        int state = this.buffer.readByte();
        this.buffer.skipBytes(3);
        if (state == 0x00) {
            if (this.buffer.readableBytes() < length + 3) {
                this.buffer.resetReaderIndex();
            } else {
                this.fingerList = this.parseFingerList();
                this.buffer.discardReadBytes();
                this.currentIndex = -1;
                this.processFingerList();
            }
        } else {
            this.loggerPrint("SHTZSerialPort 无已注册指纹");
            this.buffer.discardReadBytes();
            if (this.state == SHTZTools.FINGER_STATE_REGIST) {
                this.loggerPrint("SHTZSerialPort 指纹注册第一步开始");
                if(null != this.listener && null != this.listener.get()){
                    this.listener.get().onSHTZRegistStepChanged(1);
                }
                this.sendCommand(SHTZTools.FINGER_COMMAND_REGIST_FIRST);
            } else if (this.state == SHTZTools.FINGER_STATE_DOWNLOAD) {
                this.loggerPrint("SHTZSerialPort 无可下载的指纹特征");
                if(null != this.listener && null != this.listener.get()){
                    this.listener.get().onSHTZNoFingerExist();
                }
                this.changeFingerPrintState(SHTZTools.FINGER_STATE_COMPARE);
            }
        }
    }

    private void processRefuseRepeatCommand() {
        this.buffer.skipBytes(8);
        this.buffer.discardReadBytes();
        this.changeFingerPrintState(SHTZTools.FINGER_STATE_COMPARE);
    }

    private void processHandDetectionCommand() {
        this.buffer.skipBytes(8);
        this.buffer.discardReadBytes();
        this.loggerPrint("SHTZSerialPort 查询所有已注册指纹");
        this.sendCommand(SHTZTools.FINGER_COMMAND_SEARCH_FINGER);
    }


    @Override
    public void onConnected(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.ready = false;
        this.buffer.clear();
        if(null != this.listener && null != this.listener.get()){
            this.listener.get().onSHTZConnected();
        }
        this.changeFingerPrintState(SHTZTools.FINGER_STATE_REGIST_MODEL);
    }

    @Override
    public void onReadThreadReleased(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onSHTZPrint("SHTZSerialPort read thread released");
        }
    }


    @Override
    public void onException(PWSerialPortHelper helper, Throwable throwable) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if(null != this.listener && null != this.listener.get()){
            this.listener.get().onSHTZException(throwable);
        }
        if (this.enabled) {
            if (this.state == SHTZTools.FINGER_STATE_REGIST) {
                this.loggerPrint("SHTZSerialPort 指纹注册失败");
                if(null != this.listener && null != this.listener.get()){
                    this.listener.get().onSHTZRegistFailured();
                }
            } else if (this.state == SHTZTools.FINGER_STATE_UPLOAD) {
                this.loggerPrint("SHTZSerialPort 指纹上传失败");
                if(null != this.listener && null != this.listener.get()){
                    this.listener.get().onSHTZUploadFailured();
                }
            } else if (this.state == SHTZTools.FINGER_STATE_DOWNLOAD) {
                this.loggerPrint("SHTZSerialPort 指纹下载失败");
                if(null != this.listener && null != this.listener.get()){
                    this.listener.get().onSHTZDownloadFailured();
                }
            }
            if(null != this.listener && null != this.listener.get()){
                this.listener.get().onSHTZReset();
            }
        }
    }

    @Override
    public void onStateChanged(PWSerialPortHelper helper, PWSerialPortState state) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onSHTZPrint("SHTZSerialPort state changed: " + state.name());
        }
    }

    @Override
    public void onByteReceived(PWSerialPortHelper helper, byte[] buffer, int length) throws IOException {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (!this.ready) {
            this.ready = true;
            if(null != this.listener && null != this.listener.get()){
                this.listener.get().onSHTZReady();
            }
        }
        this.buffer.writeBytes(buffer, 0, length);
        if (this.buffer.readableBytes() < 8) {
            return;
        }
        this.buffer.markReaderIndex();
        byte[] data = new byte[8];
        this.buffer.readBytes(data, 0, 8);
        this.buffer.resetReaderIndex();
        if (!SHTZTools.checkFrame(data)) {
            return;
        }
        this.loggerPrint("SHTZSerialPort Recv:" + SHTZTools.bytes2HexString(data, true, ", "));
        int command = data[1] & 0xff;
        switch (command) {
            case SHTZTools.FINGER_COMMAND_CLEAR://删除所有已注册指纹
                this.processClearCommand();
                break;
            case SHTZTools.FINGER_COMMAND_BREAK://中断当前操作
                this.processBreakCommand();
                break;
            case SHTZTools.FINGER_COMMAND_UPLOAD://中断当前操作
                this.processUploadCommand();
                break;
            case SHTZTools.FINGER_COMMAND_DELETE:
                this.processDeleteCommand();
                break;
            case SHTZTools.FINGER_COMMAND_COMPARE:
                this.processCompareCommand();
                break;
            case SHTZTools.FINGER_COMMAND_DOWNLOAD:
                this.processDownloadCommand();
                break;
            case SHTZTools.FINGER_COMMAND_REGIST_FIRST://指纹注册第一步结果
                this.processRegistFirstCommand();
                break;
            case SHTZTools.FINGER_COMMAND_REGIST_SECOND://指纹注册第二步结果
                this.processRegistSecondCommand();
                break;
            case SHTZTools.FINGER_COMMAND_REGIST_THIRD: //指纹注册第三步结果
                this.processRegistThirdCommand();
                break;
            case SHTZTools.FINGER_COMMAND_SEARCH_FINGER://查询所有已经注册的指纹
                this.processSearchFingerCommand();
                break;
            case SHTZTools.FINGER_COMMAND_REGIST_REFUSE_REPEAT://设置拒绝重复注册
                this.processRefuseRepeatCommand();
                break;
            case SHTZTools.FINGER_COMMAND_REGIST_HAND_DETECTION://设置抬手检测
                this.processHandDetectionCommand();
                break;
            default:
                this.processUnknownCommand();
                break;
        }
    }

    private class TZFPHandler extends Handler {
        public TZFPHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x0c:
                    if (state == SHTZTools.FINGER_STATE_COMPARE) {
                        sendCommand(SHTZTools.FINGER_COMMAND_COMPARE);
                    }
                    break;
                case 0x02:
                    if (state == SHTZTools.FINGER_STATE_REGIST) {
                        sendCommand(SHTZTools.FINGER_COMMAND_REGIST_SECOND);
                    }
                    break;
                case 0x03:
                    if (state == SHTZTools.FINGER_STATE_REGIST) {
                        sendCommand(SHTZTools.FINGER_COMMAND_REGIST_THIRD);
                    }
                    break;
            }
        }
    }
}