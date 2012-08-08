package com.dianxinos.lucenecontacts;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuceneActivity extends Activity {
    private static final String TAG = LuceneActivity.class.getName();
    private static final Map<Character, Character> T9_MAP = new HashMap<Character, Character>() {{
        put('a', '2');
        put('b', '2');
        put('c', '2');

        put('d', '3');
        put('e', '3');
        put('f', '3');

        put('g', '4');
        put('h', '4');
        put('i', '4');

        put('j', '5');
        put('k', '5');
        put('l', '5');

        put('m', '6');
        put('n', '6');
        put('o', '6');

        put('p', '7');
        put('q', '7');
        put('r', '7');
        put('s', '7');

        put('t', '8');
        put('u', '8');
        put('v', '8');

        put('w', '9');
        put('x', '9');
        put('y', '9');
        put('z', '9');
    }};
    private Directory directory = null;
    private IndexWriter indexWriter = null;
    private IndexWriterConfig indexWriterConfig = null;
    private PerFieldAnalyzerWrapper indexAnalyzer = null;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final TextView textView = (TextView) findViewById(R.id.text);
        textView.setMovementMethod(new ScrollingMovementMethod());

        final EditText editText = (EditText) findViewById(R.id.input);
        final Button button = (Button) findViewById(R.id.search);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = editText.getText().toString();
                final int n = 10;
                List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>(n);
                long start = System.currentTimeMillis();
                long hits = query(query, n, docs);
                textView.setText("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
            }
        });

        Logger.LEVEL = Log.VERBOSE;
        try {
            File indexDir = new File(Environment.getExternalStorageDirectory(), "lucene");
            boolean alreadyExisted = indexDir.exists();
            directory = FSDirectory.open(indexDir);
            NRTCachingDirectory cachedFSDir = new NRTCachingDirectory(directory, 0.5, 1.0);
            Map<String, Analyzer> fieldAnalyzers = new HashMap<String, Analyzer>();
            fieldAnalyzers.put("name", new StandardAnalyzer(Version.LUCENE_36));
            fieldAnalyzers.put("phone", new NGramAnalyzer(Version.LUCENE_36));
            fieldAnalyzers.put("pinyin", new EdgeNGramAnalyzer(Version.LUCENE_36));
            fieldAnalyzers.put("jianpin", new EdgeNGramAnalyzer(Version.LUCENE_36));
            indexAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_36), fieldAnalyzers);
            indexWriterConfig = new IndexWriterConfig(Version.LUCENE_36, indexAnalyzer);
            indexWriterConfig.setMergeScheduler(cachedFSDir.getMergeScheduler());
            try {
                indexWriter = new IndexWriter(cachedFSDir, indexWriterConfig);
            } catch (IOException e) {
                throw e;
            }
            if (!alreadyExisted) {
                long start = System.currentTimeMillis();
                long total = rebuildContactsIndex();
                long end = System.currentTimeMillis();
                textView.append("\nbuild:" + total + " contacts, time used(ms):" + (end - start));
            }
            try {
                textView.append("\nnumDocs:" + indexWriter.numDocs() + "\n");
//                List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>(5);
//
//                textView.append("query:999\n");
//                long start = System.currentTimeMillis();
//                long hits = query("999", 5, docs);
//                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
//                docs.clear();
//
//                textView.append("query:92649\n");
//                start = System.currentTimeMillis();
//                hits = query("92649", 5, docs);
//                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
//                docs.clear();
//
//                textView.append("query:82642\n");
//                start = System.currentTimeMillis();
//                hits = query("82642", 5, docs);
//                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
//                docs.clear();
//
//                textView.append("query:825\n");
//                start = System.currentTimeMillis();
//                hits = query("825", 5, docs);
//                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
//                docs.clear();
//
//                textView.append("query:1381\n");
//                start = System.currentTimeMillis();
//                hits = query("1381", 5, docs);
//                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
//                docs.clear();
//
//                textView.append("query:111\n");
//                start = System.currentTimeMillis();
//                hits = query("111", 5, docs);
//                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
//                docs.clear();
//
//                textView.append("query:7933\n");
//                start = System.currentTimeMillis();
//                hits = query("7933", 5, docs);
//                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
//                docs.clear();
            } catch (IOException e) {
                Logger.w(TAG, e.toString(), e);
            }

        } catch (IOException e) {
            Logger.w(TAG, e.toString(), e);
        }
    }


    public long query(String query, int count, List<Map<String, Object>> docs) {
        Map<String, Float> boosts = new HashMap<String, Float>();
        boosts.put("jianpin", 5.0F);
        boosts.put("pinyin", 5.0F);
        boosts.put("phone", 1.0F);
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(Version.LUCENE_36, boosts.keySet().toArray(new String[0]), new StandardAnalyzer(Version.LUCENE_36), boosts);
        try {
            Query q = (query == null || query.isEmpty()) ? new MatchAllDocsQuery() : multiFieldQueryParser.parse(query);
            Logger.d(TAG, "query:" + q.toString());
            IndexReader indexReader = IndexReader.open(indexWriter, true);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            TopDocs td = indexSearcher.search(q, count);
            long hits = td.totalHits;
            ScoreDoc[] scoreDocs = td.scoreDocs;
            for (ScoreDoc scoreDoc : scoreDocs) {
                Map<String, Object> doc = new HashMap<String, Object>();
                Document document = indexReader.document(scoreDoc.doc);
                doc.put("name", document.get("name"));
                doc.put("phone", document.get("phone"));
                docs.add(doc);
            }
            return hits;
        } catch (Exception e) {
            Logger.w(TAG, e.toString(), e);
        }
        return 0;
    }

    public long rebuildContactsIndex() {
        ContentResolver cr = getContentResolver();
        //取得电话本中开始一项的光标，必须先moveToNext()
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        long hits = 0;
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        while (cursor.moveToNext()) {
            hits++;
            try {
                //取得联系人的名字索引
                int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                String name = cursor.getString(nameIndex);
                if (name != null) {
                    name = name.trim();
                }
                if (name == null || name.isEmpty()) {
                    continue;
                }

                //取得联系人的ID索引值
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                //查询该位联系人的电话号码，类似的可以查询email，photo
                Cursor phone = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = "
                                + contactId, null, null);//第一个参数是确定查询电话号，第三个参数是查询具体某个人的过滤值
                //一个人可能有几个号码
                List<String> phones = new ArrayList<String>();
                while (phone.moveToNext()) {
                    String phoneNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).trim();
                    if (phoneNumber.isEmpty()) {
                        continue;
                    }
                    phones.add(phoneNumber.replaceAll("[^+\\d]", ""));
                }
                phone.close();
                if (phones.isEmpty()) {
                    continue;
                }

                Document document = new Document();
                char[] nameChars = name.toCharArray();
                StringBuilder firstLetters = new StringBuilder();
                StringBuilder pinyinLetters = new StringBuilder();
                for (char c : nameChars) {
                    String[] strs = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (null != strs) {
                        firstLetters.append(strs[0].charAt(0));
                        pinyinLetters.append(strs[0]);
                    } else if (!Character.isSpace(c)) {
                        firstLetters.append(Character.toLowerCase(c));
                        pinyinLetters.append(Character.toLowerCase(c));
                    }
                }
                String jianpin = getT9String(firstLetters.toString());
                String pinyin = getT9String(pinyinLetters.toString());
                document.add(new Field("name", name, Field.Store.YES, Field.Index.NOT_ANALYZED));
                document.add(new Field("pinyin", pinyin, Field.Store.NO, Field.Index.ANALYZED));
                if (!pinyin.equalsIgnoreCase(jianpin)) {
                    document.add(new Field("jianpin", jianpin, Field.Store.NO, Field.Index.ANALYZED));
                }
                for (String phoneNumber : phones) {
                    document.add(new Field("phone", phoneNumber, Field.Store.YES, Field.Index.ANALYZED));
                }

                Logger.d(TAG, "name:" + name + ",phone:" + phones + ",pinyin:" + pinyin + ",jianpin:" + jianpin);
                indexWriter.updateDocument(new Term("id", contactId), document);
            } catch (Exception e) {
                Logger.d(TAG, e.toString(), e);
            }
        }
        cursor.close();
        Map<String, String> userData = new HashMap<String, String>();
        userData.put("action", "rebuild");
        try {
            indexWriter.commit(userData);
        } catch (IOException e) {
            Logger.d(TAG, e.toString(), e);
        }
        return hits;
    }

    private String getT9String(String str) {
        char[] chars = str.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : chars) {
            Character t9Char = T9_MAP.get(c);
            if (null != t9Char) {
                stringBuilder.append(t9Char);
            } else {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "destroy and save index");
        try {
            indexWriter.close();
        } catch (IOException e) {
            Logger.w(TAG, e.toString(), e);
        }
        finish();
    }

}
