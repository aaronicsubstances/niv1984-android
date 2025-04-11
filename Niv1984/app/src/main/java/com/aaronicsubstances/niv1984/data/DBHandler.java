package com.aaronicsubstances.niv1984.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.aaronicsubstances.niv1984.etc.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class DBHandler extends SQLiteOpenHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBHandler.class);
    private static final String DB_NAME = "app";

    private static final int DB_VERSION = 1;

    // below variable is for our table name.
    private static final String TABLE_NAME = "ignored_footnotes";

    private static final String ID_COL = "id";
    private static final String BOOK_CODE_COL = "bcode";
    private static final String VALUE_KEY_COL = "comment_key";
    private static final String VALUE_COL = "value";
    private static final String SORT_COL = "rank";
    private static final String DATE_CREATED_COL = "date_created";
    private static final String DATE_DELETED_COL = "date_deleted";

    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        LOGGER.warn("Creating SQLite database for version {}...",
                DB_VERSION);

        // on below line we are creating
        // an sqlite query and we are
        // setting our column names
        // along with their data types.
        String query = "CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + BOOK_CODE_COL + " TEXT,"
                + VALUE_KEY_COL + " TEXT,"
                + VALUE_COL + " TEXT,"
                + SORT_COL + " INTEGER DEFAULT 0,"
                + DATE_CREATED_COL + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + DATE_DELETED_COL + " TIMESTAMP)";

        // at last we are calling a exec sql
        // method to execute above sql query
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOGGER.warn("Upgrading SQLite database from version {} to version {}...",
                oldVersion, newVersion);

        // this method is called to check if the table exists already.
        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        //onCreate(db);
    }

    public List<String[]> loadComments(String bcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, new String[]{VALUE_KEY_COL, VALUE_COL},
                String.format("%s = ? AND %s IS NULL", BOOK_CODE_COL, DATE_DELETED_COL),
                new String[]{ bcode }, null, null, null);
        List<String[]> results = new ArrayList<>();
        while (c.moveToNext()) {
            results.add(new String[]{c.getString(0), c.getString(1)});
        }
        c.close();
        db.close();
        return results;
    }

    public void updateComment(String bcode, String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BOOK_CODE_COL, bcode);
        values.put(VALUE_KEY_COL, key);
        values.put(DATE_DELETED_COL, new Date().getTime());

        // first soft delete any existing entries for book code and
        // comment key
        db.update(TABLE_NAME,  values,
                String.format("%s = ? AND %s = ? AND %s IS NULL",
                        BOOK_CODE_COL, VALUE_KEY_COL, DATE_DELETED_COL),
                new String[]{ bcode, key });

        // then make a new record for new/updated comment text
        if (value != null && !value.isEmpty()) {
            values = new ContentValues();
            values.put(BOOK_CODE_COL, bcode);
            values.put(VALUE_KEY_COL, key);
            values.put(VALUE_COL, value);

            // after adding all values we are passing
            // content values to our table.
            db.insert(TABLE_NAME, null, values);
        }

        // at last we are closing our
        // database after addition.
        db.close();
    }

    public void serializeComments(File destFile) throws IOException {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, new String[]{ BOOK_CODE_COL, VALUE_KEY_COL, VALUE_COL},
                DATE_DELETED_COL + " IS NULL", null, null, null,
                BOOK_CODE_COL + ", " + SORT_COL);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(destFile)))) {
            writer.write("book,ref,comment\n");
            while (c.moveToNext()) {
                writer.write(c.getString(0) + "," +
                        c.getString(1) + "," +
                        Utils.escapeCsv(c.getString(2)) + "\n");
            }
        }
        finally {
            c.close();
            db.close();
        }
    }
}
