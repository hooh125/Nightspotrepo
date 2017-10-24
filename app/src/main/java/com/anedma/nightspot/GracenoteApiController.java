package com.anedma.nightspot;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.anedma.nightspot.database.FingerprintDbHelper;
import com.anedma.nightspot.dto.Fingerprint;
import com.anedma.nightspot.exception.FingerprintInsertException;
import com.gracenote.gnsdk.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by a-edu on 21/10/2017.
 */

public class GracenoteApiController implements IGnSystemEvents, IGnMusicIdStreamEvents {

    private static GracenoteApiController instance;
    private Context context;
    private Activity activity;
    private static final String 				gnsdkClientId 			= "933788263";
    private static final String 				gnsdkClientTag 			= "A8B4ECE6DAE23A832C82CB041EAE6EBF";
    private static final String 				gnsdkLicenseFilename 	= "license.txt";
    private static final String appString = "NightSpot";
    private boolean isProcessing = false;


    // Gracenote objects
    private GnManager gnManager;
    private GnUser gnUser;
    private GnMusicIdStream gnMusicIdStream;
    private IGnAudioSource gnMicrophone;
    private GnLocale gnLocale;
    private GnLog gnLog;
    private List<GnMusicId> idObjects				= new ArrayList<>();
    private List<GnMusicIdFile>			fileIdObjects			= new ArrayList<>();
    private List<GnMusicIdStream>		streamIdObjects			= new ArrayList<>();
    private long lastLookup_startTime;

