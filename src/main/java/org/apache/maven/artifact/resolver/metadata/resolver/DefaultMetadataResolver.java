package org.apache.maven.artifact.resolver.metadata.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.resolver.metadata.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.metadata.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.metadata.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.metadata.Artifact;
import org.apache.maven.artifact.resolver.metadata.ArtifactRepository;
import org.apache.maven.artifact.resolver.metadata.DefaultArtifact;
import org.apache.maven.artifact.resolver.metadata.conflict.ConflictResolver;
import org.apache.maven.artifact.resolver.metadata.transform.ClasspathTransformation;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/*
 * default implementation of the metadata resolver
 * 
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 * 
 * @plexus.component
 */
public class DefaultMetadataResolver
    extends AbstractLogEnabled
    implements MetadataResolver
{
    //------------------------------------------------------------------------

    /** @plexus.requirement */
    ArtifactResolver artifactResolver;

    /** @plexus.requirement */
    MetadataSource metadataSource;

    /** @plexus.requirement */
    ConflictResolver conflictResolver;

    /** @plexus.requirement */
    ClasspathTransformation classpathTransformation;

    //------------------------------------------------------------------------
    public MetadataResolutionResult resolveMetadata( MetadataResolutionRequest req )
        throws MetadataResolutionException
    {
        try
        {
            getLogger().debug( "Received request for: " + req.getQuery() );

            MetadataResolutionResult res = new MetadataResolutionResult();

            MetadataTreeNode tree = resolveMetadataTree( req.getQuery(), null, req.getLocalRepository(), req.getRemoteRepositories() );

            res.setTree( tree );
            return res;
        }
        catch ( MetadataResolutionException mrEx )
        {
            throw mrEx;
        }
        catch ( Exception anyEx )
        {
            throw new MetadataResolutionException( anyEx );
        }
    }

    //------------------------------------------------------------------------
    private MetadataTreeNode resolveMetadataTree( ArtifactMetadata query, MetadataTreeNode parent, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws MetadataResolutionException
    {
        try
        {
            Artifact pomArtifact = new DefaultArtifact( query.getGroupId(), query.getArtifactId(), query.getVersion(), query.getType() == null ? "jar" : query.getType(), null, false,
                                                        query.getScope(), null );

            getLogger().debug( "resolveMetadata request:" + "\n> artifact   : " + pomArtifact.toString() + "\n> remoteRepos: " + remoteRepositories + "\n> localRepo  : " + localRepository );

            String error = null;

            ArtifactResolutionRequest arr = new ArtifactResolutionRequest();
            arr.setArtifact( pomArtifact );
            arr.setLocalRepository( localRepository );
            arr.setRemoteRepostories( remoteRepositories );

            ResolutionRequest request = new ResolutionRequest().setArtifact( pomArtifact ).setLocalRepository( localRepository ).setRemoteRepostories( remoteRepositories );

            ResolutionResult result = artifactResolver.resolve( request );

            // Here we just need to deal with basic retrieval problems.
            if ( result.hasMetadataResolutionExceptions() )
            {
                pomArtifact.setResolved( false );
            }

            if ( !pomArtifact.isResolved() )
            {
                getLogger().info(
                                  "*************> Did not resolve " + pomArtifact.toString() + "\nURL: " + pomArtifact.getDownloadUrl() + "\nRepos: " + remoteRepositories + "\nLocal: "
                                      + localRepository );
            }

            if ( error != null )
            {
                getLogger().info( "*************> Did not resolve " + pomArtifact.toString() + "\nRepos: " + remoteRepositories + "\nLocal: " + localRepository + "\nerror: " + error );
            }

            if ( pomArtifact.isResolved() )
            {
                MetadataResolution metadataResolution = metadataSource.retrieve( query, localRepository, remoteRepositories );
                ArtifactMetadata found = metadataResolution.getArtifactMetadata();

                if ( pomArtifact.getFile() != null && pomArtifact.getFile().toURI() != null )
                    found.setArtifactUri( pomArtifact.getFile().toURI().toString() );

                MetadataTreeNode node = new MetadataTreeNode( found, parent, true, found.getScopeAsEnum() );
                Collection<ArtifactMetadata> dependencies = metadataResolution.getArtifactMetadata().getDependencies();

                if ( dependencies != null && dependencies.size() > 0 )
                {
                    int nKids = dependencies.size();
                    node.setNChildren( nKids );
                    int kidNo = 0;
                    for ( ArtifactMetadata a : dependencies )
                    {
                        MetadataTreeNode kidNode = resolveMetadataTree( a, node, localRepository, remoteRepositories );
                        node.addChild( kidNo++, kidNode );
                    }
                }
                return node;
            }
            else
            {
                return new MetadataTreeNode( pomArtifact, parent, false, query.getArtifactScope() );
            }
        }
        catch ( Exception anyEx )
        {
            throw new MetadataResolutionException( anyEx );
        }
    }

    //------------------------------------------------------------------------
    public List<Artifact> resolveArtifact( List<ArtifactMetadata> mdCollection, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactResolutionException
    {
        if ( mdCollection == null || mdCollection.isEmpty() )
            return null;

        ArrayList<Artifact> res = new ArrayList<Artifact>( mdCollection.size() );
        Artifact artifact = null;

        // TODO: optimize retrieval by zipping returns from repo managers (nexus)
        for ( ArtifactMetadata md : mdCollection )
        {
            artifact = new DefaultArtifact( md.getGroupId(), md.getArtifactId(), md.getVersion(), md.getType() == null ? "jar" : md.getType(), null, false, md.getScope(), null );

            ResolutionRequest request = new ResolutionRequest().setArtifact( artifact ).setLocalRepository( localRepository ).setRemoteRepostories( remoteRepositories );

            ResolutionResult result = artifactResolver.resolve( request );

            res.add( artifact );
        }
        return res;
    }
}
