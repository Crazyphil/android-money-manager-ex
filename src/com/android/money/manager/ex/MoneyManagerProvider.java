/*******************************************************************************
 * Copyright (C) 2012 The Android Money Manager Ex Project
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 ******************************************************************************/
package com.android.money.manager.ex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.money.manager.ex.database.Dataset;
import com.android.money.manager.ex.database.MoneyManagerOpenHelper;
import com.android.money.manager.ex.database.QueryAccountBills;
import com.android.money.manager.ex.database.QueryCategorySubCategory;
import com.android.money.manager.ex.database.TableAccountList;
import com.android.money.manager.ex.database.TableAssets;
import com.android.money.manager.ex.database.TableBillsDeposits;
import com.android.money.manager.ex.database.TableBudgetTable;
import com.android.money.manager.ex.database.TableBudgetYear;
import com.android.money.manager.ex.database.TableCategory;
import com.android.money.manager.ex.database.TableCheckingAccount;
import com.android.money.manager.ex.database.TableCurrencyFormats;
import com.android.money.manager.ex.database.TableInfoTable;
import com.android.money.manager.ex.database.TablePayee;
import com.android.money.manager.ex.database.TableSplitTransactions;
import com.android.money.manager.ex.database.TableStock;
import com.android.money.manager.ex.database.TableSubCategory;
import com.android.money.manager.ex.database.ViewAllData;

/**
 * MoneyManagerProvider is the extension of the base class of Android
 * ContentProvider. Its purpose is to implement the read access and modify the
 * application data
 * 
 * @author Alessandro Lazzari (lazzari.ale@gmail.com)
 * @version 1.0.0
 * 
 */
public class MoneyManagerProvider extends ContentProvider {
	// tag LOGCAT
	private static final String LOGCAT = MoneyManagerProvider.class.getSimpleName();
	// helper to access database	
	private MoneyManagerOpenHelper databaseHelper;
	// object definition for the call to check the content
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
	// object map for the definition of the objects referenced in the URI
	private static Map<Integer, Object> mapContent = new HashMap<Integer, Object>();
	
	private Dataset[] objMoneyManager = new Dataset[] { new TableAccountList(),
			new TableAssets(), new TableBillsDeposits(),
			new TableBudgetTable(), new TableBudgetYear(), new TableCategory(),
			new TableCheckingAccount(), new TableCurrencyFormats(),
			new TableInfoTable(), new TablePayee(),
			new TableSplitTransactions(), new TableStock(),
			new TableSubCategory(), new ViewAllData(),
			new QueryAccountBills(), new QueryCategorySubCategory()};
	
	public static final String AUTHORITY = "com.android.money.manager.ex.provider";
	
	public MoneyManagerProvider() {
		super();
		// Cycle all datasets for the composition of UriMatcher
		for(int i = 0; i < objMoneyManager.length; i ++) {
			// add URI
			sUriMatcher.addURI(AUTHORITY, objMoneyManager[i].getBasepath(), i);
			// put map in the object being added in UriMatcher
			mapContent.put(i, objMoneyManager[i]);
		}
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.v(LOGCAT, "Delete URI: " + uri);
		// find object from uri
		Object ret = getObjectFromUri(uri);
		// safety control of having the where if not clean the table
		if (TextUtils.isEmpty(selection)) {
			Log.e(LOGCAT, "Delete not permitted because not define where clausole");
			return 0;
		}
		// take a database reference 
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		int rowsDelete = 0;
		// check type of istance dataset
		if (Dataset.class.isInstance(ret)) {
			Dataset dataset = ((Dataset)ret);
			switch (dataset.getType()) {
			case TABLE:
				String log = "DELETE FROM " + dataset.getSource();
				// compose log verbose
				if (TextUtils.isEmpty(selection) == false) { log += " WHERE " + selection; }
				if (selectionArgs != null) { log += "; ARGS=" + Arrays.asList(selectionArgs).toString(); }
				Log.i(LOGCAT, log);
				try {
					rowsDelete = database.delete(dataset.getSource(), selection, selectionArgs);
				} catch (SQLiteException sqlLiteExc) {
					Log.e(LOGCAT, "SQLiteException: " + sqlLiteExc.getMessage());
				} catch (Exception exc) {
					Log.e(LOGCAT, exc.getMessage());
				}
				break;
			default:
				throw new IllegalArgumentException("Type of dataset not supported for delete");
			}		
		} else {
			throw new IllegalArgumentException("Object ret of mapContent is not istance of dataset");
		}
		// delete notify
		getContext().getContentResolver().notifyChange(uri, null);
		// return rows delete
		return rowsDelete;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.v(LOGCAT, "Insert Uri: " + uri);
		// find object from uri
		Object ret = getObjectFromUri(uri);
		// database reference
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		long id = 0;
		String parse;
		// check instance type object
		if (Dataset.class.isInstance(ret)) {
			Dataset dataset = ((Dataset)ret);
			switch (dataset.getType()) {
			case TABLE:
				String log = "INSERT INTO " + dataset.getSource();
				// compose log verbose
				if (values != null) { log += " VALUES ( " + values.toString() + ")"; }
				Log.i(LOGCAT, log);
				try {
					id = database.insert(dataset.getSource(), null, values);
				} catch (SQLiteException sqlLiteExc) {
					Log.e(LOGCAT, "SQLiteException: " + sqlLiteExc.getMessage());
				} catch (Exception exc) {
					Log.e(LOGCAT, exc.getMessage());
				}
				parse = dataset.getBasepath() + "/" + id;
				break;
			default:
				throw new IllegalArgumentException("Type of dataset not supported for update");
			}		
		} else {
			throw new IllegalArgumentException("Object ret of mapContent is not istance of dataset");
		}
		// notify the data inserted
		getContext().getContentResolver().notifyChange(uri, null);
		// return Uri with primarykey inserted
		return Uri.parse(parse);
	}

