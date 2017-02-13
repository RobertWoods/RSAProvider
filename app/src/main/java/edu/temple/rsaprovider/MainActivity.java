package edu.temple.rsaprovider;

import android.content.ContentValues;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity {

    EditText mEditText;
    KeyPair keyPair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditText = (EditText) findViewById(R.id.encryptEditText);
//        getContentResolver().query("content://edu.temple.rsaprovider", )
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyPair = generator.generateKeyPair();
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

    }

    private PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    private PrivateKey getPrivateKey(){
        return keyPair.getPrivate();
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

    public ContentValues getValuesForQuery(int id){
        ContentValues values = new ContentValues();
        values.put("OWNER", id);
        return values;
    }

    public ContentValues getValuesForInsert(int id, KeyPair keyPair){
        ContentValues values = new ContentValues();
        values.put("OWNER", id);
        values.put("PUBLIC", keyPair.getPrivate().getEncoded());
        values.put("PRIVATE", keyPair.getPublic().getEncoded());
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
        return null;
    }

    public PrivateKey getPrivateKeyFromBlob(byte[] bytes){
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new X509EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


}
