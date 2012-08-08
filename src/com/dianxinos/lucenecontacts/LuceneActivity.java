package com.dianxinos.lucenecontacts;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.Version;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class LuceneActivity extends Activity {
	private static final String TAG = LuceneActivity.class.getName();
	private static final Map<Character, Character> T9_MAP = new HashMap<Character, Character>() {
		{
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
		}
	};
	private Directory directory = null;
	private IndexWriter indexWriter = null;
	private IndexWriterConfig indexWriterConfig = null;
	private PerFieldAnalyzerWrapper indexAnalyzer = null;

	private TextView writeView = null;
	private String writevalue = "";
	List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>(5);;
	TextView textView = null;
	TextView indextotal = null;
	private int searchLength = 0;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		textView = (TextView) findViewById(R.id.text);
		writeView = (TextView) findViewById(R.id.write);
		indextotal = (TextView) findViewById(R.id.indextotal);
		;

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
				// long start = System.currentTimeMillis();
				// long total1 = rebuildContactsIndex();
				// long total2 = rebuildIndex();
				// long end = System.currentTimeMillis();
				// textView.append("\nbuild:" + (total1 + total2) +
				// " contacts, time used(ms):" + (end - start));

				// IndexTask task = new IndexTask(indextotal);
				// task.execute(100);
				new AsyncTask() {
					private long total1 = 0;
					private long total2 = 0;
					private long start = 0;
					private long end = 0;
					@Override
					protected Object doInBackground(Object... params) {
						 start = System.currentTimeMillis();
						 total1 = rebuildContactsIndex();
						 total2 = rebuildIndex();
						 end = System.currentTimeMillis();
						return null;
					}
					protected void onPostExecute(Object result) {
						indextotal.setText("index contacts totalnum:" + (total1 + total2) + " , time used(ms):" + (end - start));
					};

				}.execute(null);
			}else{
				indextotal.setText("index finished");
			}
			try {
				textView.append("\nnumDocs:" + indexWriter.numDocs() + "\n");
			} catch (IOException e) {
				Logger.w(TAG, e.toString(), e);
			}

		} catch (IOException e) {
			Logger.w(TAG, e.toString(), e);
		}
	}

	/**
	 * Description: T9输入值匹配
	 * 
	 * @Version1.0 2012-8-8 下午2:07:41 marke created
	 * @param view
	 */
	public void setT9Value(View view) {
		int id = view.getId();
		switch (id) {
		case R.id.button1: {
			writevalue += "1";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button2: {
			writevalue += "2";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button3: {
			writevalue += "3";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button4: {
			writevalue += "4";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button5: {
			writevalue += "5";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button6: {
			writevalue += "6";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button7: {
			writevalue += "7";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button8: {
			writevalue += "8";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button9: {
			writevalue += "9";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button10: {
			writevalue += "0";
			writeView.setText(writevalue);
			if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
				query(writevalue, 5, docs);
				textView.setText("docs:" + docs.toString() + "\n");
				docs.clear();
			}
			break;
		}
		case R.id.button11: {
			break;
		}
		case R.id.button12: {
			indextotal.setText("index totalnum...");
			new AsyncTask() {
				private long total1 = 0;
				private long total2 = 0;
				private long start = 0;
				private long end = 0;
				@Override
				protected Object doInBackground(Object... params) {
					 start = System.currentTimeMillis();
					 total1 = rebuildContactsIndex();
					 total2 = rebuildIndex();
					 end = System.currentTimeMillis();
					return null;
				}
				protected void onPostExecute(Object result) {
					indextotal.setText("index contacts totalnum:" + (total1 + total2) + " , time used(ms):" + (end - start));
				};

			}.execute(null);
			break;
		}
		case R.id.button13: {
			if (!writevalue.equals("") && null != writevalue) {
				writevalue = writevalue.substring(0, writevalue.length() - 1);
				writeView.setText(writevalue);
				if (writevalue == null || writevalue.equals("")) {
					textView.setText("");
				} else {
					if (writevalue.startsWith("0") || writevalue.startsWith("1") || writevalue.length() > searchLength) {
						query(writevalue, 5, docs);
						textView.setText("docs:" + docs.toString() + "\n");
						docs.clear();
					}
				}

			} else {
				textView.setText("");
			}
			break;
		}
		}
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
				if (line.isEmpty())
					continue;
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

	public long query(String query, int count, List<Map<String, Object>> docs) {
		Map<String, Float> boosts = new HashMap<String, Float>();
		if (query.startsWith("0") || query.startsWith("1")) {
			boosts.put("phone", 5.0F);
		} else {
			boosts.put("jianpin", 5.0F);
			boosts.put("pinyin", 5.0F);
			boosts.put("phone", 1.0F);
		}

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
		// 取得电话本中开始一项的光标，必须先moveToNext()
		Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		long hits = 0;
		HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		format.setVCharType(HanyuPinyinVCharType.WITH_V);
		while (cursor.moveToNext()) {
			hits++;
			try {
				// 取得联系人的名字索引
				int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
				String name = cursor.getString(nameIndex);
				if (name != null) {
					name = name.trim();
				}
				if (name == null || name.isEmpty()) {
					continue;
				}

				// 取得联系人的ID索引值
				String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
				// 查询该位联系人的电话号码，类似的可以查询email，photo
				Cursor phone = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);// 第一个参数是确定查询电话号，第三个参数是查询具体某个人的过滤值
				// 一个人可能有几个号码
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
