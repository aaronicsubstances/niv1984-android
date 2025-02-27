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
    private static final String HREF_COL = "href";
    private static final String DATE_CREATED_COL = "date_created";

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
                + HREF_COL + " TEXT,"
                + DATE_CREATED_COL + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

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

    public List<String> loadFootnoteStates(String bcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, new String[]{ HREF_COL },
                String.format("%s = ?", BOOK_CODE_COL),
                new String[]{ bcode }, null, null, null);
        List<String> results = new ArrayList<>();
        while (c.moveToNext()) {
            results.add(c.getString(0));
        }
        c.close();
        db.close();
        return results;
    }

    public boolean toggleFootnoteStatus(String bcode, String href) {
        // on below line we are creating a
        // variable for content values.
        ContentValues values = new ContentValues();

        // on below line we are passing all values
        // along with its key and value pair.
        values.put(BOOK_CODE_COL, bcode);
        values.put(HREF_COL, href);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, new String[]{ HREF_COL },
                String.format("%s = ? AND %s = ?", BOOK_CODE_COL, HREF_COL),
                new String[]{ bcode, href }, null, null, null);
        if (c.moveToNext()) {
            c.close();
            db = this.getWritableDatabase();
            db.delete(TABLE_NAME,
                    String.format("%s = ? AND %s = ?", BOOK_CODE_COL, HREF_COL),
                    new String[]{ bcode, href });
            db.close();
            return false;
        }
        else {
            c.close();
            // on below line we are creating a variable for
            // our sqlite database and calling writable method
            // as we are writing data in our database.
            db = this.getWritableDatabase();

            // after adding all values we are passing
            // content values to our table.
            db.insert(TABLE_NAME, null, values);

            // at last we are closing our
            // database after adding database.
            db.close();
            return true;
        }
    }

    public void serializeFootnotes(File destFile) throws IOException {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, new String[]{ BOOK_CODE_COL, HREF_COL },
                null, null, null, null,
                BOOK_CODE_COL + ", " + HREF_COL);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(destFile)))) {
            while (c.moveToNext()) {
                writer.write(c.getString(0) + "," +
                        c.getString(1) + "\n");
            }
        }
        finally {
            c.close();
            db.close();
        }
    }
}
