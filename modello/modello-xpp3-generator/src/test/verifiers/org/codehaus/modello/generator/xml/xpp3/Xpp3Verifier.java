package org.codehaus.modello.generator.xml.xpp3;

/*
 * LICENSE
 */

import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.*;
import org.codehaus.modello.generator.*;
import java.io.*;
import java.util.*;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class Xpp3Verifier
    extends AbstractVerifier
{
    public void verify()
        throws Exception
    {
        Model expected = new Model();

        expected.setExtend( "/foo/bar" );

        expected.setModelVersion( "4.0.0" );

        Scm scm = new Scm();

        String connection = "connection";

        String developerConnection = "developerConnection";

        String url = "url";

        scm.setConnection( connection );

        scm.setDeveloperConnection( developerConnection );

        scm.setUrl( url );

        expected.setScm( scm );

        MavenXpp3Writer writer = new MavenXpp3Writer();

        StringWriter string = new StringWriter();

        writer.write( string, expected );

        MavenXpp3Reader reader = new MavenXpp3Reader();

        String xml = string.toString();

        System.err.println( xml );

        Model actual = reader.read( new StringReader( xml ) );

        assertNotNull( "Actual", actual );

        assertModel( expected, actual );
    }

    public void assertModel( Model expected, Model actual )
    {
        assertNotNull( "Actual model", actual );

        assertEquals( "/model/extend", expected.getExtend(), actual.getExtend() );

//        assertParent( expected.getParent(), actual.getParent() );

        assertEquals( "/model/modelVersion", expected.getModelVersion(), actual.getModelVersion() );

        assertEquals( "/model/groupId", expected.getGroupId(), actual.getGroupId() );

        assertEquals( "/model/artifactId", expected.getArtifactId(), actual.getArtifactId() );

        assertEquals( "/model/type", expected.getType(), actual.getType() );

        assertEquals( "/model/name", expected.getName(), actual.getName() );

        assertEquals( "/model/version", expected.getVersion(), actual.getVersion() );

        assertEquals( "/model/shortDescription", expected.getShortDescription(), actual.getShortDescription() );

        assertEquals( "/model/description", expected.getDescription(), actual.getDescription() );

        assertEquals( "/model/url", expected.getUrl(), actual.getUrl() );

        assertEquals( "/model/logo", expected.getLogo(), actual.getLogo() );

//        assertIssueManagement();

//        assertCiManagement();

        assertEquals( "/model/inceptionYear", expected.getInceptionYear(), actual.getInceptionYear() );

        assertEquals( "/model/siteAddress", expected.getSiteAddress(), actual.getSiteAddress() );

        assertEquals( "/model/siteDirectory", expected.getSiteDirectory(), actual.getSiteDirectory() );

        assertEquals( "/model/distributionSite", expected.getDistributionSite(), actual.getDistributionSite() );

        assertEquals( "/model/distributionDirectory", expected.getDistributionDirectory(), actual.getDistributionDirectory() );

        assertMailingLists( expected.getMailingLists(), actual.getMailingLists() );
/*
        assertDevelopers( );

        assertContributors( );

        assertDependencies( );

        assertLicenses( );

        assertPackageGroups( );

        assertReports( );
*/
        assertScm( expected.getScm(), actual.getScm() );
/*
        assertBuild( );

        assertOrganization( expected.getOrganization(), actual.getOrganization() );
*/
    }

    public void assertMailingLists( List expected, List actual )
    {
        assertNotNull( "/model/mailingLists", actual );

        assertEquals( "/model/mailingLists.size", expected.size(), actual.size() );

        for ( int i = 0; i < expected.size(); i++ )
        {
            assertMailingList( i, (MailingList) expected.get( i ), actual.get( i ) );
        }
    }

    public void assertMailingList( int i, MailingList expected, Object actualObject )
    {
        assertNotNull( "/model/mailingLists[" + i + "]", actualObject );

        assertInstanceOf( "/model/mailingLists", MailingList.class, actualObject.getClass() );

        MailingList actual = (MailingList) actualObject;

        assertEquals( "/model/mailingLists[" + i + "]/name", expected.getName(), actual.getName() );

        assertEquals( "/model/mailingLists[" + i + "]/subscribe", expected.getSubscribe(), actual.getSubscribe() );

        assertEquals( "/model/mailingLists[" + i + "]/unsubscribe", expected.getUnsubscribe(), actual.getUnsubscribe() );

        assertEquals( "/model/mailingLists[" + i + "]/archive", expected.getArchive(), actual.getArchive() );
    }

    public void assertScm( Scm expected, Object actualObject )
    {
        assertNotNull( "/model/scm", actualObject );

        assertInstanceOf( "/model/scm", Scm.class, actualObject.getClass() );

        Scm actual = (Scm) actualObject;

        assertEquals( "/model/scm/connection", expected.getConnection(), actual.getConnection() );

        assertEquals( "/model/scm/developerConnection", expected.getDeveloperConnection(), actual.getDeveloperConnection() );

        assertEquals( "/model/scm/url", expected.getUrl(), actual.getUrl() );
    }
}