    private GracenoteApiController(Context context, Activity activity) throws GnException {
        this.context = context;
        this.activity = activity;
        gnManager = new GnManager(this.context, getAssetAsString(gnsdkLicenseFilename), GnLicenseInputMode.kLicenseInputModeString);
        gnManager.systemEventHandler(this);
        gnUser = new GnUser( new GnUserStore(context), gnsdkClientId, gnsdkClientTag, appString );
        GnStorageSqlite.enable();
        GnLookupLocalStream.enable();
        Thread ingestThread = new Thread( new LocalBundleIngestRunnable(context) );
        ingestThread.start();
        gnLocale =
                new GnLocale(GnLocaleGroup.kLocaleGroupMusic,
                        GnLanguage.kLanguageEnglish,
                        GnRegion.kRegionGlobal,
                        GnDescriptor.kDescriptorDefault,
                        gnUser);
        gnLocale.setGroupDefault();
        gnMicrophone = new AudioVisualizeAdapter( new GnMic() );
        gnMusicIdStream = new GnMusicIdStream( gnUser, GnMusicIdStreamPreset.kPresetMicrophone, this);
        gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataContent, true);
        gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataSonicData, true);
        gnMusicIdStream.options().resultSingle( true );
        streamIdObjects.add( gnMusicIdStream );
    }

    static GracenoteApiController getInstance(Context context, Activity activity) throws GnException {
        if(instance == null) {
            instance = new GracenoteApiController(context, activity);
        }
        return instance;
    }

    void startAudioProcessing() {
        if ( gnMusicIdStream != null ) {

            // Create a thread to process the data pulled from GnMic
            // Internally pulling data is a blocking call, repeatedly called until
            // audio processing is stopped. This cannot be called on the main thread.
            Thread audioProcessThread = new Thread(new AudioProcessRunnable());
            audioProcessThread.start();

        }
    }

    void stopAudioProcessing() {
        if ( gnMusicIdStream != null ) {

            try {

                // to ensure no pending identifications deliver results while your app is
                // paused it is good practice to call cancel
                // it is safe to call identifyCancel if no identify is pending
                gnMusicIdStream.identifyCancel();

                // stopping audio processing stops the audio processing thread started
                // in onResume
                gnMusicIdStream.audioProcessStop();

            } catch (GnException e) {

                Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() + " : " + e.errorAPI() + ": " +  e.errorDescription());

            }

        }
    }

    void startIdentify() {
        isProcessing = true;
        try {
            gnMusicIdStream.identifyAlbumAsync();
            lastLookup_startTime = SystemClock.elapsedRealtime();
        } catch (GnException e) {
            Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() + ", " + e.errorAPI() + ": " +  e.errorDescription() );
        }
    }

    private String getAssetAsString( String assetName ){

        String 		assetString = null;
        InputStream assetStream;

        try {

            assetStream = context.getAssets().open(assetName);
            if(assetStream != null){

                java.util.Scanner s = new java.util.Scanner(assetStream).useDelimiter("\\A");

                assetString = s.hasNext() ? s.next() : "";
                assetStream.close();

            }else{
                Log.e(appString, "Asset not found:" + assetName);
            }

        } catch (IOException e) {

            Log.e( appString, "Error getting asset as string: " + e.getMessage() );

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

    }

    @Override
    public void musicIdStreamIdentifyingStatusEvent(GnMusicIdStreamIdentifyingStatus gnMusicIdStreamIdentifyingStatus, IGnCancellable iGnCancellable) {

    }

    @Override
    public void musicIdStreamAlbumResult(GnResponseAlbums gnResponseAlbums, IGnCancellable iGnCancellable) {
        Log.d("RESPONSE", "Gracenote ha devuelto " + gnResponseAlbums.resultCount());
        if(gnResponseAlbums.resultCount() > 0) {
            GnAlbumIterator iterator = gnResponseAlbums.albums().getIterator();
            while(iterator.hasNext()) {
                try {
                    GnAlbum gnAlbum = iterator.next();
                    Log.d("MATCH", "Se ha encontrado la cancion del artista: " + gnAlbum.artist().name().display());
                    String artist = gnAlbum.artist().name().display();
                    String song = gnAlbum.trackMatched().title().display();
                    String genre = gnAlbum.trackMatched().genre(GnDataLevel.kDataLevelInvalid);
                    String album = gnAlbum.title().display();
                    Fingerprint fp = new Fingerprint(artist, song, genre, album);
                    FingerprintDbHelper dbHelper = new FingerprintDbHelper(context);
                    dbHelper.insertFingerprint(fp);
                    dbHelper.close();
                } catch (GnException | FingerprintInsertException e) {
                    e.printStackTrace();
                }
            }
        }
        isProcessing = false;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    @Override
    public void musicIdStreamIdentifyCompletedWithError(GnError gnError) {
        isProcessing = false;
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

                InputStream 	bundleInputStream 	= null;
                int				ingestBufferSize	= 1024;
                byte[] 			ingestBuffer 		= new byte[ingestBufferSize];
                int				bytesRead			= 0;

                GnLookupLocalStreamIngest ingester = new GnLookupLocalStreamIngest(new BundleIngestEvents());

                try {

                    bundleInputStream = context.getAssets().open("1557.b");

                    do {

                        bytesRead = bundleInputStream.read(ingestBuffer, 0, ingestBufferSize);
                        if ( bytesRead == -1 )
                            bytesRead = 0;

                        ingester.write( ingestBuffer, bytesRead );

                    } while( bytesRead != 0 );

                } catch (IOException e) {
                    e.printStackTrace();
                }

                ingester.flush();

            } catch (GnException e) {
                Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
            }

        }
    }

    class AudioProcessRunnable implements Runnable {

        @Override
        public void run() {
            try {

                // start audio processing with GnMic, GnMusicIdStream pulls data from GnMic internally
                gnMusicIdStream.audioProcessStart( gnMicrophone );

            } catch (GnException e) {

                Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() + ": " + e.errorAPI() + ": " +  e.errorDescription()  );
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

        private IGnAudioSource 	audioSource;
        private int				numBitsPerSample;
        private int				numChannels;

        public AudioVisualizeAdapter( IGnAudioSource audioSource ){
            this.audioSource = audioSource;
        }

        @Override
        public long sourceInit() {
            if ( audioSource == null ){
                return 1;
            }
            long retVal = audioSource.sourceInit();

            // get format information for use later
            if ( retVal == 0 ) {
                numBitsPerSample = (int)audioSource.sampleSizeInBits();
                numChannels = (int)audioSource.numberOfChannels();
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
            if ( audioSource == null ){
                return 0;
            }
            return audioSource.samplesPerSecond();
        }

        @Override
        public long getData(ByteBuffer buffer, long bufferSize) {
            if ( audioSource == null ){
                return 0;
            }

            long numBytes = audioSource.getData(buffer, bufferSize);

            if ( numBytes != 0 ) {
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
            if ( audioSource != null ){
                audioSource.sourceClose();
            }
        }

        // calculate the rms as a percent of maximum
        private int rmsPercentOfMax( ByteBuffer buffer, long bufferSize, int numBitsPerSample, int numChannels) {
            double rms = 0.0;
            if ( numBitsPerSample == 8 ) {
                rms = rms8( buffer, bufferSize, numChannels );
                return (int)((rms*100)/(double)((double)(Byte.MAX_VALUE/2)));
            } else {
                rms = rms16( buffer, bufferSize, numChannels );
                return (int)((rms*100)/(double)((double)(Short.MAX_VALUE/2)));
            }
        }

        // calculate the rms of a buffer containing 8 bit audio samples
        private double rms8 ( ByteBuffer buffer, long bufferSize, int numChannels ) {

            long sum = 0;
            long numSamplesPerChannel = bufferSize/numChannels;

            for(int i = 0; i < numSamplesPerChannel; i+=numChannels)
            {
                byte sample = buffer.get();
                sum += (sample * sample);
            }

            return Math.sqrt( (double)(sum / numSamplesPerChannel) );
        }

        // calculate the rms of a buffer containing 16 bit audio samples
        private double rms16 ( ByteBuffer buffer, long bufferSize, int numChannels ) {

            long sum = 0;
            long numSamplesPerChannel = (bufferSize/2)/numChannels;	// 2 bytes per sample

            buffer.rewind();
            for(int i = 0; i < numSamplesPerChannel; i++)
            {
                short sample = Short.reverseBytes(buffer.getShort()); // reverse because raw data is little endian but Java short is big endian

                sum += (sample * sample);
                if ( numChannels == 2 ){
                    buffer.getShort();
                }
            }

            return Math.sqrt( (double)(sum / numSamplesPerChannel) );
        }
    }

}
