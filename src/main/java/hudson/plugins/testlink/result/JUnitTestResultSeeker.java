/* 
 * The MIT License
 * 
 * Copyright (c) 2010 Bruno P. Kinoshita <http://www.kinoshita.eti.br>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.testlink.result;

import hudson.model.BuildListener;
import hudson.plugins.testlink.result.parser.junit.JUnitParser;
import hudson.plugins.testlink.result.parser.junit.TestCase;
import hudson.plugins.testlink.result.parser.junit.TestSuite;
import hudson.plugins.testlink.result.scanner.Scanner;
import hudson.plugins.testlink.util.ParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import br.eti.kinoshita.testlinkjavaapi.model.Attachment;
import br.eti.kinoshita.testlinkjavaapi.model.CustomField;
import br.eti.kinoshita.testlinkjavaapi.model.ExecutionStatus;

/**
 * This class is responsible for scanning directories looking for JUnit Test 
 * results.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 2.1.1
 */
public class JUnitTestResultSeeker 
extends TestResultSeeker
{
	
	private static final long serialVersionUID = -4334626130548695227L;
	
	protected final JUnitParser parser = new JUnitParser();
	
	/**
	 * Constructor.
	 * 
	 * @param report TestLink Report.
	 * @param keyCustomFieldName Name of the Key custom field.
	 * @param listener Hudson Build listener.
	 */
	public JUnitTestResultSeeker(TestLinkReport report,
			String keyCustomFieldName, BuildListener listener)
	{
		super(report, keyCustomFieldName, listener);
	}

	/* (non-Javadoc)
	 * @see hudson.plugins.testlink.result.TestResultSeeker#seek(java.io.File, java.lang.String, hudson.plugins.testlink.result.TestLinkReport, hudson.model.BuildListener)
	 */
	public List<TestResult> seek( File directory, String includePattern ) 
	throws TestResultSeekerException
	{
		
		final List<TestResult> results = new ArrayList<TestResult>();
		
		if ( StringUtils.isBlank(includePattern) ) // skip JUnit
		{
			listener.getLogger().println( "Empty JUnit include pattern. Skipping JUnit test results." );
		}
		else
		{
			try
			{
				final Scanner scanner = new Scanner();
				
				String[] junitReports = scanner.scan(directory, includePattern, listener);
				
				listener.getLogger().println( "Found ["+junitReports.length+"] JUnit reports." );
				
				this.doJunitReports( directory, junitReports, results );
			} 
			catch (IOException e)
			{
				throw new TestResultSeekerException( "IO Error scanning for include pattern ["+includePattern+"]: " + e.getMessage(), e );
			}
			catch( Throwable t ) 
			{
				throw new TestResultSeekerException( "Unkown internal error. Please, open an issue in Jenkins JIRA with the complete stack trace.", t );
			}
		}
		
		return results;
	}

	/**
	 * Parses JUnit report files to look for Test Results of TestLink 
	 * Automated Test Cases.
	 * 
	 * @param directory Directory where to search for.
	 * @param junitReports Array of JUnit report files.
	 * @param testResults List of Test Results.
	 */
	protected void doJunitReports( 
		File directory, 
		String[] junitReports, 
		List<TestResult> testResults)
	{
		
		for ( int i = 0 ; i < junitReports.length ; ++i )
		{
			listener.getLogger().println( "Parsing ["+junitReports[i]+"]." );
			
			File junitFile = new File(directory, junitReports[i]);
			
			try
			{
				final TestSuite junitSuite = parser.parse( junitFile );
				
				this.doJunitSuite( junitSuite, junitFile, testResults );
			}
			catch ( ParserException e )
			{
				listener.getLogger().println( "Failed to parse JUnit report ["+junitFile+"]: " + e.getMessage() );
				e.printStackTrace( listener.getLogger() );
			}
		}
	}

	/**
	 * Inspects a JUnit test suite looking for test results for the automated 
	 * test cases in TestLink. When it finds a test result, this test result 
	 * is added to the List of Test Results.
	 * 
	 * @param junitSuite JUnit test suite.
	 * @param junitFile JUnit file (added as an attachment for each test result 
	 * 				    found).
	 * @param testResults List of Test Results.
	 */
	protected void doJunitSuite( 
		TestSuite junitSuite, 
		File junitFile, 
		List<TestResult> testResults ) 
	{
		listener.getLogger().println( "Inspecting JUnit test suite ["+junitSuite.getName()+"]. This suite contains ["+junitSuite.getTestCases().size()+"] test cases, ["+junitSuite.getFailures()+"] failures and ["+junitSuite.getErrors()+"] errors." );
		
		final List<TestCase> junitTestCases = junitSuite.getTestCases();
		
		for( TestCase junitTestCase : junitTestCases )
		{
			listener.getLogger().println( "Processing JUnit test ["+junitTestCase.getName()+"]." );
			
			final TestResult testResult = this.doFindTestResult( junitTestCase, junitFile );
			
			if ( testResult != null )
			{
				listener.getLogger().println( "Found TestLink Automated Test Case result in JUnit test case ["+junitTestCase.getName()+"]. Status: ["+testResult.getTestCase().getExecutionStatus().toString()+"]." );
				testResults.add( testResult );
			}
			else
			{
				listener.getLogger().println( "Could not find TestLink Automated Test Case result in JUnit test case ["+junitTestCase.getName()+"]." );
			}
		}
	}

	/**
	 * Tries to find the Test Result for a given JUnit test. This method 
	 * utilizes the junitFile to add it as an attachment to the test result.
	 * 
	 * @param junitTestCase JUnit test.
	 * @param junitFile JUnit test file.
	 * @return a Test Result or <code>null</code> if unable to find it.
	 */
	protected TestResult doFindTestResult( TestCase junitTestCase, File junitFile ) 
	{
		final String junitTestCaseClassName = junitTestCase.getClassName();
		
		final List<br.eti.kinoshita.testlinkjavaapi.model.TestCase> testLinkTestCases =
			this.report.getTestCases();
		
		listener.getLogger().println( "Looking for TestLink Automated Test Case custom field ["+keyCustomFieldName+"] with value equals ["+junitTestCaseClassName+"]." );
		
		for ( br.eti.kinoshita.testlinkjavaapi.model.TestCase testLinkTestCase : testLinkTestCases )
		{
			final List<CustomField> customFields = testLinkTestCase.getCustomFields();
			
			for( CustomField customField : customFields )
			{
				final String customFieldValue = customField.getValue();
				Boolean isKeyCustomField = customField.getName().equals(keyCustomFieldName);
				
				if ( isKeyCustomField && junitTestCaseClassName.equals( customFieldValue ) )
				{
					ExecutionStatus status = this.getJUnitExecutionStatus( junitTestCase );
					testLinkTestCase.setExecutionStatus( status );
					TestResult testResult = new TestResult(testLinkTestCase, report.getBuild(), report.getTestPlan());
					
					String notes = this.getJUnitNotes( junitTestCase );
					
					try
					{
						Attachment junitAttachment = this.getJUnitAttachment( testResult.getTestCase().getVersionId(), junitFile );
						testResult.addAttachment( junitAttachment );
					}
					catch ( IOException ioe )
					{
						notes += "\n\nFailed to add JUnit attachment to this test case execution. Error message: " + ioe.getMessage();
						ioe.printStackTrace( listener.getLogger() );
					}
					
					testResult.setNotes( notes );
					return testResult;
				} // endif
			} //end for custom fields
		} // end for testlink test cases
		
		return null;
	}

	/**
	 * Retrieves the Execution Status of the JUnit test.
	 * 
	 * @param testCase JUnit test.
	 * @return the Execution Status of the JUnit test.
	 */
	protected ExecutionStatus getJUnitExecutionStatus( TestCase testCase )
	{
		ExecutionStatus status = ExecutionStatus.FAILED;
		if ( (testCase.getFailures().size() + testCase.getErrors().size()) <= 0 )
		{
			status = ExecutionStatus.PASSED;
		}
		return status;
	}
	
	/**
	 * Retrieves the Notes about the JUnit test.
	 * 
	 * @param testCase JUnit test.
	 * @return Notes about the JUnit test.
	 */
	protected String getJUnitNotes( TestCase testCase )
	{
		StringBuilder notes = new StringBuilder();
		
		notes.append( "name: " );
		notes.append( testCase.getName()+ "\n" );
		
		notes.append( "classname: " );
		notes.append( testCase.getClassName() + "\n" );
		
		notes.append( "errors: " );
		notes.append( testCase.getErrors().size() + "\n" );
		
		notes.append( "failures: " );
		notes.append( testCase.getFailures().size() + "\n" );
		
		notes.append( "time: " );
		notes.append( testCase.getTime()+ "\n" );
		
		return notes.toString();
	}
	
	/**
	 * Retrieves the JUnit report file as attachment for TestLink.
	 * 
	 * @param versionId TestLink Test Case version ID.
	 * @param junitReportFile JUnit report file.
	 * @return attachment for TestLink.
	 */
	protected Attachment getJUnitAttachment( Integer versionId,
			File junitReportFile ) 
	throws IOException
	{
		Attachment attachment = new Attachment();
		
		String fileContent = this.getBase64FileContent(junitReportFile );
		attachment.setContent( fileContent );
		attachment.setDescription( "JUnit report file " + junitReportFile.getName() );
		attachment.setFileName( junitReportFile.getName() );
		attachment.setFileSize( junitReportFile.length() );
		attachment.setTitle( junitReportFile.getName() );
		attachment.setFileType("text/xml");
		
		return attachment;
	}
	
}
