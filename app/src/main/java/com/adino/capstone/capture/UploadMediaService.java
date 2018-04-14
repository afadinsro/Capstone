package com.adino.capstone.capture;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.adino.capstone.R;
import com.adino.capstone.glide.GlideApp;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

import static com.adino.capstone.util.Constants.IMAGE_FILE_ABS_PATH;
import static com.adino.capstone.util.Constants.PHOTOS;
import static com.adino.capstone.util.Constants.PUSHED_REPORT_KEY;
import static com.adino.capstone.util.Constants.REPORTS;
import static com.adino.capstone.util.Constants.REPORT_FIELD_IMAGEURL;

/**
 * Created by afadinsro on 3/12/18.
 */

public class UploadMediaService extends JobService {

    private File mediaFile;
    private String key;
    private UploadTask uploadTask;
    private String path;
    private Activity activity;
    private boolean retry = true;
    private boolean moreWork = true;
    private static UploadImageTask imageTask;
    private static final String TAG = "UploadMediaService";
    private StorageReference mPhotosStorageReference;
    private DatabaseReference reference;
    private Thread uploadThread;


    //@SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(final JobParameters job) {
        Log.d(TAG, "onStartJob: Job started");
        Toast.makeText(getApplicationContext(), "In background service.", Toast.LENGTH_SHORT).show();
        uploadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: Thread running...");
                Context context = getApplicationContext();
                mPhotosStorageReference = FirebaseStorage.getInstance().getReference().child(PHOTOS);
                if(job.getExtras() != null){
                    key = job.getExtras().getString(PUSHED_REPORT_KEY);
                    Log.d(TAG, "onStartJob: Push key: " + key);
                    path = job.getExtras().getString(IMAGE_FILE_ABS_PATH);
                    mediaFile = (path != null) ? new File(path) : null;
                    Log.d(TAG, "onStartJob: Media file path: " + mediaFile);
                }
                uploadImage();

            }
        });
        uploadThread.start();


        if(job.getExtras() != null){
            key = job.getExtras().getString(PUSHED_REPORT_KEY);
            Log.d(TAG, "onStartJob: Push key: " + key);
            path = job.getExtras().getString(IMAGE_FILE_ABS_PATH);
            mediaFile = (path != null) ? new File(path) : null;
            Log.d(TAG, "onStartJob: Media file path: " + mediaFile);
        }

        imageTask = new UploadImageTask(getApplicationContext()){
//            @Override
//            protected void onPostExecute(Boolean b){
//                retry = moreWork = b;
//                Toast.makeText(getApplicationContext(), "Background job finished: " + b.toString(), Toast.LENGTH_SHORT).show();
//                jobFinished(job, b);
//
//            }
//
//            @Override
//            protected void onPreExecute() {
//                Toast.makeText(getApplicationContext(), "Starting background service", Toast.LENGTH_SHORT).show();
//            }
        };
        imageTask.execute(path, key);

        // Call jobFinished to free resources
        Log.d(TAG, "run: Calling jobFinished...");
        jobFinished(job, false);

        return false; // Is there still more work to do?
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        Log.d(TAG, "onStopJob: Job stopped");
        if(uploadThread != null){
            uploadThread.interrupt();
            uploadThread = null;
        }
        return false; // Should the job be retried?
    }

    /**
     *  Uploads the captured image to Firebase storage using an upload task
     *  On successful upload, a record of the report is uploaded to the database
     *  with all report details including a link to the image in storage
     */
    private void uploadImage(){
        Log.d(TAG, "uploadImage: Uploading image.");
        try {
            Uri fileUri = Uri.fromFile(mediaFile);
            // Assign key for file
            StorageReference photoRef = mPhotosStorageReference.child(fileUri.getLastPathSegment());

            // Create file metadata including the content type
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpg")
                    .build();
            uploadTask = photoRef.putFile(fileUri, metadata);
            Log.d(TAG, "uploadImage: putFile called");
            //uploadTask = photoRef.putBytes(photo, metadata);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.d(TAG, "onSuccess: Upload successful");
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    assert downloadUrl != null;
                    Log.d(TAG, "onSuccess: download URL obtained");
                    reference = FirebaseDatabase.getInstance().getReference().child(REPORTS).child(userID);
                    Log.d(TAG, "onSuccess: Get database reference");
                    // Update the image URL upon successful upload
                    Log.d(TAG, "onSuccess: key is " + key);
                    Log.d(TAG, "onSuccess: report field is " + REPORT_FIELD_IMAGEURL);
                    reference.child(key).child(REPORT_FIELD_IMAGEURL).setValue(downloadUrl);
                    Log.d(TAG, "onSuccess: Update imageURL");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    retry = true;
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void loadStatusIcon(int drawable) {
        GlideApp.with(getApplicationContext())
                .load(drawable)
                .placeholder(R.drawable.ic_autorenew_black_24dp)
                .error(R.drawable.ic_broken_image_black_24dp)
                .fallback(R.drawable.ic_image_black_24dp)
                .into((ImageView) activity.findViewById(R.id.img_report_pic));
    }



    static class UploadImageTask extends AsyncTask<String, Void, Boolean>{

        private boolean retry = true;
        private File mediaFile;
        private Context context;
        private String key;
        private UploadTask uploadTask;
        private StorageReference mPhotosStorageReference = FirebaseStorage.getInstance()
                .getReference().child(PHOTOS);

        private static final String TAG = "UploadImageTask";

        public UploadImageTask(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Log.d(TAG, "doInBackground: Background work started");
            String path = params[0];
            mediaFile = (path != null) ? new File(path) : null;
            key = params[1];

            uploadImage();

            return retry;
        }

        @Override
        protected void onPostExecute(Boolean b){
            retry = b;

            Toast.makeText(context, "Background job finished: " + b.toString(), Toast.LENGTH_SHORT).show();

        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(context, "Starting background service", Toast.LENGTH_SHORT).show();
        }

        /**
         *  Uploads the captured image to Firebase storage using an upload task
         *  On successful upload, a record of the report is uploaded to the database
         *  with all report details including a link to the image in storage
         */
        private void uploadImage(){
            Log.d(TAG, "uploadImage: Uploading image.");

            Uri fileUri = Uri.fromFile(mediaFile);
            // Assign key for file
            StorageReference photoRef = mPhotosStorageReference.child(fileUri.getLastPathSegment());

            // Create file metadata including the content type
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpg")
                    .build();
            uploadTask = photoRef.putFile(fileUri, metadata);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    assert downloadUrl != null;
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                            .child(REPORTS).child(userID);
                    // Update the image URL upon successful upload
                    reference.child(key).setValue(downloadUrl);
                    retry = false;

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    retry = true;
                }
            });
        }
    }

}
