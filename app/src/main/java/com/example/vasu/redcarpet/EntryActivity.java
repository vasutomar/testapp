package com.example.vasu.redcarpet;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import id.zelory.compressor.Compressor;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observable;

public class EntryActivity extends AppCompatActivity {


    private static final int TAKE_PICTURE = 1;
    private de.hdodenhof.circleimageview.CircleImageView img;
    private EditText phonenumber;
    private String downloadURL;

    private PhoneAuthProvider.ForceResendingToken rensendingToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks stateCallBack;
    private FirebaseAuth firebaseAuth;

    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;

    private int vis;
    private Uri imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        img = findViewById(R.id.imageV);
        phonenumber = findViewById(R.id.PhoneNo);

        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE &&
                resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                Uri tempUri = null;
                try {
                    tempUri = getImageUri(getApplicationContext(), imageBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imagePath = tempUri;
                Log.e("image ",imagePath.toString());
            }
        }
    }

    @SuppressLint("CheckResult")
    public Uri getImageUri(Context inContext, Bitmap inImage) throws IOException {

        File filesDir = getApplicationContext().getFilesDir();
        File file = new File(filesDir, "vasu.jpg");
        file.createNewFile();

        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, os);
        os.close();

        new Compressor(this)
                .compressToFileAsFlowable(file)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws IOException {
                        Bitmap compressedImageBitmap = new Compressor(getApplicationContext()).compressToBitmap(file);
                        img.setImageBitmap(compressedImageBitmap);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });

        return Uri.parse(file.getAbsolutePath());
    }

    public void sendOTP(View v) throws InterruptedException {
        String userNumber = phonenumber.getText().toString();
        if(userNumber.length() == 10) {
            updateExistingNumber(userNumber);
        }
        else {
            Toast.makeText(EntryActivity.this,"Please enter valid number.",
                    Toast.LENGTH_LONG).show();
        }
    }


    void updateExistingNumber(final String userNumber) {

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        final DatabaseReference ref = database.child("verified-users");

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("verified-users");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int exist = 0;

                for(DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user.number != null) {
                        if (user.number.equals(userNumber)) {
                            exist = 1;
                            User updatedUser = new User(
                                    user.count + 1,
                                    user.number,
                                    user.pictureURL,
                                    user.uniqueID
                            );
                            ref.child(dataSnapshot.getKey()).setValue(updatedUser);
                            vis = user.count + 1;
                            Toast.makeText(EntryActivity.this, "Welcome for the " + vis + " time.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }

                if(exist == 0) {
                    updateNewUser(userNumber);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    void updateNewUser(String usno) {
            Toast.makeText(EntryActivity.this,"Generating OTP, Please wait.",Toast.LENGTH_LONG).show();
            usno = "+91" + usno;
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    usno,
                    30,
                    TimeUnit.SECONDS,
                    EntryActivity.this,
                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                            signInWithPhoneAuthCredential(phoneAuthCredential);
                        }

                        @Override
                        public void onVerificationFailed(FirebaseException e) {
                            Log.w("Error", "onVerificationFailed", e);
                            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(EntryActivity.this, e.getMessage().toString(),
                                        Toast.LENGTH_LONG).show();
                            } else if (e instanceof FirebaseTooManyRequestsException) {
                                Toast.makeText(EntryActivity.this, e.getMessage().toString(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                            super.onCodeSent(s, forceResendingToken);
                            rensendingToken = forceResendingToken;
                        }
                    }
            );
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(EntryActivity.this,"User Verified",
                                    Toast.LENGTH_LONG).show();
                            addUserToDatabase(phonenumber.getText().toString(),true);
                            FirebaseUser user = task.getResult().getUser();
                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w("done", "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                addUserToDatabase(phonenumber.getText().toString(),false);
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }

    protected void addUserToDatabase(String number,boolean authentic) {

        mFirebaseInstance = FirebaseDatabase.getInstance();

        if(authentic) {
            mFirebaseDatabase = mFirebaseInstance.getReference("verified-users");
        } else {
            mFirebaseDatabase = mFirebaseInstance.getReference("supicious-users");
        }
        mFirebaseStorage = FirebaseStorage.getInstance();
        mStorageReference = mFirebaseStorage.getReference();

        //Uploading picture to firebase.
        if(imagePath!=null) {
            final ProgressDialog progressDialog = new ProgressDialog(EntryActivity.this);
            progressDialog.setTitle("Uploading Details to server. Please wait.");
            progressDialog.show();

            StorageReference ref;
            String refString;
            if(authentic) {
                refString = "images/authentic"+ number.toString()+".png";
            } else {
                refString = "images/suspicious"+ number.toString()+".png";
            }
            ref = mStorageReference.child(refString);
            ref.putFile(imagePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    progressDialog.dismiss();
                    Toast.makeText(EntryActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(EntryActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                }
            });

            ref.child(refString).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //createUniqueID
                    if(uri!=null)
                        downloadURL = uri.toString();
                    else
                        downloadURL = "Empty";
                }
            });
        } else {
            Toast.makeText(EntryActivity.this,"Null image",
                    Toast.LENGTH_LONG).show();
        }
        String uniqueID = mFirebaseDatabase.push().getKey();
        User user = new User(1,number,downloadURL,uniqueID);
        mFirebaseDatabase.child(uniqueID).setValue(user);
    }
}

class User {

    public int count;
    public String number;
    public String pictureURL;
    public String uniqueID;

    public User() {

    }

    public User(int count, String number, String pictureURL,String uniqueID) {
        this.count = count;
        this.number = number;
        this.pictureURL = pictureURL;
        this.uniqueID = uniqueID;
    }
}
