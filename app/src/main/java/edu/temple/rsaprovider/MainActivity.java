package edu.temple.rsaprovider;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback {

    private final String[] modes = {"Send Key", "Send Text"};
    private final String[] sentMode = {"K", "T"};

    EditText mEditText;
    EditText mIdEditText;
    NfcAdapter mNfcAdapter;

    PublicKey domesticPublicKey = null;
    PrivateKey domesticPrivateKey = null;
    PublicKey foreignKey = null;
    PrivateKey nfcForeignKey = null;
    public int mode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(mNfcAdapter!=null)
            mNfcAdapter.setNdefPushMessageCallback(this, this);
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

        findViewById(R.id.beamModeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mode ^=/*^ one day...*/ 1;
                Toast.makeText(getBaseContext(), "Mode: " + modes[mode], Toast.LENGTH_SHORT).show();
            }
        });



    }

    @Override
    public void onResume(){
        super.onResume();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())){
            processIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            processIntent(intent);
        }
    }

    private void processIntent(Intent intent) {
        Parcelable p[] = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if(p!=null){
            NdefMessage m = (NdefMessage) p[0];
            String message = new String(m.getRecords()[0].getPayload());
            String messageCode = message.substring(0,1);
            Log.d("NFC", message);
            Log.d("NFC", messageCode);
            message = message.substring(1);
            if(messageCode.equals(sentMode[0])){
                //Key sent
                nfcForeignKey = stringToKey(message);
            } else {
                //Message sent
                mEditText.setText(message);
            }
        } else {
            Log.d("NFC", "Parcelable was empty");
        }

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
        if(nfcForeignKey != null){
            return nfcForeignKey;
        } else if(domesticPrivateKey == null){
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
        try {
            int id = Integer.parseInt(mIdEditText.getText().toString());
            getContentResolver().insert(EncryptionDbHelper.KeyContract.CONTENT_URI,
                    getValuesForInsert(id, domesticPrivateKey, domesticPublicKey));
        } catch (Exception e){}
    }

    public String keyToString(PrivateKey pk){
        return Base64.encodeToString(pk.getEncoded(), Base64.DEFAULT);
    }

    public PrivateKey stringToKey(String encoded){
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec
                = new PKCS8EncodedKeySpec(Base64.decode(encoded, Base64.DEFAULT));
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(pkcs8EncodedKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        String text = sentMode[mode];
        if(modes[mode].equals(modes[0])) {
            text += keyToString(domesticPrivateKey);
        } else {
            text += mEditText.getText().toString();
        }
        Log.d("NFC", "Message: " + text);
        return new NdefMessage( new NdefRecord[] {
                NdefRecord.createMime("application/edu.temple.rsaprovider", text.getBytes()) } );
    }
}
