package cn.qd.peiwen.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import cn.haier.bio.medical.fp.shtz.ISHTZListener;
import cn.haier.bio.medical.fp.shtz.SHTZManager;

public class MainActivity extends AppCompatActivity implements ISHTZListener {
    private int finger = 0;
    private boolean fingerBusy = true;
    private boolean register = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String path = "/dev/ttyUSB0"; //沃特超低温hub板地址
        SHTZManager.getInstance().init(path);
        SHTZManager.getInstance().changeListener(this);
        SHTZManager.getInstance().enable();
    }


    @Override
    public void onSHTZReady() {
        Log.d("TAG","onSHTZReady");
    }

    @Override
    public void onSHTZReset() {
        Log.d("TAG","onSHTZReset");
    }


    @Override
    public void onSHTZConnected() {
        Log.d("TAG","onSHTZConnected");
    }

    @Override
    public void onSHTZDisconnected() {
        Log.d("TAG","onSHTZDisconnected");
    }

    @Override
    public void onSHTZPrint(String message) {
        Log.d("TAG","" + message);
    }


    @Override
    public void onSHTZBusyChanged(boolean busy) {
        this.fingerBusy = busy;
    }

    @Override
    public void onSHTZException(Throwable throwable) {
        Log.d("TAG","onSHTZException");
    }

    @Override
    public void onSHTZRegistStated() {
        this.register = true;
        Log.d("TAG","onSHTZRegistStated");
    }

    @Override
    public void onSHTZRegistTimeout() {
        this.register = false;
        Log.d("TAG","onSHTZRegistTimeout");
    }

    @Override
    public void onSHTZRegistCanceled() {
        this.register = false;
        Log.d("TAG","onSHTZRegistCanceled");
    }

    @Override
    public void onSHTZRegistFailured() {
        this.register = false;
        Log.d("TAG","onSHTZRegistFailured");
    }

    @Override
    public void onSHTZFingerAlreadyExists() {
        this.register = false;
        Log.d("TAG","onSHTZFingerAlreadyExists");
    }

    @Override
    public void onSHTZRegistStepChanged(int step) {
        Log.d("TAG","onSHTZRegistStepChanged");
    }

    @Override
    public void onSHTZRegistSuccessed(int finger) {
        this.finger = finger;
        this.register = false;
        Log.d("TAG","onSHTZRegistSuccessed " + finger);
    }

    @Override
    public void onSHTZUploadStated() {
        Log.d("TAG","onSHTZUploadStated");
    }

    @Override
    public void onSHTZUploadFailured() {
        Log.d("TAG","onSHTZUploadFailured");
    }

    @Override
    public void onSHTZUploadSuccessed() {
        Log.d("TAG","onSHTZUploadSuccessed");
    }

    @Override
    public void onSHTZNoFingerExist() {
        Log.d("TAG","onSHTZNoFingerExist");
    }

    @Override
    public void onSHTZDownloadStated() {
        Log.d("TAG","onSHTZDownloadStated");
    }

    @Override
    public void onSHTZDownloadFailured() {
        Log.d("TAG","onSHTZDownloadFailured");
    }

    @Override
    public void onSHTZDownloadSuccessed() {
        Log.d("TAG","onSHTZDownloadSuccessed");
    }

    @Override
    public void onSHTZFingerUNRegistered() {
        Log.d("TAG","onSHTZFingerUNRegistered");
    }

    @Override
    public void onSHTZFingerRecognized(int finger) {
        Log.d("TAG","onSHTZFingerRecognized " + finger);
    }

    @Override
    public boolean isFingerValid(int finger) {
        return true;
    }


    @Override
    public void onSHTZClearStarted() {
        Log.d("TAG","onSHTZClearStarted " + this.finger);
    }

    @Override
    public void onSHTZClearSuccessed() {
        Log.d("TAG","onSHTZClearSuccessed " + this.finger);
    }

    @Override
    public void onSHTZDeleteStarted() {
        Log.d("TAG","onSHTZDeleteStarted " + this.finger);
    }

    @Override
    public void onSHTZDeleteSuccessed() {
        Log.d("TAG","onSHTZDeleteSuccessed " + this.finger);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.register:
                if(!this.fingerBusy) {
                    SHTZManager.getInstance().regist();
                }
                break;
            case R.id.cancel:
                if(this.register) {
                    SHTZManager.getInstance().cancelRegist();
                }
                break;
            case R.id.delete:
                if(this.finger != 0 && !this.fingerBusy) {
                    SHTZManager.getInstance().delete(this.finger);
                }
                break;
            case R.id.clear:
                if(this.finger != 0 && !this.fingerBusy) {
                    SHTZManager.getInstance().clear();
                }
                break;
        }
    }
}
