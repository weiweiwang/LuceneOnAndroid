package com.dianxinos;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setMovementMethod(new ScrollingMovementMethod());
        Logger.LEVEL = Log.VERBOSE;
        try {
            File indexDir = new File(Environment.getExternalStorageDirectory(), "lucene");
            boolean alreadyExisted = indexDir.exists();
            directory = FSDirectory.open(indexDir);
            NRTCachingDirectory cachedFSDir = new NRTCachingDirectory(directory, 1.0, 2.0);
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
                long total = rebuildIndex();
                long end = System.currentTimeMillis();
                textView.append("\nbuild:" + total + " contacts, time used(ms):" + (end - start));
            }
            try {
                textView.append("\nnumDocs:" + indexWriter.numDocs() + "\n");
                List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>(5);

                textView.append("query:999\n");
                long start = System.currentTimeMillis();
                long hits = query("999", 5, docs);
                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
                docs.clear();

                textView.append("query:92649\n");
                start = System.currentTimeMillis();
                hits = query("92649", 5, docs);
                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
                docs.clear();

                textView.append("query:82642\n");
                start = System.currentTimeMillis();
                hits = query("82642", 5, docs);
                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
                docs.clear();

                textView.append("query:825\n");
                start = System.currentTimeMillis();
                hits = query("825", 5, docs);
                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
                docs.clear();

                textView.append("query:1381\n");
                start = System.currentTimeMillis();
                hits = query("1381", 5, docs);
                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
                docs.clear();

                textView.append("query:111\n");
                start = System.currentTimeMillis();
                hits = query("111", 5, docs);
                textView.append("time used(ms):" + (System.currentTimeMillis() - start) + ",hits:" + hits + ",docs:" + docs.toString() + "\n");
                docs.clear();
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
            Query q = query == null ? new MatchAllDocsQuery() : multiFieldQueryParser.parse(query);
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

    public long rebuildIndex() {
        long total = 0;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("name_phone.txt")));
            String line = null;
            int id = 0;
            HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
            format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
            format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            format.setVCharType(HanyuPinyinVCharType.WITH_V);
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                total++;
                String[] splits = line.split("\t");
                Document document = new Document();
                String name = splits[0];
                String phone = splits[1];
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
                ++id;
                Logger.d(TAG, "name:" + name + ",phone:" + phone + ",pinyin:" + pinyin + ",jianpin:" + jianpin);
                document.add(new Field("name", name, Field.Store.YES, Field.Index.NOT_ANALYZED));
                document.add(new Field("pinyin", pinyin, Field.Store.NO, Field.Index.ANALYZED));
                if (!pinyin.equalsIgnoreCase(jianpin)) {
                    document.add(new Field("jianpin", jianpin, Field.Store.NO, Field.Index.ANALYZED));
                }
                document.add(new Field("phone", phone, Field.Store.YES, Field.Index.ANALYZED));
                document.add(new Field("id", String.valueOf(id), Field.Store.YES, Field.Index.NOT_ANALYZED));
                indexWriter.updateDocument(new Term("id", String.valueOf(id)), document);
            }
            Map<String, String> userData = new HashMap<String, String>();
            userData.put("action", "rebuild");
            indexWriter.commit(userData);
        } catch (Exception e) {
            Logger.w(TAG, e.toString(), e);
        }
        return total;
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
    }

}
