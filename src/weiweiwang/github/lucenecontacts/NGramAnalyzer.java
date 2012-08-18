package weiweiwang.github.lucenecontacts;

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
    private  int minGram;
    private int maxGram;
    public NGramAnalyzer(Version version,int minGram,int maxGram)
    {
        matchVersion = version;
        this.minGram = minGram;
        this.maxGram= maxGram;
    }
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new NGramTokenFilter(new StandardTokenizer(matchVersion,reader),minGram,maxGram);
    }
}