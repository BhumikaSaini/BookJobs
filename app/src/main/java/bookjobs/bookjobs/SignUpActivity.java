package bookjobs.bookjobs;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {
    private static final int SIGNED_UP_USERNAME = 0;

    private EditText mNameView;
    private EditText mEmailView;
    private EditText mPasswordView;
    private Button mSignUpButton;

    //database communication's var
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    /**
     * Keep track of the signup task to ensure we can cancel it if requested.
     */
    private UserSignUpTask mAuthTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // set up the wings
        mNameView = (EditText) findViewById(R.id.signup_name);
        mEmailView = (EditText) findViewById(R.id.signup_email);
        mPasswordView = (EditText) findViewById(R.id.signup_password);
        mSignUpButton = (Button) findViewById(R.id.signup_submit_button);

        // for debug
        // TODO: remove after use
        mNameView.setText("Hank");
        mEmailView.setText("Hankemail@gmail.com");
        mPasswordView.setText("123456");

        // set up onclick listener
        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSignup();
            }
        });
    }

    private void attemptSignup(){
        // get email and password text
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        // View and process parameters.Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        View focusView = null;
        boolean cancel = false;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            //TODO: add name and other information to the signup
            mAuthTask = new UserSignUpTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    public class UserSignUpTask extends AsyncTask<Void, Void, Boolean> {
        private final String mEmail;
        private final String mPassword;
        private final String DBTAG = "Database in SIGNUP";
        private final String TAG = "SIGNUP TASK";
        private final String FAIL_TAG = "SIGNUP Failed";

        UserSignUpTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            mDatabase = FirebaseDatabase.getInstance().getReference();
            Log.d(DBTAG, mDatabase.toString());

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success){
            mAuthTask = null;

            if(success){
                mAuth = FirebaseAuth.getInstance();
                mAuth.createUserWithEmailAndPassword(mEmail, mPassword)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d(TAG, "signUp:onComplete:" + task.isSuccessful());

                                if (task.isSuccessful()){
                                    onSignUpSuccess(task.getResult().getUser());
                                } else {
                                    boolean cancel = false;
                                    View focusView = null;
                                    try {
                                        throw task.getException();
                                    } catch (FirebaseAuthInvalidCredentialsException e) {
                                        // invalid email address, according to FireBase's standard
                                        e.printStackTrace();
                                        mEmailView.setError(getString(R.string.error_invalid_email));
                                        focusView = mEmailView;
                                        cancel = true;
                                    } catch (FirebaseAuthUserCollisionException e) {
                                        e.printStackTrace();
                                        mEmailView.setError(getString(R.string.error_already_used_email));
                                        focusView = mEmailView;
                                        cancel = true;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    if (cancel){
                                        focusView.requestFocus();
                                    }
                                    Toast.makeText(SignUpActivity.this, "Failed to register",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
            else {
                Toast.makeText(SignUpActivity.this, "Failed to register",
                        Toast.LENGTH_SHORT).show();
            }
        }

        private void onSignUpSuccess(FirebaseUser user){
            user.getEmail();
            // go back to Login activity
            Intent backToLogin = new Intent();
            backToLogin.putExtra("username", user.getEmail());
            setResult(SIGNED_UP_USERNAME, backToLogin);
            finish();
        }

        @Override
        protected void onCancelled(){
            mAuthTask = null;
        }
    }
}