package android.content;

import android.os.Parcel;
import android.os.Parcelable;

public final class ContentCaptureOptions implements Parcelable {
    public static final Creator<ContentCaptureOptions> CREATOR = new Creator<ContentCaptureOptions>() {
        @Override
        public ContentCaptureOptions createFromParcel(Parcel in) {
            return new ContentCaptureOptions();
        }

        @Override
        public ContentCaptureOptions[] newArray(int size) {
            return new ContentCaptureOptions[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
