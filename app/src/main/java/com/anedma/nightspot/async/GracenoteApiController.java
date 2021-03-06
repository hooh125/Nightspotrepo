package com.anedma.nightspot.async;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.anedma.nightspot.async.response.GracenoteResponse;
import com.anedma.nightspot.dto.Track;
import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAlbumIterator;
import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnList;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnLog;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnLookupLocalStream;
import com.gracenote.gnsdk.GnLookupLocalStreamIngest;
import com.gracenote.gnsdk.GnLookupLocalStreamIngestStatus;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnMic;
import com.gracenote.gnsdk.GnMusicId;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdStream;
import com.gracenote.gnsdk.GnMusicIdStreamIdentifyingStatus;
import com.gracenote.gnsdk.GnMusicIdStreamPreset;
import com.gracenote.gnsdk.GnMusicIdStreamProcessingStatus;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.GnStorageSqlite;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;
import com.gracenote.gnsdk.IGnAudioSource;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnLookupLocalStreamIngestEvents;
import com.gracenote.gnsdk.IGnMusicIdStreamEvents;
import com.gracenote.gnsdk.IGnSystemEvents;

import org.apache.commons.validator.routines.UrlValidator;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 21/10/2017.
 *
 */

public class GracenoteApiController implements IGnSystemEvents, IGnMusicIdStreamEvents {

    private static final String LOG_TAG = "GRACENOTEAPICONTROLLER";
    private static GracenoteApiController instance;
    private static GracenoteResponse delegate;
    private Context context;
    private static final String gnsdkClientId = "933788263";
    private static final String gnsdkClientTag = "A8B4ECE6DAE23A832C82CB041EAE6EBF";
    private static final String gnsdkLicenseFilename = "license.txt";
    private static final String appString = "NightSpot";
    private boolean isProcessing = false;


    // Gracenote objects
    private GnManager gnManager;
    private GnUser gnUser;
    private GnMusicIdStream gnMusicIdStream;
    private IGnAudioSource gnMicrophone;
    private GnLocale gnLocale;
    private GnLog gnLog;
    private List<GnMusicId> idObjects = new ArrayList<>();
    private List<GnMusicIdFile> fileIdObjects = new ArrayList<>();
    private List<GnMusicIdStream> streamIdObjects = new ArrayList<>();
    private long lastLookup_startTime;