	@Override
	public boolean onCreate() {
		// open connection to database
		databaseHelper = new MoneyManagerOpenHelper(getContext());
		// This statement serves to force the creation of the database
		SQLiteDatabase database = databaseHelper.getReadableDatabase();
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Log.v(LOGCAT, "Query URI: " + uri);
		// find object from uri
		Object ret = getObjectFromUri(uri);
		// take a database reference  
		SQLiteDatabase database = databaseHelper.getReadableDatabase();
		Cursor cursorRet;
		// compose log verbose instruction
		String log;
		if (projection != null) { log = "SELECT " + Arrays.asList(projection).toString(); } else {log = "SELECT *"; }
		// check type of instance dataset
		if (Dataset.class.isInstance(ret)) {
			Dataset dataset = ((Dataset)ret);
			// compose log
			log += " FROM " + dataset.getSource();
			if (TextUtils.isEmpty(selection) == false) { log += " WHERE " + selection; }
			if (TextUtils.isEmpty(sortOrder) == false) { log += " OREDER BY " + sortOrder; }
			if (selectionArgs != null) { log += "; ARGS=" + Arrays.asList(selectionArgs).toString(); }
			// log
			Log.i(LOGCAT, log);
			switch (dataset.getType()) {
			case QUERY:
				String query = prepareQuery(dataset.getSource(), projection, selection, sortOrder);
				cursorRet = database.rawQuery(query, selectionArgs);
				break;
			case TABLE: case VIEW:
				SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
				queryBuilder.setTables(dataset.getSource());
				cursorRet = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			default:
				throw new IllegalArgumentException("Type of dataset not definied");
			}		
		} else {
			throw new IllegalArgumentException("Object ret of mapContent is not istance of dataset");
		}
		// notify listeners waiting for the data is ready
		cursorRet.setNotificationUri(getContext().getContentResolver(), uri);
		return cursorRet;
	}

	@Override
	public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
		Log.v(LOGCAT, "Update Uri: " + uri);
		// find object from uri
		Object ret = getObjectFromUri(uri);
		// instace of database 
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		int rowsUpdate = 0;
		// check ret what type of class 
		if (Dataset.class.isInstance(ret)) {
			Dataset dataset = ((Dataset)ret);
			switch (dataset.getType()) {
			case TABLE:
				String log = "UPDATE " + dataset.getSource();
				// compose log verbose
				if (values != null) { log += " SET " + values.toString(); }
				if (TextUtils.isEmpty(whereClause) == false) { log += " WHERE " + whereClause; }
				if (whereArgs != null) { log += "; ARGS=" + Arrays.asList(whereArgs).toString(); }
				Log.i(LOGCAT, log);
				// update
				try {
					rowsUpdate = database.update(dataset.getSource(), values, whereClause, whereArgs);
				} catch (SQLiteException sqlLiteExc) {
					Log.e(LOGCAT, "SQLiteException: " + sqlLiteExc.getMessage());
				} catch (Exception exc) {
					Log.e(LOGCAT, exc.getMessage());
				}
				break;
			default:
				throw new IllegalArgumentException("Type of dataset not supported for update");
			}		
		} else {
			throw new IllegalArgumentException("Object ret of mapContent is not istance of dataset");
		}
		// notify update
		getContext().getContentResolver().notifyChange(uri, null);
		// return rows modified
		return rowsUpdate;
	}
	
	private String prepareQuery(String query, String[] projection, String selection, String sortOrder) {
		String selectList = "", from = "", where = "", sort = "";
		// composizione della selectlist
		if (projection == null) {
			selectList = "SELECT *";
		} else {
			
			selectList = "SELECT ";
			// ciclo i campi
			for(int i = 0; i < projection.length; i ++) {
				if (i > 0) { selectList += ", "; }
				selectList += projection[i];
			}
		}
		// composizione del from
		from = "FROM (" + query + ") T";
		// composzione del where
		if (TextUtils.isEmpty(selection) == false) {
			if (selection.contains("WHERE") == false) { where += "WHERE"; }
			where += " " + selection;
		}
		// composizione del sort
		if (TextUtils.isEmpty(sortOrder) == false) {
			if (sortOrder.contains("ORDER BY") == false) { sort += "ORDER BY " ; }
			sort += " " + sortOrder;
		}
		// composizione della query da ritornare
		query = selectList + " " + from;
		// controllo se ho where e sort
		if (TextUtils.isEmpty(where) == false) { query += " " + where; }
		if (TextUtils.isEmpty(sort) == false) { query += " " + sort; }
		// restituisco la query
		return query;
	}
	
	private Object getObjectFromUri(Uri uri) {
		// match dell'uri
		int uriMatch = sUriMatcher.match(uri);
		Log.v(LOGCAT, "Uri Match Result: "  + Integer.toString(uriMatch));
		// find key into hash map
		Object objectRet = mapContent.get(uriMatch);
		if (objectRet == null) {
			throw new IllegalArgumentException("Unknown URI for Update: " + uri);
		}
		
		return objectRet; 
	}
}
