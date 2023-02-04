package shmVirus.MyFeed;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class ActivityFeedsList extends AppCompatActivity {
    String sUserName, sFeedTitle, sFeedURL;
    SQLiteDatabase db;
    Cursor cursor;
    ListView lFeedLists;
    TextView placeholder;
    ArrayList<String> feedsTitle;
    ArrayList<String> feedsURL;
    ArrayAdapter<String> adapter;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feeds_list);

        Bundle Extra = getIntent().getExtras();
        sUserName = Extra.getString("userName");
        setTitle(sUserName + ": Feeds");

        lFeedLists = findViewById(R.id.feedsList);
        placeholder = findViewById(R.id.placeholder);

        // prompt before showing instructions
        sharedPreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if (sharedPreferences.getString("learned", "").isEmpty()) {     // user didn't learnt previously about rss feeds
            AlertDialog.Builder experienceDialog = new AlertDialog.Builder(ActivityFeedsList.this);
            experienceDialog.setMessage("Have you used any RSS Feed Reader before?");
            experienceDialog.setCancelable(false);

            experienceDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("learned", "yes");     // user already knows about rss feeds
                    editor.apply();
                    features();
                }
            });
            experienceDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    showInstructions();     // user doesn't knows about rss feeds, so showing instructions
                }
            });
            experienceDialog.show();
        } else {    // user learnt previously about rss feeds
            features();
        }
    }

    public void features() {    // places feeds titles to UI and listens clicks to feeds
        db = openOrCreateDatabase("FeederUsers", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + sUserName + "(dFeedTitle VARCHAR, dFeedURL VARCHAR);");

        feedsTitle = new ArrayList<String>();
        feedsURL = new ArrayList<String>();
        showFeeds();

        lFeedLists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(ActivityFeedsList.this, ActivityFeedContents.class);
                intent.putExtra("feed", feedsURL.get(position));
                intent.putExtra("title", feedsTitle.get(position));
                startActivity(intent);
            }
        });
    }

    public void showInstructions() {    // showing instruction about using this application
        AlertDialog.Builder instructionsDialog = new AlertDialog.Builder(ActivityFeedsList.this);
        instructionsDialog.setTitle("Instructions");
        View view = this.getLayoutInflater().inflate(R.layout.dialog_instructions, null);
        instructionsDialog.setView(view);
        instructionsDialog.setCancelable(false);

        instructionsDialog.setPositiveButton("Okay, I Understand", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("learned", "yes");     // marking user learnt about rss feeds
                editor.apply();
                features();
            }
        });
        instructionsDialog.show();
    }

    public void updateFeeds() {     // updating feeds, required after adding, editing, deleting feeds
        cursor = db.rawQuery("SELECT * FROM " + sUserName, null);
        cursor.moveToFirst();
        if (cursor != null && cursor.getCount() > 0) {  // adding feeds when feeds available
            cursor.moveToFirst();
            int i = cursor.getCount();
            feedsTitle = new ArrayList<String>();
            feedsURL = new ArrayList<String>();
            while (i-- > 0) {
                feedsTitle.add(cursor.getString(cursor.getColumnIndexOrThrow("dFeedTitle")));
                feedsURL.add(cursor.getString(cursor.getColumnIndexOrThrow("dFeedURL")));
                cursor.moveToNext();
            }
        } else {    // clearing feeds when no feeds available to database
            feedsTitle.clear();
            feedsURL.clear();
        }
    }

    public void showFeeds() {   // placing feeds title to the UI
        updateFeeds();          // updating database before showing to UI

        if (!feedsTitle.isEmpty()) {    // placings feeds when available
            placeholder.setText("");
            adapter = new ArrayAdapter<String>(ActivityFeedsList.this, R.layout.listview_feeds, feedsTitle);
            lFeedLists.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            lFeedLists.invalidateViews();
            lFeedLists.refreshDrawableState();
        } else {    // showing a message when no feeds are available
            adapter = new ArrayAdapter<String>(ActivityFeedsList.this, R.layout.listview_feeds, feedsTitle);
            lFeedLists.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            lFeedLists.invalidateViews();
            lFeedLists.refreshDrawableState();
            placeholder.setText("No Feeds, Add Feed URLs!");
        }
    }

    public InputStream getInputStream(URL url) {
        try {
            return url.openConnection().getInputStream();   // output of a URL
        } catch (IOException e) {
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    public boolean validateURL(String feedURL) {    // validating an URL, if the URL is parable for rss or not
        final boolean[] valid = {false};

        try {
            new AsyncTask<Integer, Void, Void>() {
                Exception exception = null;

                @Override
                protected Void doInBackground(Integer... params) {
                    try {
                        URL url = new URL(feedURL);

                        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                        factory.setNamespaceAware(false);

                        // ignoring null outputs of url; when urls returning something, validating this something
                        if (getInputStream(url) != null) {
                            XmlPullParser pullParser = factory.newPullParser();
                            pullParser.setInput(getInputStream(url), "UTF-8");  // contents of xml

                            int tagType = pullParser.getEventType();    // pulling an XML tag

                            while (tagType != pullParser.END_DOCUMENT) {    // checking if reached end of the file
                                if (tagType == pullParser.START_TAG) {      // checking if the tag is staring tag or not
                                    if (pullParser.getName().equalsIgnoreCase("rss")) {
                                        // XML contains a rss tag, so it's a valid rss feed
                                        valid[0] = true;
                                        break;
                                    }
                                }
                                tagType = pullParser.next();    // traversing tags
                            }
                        }
                    } catch (XmlPullParserException | IOException e) {
                        exception = e;
                    }
                    return null;
                }
            }.execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return valid[0];
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {     // showing menus for FeedsList activity
        getMenuInflater().inflate(R.menu.menu_feeds_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();      // getting id of clicked menu

        // taking appropriate actions for different menus
        if (itemID == R.id.mAddFeed) {
            // can't use getApplicationContext due to security, need only activity context; placing getApplicationContext will result crashes
            AlertDialog.Builder addDialogBuilder = new AlertDialog.Builder(ActivityFeedsList.this);
            addDialogBuilder.setTitle("Add Feed");

            // programmatically setting views for adding feeds
            EditText eFeedTitle = new EditText(ActivityFeedsList.this);
            eFeedTitle.setInputType(InputType.TYPE_CLASS_TEXT);
            eFeedTitle.setHint(R.string.hintFeedTitle);
            EditText eFeedURL = new EditText(ActivityFeedsList.this);
            eFeedURL.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
            eFeedURL.setHint(R.string.hintFeedURL);

            LinearLayout view = new LinearLayout(ActivityFeedsList.this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.addView(eFeedTitle);
            view.addView(eFeedURL);

            addDialogBuilder.setView(view);
            addDialogBuilder.setCancelable(true);

            addDialogBuilder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // null method override below, to stop closing dialog box when this button clicked with incorrect information
                }
            });
            addDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            AlertDialog dialog = addDialogBuilder.create();
            dialog.show();

            // manually overriding positive button
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sFeedTitle = eFeedTitle.getText().toString();
                    sFeedURL = eFeedURL.getText().toString();

                    if (sFeedTitle.isEmpty() || sFeedURL.isEmpty()) {
                        Toast.makeText(ActivityFeedsList.this, "Can't be Empty!", Toast.LENGTH_SHORT).show();
                    } else {
                        if (validateURL(sFeedURL)) {
                            ContentValues values = new ContentValues();
                            values.put("dFeedTitle", sFeedTitle);
                            values.put("dFeedURL", sFeedURL);
                            db.insert(sUserName, null, values);
                            dialog.dismiss();
                            showFeeds();
                        } else {
                            Toast.makeText(ActivityFeedsList.this, "Can' Parse, Invalid Feed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        } else if (itemID == R.id.mEditFeed && !feedsURL.isEmpty()) {
            AlertDialog.Builder editDialogBuilder = new AlertDialog.Builder(ActivityFeedsList.this);
            editDialogBuilder.setTitle("Edit Feed");
            editDialogBuilder.setCancelable(true);
            String[] selectedFeed = new String[1];
            String[] list = feedsTitle.toArray(new String[0]);

            editDialogBuilder.setSingleChoiceItems(list, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    selectedFeed[0] = list[which];
                }
            });
            editDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            editDialogBuilder.setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // null method override below, to stop closing dialog box when this button clicked with incorrect information
                }
            });
            AlertDialog dialog = editDialogBuilder.create();
            dialog.show();

            // manually overriding positive button
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedFeed[0] == null) {
                        Toast.makeText(ActivityFeedsList.this, "Select to Edit!", Toast.LENGTH_SHORT).show();
                    } else {
                        AlertDialog.Builder addDialogBuilder = new AlertDialog.Builder(ActivityFeedsList.this);
                        addDialogBuilder.setTitle("Edit Feed");

                        EditText eFeedTitle = new EditText(ActivityFeedsList.this);
                        eFeedTitle.setInputType(InputType.TYPE_CLASS_TEXT);
                        eFeedTitle.setHint(R.string.hintFeedTitle);

                        LinearLayout view = new LinearLayout(ActivityFeedsList.this);
                        view.setOrientation(LinearLayout.VERTICAL);
                        view.addView(eFeedTitle);
                        addDialogBuilder.setView(view);
                        addDialogBuilder.setCancelable(true);

                        addDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //dialog.dismiss();
                            }
                        });
                        addDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // null method override below, to stop closing dialog box when this button clicked with incorrect information
                            }
                        });
                        AlertDialog dialogE = addDialogBuilder.create();
                        dialogE.show();

                        // manually overriding positive button
                        Button positiveButton = dialogE.getButton(AlertDialog.BUTTON_POSITIVE);
                        positiveButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sFeedTitle = eFeedTitle.getText().toString();
                                if (sFeedTitle.isEmpty()) {
                                    Toast.makeText(ActivityFeedsList.this, "Can't be Empty!", Toast.LENGTH_SHORT).show();
                                } else {
                                    ContentValues values = new ContentValues();
                                    values.put("dFeedTitle", sFeedTitle);
                                    //values.put("dFeedURL", sFeedURL);
                                    db.update(sUserName, values, "dFeedTitle = ?", new String[]{selectedFeed[0]});
                                    dialogE.dismiss();
                                    dialog.dismiss();
                                    showFeeds();
                                }
                            }
                        });
                    }
                }
            });
        } else if (itemID == R.id.mDeleteFeeds && !feedsURL.isEmpty()) {
            ArrayList<String> selectedItems = new ArrayList<String>();
            AlertDialog.Builder deleteDialogBuilder = new AlertDialog.Builder(ActivityFeedsList.this);
            deleteDialogBuilder.setTitle("Delete Feeds");
            deleteDialogBuilder.setCancelable(true);
            String[] list = feedsTitle.toArray(new String[0]);

            deleteDialogBuilder.setMultiChoiceItems(list, null, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which, boolean isChecked) {
                    if (isChecked) {
                        selectedItems.add(list[which]);
                    } else selectedItems.remove(list[which]);
                }
            });
            deleteDialogBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // null method override below, to stop closing dialog box when this button clicked with incorrect information
                }
            });
            deleteDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            AlertDialog dialog = deleteDialogBuilder.create();
            dialog.show();

            // manually overriding positive button
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedItems.size() > 0) {
                        for (int x = 0; x < selectedItems.size(); x++) {
                            db.delete(sUserName, "dFeedTitle = ?", new String[]{selectedItems.get(x)});
                        }
                        showFeeds();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(ActivityFeedsList.this, "Specify Feeds to Delete!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (itemID == R.id.mInstructions) {
            showInstructions();     // showing instructions when clicked Show Instructions
        } else if (itemID == R.id.mClose) {
            finishAffinity();       // closing application when clicked Exit Application
        } else if (itemID == R.id.mLogOut) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", "");
            editor.apply();         // clearing username, as logout selected
            Intent intent = new Intent(ActivityFeedsList.this, ActivityMain.class);
            startActivity(intent);
            finish();
        } else if (itemID == R.id.mDeleteAccount) {
            AlertDialog.Builder deleteAccountDialogBuilder = new AlertDialog.Builder(ActivityFeedsList.this);
            deleteAccountDialogBuilder.setTitle("Account Deletion Confirmation");

            EditText deleteAccount = new EditText(ActivityFeedsList.this);
            deleteAccount.setInputType(InputType.TYPE_CLASS_TEXT);
            deleteAccount.setHint("type \"CONFIRM\" to delete account");
            deleteAccountDialogBuilder.setView(deleteAccount);
            deleteAccountDialogBuilder.setCancelable(true);

            deleteAccountDialogBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // null method override below, to stop closing dialog box when this button clicked with incorrect information
                }
            });
            deleteAccountDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            AlertDialog dialog = deleteAccountDialogBuilder.create();
            dialog.show();

            // manually overriding positive button
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String delete = deleteAccount.getText().toString();     // confirmation

                    if (delete.equals("CONFIRM")) {
                        db.execSQL("DROP TABLE " + sUserName);  // deleting data of the user
                        db.delete("users", "dUserName = ?", new String[]{sUserName});   // deleting user from users
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();     // clearing login automation and instruction
                        editor.apply();
                        Intent intent = new Intent(ActivityFeedsList.this, ActivityMain.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ActivityFeedsList.this, "Didn't Confirm Deletion!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            });
        }
        return true;
    }
}