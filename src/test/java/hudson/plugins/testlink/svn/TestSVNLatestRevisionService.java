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
package hudson.plugins.testlink.svn;

import java.net.MalformedURLException;

import junit.framework.TestCase;

import org.tmatesoft.svn.core.SVNException;

/**
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 2.0
 */
public class TestSVNLatestRevisionService 
extends TestCase
{
	
	private static final String repoUrl = "https://tap4j.svn.sourceforge.net/svnroot/tap4j/";
	private static final String userName = "";
	private static final String password = "";
	private SVNLatestRevisionService service;
	
	public void setUp()
	{
		try
		{
			this.service = new SVNLatestRevisionService(repoUrl, userName, password);
		} 
		catch (MalformedURLException e)
		{
			fail("Bad SVN Url: [" + repoUrl + "] : " + e.getMessage());
		}
	}
	
	public void testGetLatestSVNRevisionAndSvnUrl()
	{
		assertNotNull( this.service.getRepositoryURL() );
		
		Long revision = Long.valueOf( -1 );
		try
		{
			revision = this.service.getLatestRevision();
		} 
		catch (SVNException e)
		{
			fail("Failed to get latest revision: " + e.getMessage());
		}
		
		assertNotNull( revision );
		
		assertTrue( revision > 0 );
		
	}
	
	public void testGetLatestRevisionWithCredentials()
	{
		try
		{
			this.service = new SVNLatestRevisionService(repoUrl, "fakeUser", "");
		}
		catch (MalformedURLException e)
		{
			fail("Bad SVN Url: [" + repoUrl + "] : " + e.getMessage());
		}
		
		Long revision = Long.valueOf( -1 );
		try
		{
			revision = this.service.getLatestRevision();
		} 
		catch (SVNException e)
		{
			fail("Failed to get latest revision: " + e.getMessage());
		}
		
		assertNotNull( revision );
		
		assertTrue( revision > 0 );
	}
	
	public void testGetLatestRevisionWithoutCredentials()
	{
		try
		{
			this.service = new SVNLatestRevisionService(repoUrl, null, null);
		}
		catch (MalformedURLException e)
		{
			fail("Bad SVN Url: [" + repoUrl + "] : " + e.getMessage());
		}
		
		Long revision = Long.valueOf( -1 );
		try
		{
			revision = this.service.getLatestRevision();
		} 
		catch (SVNException e)
		{
			fail("Failed to get latest revision: " + e.getMessage() );
		}
		
		assertNotNull( revision );
		
		assertTrue( revision > 0 );
	}

}
