package cn.haier.bio.medical.fp.shtz;

public interface ISHTZListener {
    void onSHTZReady();
    void onSHTZReset();
    void onSHTZConnected();
    void onSHTZDisconnected();
    void onSHTZPrint(String message);
    void onSHTZBusyChanged(boolean busy);
    void onSHTZException(Throwable throwable);

    void onSHTZRegistStated();
    void onSHTZRegistTimeout();
    void onSHTZRegistCanceled();
    void onSHTZRegistFailured();
    void onSHTZFingerAlreadyExists();
    void onSHTZRegistStepChanged(int step);
    void onSHTZRegistSuccessed(int finger);

    void onSHTZClearStarted();
    void onSHTZClearSuccessed();

    void onSHTZDeleteStarted();
    void onSHTZDeleteSuccessed();

    void onSHTZUploadStated();
    void onSHTZUploadFailured();
    void onSHTZUploadSuccessed();

    void onSHTZNoFingerExist();
    void onSHTZDownloadStated();
    void onSHTZDownloadFailured();
    void onSHTZDownloadSuccessed();
    void onSHTZFingerUNRegistered();
    void onSHTZFingerRecognized(int finger);

    boolean isFingerValid(int finger);

}
