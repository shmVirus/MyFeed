package shmVirus.MyFeed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

public class ActivityMain extends AppCompatActivity {
    Button bLogin;
    TextView bRegister;
    String sUserName, sPassWord, sRePassWord;
    SQLiteDatabase db;
    Cursor cursor;

    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();   // hiding/removing actionbar

        bLogin = findViewById(R.id.bLogin);
        bRegister = findViewById(R.id.bRegister);
//        Switch themeSwitch = findViewById(R.id.bTheme);
//
//        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
//                if(isChecked){
//                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//                }else {
//                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//                }
//
//            }
//        });


        // to check if user already logged in or not, if already logged in then skip login page
        sharedPreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if (!sharedPreferences.getString("username", "").isEmpty()) {
            Intent intent = new Intent(ActivityMain.this, ActivityFeedsList.class);
            intent.putExtra("userName", sharedPreferences.getString("username", ""));
            startActivity(intent);
            finish();
        }

        // opening/creating database for the application, also defining users table for saving registered users list
        db = openOrCreateDatabase("FeederUsers", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS users(dUserName VARCHAR unique, dPassWord VARCHAR);");

        // listening login or registration button click and implementation of corresponding events
        loginOrRegister();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void loginOrRegister() {
        bRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pop-up dialog box for registration
                AlertDialog.Builder registrationDialog = new AlertDialog.Builder(ActivityMain.this);
                registrationDialog.setTitle("Registration");    // dialog box name/title

                // inflating/setting view for registration dialog, view defined as a different xml layout
                View view = ActivityMain.this.getLayoutInflater().inflate(R.layout.dialog_registration, null);
                registrationDialog.setView(view);
                registrationDialog.setCancelable(false);    // preventing closing dialog when clicked outside of dialog box

                EditText eUserName = view.findViewById(R.id.eUserName);
                EditText ePassWord = view.findViewById(R.id.ePassWord);
                EditText eRePassWord = view.findViewById(R.id.eRePassWord);

                registrationDialog.setPositiveButton("Register", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // null method override below, to stop closing dialog box when this button clicked with incorrect information
                    }
                });
                registrationDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();  // closing dialog
                    }
                });

                AlertDialog dialog = registrationDialog.create();
                dialog.show();
                // manually overriding positive button
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sUserName = eUserName.getText().toString();
                        sPassWord = ePassWord.getText().toString();
                        sRePassWord = eRePassWord.getText().toString();

                        if (sUserName.isEmpty() || sPassWord.isEmpty() || sRePassWord.isEmpty()) {
                            Toast.makeText(ActivityMain.this, "Can't REGISTER with Empty Values!", Toast.LENGTH_SHORT).show();
                        } else {
                            if (!sUserName.matches("[a-zA-Z]+")) {
                                eUserName.setError("UserName can only contain letters!");
                            } else if (!(sPassWord.length() >= 5)) {
                                ePassWord.setError("Password be equal or more than 5 Characters!");
                            } else if (!sPassWord.equals(sRePassWord)) {
                                eRePassWord.setError("Passwords doesn't match!");
                            } else {
                                // to prevent multiple registration with same username
                                cursor = db.rawQuery("SELECT COUNT (*) FROM " + "users" + " WHERE " + "dUserName = ?", new String[]{sUserName});
                                cursor.moveToFirst();
                                // when number of rows with given username is less or equal to 0, this means no user registered this given username
                                if (cursor.getInt(0) <= 0) {    // hence user is allowed for registration with given username
                                    db.execSQL("INSERT INTO users VALUES('" + sUserName + "','" + sPassWord + "');");
                                    Toast.makeText(ActivityMain.this, "Registered!", Toast.LENGTH_SHORT).show();
                                    eUserName.getText().clear();
                                    ePassWord.getText().clear();
                                    eRePassWord.getText().clear();

                                    // keeping username of the currently registered user, to skip login for next time launching
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("username", sUserName);
                                    editor.apply();

                                    // moving to next page/activity right after registration
                                    Intent intent = new Intent(ActivityMain.this, ActivityFeedsList.class);
                                    intent.putExtra("userName", sUserName);
                                    dialog.dismiss();
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(ActivityMain.this, "Can't Register, User Exists!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                });
            }
        });

        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText eUserName = findViewById(R.id.elUserName);
                EditText ePassWord = findViewById(R.id.elPassWord);

                cursor = db.rawQuery("SELECT COUNT (*) FROM " + "users", null);
                cursor.moveToFirst();
                if (cursor.getInt(0) <= 0) {    // empty user table
                    Toast.makeText(ActivityMain.this, "No Users!\nRegister to become First User!", Toast.LENGTH_SHORT).show();
                } else {
                    sUserName = eUserName.getText().toString();
                    sPassWord = ePassWord.getText().toString();
                    if (sUserName.isEmpty() || sPassWord.isEmpty()) {   // clicked login button keeping any of the input fields empty
                        Toast.makeText(ActivityMain.this, "Provide Credentials to Login!", Toast.LENGTH_SHORT).show();
                    } else {
                        if (!sUserName.matches("[a-zA-Z]+")) {
                            eUserName.setError("Name can only contain letters!");
                        } else if (!(sPassWord.length() >= 5)) {
                            ePassWord.setError("Passwords should have equal or more than 5 Characters!");
                        } else {
                            // for login checking if username exists in database or not
                            cursor = db.rawQuery("SELECT COUNT (*) FROM " + "users" + " WHERE " + "dUserName = ?", new String[]{sUserName});
                            cursor.moveToFirst();

                            if (cursor.getInt(0) > 0) { // proceeding when username exists
                                // checking if username and password correctly matches or not
                                cursor = db.rawQuery("SELECT  COUNT (*) FROM " + "users" + " WHERE " + "dUserName = ? AND dPassWord = ?", new String[]{sUserName, sPassWord});
                                cursor.moveToFirst();
                                if (cursor.getInt(0) > 0) {
                                    // to get username for next page; thought it could be removed, but keeping it as TODO: future optimization
                                    cursor = db.rawQuery("SELECT  * FROM " + "users" + " WHERE " + "dUserName = ? AND dPassWord = ?", new String[]{sUserName, sPassWord});
                                    cursor.moveToFirst();
                                    @SuppressLint("Range") String feed = cursor.getString(cursor.getColumnIndex("dUserName"));
                                    Intent intent = new Intent(ActivityMain.this, ActivityFeedsList.class);
                                    intent.putExtra("userName", feed);

                                    // keeping username of the currently logged user, to skip login for next time launching
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("username", sUserName);
                                    editor.apply();

                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(ActivityMain.this, "Incorrect Credentials!", Toast.LENGTH_SHORT).show();
                                }
                            } else {    // when username doesn't exists
                                Toast.makeText(ActivityMain.this, "User doesn't Exists!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        });
    }
}