    private GracenoteApiController(Context context, GracenoteResponse delegate) throws GnException {
        this.context = context;
        this.delegate = delegate;
        gnManager = new GnManager(this.context, getAssetAsString(gnsdkLicenseFilename), GnLicenseInputMode.kLicenseInputModeString);
        gnManager.systemEventHandler(this);
        gnUser = new GnUser(new GnUserStore(context), gnsdkClientId, gnsdkClientTag, appString);
        GnStorageSqlite.enable();
        GnLookupLocalStream.enable();
        Thread ingestThread = new Thread(new LocalBundleIngestRunnable(context));
        ingestThread.start();
        gnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic,
                        GnLanguage.kLanguageEnglish,
                        GnRegion.kRegionGlobal,
                        GnDescriptor.kDescriptorDefault,
                        gnUser);
        gnLocale.setGroupDefault();
        gnMicrophone = new AudioVisualizeAdapter(new GnMic());
        gnMusicIdStream = new GnMusicIdStream(gnUser, GnMusicIdStreamPreset.kPresetMicrophone, this);
        gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataContent, true);
        gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataSonicData, true);
        gnMusicIdStream.options().resultSingle(true);
        streamIdObjects.add(gnMusicIdStream);
    }

    public static GracenoteApiController getInstance(Context context, GracenoteResponse delegate) throws GnException {
        if (instance == null) {
            instance = new GracenoteApiController(context, delegate);
        }
        return instance;
    }

    public void killInstance() {
        stopAudioProcessing();
        instance = null;
        delegate = null;
    }

    public void startAudioProcessing() {
        if (gnMusicIdStream != null) {

            // Create a thread to process the data pulled from GnMic
            // Internally pulling data is a blocking call, repeatedly called until
            // audio processing is stopped. This cannot be called on the main thread.
            Thread audioProcessThread = new Thread(new AudioProcessRunnable());
            audioProcessThread.start();
            delegate.audioProcessStarted();
        }
    }

    public void stopAudioProcessing() {
        if (gnMusicIdStream != null) {

            try {

                // to ensure no pending identifications deliver results while your app is
                // paused it is good practice to call cancel
                // it is safe to call identifyCancel if no identify is pending
                gnMusicIdStream.identifyCancel();

                // stopping audio processing stops the audio processing thread started
                // in onResume
                gnMusicIdStream.audioProcessStop();
                delegate.audioProcessStopped();
            } catch (GnException e) {

                Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() + " : " + e.errorAPI() + ": " + e.errorDescription());

            }

        }
    }



    public void startIdentify() {
        isProcessing = true;
        try {
            gnMusicIdStream.identifyAlbumAsync();
            lastLookup_startTime = SystemClock.elapsedRealtime();
        } catch (GnException e) {
            Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() + ", " + e.errorAPI() + ": " + e.errorDescription());
        }
    }

    private String getAssetAsString(String assetName) {

        String assetString = null;
        InputStream assetStream;

        try {

            assetStream = context.getAssets().open(assetName);
            if (assetStream != null) {

                java.util.Scanner s = new java.util.Scanner(assetStream).useDelimiter("\\A");

                assetString = s.hasNext() ? s.next() : "";
                assetStream.close();

            } else {
                Log.e(appString, "Asset not found:" + assetName);
            }

        } catch (IOException e) {

            Log.e(appString, "Error getting asset as string: " + e.getMessage());

        }

        return assetString;
    }

    @Override
    public void localeUpdateNeeded(GnLocale gnLocale) {

    }

    @Override
    public void listUpdateNeeded(GnList gnList) {

    }

    @Override
    public void systemMemoryWarning(long l, long l1) {

    }

    @Override
    public void musicIdStreamProcessingStatusEvent(GnMusicIdStreamProcessingStatus gnMusicIdStreamProcessingStatus, IGnCancellable iGnCancellable) {
        //Log.d(LOG_TAG, "Status event processing: isCancelled" + iGnCancellable.isCancelled());
    }

    @Override
    public void musicIdStreamIdentifyingStatusEvent(GnMusicIdStreamIdentifyingStatus gnMusicIdStreamIdentifyingStatus, IGnCancellable iGnCancellable) {
        //Log.d(LOG_TAG, "Status event indentifying: isCancelled" + iGnCancellable.isCancelled());
    }

    @Override
    public void musicIdStreamAlbumResult(GnResponseAlbums gnResponseAlbums, IGnCancellable iGnCancellable) {
        Log.d("RESPONSE", "Gracenote ha devuelto " + gnResponseAlbums.resultCount());
        if (gnResponseAlbums.resultCount() > 0) {
            GnAlbumIterator iterator = gnResponseAlbums.albums().getIterator();
            while (iterator.hasNext()) {
                try {
                    GnAlbum gnAlbum = iterator.next();
                    URL albumImgURL;
                    try {
                        albumImgURL = new URL(gnAlbum.coverArt().assets().getIterator().next().urlHttp());
                    } catch (MalformedURLException e) {
                        albumImgURL = null;
                    }
                    String trackArtist = gnAlbum.trackMatched().artist().name().display();
                    String albumArtist = gnAlbum.artist().name().display();
                    String artist = (!trackArtist.isEmpty()) ? trackArtist : albumArtist;
                    String song = gnAlbum.trackMatched().title().display();
                    String album = gnAlbum.title().display();
                    Track track;
                    if (checkValidURL(albumImgURL) && albumImgURL != null) {
                        track = new Track(artist, song, album, albumImgURL);
                    } else {
                        track = new Track(artist, song, album);
                    }
                    if(delegate != null) {
                        delegate.trackReturned(track);
                    }
                } catch (GnException e) {
                    e.printStackTrace();
                }
            }
        }
        isProcessing = false;
        delegate.processingFinished();
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    private boolean checkValidURL(URL url) {
        String[] schemes = {"http", "https"}; // DEFAULT schemes = "http", "https", "ftp"
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (urlValidator.isValid(url.toString())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void musicIdStreamIdentifyCompletedWithError(GnError gnError) {
        isProcessing = false;
        delegate.processingFinished();
    }

    @Override
    public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {

    }

    class LocalBundleIngestRunnable implements Runnable {
        Context context;

        LocalBundleIngestRunnable(Context context) {
            this.context = context;
        }

        public void run() {
            try {

                // our bundle is delivered as a package asset
                // to ingest the bundle access it as a stream and write the bytes to
                // the bundle ingester
                // bundles should not be delivered with the package as this, rather they
                // should be downloaded from your own online service

                InputStream bundleInputStream = null;
                int ingestBufferSize = 1024;
                byte[] ingestBuffer = new byte[ingestBufferSize];
                int bytesRead = 0;

                GnLookupLocalStreamIngest ingester = new GnLookupLocalStreamIngest(new BundleIngestEvents());

                try {

                    bundleInputStream = context.getAssets().open("1557.b");

                    do {

                        bytesRead = bundleInputStream.read(ingestBuffer, 0, ingestBufferSize);
                        if (bytesRead == -1)
                            bytesRead = 0;

                        ingester.write(ingestBuffer, bytesRead);

                    } while (bytesRead != 0);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                ingester.flush();

            } catch (GnException e) {
                Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            }

        }
    }

    class AudioProcessRunnable implements Runnable {

        @Override
        public void run() {
            try {

                // start audio processing with GnMic, GnMusicIdStream pulls data from GnMic internally
                gnMusicIdStream.audioProcessStart(gnMicrophone);

            } catch (GnException e) {

                Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() + ": " + e.errorAPI() + ": " + e.errorDescription());
            }
        }
    }

    private class BundleIngestEvents implements IGnLookupLocalStreamIngestEvents {

        @Override
        public void statusEvent(GnLookupLocalStreamIngestStatus status, String bundleId, IGnCancellable canceller) {
            Log.d("Bundle ingest:", status.toString());
        }
    }

    class AudioVisualizeAdapter implements IGnAudioSource {

        private IGnAudioSource audioSource;
        private int numBitsPerSample;
        private int numChannels;

        public AudioVisualizeAdapter(IGnAudioSource audioSource) {
            this.audioSource = audioSource;
        }

        @Override
        public long sourceInit() {
            if (audioSource == null) {
                return 1;
            }
            long retVal = audioSource.sourceInit();

            // get format information for use later
            if (retVal == 0) {
                numBitsPerSample = (int) audioSource.sampleSizeInBits();
                numChannels = (int) audioSource.numberOfChannels();
            }

            return retVal;
        }

        @Override
        public long numberOfChannels() {
            return numChannels;
        }

        @Override
        public long sampleSizeInBits() {
            return numBitsPerSample;
        }

        @Override
        public long samplesPerSecond() {
            if (audioSource == null) {
                return 0;
            }
            return audioSource.samplesPerSecond();
        }

        @Override
        public long getData(ByteBuffer buffer, long bufferSize) {
            if (audioSource == null) {
                return 0;
            }

            long numBytes = audioSource.getData(buffer, bufferSize);

            if (numBytes != 0) {
                // perform visualization effect here
                // Note: Since API level 9 Android provides android.media.audiofx.Visualizer which can be used to obtain the
                // raw waveform or FFT, and perform measurements such as peak RMS. You may wish to consider Visualizer class
                // instead of manually extracting the audio as shown here.
                // This sample does not use Visualizer so it can demonstrate how you can access the raw audio for purposes
                // not limited to visualization.
                //audioVisualizeDisplay.setAmplitudePercent(rmsPercentOfMax(buffer,bufferSize,numBitsPerSample,numChannels), true);
            }

            return numBytes;
        }

        @Override
        public void sourceClose() {
            if (audioSource != null) {
                audioSource.sourceClose();
            }
        }

        // calculate the rms as a percent of maximum
        private int rmsPercentOfMax(ByteBuffer buffer, long bufferSize, int numBitsPerSample, int numChannels) {
            double rms = 0.0;
            if (numBitsPerSample == 8) {
                rms = rms8(buffer, bufferSize, numChannels);
                return (int) ((rms * 100) / (double) ((double) (Byte.MAX_VALUE / 2)));
            } else {
                rms = rms16(buffer, bufferSize, numChannels);
                return (int) ((rms * 100) / (double) ((double) (Short.MAX_VALUE / 2)));
            }
        }

        // calculate the rms of a buffer containing 8 bit audio samples
        private double rms8(ByteBuffer buffer, long bufferSize, int numChannels) {

            long sum = 0;
            long numSamplesPerChannel = bufferSize / numChannels;

            for (int i = 0; i < numSamplesPerChannel; i += numChannels) {
                byte sample = buffer.get();
                sum += (sample * sample);
            }

            return Math.sqrt((double) (sum / numSamplesPerChannel));
        }

        // calculate the rms of a buffer containing 16 bit audio samples
        private double rms16(ByteBuffer buffer, long bufferSize, int numChannels) {

            long sum = 0;
            long numSamplesPerChannel = (bufferSize / 2) / numChannels;    // 2 bytes per sample

            buffer.rewind();
            for (int i = 0; i < numSamplesPerChannel; i++) {
                short sample = Short.reverseBytes(buffer.getShort()); // reverse because raw data is little endian but Java short is big endian

                sum += (sample * sample);
                if (numChannels == 2) {
                    buffer.getShort();
                }
            }

            return Math.sqrt((double) (sum / numSamplesPerChannel));
        }
    }

}
