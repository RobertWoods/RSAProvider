package edu.temple.rsaprovider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by rober_000 on 2/13/2017.
 */

public class EncryptionDbHelper extends SQLiteOpenHelper implements BaseColumns {

    private static final String DATABASE_NAME = "owo.db";
    private static final int DB_VERSION = 1;

    public EncryptionDbHelper(Context context){
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(KeyContract.SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(KeyContract.SQL_DELETE_TABLE);
        sqLiteDatabase.execSQL(KeyContract.SQL_CREATE_TABLE);
    }

    public static final class KeyContract {
        public static final String TABLE_NAME = "keys";
        public static final String COLUMN_NAME_ID = "keys_id";
        public static final String COLUMN_NAME_OWNER = "keys_owner";
        public static final String COLUMN_NAME_PRIVATE_KEY = "keys_private_key";
        public static final String COLUMN_NAME_PUBLIC_KEY = "keys_public_key";
        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_OWNER + " INTEGER UNIQUE," +
                        COLUMN_NAME_PRIVATE_KEY + " BLOB," +
                        COLUMN_NAME_PUBLIC_KEY + " BLOB);";
        private static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String AUTHORITY =
                "edu.temple.rsaprovider";
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY);
    }

}
