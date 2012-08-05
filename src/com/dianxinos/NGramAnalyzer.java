package com.dianxinos;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * @author wangweiwei
 *         Date: 8/5/12
 *         Time: 4:46 PM
 */
public class NGramAnalyzer extends Analyzer {
    protected final Version matchVersion;
    public NGramAnalyzer(Version version)
    {
        matchVersion = version;
    }
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new NGramTokenFilter(new StandardTokenizer(matchVersion,reader),2,6);
    }
}