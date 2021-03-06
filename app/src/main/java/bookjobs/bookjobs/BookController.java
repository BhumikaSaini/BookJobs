package bookjobs.bookjobs;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Created by Hung on 9/12/2016.
 */
public class BookController {

    private static DatabaseReference mDatabase;
    private static DatabaseReference mBooksDatabase;
    private static final String DBTAG = "DB in BookController";
    public BookController() {}

    /**
     * Book strictly contains the information regarding to the book, i.e. Genre, author, ISBN and title
     * That means to keep track of the owner, we need to update the users database in own
     *
     */
    public static class UploadBookTask extends AsyncTask<Void, Void, String> {
        private Book mBook;
        private String mUserEmail;
        private DatabaseReference mCurrentUser;
        final String OWN_LIST = "uOwns";
        private AddBookActivity mActivity;
        private ArrayList<Uri> pictures;
        private MyCompleteListener completeListener;
        static String bookRef = null;
        private static final String TAG = "UPLOAD_RESULT";

        public UploadBookTask(AddBookActivity activity, MyCompleteListener mCompleteListener, Book mBook, String mUser, ArrayList<Uri> picturesList) {
            this.mBook = mBook;
            this.mUserEmail = mUser;
            this.completeListener = mCompleteListener;
            this.mActivity = activity;
            this.pictures = picturesList;
        }


        @Override
        protected String doInBackground(Void... params) {
            // Database Connection, if no connection or what not, exception will be here
            mDatabase = FirebaseDatabase.getInstance().getReference();
            Log.d(DBTAG, mDatabase.toString());

            // 'child database'
            mBooksDatabase = mDatabase.child("books");
            mCurrentUser = mDatabase.child("users");

            // address to upload the book, later we can call newBookRef.getKey() to get the ID
            // and use the ID to indicate the relationship between the owner and the book
            final DatabaseReference newBookRef = mBooksDatabase.push();
            try {
                newBookRef.setValue(mBook, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Log.e(DBTAG, "Data could not be saved. " + databaseError.getMessage());
                        } else {
                            // update the 'owns' list in user's data
                            bookRef = newBookRef.getKey();
                            onResult(bookRef);
                            Log.d(DBTAG, "Data saved successfully. bookRef = " + bookRef);
                            completeListener.onSuccess();
                            updateOwnerCollection(bookRef);
                            //TODO: we can use this to count how many of the same books an user has
                        }
                    }


                });
            } catch (DatabaseException e){
                Log.e(DBTAG, "Error occurred", e);
            }

            // if owner is desired in book, we can modify this part
            return bookRef;
        }

        private void updateOwnerCollection(final String BookReference) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            Query query = mCurrentUser.orderByChild("email").equalTo(user.getEmail());

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String value = dataSnapshot.getValue().toString();
                    String userRef = value.substring(1, value.indexOf('='));
                    dataSnapshot.child(userRef).child("owns").child(bookRef).getRef().setValue(1);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Failed to reach the server");
                }
            });
        }


        protected void onResult(String result) {
            Log.d(TAG, "Book Reference: " + result);
            if (result!=null){
                //an UUID was returned for the book. this means upload was succeeded.
                mActivity.uploadResult = result;

            } else {
                completeListener.onFail();
                //otherwise...
            }
        }
    }

    public static void updateBookPicture(String uri, String bookRef){
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mBooksDatabase = mDatabase.child("books").child(bookRef);
        final DatabaseReference newPhoto = mBooksDatabase.child("photos").push();
        newPhoto.setValue(uri);
    }

}
