package com.couchbase.helloworld.model;

import android.support.annotation.NonNull;
import android.util.Log;
import com.couchbase.helloworld.couchbase.AbstractCouchbaseLiteObject;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

public class Product extends AbstractCouchbaseLiteObject {

  private String barkod;
  private String Stokkod;
  private String UrunAciklama;
  private String TAG = "Product";

  public String getUrunAciklama() {
    return UrunAciklama;
  }

  public String getBarkod() {
    return barkod;
  }

  public String getStokkod() {
    return Stokkod;
  }

  private Product() {
    //do nothing
  }

  private Product(Builder builder) {
    setDatabase(builder.database);
    this.barkod = builder.barkod;
    this.Stokkod = builder.Stokkod;
    this.UrunAciklama = builder.UrunAciklama;
  }

  // dummy data
  private List<Product> populateProducts() throws CouchbaseLiteException {

    List<Product> productList = new ArrayList<>();
    for (int i = 0; i < ITEM_SIZE; i++) {
      productList.add(new Product.Builder().setBarkod(String.valueOf(i))
          .setStokkod("1281810047")
          .setUrunAciklama("Gomlek")
          .build());

      productList.add(new Product.Builder().setBarkod(String.valueOf(i))
          .setStokkod("1281810031")
          .setUrunAciklama("Mont")
          .build());

      productList.add(new Product.Builder().setBarkod(String.valueOf(i))
          .setStokkod("1281810035")
          .setUrunAciklama("Kazak")
          .build());

      productList.add(new Product.Builder().setBarkod(String.valueOf(i))
          .setStokkod("1281810052")
          .setUrunAciklama("Pantalon")
          .build());

      productList.add(new Product.Builder().setBarkod(String.valueOf(i))
          .setStokkod("1281810029")
          .setUrunAciklama("Tshirt")
          .build());
    }

    return productList;
  }

  @Override protected String getDocumentType() {
    return "product";
  }

  @Override public void updateDocument(String documentId) throws CouchbaseLiteException {
    Document retrievedDocument = getDocument(documentId);
    Map<String, Object> updatedProperties = new HashMap<>();
    updatedProperties.putAll(retrievedDocument.getProperties());
    updatedProperties.put("UrunAciklama", "mont");
    updatedProperties.put("boy", "42");
    retrievedDocument.putProperties(updatedProperties);
  }

  public void deleteAllProducts() throws CouchbaseLiteException {
    deleteAllDocuments("barkod", getDocumentType());
  }

  public void fetchAllProducts() throws CouchbaseLiteException {
    create(populateProducts());
  }

  @NonNull private Map<String, Object> getPropertyList(Product product)
      throws CouchbaseLiteException {
    String currentTimeString = getCurrentTimeAsString();
    Map<String, Object> property = new HashMap<>();
    property.put("type", getDocumentType());
    property.put("barkod", product.barkod);
    property.put("Stokkod", product.Stokkod);
    property.put("UrunAciklama", product.UrunAciklama);
    property.put("creationDate", currentTimeString);
    return property;
  }

  @Override protected List<AbstractCouchbaseLiteObject> transformData(QueryEnumerator enumator) {
    if (super.data == null) {
      super.data = new ArrayList<>();
    } else {
      super.data.clear();
    }
    for (int i = 0; i < enumator.getCount(); i++) {
      QueryRow row = enumator.getRow(i);
      data.add(new Product.Builder().setBarkod(row.getDocument().getProperty("barkod").toString())
          .setStokkod(row.getDocument().getProperty("Stokkod").toString())
          .setUrunAciklama(row.getDocument().getProperty("UrunAciklama").toString())
          .build());
    }
    return data;
  }

  public void updateProduct(String key, String value) throws CouchbaseLiteException {
    final LiveQuery liveQuery = retrieveAllByWhere(key, value);
    updateDocuments(liveQuery);
    liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
      @Override public void changed(LiveQuery.ChangeEvent event) {
        liveQuery.stop();
        try {
          for (Iterator<QueryRow> it = event.getRows(); it.hasNext(); ) {
            QueryRow row = it.next();
            updateDocument(row.getDocument().getId());
          }
        } catch (CouchbaseLiteException e) {
          e.printStackTrace();
        }
      }
    });
    liveQuery.start();
  }

  public void create(Product product) throws CouchbaseLiteException {
    create(Collections.singletonList(product));
  }

  public void create(final List<Product> products) throws CouchbaseLiteException {
    final DateTime start = DateTime.now();
    Runnable runnable = new Runnable() {
      @Override public void run() {
        for (int i = 0; i < products.size(); i++) {
          int j = i % products.size();
          Product product = products.get(j);
          try {
            createDocument(createDocumentIdWithKey(product.getBarkod()), getPropertyList(product));
          } catch (CouchbaseLiteException e) {
            e.printStackTrace();
          }
        }
        notifyView(EventType.INSERTED);
        DateTime finish = DateTime.now();
        Seconds seconds = Seconds.secondsBetween(start, finish);

        Log.e(TAG, String.format("total created item count is %s in %s sn", ITEM_SIZE,
            seconds.getSeconds()));
      }
    };
    new Thread(runnable).start();
  }

  public static class Builder {
    private Database database;
    private String barkod;
    private String Stokkod;
    private String UrunAciklama;

    public Builder setDatabase(Database database) {
      this.database = database;
      return this;
    }

    public Builder setBarkod(String barkod) {
      this.barkod = barkod;
      return this;
    }

    public Builder setStokkod(String Stokkod) {
      this.Stokkod = Stokkod;
      return this;
    }

    public Builder setUrunAciklama(String urunAciklama) {
      this.UrunAciklama = urunAciklama;
      return this;
    }

    public Product build() {
      return new Product(this);
    }
  }
}