package com.example.vasu.redcarpet;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
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
    private ImageView img;
    private EditText phonenumber;
    private String downloadURL;

    private String verificationID;
    private String codeSent;

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

        File tempDir= Environment.getExternalStorageDirectory();
        tempDir=new File(tempDir.getAbsolutePath()+"/.temp/");
        tempDir.mkdir();
        File tempFile = File.createTempFile("vasu", ".jpg", tempDir);

        final Bitmap[] compressedImageBitmap = new Bitmap[1];
        new Compressor(this)
                .compressToFileAsFlowable(tempFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws IOException {
                        compressedImageBitmap[0] = new Compressor(getApplicationContext()).compressToBitmap(file);
                        img.setImageBitmap(compressedImageBitmap[0]);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        img.setImageBitmap(inImage);
        return Uri.parse(path);
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
                            //signInWithPhoneAuthCredential(phoneAuthCredential);
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
                            codeSent = s;
                            askUserForOTP();
                        }
                    }
            );
    }

    int timeoutSig = 0;

    private void askUserForOTP() {

        timeoutSig = 1;
        final AlertDialog dialogBuilder = new AlertDialog.Builder(EntryActivity.this).create();
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_diag,null);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogBuilder.dismiss();
                if(timeoutSig==1) {
                    Toast.makeText(EntryActivity.this, "Timeout"
                            , Toast.LENGTH_LONG).show();
                }
                return;
            }
        }, 30000);

        final EditText editText = (EditText) dialogView.findViewById(R.id.edt_comment);
        Button button1 = (Button) dialogView.findViewById(R.id.buttonSubmit);
        Button button2 = (Button) dialogView.findViewById(R.id.buttonCancel);

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogBuilder.dismiss();
            }
        });
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeoutSig = 0;
                String entry = editText.getText().toString();
                if(entry!=null) {
                    checkOTP(entry);
                }
                dialogBuilder.dismiss();
            }
        });

        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
    }

    private void checkOTP(String entry) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(codeSent,entry);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(EntryActivity.this,"New Visitor Saved!",
                                    Toast.LENGTH_LONG).show();
                            addUserToDatabase(phonenumber.getText().toString(),true);
                            FirebaseUser user = task.getResult().getUser();
                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w("done", "signInWithCredential:failure", task.getException());
                            Toast.makeText(EntryActivity.this,"Verification failed.",
                                    Toast.LENGTH_LONG).show();
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

        if(imagePath!=null) {
            final ProgressDialog progressDialog = new ProgressDialog(EntryActivity.this);
            progressDialog.setTitle("Uploading Details to server. Please wait.");
            progressDialog.show();

            final StorageReference ref;
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
                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            downloadURL = uri.toString();
                        }
                    });
                    //Toast.makeText(EntryActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    //Toast.makeText(EntryActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                }
            });

            /*ref.child(refString).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //createUniqueID
                    if(uri!=null)
                        downloadURL = uri.toString();
                    else
                        downloadURL = "Empty";
                }
            });*/
        } else {
            Toast.makeText(EntryActivity.this,"Null image",
                    Toast.LENGTH_LONG).show();
        }
        String uniqueID = mFirebaseDatabase.push().getKey();
        User user = new User(1,number,downloadURL,uniqueID);
        mFirebaseDatabase.child(uniqueID).setValue(user);
    }
}
