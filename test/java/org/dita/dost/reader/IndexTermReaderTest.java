/*
 * This file is part of the DITA Open Toolkit project hosted on
 * Sourceforge.net. See the accompanying license.txt file for 
 * applicable licenses.
 */

/*
 * (c) Copyright IBM Corp. 2011 All Rights Reserved.
 */
package org.dita.dost.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.dita.dost.TestUtils;
import org.dita.dost.index.IndexTerm;
import org.dita.dost.index.IndexTermCollection;
import org.dita.dost.index.IndexTermTarget;
import org.dita.dost.util.Constants;
import org.dita.dost.util.StringUtils;

/**
 * IndexTermReader unit test.
 * 
 * @author Jarno Elovirta
 */
public class IndexTermReaderTest {

    private final File resourceDir = new File("test-stub", IndexTermReaderTest.class.getSimpleName());
    private File tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = TestUtils.createTempDir(getClass());
    }

    @Test
    public void testExtractIndexTerm() throws SAXException {
        if (System.getProperty(Constants.SAX_DRIVER_PROPERTY) == null) {
            StringUtils.initSaxDriver();
        }
        final File target = new File(tempDir, "concept.html");
        final IndexTermReader handler = new IndexTermReader();
        handler.setTargetFile(target.getAbsolutePath());
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(handler);
        final File source = new File(resourceDir, "concept.dita");
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(source);
            xmlReader.parse(new InputSource(inputStream));
        } catch (final Exception e) {
            fail(e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    fail(e.getMessage());
                }
            }
        }		
        @SuppressWarnings("unchecked")
        final List<IndexTerm> act = IndexTermCollection.getInstantce().getTermList();

        final List<IndexTerm> exp = new ArrayList<IndexTerm>();
        exp.add(generateIndexTerms(target, "Primary", "Secondary", "Tertiary"));
        exp.add(generateIndexTerms(target, "Primary normalized", "Secondary normalized", "Tertiary normalized"));
        // The following exhibits a bug in IndexTermReader
        exp.add(generateIndexTerms(target, " Primary unnormalized "));
        final IndexTerm exception = generateIndexTerm(null, " Primary unnormalized  ");
        exception.addSubTerm(generateIndexTerm(target, " Secondary unnormalized "));
        final IndexTerm secondException = generateIndexTerm(null, " Secondary unnormalized  ");
        secondException.addSubTerm(generateIndexTerm(target, " Tertiary unnormalized "));
        exception.addSubTerm(secondException);
        exp.add(exception);
        
        assertEquals(exp, act);
    }

    @After
    public void tearDown() throws IOException {
        TestUtils.forceDelete(tempDir);
    }

    private IndexTerm generateIndexTerms(final File target, final String... texts) {
        final LinkedList<IndexTerm> stack = new LinkedList<IndexTerm>();
        for (final String text: texts) {
            final IndexTerm primary = generateIndexTerm(target, text);
            if (!stack.isEmpty()) {
                stack.getLast().addSubTerm(primary);
            }
            stack.addLast(primary);
        }
        return stack.getFirst();
    }

    private IndexTerm generateIndexTerm(final File target, final String text) {
        final IndexTerm primary = new IndexTerm();
        primary.setTermName(text);
        primary.setTermKey(text);
        if (target != null) {
            final IndexTermTarget primaryTarget = new IndexTermTarget();
            primaryTarget.setTargetName("Index test");
            primaryTarget.setTargetURI(target.getAbsolutePath() + "#concept");
            primary.addTarget(primaryTarget);
        }
        return primary;
    }

}