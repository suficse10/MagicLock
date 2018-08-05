package com.logicbangla.magiclockbt;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.logicbangla.magiclockbt.database.DatabaseHelper;

public class PasswordActivity extends AppCompatActivity {

    EditText eTPassword;
    DatabaseHelper dbhelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        eTPassword = findViewById(R.id.editTextPass);
        dbhelper = new DatabaseHelper(PasswordActivity.this);
    }

    public void savePassword(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(PasswordActivity.this);
        dialog.setTitle("Set Password");
        dialog.setMessage("Do you want to set the password ?");
        dialog.setCancelable(false);

        dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String passWord = eTPassword.getText().toString().trim();
                dbhelper.updatePassword(passWord, 1);
                Toast.makeText(PasswordActivity.this, "Password is saved", Toast.LENGTH_SHORT).show();
                eTPassword.setText(null);

                // Make an intent to start next activity while taking an extra which is the Password.
                Intent i = new Intent(PasswordActivity.this, MainActivity.class);
                i.putExtra("pass", passWord);
                startActivity(i);
            }
        });
        dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                eTPassword.setText(null);
                Intent i = new Intent(PasswordActivity.this, MainActivity.class);
                startActivity(i);
            }
        });
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }
}
