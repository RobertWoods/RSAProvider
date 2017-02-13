package edu.temple.rsaprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class RSAContentProvider extends ContentProvider {

    EncryptionDbHelper helper;

    public RSAContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        helper.getWritableDatabase().insert(EncryptionDbHelper.KeyContract.TABLE_NAME,
                null, values);
        return uri;
    }

    @Override
    public boolean onCreate() {
        helper = new EncryptionDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor c = helper.getWritableDatabase().query(EncryptionDbHelper.KeyContract.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
