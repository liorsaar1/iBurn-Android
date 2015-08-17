package com.gaiagps.iburn.database;

import android.content.Context;

import com.gaiagps.iburn.PlayaClient;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;


/**
 *
 * This class handles bootstrapping the application database
 * from a pre-populated SQL database stored in assets.
 *
 * To test bootstrapping the application database from JSON, comment
 * out the entirety of this class.
 *
 * @author davidbrodsky
 * @description SQLiteWrapper is written to be application agnostic.
 * requires Strings: DATABASE_NAME, DATABASE_VERSION,
 * CREATE_TABLE_STATEMENT, TABLE_NAME
 */
public class DBWrapper extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "playaDatabase.db";
    private static final int DATABASE_VERSION = PlayaClient.DATABASE_VERSION;

    static Context c;

    public DBWrapper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        c = context;
        setForcedUpgrade(DATABASE_VERSION);
    }
}