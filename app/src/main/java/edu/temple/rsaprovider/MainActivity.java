package edu.temple.rsaprovider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity {

    EditText mEditText;
    EditText mIdEditText;
    PublicKey domesticPublicKey = null;
    PrivateKey domesticPrivateKey = null;
    PublicKey foreignKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditText = (EditText) findViewById(R.id.encryptEditText);
        mIdEditText = (EditText) findViewById(R.id.idEditText);
        findViewById(R.id.encryptButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = encrypt(mEditText.getText().toString(), getPublicKey());
                mEditText.setText(text);
            }
        });

        findViewById(R.id.decryptButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = decrypt(mEditText.getText().toString(), getPrivateKey());
                mEditText.setText(text);
            }
        });

        findViewById(R.id.genNewPairButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getKeyPair();
            }
        });

        findViewById(R.id.getPublicKeyButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    foreignKey = getUsersPublicKey(Integer.parseInt(mIdEditText.getText().toString()));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private PublicKey getPublicKey() {
        if(foreignKey != null){
            Log.d("hey", "It's not null");
            return foreignKey;
        }
        if(domesticPublicKey == null){
            int id = Integer.parseInt(mIdEditText.getText().toString());
            getKeyPair();
            getContentResolver().insert(EncryptionDbHelper.KeyContract.CONTENT_URI,
                    getValuesForInsert(id, domesticPrivateKey, domesticPublicKey));
        }

        Log.d("hey", "It's null");
        return domesticPublicKey;
    }

    private PrivateKey getPrivateKey(){
        if(domesticPrivateKey == null){
            int id = Integer.parseInt(mIdEditText.getText().toString());
            getKeyPair();
            getContentResolver().insert(EncryptionDbHelper.KeyContract.CONTENT_URI,
                    getValuesForInsert(id, domesticPrivateKey, domesticPublicKey));
        }
        return domesticPrivateKey;
    }

    public String encrypt(String plain, PublicKey pk) {
        byte bytes[] = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pk);
            bytes = cipher.doFinal(plain.getBytes());
        } catch (Exception e) {}
        return byteToHex(bytes);
    }

    public String decrypt(String encrypted, PrivateKey pk){
        String decrypted = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, pk);
            byte[] bytes = cipher.doFinal(hexToByte(encrypted));
            decrypted = new String(bytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decrypted;
    }

    public String byteToHex(byte[] bytes){
        String hex = "";
        for(byte b : bytes){
            hex += String.format("%02X", b);
        }
        return hex;
    }

    public byte[] hexToByte(String hex){
        byte[] bytes = new byte[hex.length()/2];
        for(int i=0;i<hex.length();i+=2){
            bytes[i/2] = (byte) (hexToInt(hex.charAt(i))*16 + hexToInt(hex.charAt(i+1)));
        }
        return bytes;
    }

    public int hexToInt(char hex){
        switch (hex){
            case 'A':
                return 10;
            case 'B':
                return 11;
            case 'C':
                return 12;
            case 'D':
                return 13;
            case 'E':
                return 14;
            case 'F':
                return 15;
            default:
                return Integer.parseInt(""+hex);
        }
    }

    public ContentValues getValuesForInsert(int id, PrivateKey privateKey, PublicKey publicKey){
        ContentValues values = new ContentValues();
        values.put(EncryptionDbHelper.KeyContract.COLUMN_NAME_OWNER, id);
        values.put(EncryptionDbHelper.KeyContract.COLUMN_NAME_PRIVATE_KEY, privateKey.getEncoded());
        values.put(EncryptionDbHelper.KeyContract.COLUMN_NAME_PUBLIC_KEY, publicKey.getEncoded());
        return values;
    }

    public PublicKey getPublicKeyFromBlob(byte[] bytes){
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.d("Hmm", "Hmm");
        return null;
    }

    public PrivateKey getPrivateKeyFromBlob(byte[] bytes){
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PublicKey getUsersPublicKey(int id) throws URISyntaxException {
        Cursor c = getContentResolver().query(EncryptionDbHelper.KeyContract.CONTENT_URI,
                new String[] { EncryptionDbHelper.KeyContract.COLUMN_NAME_PUBLIC_KEY },
                EncryptionDbHelper.KeyContract.COLUMN_NAME_OWNER + " = ?",
                new String[] { String.valueOf(id) },
                null,
                null);

        if(c.moveToFirst()) {
            Toast.makeText(this, "Retrieved public key with owner id: " + mIdEditText.getText().toString(), Toast.LENGTH_SHORT).show();
            return getPublicKeyFromBlob(c.getBlob(0));
        }
        return null;
    }

    public void getKeyPair(){
        Cursor c = getContentResolver().query(EncryptionDbHelper.KeyContract.CONTENT_URI,
                null,
                null,
                null,
                null,
                null);
        if(c.moveToFirst()) {
            domesticPublicKey = getPublicKeyFromBlob(c.getBlob(0));
            domesticPrivateKey = getPrivateKeyFromBlob(c.getBlob(1));
            foreignKey = null;
        }
        //TODO refactor getValuesForInsert so it automatically gets id
        int id = Integer.parseInt(mIdEditText.getText().toString());
        getContentResolver().insert(EncryptionDbHelper.KeyContract.CONTENT_URI,
                getValuesForInsert(id, domesticPrivateKey, domesticPublicKey));
    }

}
