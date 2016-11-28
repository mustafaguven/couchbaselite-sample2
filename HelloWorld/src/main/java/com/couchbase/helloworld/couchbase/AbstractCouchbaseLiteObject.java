package com.couchbase.helloworld.couchbase;

import android.util.Log;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

public abstract class AbstractCouchbaseLiteObject {

  private static final String TAG = "AbstractCouchbase";

  public void setOnCompletion(OnCompletion onCompletion) {
    this.onCompletion = onCompletion;
  }

  protected final int ITEM_SIZE = 20;
  private OnCompletion onCompletion;
  protected List<AbstractCouchbaseLiteObject> data;
  private Database database;

  public interface OnCompletion {
    void onDataReceived(EventType eventType, List<AbstractCouchbaseLiteObject> barcode);

    void onTotalRowCount(EventType eventType, int count);
  }

  protected void setDatabase(Database database) {
    this.database = database;
  }

  private Database getDatabase() throws CouchbaseLiteException {
    return database;
  }

  protected abstract String getDocumentType();

  protected Document createDocument(String documentId, Map<String, Object> properties)
      throws CouchbaseLiteException {
    Document document = getDatabase().getDocument(documentId);
    document.putProperties(properties);
    return document;
  }

  protected abstract void updateDocument(String documentId) throws CouchbaseLiteException;

  protected void updateDocuments(final LiveQuery liveQuery) {
    liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
      @Override public void changed(LiveQuery.ChangeEvent event) {
        liveQuery.stop();
        try {
          for (Iterator<QueryRow> it = event.getRows(); it.hasNext(); ) {
            QueryRow row = it.next();
            updateDocument(row.getDocument().getId());
          }
          notifyView(EventType.UPDATED);
        } catch (CouchbaseLiteException e) {
          e.printStackTrace();
        }
      }
    });
    liveQuery.start();
  }

  private void deleteDocument(String documentId) throws CouchbaseLiteException {
    getDatabase().getDocument(documentId).delete();
  }

  protected void deleteAllDocuments(String key, String viewName) throws CouchbaseLiteException {
    final DateTime startDelete = DateTime.now();
    final LiveQuery liveQuery = retrieveAll(key, viewName, 0);
    liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
      @Override public void changed(LiveQuery.ChangeEvent event) {
        liveQuery.stop();
        try {
          for (Iterator<QueryRow> it = event.getRows(); it.hasNext(); ) {
            QueryRow row = it.next();
            deleteDocument(row.getDocumentId());
          }
          notifyView(EventType.DELETED);
          DateTime finishDelete = DateTime.now();
          Seconds seconds = Seconds.secondsBetween(startDelete, finishDelete);

          Log.e(TAG, String.format("total deleted item count is %s in %s sn", ITEM_SIZE,
              seconds.getSeconds()));
        } catch (CouchbaseLiteException e) {
          //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
      }
    });
    liveQuery.start();
  }

  protected void notifyView(EventType eventType, List<AbstractCouchbaseLiteObject> data) {
    if (onCompletion != null) {
      onCompletion.onDataReceived(eventType, data);
    }
  }

  protected void notifyView(EventType eventType) {
    notifyView(eventType, null);
  }

  protected String getCurrentTimeAsString() {
    SimpleDateFormat dateFormatter =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    Calendar calendar = GregorianCalendar.getInstance();
    return dateFormatter.format(calendar.getTime());
  }

  public void getAllItems() throws CouchbaseLiteException {
    final LiveQuery liveQuery = retrieveAll("barkod", getDocumentType(), 0);
    liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
      @Override public void changed(final LiveQuery.ChangeEvent event) {
        liveQuery.stop();
        if (onCompletion != null) {
          onCompletion.onDataReceived(EventType.QUERIED, transformData(event.getRows()));
        }
      }
    });
    liveQuery.start();
  }

  public void getRowCount() throws CouchbaseLiteException {
    Runnable runnable = new Runnable() {
      @Override public void run() {
        final Query query;
        try {
          query = retrieveOnlyCount("barkod", getDocumentType());
          QueryEnumerator result = query.run();
          onCompletion.onTotalRowCount(EventType.QUERIED, result.getCount());
        } catch (CouchbaseLiteException e) {
          e.printStackTrace();
        }
      }
    };
    new Thread(runnable).start();

    /*liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
      @Override public void changed(final LiveQuery.ChangeEvent event) {
        liveQuery.stop();
        if (onCompletion != null) {
          onCompletion.onTotalRowCount(EventType.QUERIED, event.getRows().getCount());
        }
      }
    });
    liveQuery.start();*/
  }

  protected abstract List<AbstractCouchbaseLiteObject> transformData(QueryEnumerator document);

  private LiveQuery retrieveAll(final String key, String viewName, int limit)
      throws CouchbaseLiteException {
    com.couchbase.lite.View view = getDatabase().getView(viewName);
    if (view.getMap() == null) {
      Mapper map = new Mapper() {
        @Override public void map(Map<String, Object> document, Emitter emitter) {
          if (getDocumentType().equals(document.get("type"))) {
            emitter.emit(document.get(key), document);
          }
        }
      };
      view.setMap(map, "3");
    }

    LiveQuery query = view.createQuery().toLiveQuery();
    query.setDescending(true);
    if (limit > 0) {
      query.setLimit(limit);
    }
    return query;
  }

  private Query retrieveOnlyCount(final String key, String viewName) throws CouchbaseLiteException {
    com.couchbase.lite.View view = getDatabase().getView(viewName);
    if (view.getMap() == null) {
      Mapper map = new Mapper() {
        @Override public void map(Map<String, Object> document, Emitter emitter) {
          if (getDocumentType().equals(document.get("type"))) {
            emitter.emit(document.get(key), document);
          }
        }
      };
      view.setMap(map, "3");
    }

    Query query = view.createQuery();
    query.setMapOnly(true);
    return query;
  }

  protected LiveQuery retrieveAllByWhere(final String key, String value)
      throws CouchbaseLiteException {

    com.couchbase.lite.View view = getDatabase().getView(key);
    if (view.getMap() == null) {
      Mapper map = new Mapper() {
        @Override public void map(Map<String, Object> document, Emitter emitter) {
          if (getDocumentType().equals(document.get("type"))) {
            emitter.emit(document.get(key), document);
          }
        }
      };
      view.setMap(map, "2");
    }
    LiveQuery query = view.createQuery().toLiveQuery();
    query.setStartKey(value);
    query.setEndKey(value);
    return query;
  }

  protected String createDocumentIdWithKey(String key) {
    return key + "." + UUID.randomUUID();
  }

  protected Document getDocument(String id) throws CouchbaseLiteException {
    return getDatabase().getDocument(id);
  }

  public enum EventType {
    INSERTED, DELETED, UPDATED, QUERIED
  }
}
