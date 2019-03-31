package com.sparkdev.foodapp.registerscreen;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.text.TextUtils;
import android.widget.Toast;
import com.sparkdev.foodapp.R;
import com.sparkdev.foodapp.loginscreen.Login;
import com.sparkdev.foodapp.models.firebase.FirebaseAPI;
import com.sparkdev.foodapp.models.firebase.signupInterface.SignUpCompletionListener;

public class RegisterActivity extends AppCompatActivity {

    private static EditText email;
    private static EditText pass1;
    private static EditText pass2;
    private static Button signUp;
    private static FirebaseAPI firebaseAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        firebaseAPI = FirebaseAPI.getInstance(this);
        signUpButton();
    }

    public void signUpButton(){

        email = (EditText)findViewById(R.id.emailbox);
        pass1 = (EditText)findViewById(R.id.passwordbox1);
        pass2 = (EditText)findViewById(R.id.passwordbox2);
        signUp = (Button)findViewById(R.id.signup);

        signUp.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View v){
                            if(email.getText().toString().contains("@") && (pass1.getText().toString().equals(pass2.getText().toString()))){
                                firebaseAPI.registerUser(email.getText().toString(), pass1.getText().toString(), new SignUpCompletionListener() {
                                    @Override
                                    public void onSuccess() {
                                        Intent i = new Intent(getApplicationContext(),Login.class);
                                        startActivity(i);
                                    }

                                    @Override
                                    public void onFailure() {
                                        Toast.makeText(getApplicationContext(), "Please verify that you have entered a valid email and password.", Toast.LENGTH_SHORT).show();
                                        Toast.makeText(getApplicationContext(), pass1.getText().toString(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                    }
                }
        );
    }

}
